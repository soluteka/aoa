package com.aoa.aix.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;

@Value
@Builder
public class LparstatSnapshot {
    Instant timestamp;
    String host;
    String lparName;
    String environment;

    // CPU
    double cpuUser;
    double cpuSys;
    double cpuIdle;
    double cpuWait;

    // LPAR
    double entitledCapacity;
    double physicalCpuUsed;   // physc
    double entcPct;
    double lbusy;
    double app;

    // Memory / paging
    double pagingSpaceUsedPct;
    double pagingRate;
}