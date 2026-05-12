package com.aoa.aix.infrastructure.config;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Configuration
public class OpenTelemetryConfig {
	
	@Value("${otel.service.name:aoa-aix-logs-agent}")     private String serviceName;
	@Value("${otel.exporter.otlp.endpoint:http://localhost:4317}") private String endpoint;
	@Value("${otel.exporter.otlp.protocol:grpc}")         private String protocol;
	@Value("${otel.exporter.otlp.timeout-ms:10000}")      private long   timeoutMs;
	@Value("${otel.exporter.otlp.compression:gzip}")      private String compression;
    @Value("${spring.profiles.active:dev}") private String env;
    @Value("${otel.exporter.otlp.headers:}")     private String headers;

    @Value("${otel.retry.enabled:true}")           private boolean retryEnabled;
    @Value("${otel.retry.max-attempts:5}")         private int     retryMaxAttempts;
    @Value("${otel.retry.initial-backoff-ms:1000}")private long    initialBackoffMs;
    @Value("${otel.retry.max-backoff-ms:30000}")   private long    maxBackoffMs;
    @Value("${otel.retry.backoff-multiplier:2.0}") private double  backoffMultiplier;

    @Value("${otel.buffer.max-queue-size:10000}")        private int  maxQueueSize;
    @Value("${otel.buffer.schedule-delay-ms:5000}")      private long scheduleDelayMs;
    @Value("${otel.buffer.export-timeout-ms:30000}")     private long exportTimeoutMs;
    @Value("${otel.buffer.max-export-batch-size:512}")   private int  maxExportBatchSize;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault().merge(
                Resource.create(Attributes.builder()
                        .put(AttributeKey.stringKey("service.name"),        serviceName)
                        //.put(AttributeKey.stringKey("deployment.environment"), env)
                        .put(AttributeKey.stringKey("aix.agent.version"),  "0.0.1")
                        .build()));

        // ── Política de reintentos con backoff exponencial ──
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .setMaxAttempts(retryEnabled ? retryMaxAttempts : 1)
                .setInitialBackoff(Duration.ofMillis(initialBackoffMs))
                .setMaxBackoff(Duration.ofMillis(maxBackoffMs))
                .setBackoffMultiplier(backoffMultiplier)
                .build();

        // ── Exporters OTLP (logs + métricas) ──
        LogRecordExporter logExporter = buildLogExporter(retryPolicy);
        MetricExporter   metricExporter = buildMetricExporter(retryPolicy);

        // ── Buffer/Batch en memoria: si OTLP falla, los datos esperan en cola ──
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(
                        BatchLogRecordProcessor.builder(logExporter)
                                .setMaxQueueSize(maxQueueSize)
                                .setMaxExportBatchSize(maxExportBatchSize)
                                .setScheduleDelay(Duration.ofMillis(scheduleDelayMs))
                                .setExporterTimeout(Duration.ofMillis(exportTimeoutMs))
                                .build())
                .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(metricExporter)
                                .setInterval(Duration.ofMillis(scheduleDelayMs))
                                .build())
                .build();

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setLoggerProvider(loggerProvider)
                .setMeterProvider(meterProvider)
                .build();

        // Cierre ordenado al apagar la JVM (flush del buffer)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown OTel SDK: vaciando buffers…");
            sdk.getSdkLoggerProvider().shutdown().join(10, java.util.concurrent.TimeUnit.SECONDS);
            sdk.getSdkMeterProvider().shutdown().join(10, java.util.concurrent.TimeUnit.SECONDS);
        }));

        log.info("OTel inicializado | endpoint={} protocol={} env={} retry={}x",
                endpoint, protocol, env, retryMaxAttempts);

        return sdk;
    }

    // ───────────────────────── Helpers ─────────────────────────

    private LogRecordExporter buildLogExporter(RetryPolicy retry) {
        if ("http/protobuf".equalsIgnoreCase(protocol)) {
            var b = OtlpHttpLogRecordExporter.builder()
                    .setEndpoint(endpoint)
                    .setTimeout(Duration.ofMillis(timeoutMs))
                    .setRetryPolicy(retry);
            applyCompression(b::setCompression);
            applyHeaders(b::addHeader);
            return b.build();
        }
        var b = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofMillis(timeoutMs))
                .setRetryPolicy(retry);
        applyCompression(b::setCompression);
        applyHeaders(b::addHeader);
        return b.build();
    }

    private MetricExporter buildMetricExporter(RetryPolicy retry) {
        if ("http/protobuf".equalsIgnoreCase(protocol)) {
            var b = OtlpHttpMetricExporter.builder()
                    .setEndpoint(endpoint)
                    .setTimeout(Duration.ofMillis(timeoutMs))
                    .setRetryPolicy(retry);
            applyCompression(b::setCompression);
            applyHeaders(b::addHeader);
            return b.build();
        }
        var b = OtlpGrpcMetricExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(Duration.ofMillis(timeoutMs))
                .setRetryPolicy(retry);
        applyCompression(b::setCompression);
        applyHeaders(b::addHeader);
        return b.build();
    }

    private void applyCompression(java.util.function.Consumer<String> setter) {
        if (compression != null && !compression.isBlank() && !"none".equalsIgnoreCase(compression)) {
            setter.accept(compression);
        }
    }

    private void applyHeaders(java.util.function.BiConsumer<String, String> adder) {
        if (headers == null || headers.isBlank()) return;
        Arrays.stream(headers.split(","))
                .map(String::trim).filter(s -> s.contains("="))
                .forEach(h -> {
                    String[] kv = h.split("=", 2);
                    adder.accept(kv[0].trim(), kv[1].trim());
                });
    }
}