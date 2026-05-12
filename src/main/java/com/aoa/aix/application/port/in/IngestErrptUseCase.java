package com.aoa.aix.application.port.in;

public interface IngestErrptUseCase {
    void ingest(String rawSyslogPayload, String sourceHost);
}