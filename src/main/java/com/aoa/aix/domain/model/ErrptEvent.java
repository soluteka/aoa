package com.aoa.aix.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ErrptEvent {
    private Instant timestamp;
    private String host;
    private String identifier;     // ej: BFE4C025
    private String errorClass;     // P/T/I/U  (Permanent / Temp / Info / Unknown)
    private String errorType;      // H/S/O/U  (Hardware / Software / Operator / Undet.)
    private String resourceName;   // ej: ent0, hdisk0, mem0
    private String description;    // ej: ETHERNET DOWN
    private Severity severity;
}