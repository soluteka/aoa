package com.aoa.aix.infrastructure.inbound.schedule;

import com.aoa.aix.application.port.in.IngestLparstatUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LparstatScheduler {

    private final IngestLparstatUseCase ingestLparstat;

    @Scheduled(fixedRateString = "${aix.lparstat.interval-ms:30000}")
    public void poll() {
        log.debug("Triggering lparstat collection");
        ingestLparstat.collectAndIngest();
    }
}