package com.aoa.aix.application.service;

import com.aoa.aix.application.port.in.IngestLparstatUseCase;
import com.aoa.aix.application.port.out.LparstatCommandExecutor;
import com.aoa.aix.application.port.out.OtlpMetricPublisher;
import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.domain.service.MemoryAlertEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class LparstatIngestService implements IngestLparstatUseCase {

    private final LparstatCommandExecutor executor;
    private final MemoryAlertEvaluator evaluator;
    private final OtlpMetricPublisher publisher;

    @Override
    public void collectAndIngest() {
        List<LparstatSnapshot> snapshots = executor.executeAll();
        if (snapshots == null || snapshots.isEmpty()) {
            log.warn("lparstat returned no snapshots");
            return;
        }
        for (LparstatSnapshot snap : snapshots) {
            try {
                publisher.publishSnapshot(snap);
                evaluator.evaluate(snap).forEach(publisher::publishAlert);
            } catch (Exception e) {
                log.warn("[{}] Error publicando snapshot: {}", snap.getLparName(), e.getMessage());
            }
        }
    }
}