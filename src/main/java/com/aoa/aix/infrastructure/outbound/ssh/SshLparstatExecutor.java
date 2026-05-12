package com.aoa.aix.infrastructure.outbound.ssh;

import com.aoa.aix.application.port.out.LparstatCommandExecutor;
import com.aoa.aix.application.port.out.LparstatExecutionException;
import com.aoa.aix.domain.model.LparTarget;
import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.infrastructure.config.AixProperties;
import com.aoa.aix.infrastructure.outbound.parser.LparstatXmlParser;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Profile("ssh")    // se usa con perfil ssh
@RequiredArgsConstructor
public class SshLparstatExecutor implements LparstatCommandExecutor {

    private final AixProperties props;
    private final LparstatXmlParser parser;

    private ExecutorService pool;

    @PostConstruct
    void init() {
        int n = props.getLpars() == null ? 0 : props.getLpars().size();
        int maxParallel = props.getSampler() != null ? props.getSampler().getMaxParallel() : 4;
        int poolSize = Math.max(1, Math.min(Math.max(n, 1), maxParallel));
        this.pool = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "ssh-lparstat");
            t.setDaemon(true);
            return t;
        });
        log.info("SshLparstatExecutor listo | lpars={} pool={}", n, poolSize);
    }

    @PreDestroy
    void shutdown() {
        if (pool != null) pool.shutdownNow();
    }

    @Override
    public List<LparstatSnapshot> executeAll() {
        List<LparTarget> targets = props.getLpars();
        if (targets == null || targets.isEmpty()) {
            log.warn("aix.lpars[] vacio; no hay LPARs para muestrear.");
            return List.of();
        }

        long perTaskTimeoutMs = props.getSsh().getReadTimeoutMs() + 5_000L;

        List<Future<LparstatSnapshot>> futures = new ArrayList<>(targets.size());
        for (LparTarget t : targets) {
            futures.add(pool.submit(() -> sampleOne(t)));
        }

        List<LparstatSnapshot> results = new ArrayList<>(targets.size());
        for (int i = 0; i < futures.size(); i++) {
            LparTarget t = targets.get(i);
            try {
                LparstatSnapshot s = futures.get(i).get(perTaskTimeoutMs, TimeUnit.MILLISECONDS);
                if (s != null) results.add(s);
            } catch (TimeoutException te) {
                log.warn("[{}] Timeout esperando muestreo", t.getName());
                futures.get(i).cancel(true);
            } catch (ExecutionException ee) {
                log.warn("[{}] Error en muestreo: {}", t.getName(),
                        ee.getCause() == null ? ee.getMessage() : ee.getCause().getMessage());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return results;
    }

    private LparstatSnapshot sampleOne(LparTarget target) {
        try {
            String xml = fetchXmlOverSsh(target);
            return parser.parse(xml, target.getHost(), target.getName(), target.getEnvironment());
        } catch (LparstatExecutionException e) {
            log.warn("[{}] SSH fallo: {}", target.getName(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[{}] Error inesperado: {}", target.getName(), e.toString(), e);
            return null;
        }
    }

    private String fetchXmlOverSsh(LparTarget target) throws LparstatExecutionException {
        AixProperties.Ssh sshCfg = props.getSsh();
        try (SSHClient ssh = new SSHClient()) {
            if (sshCfg.isSkipHostKeyVerification()) {
                ssh.addHostKeyVerifier(new PromiscuousVerifier());
            } else if (sshCfg.getKnownHostsPath() != null) {
                ssh.loadKnownHosts(new File(sshCfg.getKnownHostsPath()));
            } else {
                ssh.loadKnownHosts();
            }
            ssh.setConnectTimeout(sshCfg.getConnectTimeoutMs());
            ssh.setTimeout(sshCfg.getReadTimeoutMs());

            ssh.connect(target.getHost(), sshCfg.getPort());
            KeyProvider key = ssh.loadKeys(target.getSshKey());
            ssh.authPublickey(target.getSshUser(), key);

            try (Session session = ssh.startSession()) {
                Session.Command cmd = session.exec(sshCfg.getCommand());
                String stdout = readFully(cmd.getInputStream());
                String stderr = readFully(cmd.getErrorStream());
                cmd.join(sshCfg.getReadTimeoutMs(), TimeUnit.MILLISECONDS);
                int exit = cmd.getExitStatus() == null ? -1 : cmd.getExitStatus();
                if (exit != 0) {
                    throw new LparstatExecutionException(
                            String.format("[%s] lparstat exit=%d stderr=%s",
                                    target.getName(), exit, stderr.trim()));
                }
                return stdout;
            }
        } catch (IOException e) {
            throw new LparstatExecutionException(
                    String.format("[%s] Error SSH a %s: %s",
                            target.getName(), target.getHost(), e.getMessage()), e);
        }
    }

    private String readFully(InputStream in) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}