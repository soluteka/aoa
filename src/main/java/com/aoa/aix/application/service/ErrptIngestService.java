package com.aoa.aix.application.service;

import com.aoa.aix.application.port.in.IngestErrptUseCase;
import com.aoa.aix.application.port.out.OtlpLogPublisher;
import com.aoa.aix.domain.model.ErrptEvent;
import com.aoa.aix.domain.model.RawAixEvent;
import com.aoa.aix.domain.model.Severity;
import com.aoa.aix.domain.service.AixErrorClassifier;
import com.aoa.aix.infrastructure.outbound.parser.ErrptParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ErrptIngestService implements IngestErrptUseCase {

    private final ErrptParser parser;
    private final AixErrorClassifier classifier;
    private final OtlpLogPublisher publisher;

    @Override
    public void ingest(String rawSyslogPayload, String sourceHost) {
        ErrptEvent event = parser.parse(rawSyslogPayload, sourceHost);
        if (event == null) return;

        Severity sev = classifier.classify(event);

        Map<String, String> attrs = new HashMap<>();
        attrs.put("errpt.id",       safe(event.getIdentifier()));
        attrs.put("errpt.class",    safe(event.getErrorClass()));
        attrs.put("errpt.type",     safe(event.getErrorType()));
        attrs.put("errpt.resource", safe(event.getResourceName()));

        RawAixEvent raw = RawAixEvent.builder()
            .timestamp(event.getTimestamp())
            .host(event.getHost())
            .source("errpt")
            .message(event.getDescription())
            .severity(sev)
            .attributes(attrs)
            .build();

        log.debug("Publishing errpt event: {}", raw);
        publisher.publish(raw);
    }

    private String safe(String s) { return s == null ? "" : s; }
}