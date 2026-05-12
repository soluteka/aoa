package com.aoa.aix.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MemoryAlert {
    String host;
    String reason;
    String metric;     // ej: paging.used_pct
    double value;
    double threshold;
    Severity severity;
}