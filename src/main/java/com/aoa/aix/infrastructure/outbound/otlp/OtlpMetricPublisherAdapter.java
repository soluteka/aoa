package com.aoa.aix.infrastructure.outbound.otlp;

import com.aoa.aix.application.port.out.OtlpMetricPublisher;
import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.domain.model.MemoryAlert;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGauge;
import io.opentelemetry.api.metrics.Meter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OtlpMetricPublisherAdapter implements OtlpMetricPublisher {

    private final Meter meter;

    private DoubleGauge cpuUser, cpuSys, cpuIdle, cpuWait;
    private DoubleGauge pagingPct, pagingRate;
    private DoubleGauge lparPhysc, lparEntcPct, lparLbusy, lparApp, lparEntitled;
    private DoubleGauge alertGauge;

    public OtlpMetricPublisherAdapter(OpenTelemetry openTelemetry) {
        this.meter = openTelemetry.getMeter("aoa.aix.agent");
    }

    @PostConstruct
    void init() {
        cpuUser = meter.gaugeBuilder("aix.cpu.user").setUnit("%").build();
        cpuSys  = meter.gaugeBuilder("aix.cpu.sys").setUnit("%").build();
        cpuIdle = meter.gaugeBuilder("aix.cpu.idle").setUnit("%").build();
        cpuWait = meter.gaugeBuilder("aix.cpu.wait").setUnit("%").build();

        pagingPct  = meter.gaugeBuilder("aix.memory.paging.used_pct").setUnit("%").build();
        pagingRate = meter.gaugeBuilder("aix.memory.paging.rate").setUnit("pages/s").build();

        lparPhysc    = meter.gaugeBuilder("aix.lpar.physc").build();
        lparEntcPct  = meter.gaugeBuilder("aix.lpar.entc_pct").setUnit("%").build();
        lparLbusy    = meter.gaugeBuilder("aix.lpar.lbusy").setUnit("%").build();
        lparApp      = meter.gaugeBuilder("aix.lpar.app").build();
        lparEntitled = meter.gaugeBuilder("aix.lpar.entitled_capacity").build();

        alertGauge = meter.gaugeBuilder("aix.alert").build();
    }

    @Override
    public void publishSnapshot(LparstatSnapshot s) {
        Attributes attrs = Attributes.builder()
                .put("host.name", s.getHost())
                .put("lpar.name", s.getLparName())
                .put("deployment.environment", s.getEnvironment())
                .build();

        cpuUser.set(s.getCpuUser(), attrs);
        cpuSys.set(s.getCpuSys(), attrs);
        cpuIdle.set(s.getCpuIdle(), attrs);
        cpuWait.set(s.getCpuWait(), attrs);

        pagingPct.set(s.getPagingSpaceUsedPct(), attrs);
        pagingRate.set(s.getPagingRate(), attrs);

        lparPhysc.set(s.getPhysicalCpuUsed(), attrs);
        lparEntcPct.set(s.getEntcPct(), attrs);
        lparLbusy.set(s.getLbusy(), attrs);
        lparApp.set(s.getApp(), attrs);
        lparEntitled.set(s.getEntitledCapacity(), attrs);
    }

    @Override
    public void publishAlert(MemoryAlert alert) {
        Attributes attrs = Attributes.builder()
                .put("host.name",       alert.getHost())
                .put("alert.reason",    alert.getReason())
                .put("alert.metric",    alert.getMetric())
                .put("alert.severity",  alert.getSeverity().name())
                .put("alert.threshold", alert.getThreshold())
                .build();
        alertGauge.set(alert.getValue(), attrs);
        log.info("🚨 Alert emitted: {} {}={} (>={})",
                alert.getSeverity(), alert.getMetric(), alert.getValue(), alert.getThreshold());
    }
}