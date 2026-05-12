package com.aoa.aix.infrastructure.inbound.syslog;

import com.aoa.aix.application.port.in.IngestErrptUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyslogMessageHandler implements MessageHandler {

    private final IngestErrptUseCase ingestErrpt;

    @Override
    public void handleMessage(Message<?> message) {
        Object payload = message.getPayload();
        String text;

        if (payload instanceof Map<?, ?> map && map.containsKey("UNDECODED")) {
            text = map.get("UNDECODED").toString();
        } else if (payload instanceof byte[] bytes) {
            text = new String(bytes);
        } else {
            text = String.valueOf(payload);
        }

        String host = String.valueOf(message.getHeaders().getOrDefault("ip_address", "unknown"));
        log.info("Syslog received from {}: {}", host, text);

        try {
            ingestErrpt.ingest(text, host);
        } catch (Exception ex) {
            log.error("Failed ingesting errpt event: {}", ex.getMessage(), ex);
        }
    }
}