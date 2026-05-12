package com.aoa.aix.application.port.out;

import com.aoa.aix.domain.model.RawAixEvent;

public interface OtlpLogPublisher {
    void publish(RawAixEvent event);
}