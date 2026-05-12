package com.aoa.aix.infrastructure.outbound.otlp;

import com.aoa.aix.application.port.out.OtlpLogPublisher;
import com.aoa.aix.domain.model.RawAixEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OtlpLogPublisherAdapter implements OtlpLogPublisher {

    private final Logger otelLogger;

    public OtlpLogPublisherAdapter(OpenTelemetry openTelemetry) {
        this.otelLogger = openTelemetry.getLogsBridge().get("aoa.aix.agent");
    }

    @Override
    public void publish(RawAixEvent event) {
        var builder = otelLogger.logRecordBuilder()
            .setSeverity(mapSeverity(event.getSeverity()))
			.setSeverityText(event.getSeverity().name())   // ← añade esta línea
            .setBody(event.getMessage())
            .setAttribute(AttributeKey.stringKey("host.name"), event.getHost())
            .setAttribute(AttributeKey.stringKey("aix.source"), event.getSource());

        event.getAttributes().forEach((k, v) ->
            builder.setAttribute(AttributeKey.stringKey(k), v));

        builder.emit();
    }

    private Severity mapSeverity(com.aoa.aix.domain.model.Severity s) {
        return switch (s) {
            case CRITICAL -> Severity.ERROR;
            case WARN -> Severity.WARN;
            default -> Severity.INFO;
        };
    }
}