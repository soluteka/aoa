package com.aoa.aix.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class RawAixEvent {
    Instant timestamp;
    String host;
    String source;
    String message;
    Severity severity;
    Map<String, String> attributes;
}