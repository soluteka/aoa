package com.aoa.aix.infrastructure.outbound.parser;

import com.aoa.aix.domain.model.ErrptEvent;
import com.aoa.aix.domain.model.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ErrptParser {

    // <PRI>MMM dd HH:mm:ss host errpt: <body>
    private static final Pattern SYSLOG_PREFIX =
            Pattern.compile("^<\\d+>.*?\\s(\\S+)\\s+errpt:\\s*(.+)$");

    // ID  TIMESTAMP  CLASS  TYPE  RESOURCE  DESCRIPTION...
    private static final Pattern ERRPT_BODY =
            Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S)\\s+(\\S)\\s+(\\S+)\\s+(.+)$");

    public ErrptEvent parse(String payload, String sourceHost) {
        if (payload == null || payload.isBlank()) {
            return ErrptEvent.builder()
                    .timestamp(Instant.now())
                    .host(sourceHost)
                    .identifier("UNKNOWN")
                    .resourceName("UNKNOWN")
                    .description("")
                    .severity(Severity.INFO)
                    .build();
        }

        String host = sourceHost;
        String body = payload.trim();

        Matcher sm = SYSLOG_PREFIX.matcher(body);
        if (sm.matches()) {
            host = sm.group(1);
            body = sm.group(2).trim();
        }

        Matcher bm = ERRPT_BODY.matcher(body);
        if (!bm.matches()) {
            log.warn("Cannot parse errpt body: {}", body);
            return ErrptEvent.builder()
                    .timestamp(Instant.now())
                    .host(host)
                    .identifier("UNKNOWN")
                    .resourceName("UNKNOWN")
                    .description(body)
                    .severity(Severity.INFO)
                    .build();
        }

        return ErrptEvent.builder()
                .timestamp(Instant.now())
                .host(host)
                .identifier(bm.group(1))
                .errorClass(bm.group(3))
                .errorType(bm.group(4))
                .resourceName(bm.group(5))
                .description(bm.group(6).trim())
                .severity(Severity.INFO) // refinada por el classifier
                .build();
    }
}