package com.aoa.aix.infrastructure.outbound.command;

import com.aoa.aix.application.port.out.LparstatCommandExecutor;
import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.infrastructure.outbound.parser.LparstatXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Executor de DESARROLLO: lee un XML de muestra desde classpath, file: o ruta absoluta.
 * Útil para correr sin AIX real (Windows/Mac dev box, CI, demos).
 */
@Slf4j
@Component
@Profile("!ssh")   // se usa cuando NO está activo 'ssh' (modo file/simulador)
@RequiredArgsConstructor
public class FileLparstatExecutor implements LparstatCommandExecutor {

    @Value("${aix.lparstat.sample-file:classpath:samples/lparstat-sample.xml}")
    private String sampleFile;

    private final LparstatXmlParser parser;
    private final ResourceLoader resourceLoader;

    @Override
    public List<LparstatSnapshot> executeAll() {
        try {
            Resource res = resourceLoader.getResource(sampleFile);
            if (!res.exists()) {
                log.warn("Sample file no encontrado: {}", sampleFile);
                return List.of();
            }
            String xml;
            try (InputStream in = res.getInputStream()) {
                xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String host = InetAddress.getLocalHost().getHostName();
            LparstatSnapshot snapshot = parser.parse(xml, host, host, "dev");
            if (snapshot == null) {
                log.warn("Parser devolvió null para sample {}", sampleFile);
                return List.of();
            }
            log.debug("Sample procesado OK: cpuUser={} memPaging={}",
                    snapshot.getCpuUser(), snapshot.getPagingSpaceUsedPct());
            return List.of(snapshot);
        } catch (Exception e) {
            log.error("Error leyendo sample file {}: {}", sampleFile, e.getMessage(), e);
            return List.of();
        }
    }
}