package com.aoa.aix.application.port.out;

import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.domain.model.MemoryAlert;

public interface OtlpMetricPublisher {
    void publishSnapshot(LparstatSnapshot snapshot);
    void publishAlert(MemoryAlert alert);
}