package com.aoa.aix.infrastructure.outbound.command;

import com.aoa.aix.application.port.out.LparstatCommandExecutor;
import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.infrastructure.outbound.parser.LparstatXmlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Executor LOCAL: ejecuta `lparstat -X` en la máquina donde corre el agente.
 * Sólo aplica cuando el agente vive DENTRO de una LPAR AIX.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class ShellLparstatExecutor implements LparstatCommandExecutor {

    @Value("${aix.lparstat.command:lparstat -X}")
    private String command;

    private final LparstatXmlParser parser;

    @Override
    public List<LparstatSnapshot> executeAll() {
        try {
            Process p = new ProcessBuilder(command.split("\\s+"))
                    .redirectErrorStream(true)
                    .start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            int exit = p.waitFor();
            if (exit != 0) {
                log.warn("lparstat exit={} output={}", exit, sb);
                return List.of();
            }

            String host = InetAddress.getLocalHost().getHostName();
            LparstatSnapshot snapshot = parser.parse(sb.toString(), host, host, "local");
            return snapshot == null ? List.of() : List.of(snapshot);
        } catch (Exception e) {
            log.error("Error ejecutando '{}': {}", command, e.getMessage());
            return List.of();
        }
    }
}