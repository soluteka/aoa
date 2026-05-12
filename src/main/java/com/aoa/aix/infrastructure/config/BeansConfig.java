package com.aoa.aix.infrastructure.config;

import com.aoa.aix.application.port.in.IngestErrptUseCase;
import com.aoa.aix.application.port.in.IngestLparstatUseCase;
import com.aoa.aix.application.port.out.LparstatCommandExecutor;
import com.aoa.aix.application.port.out.OtlpLogPublisher;
import com.aoa.aix.application.port.out.OtlpMetricPublisher;
import com.aoa.aix.application.service.ErrptIngestService;
import com.aoa.aix.application.service.LparstatIngestService;
import com.aoa.aix.domain.service.AixErrorClassifier;
import com.aoa.aix.domain.service.MemoryAlertEvaluator;
import com.aoa.aix.infrastructure.outbound.parser.ErrptParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfig {

    @Bean public AixErrorClassifier aixErrorClassifier() { return new AixErrorClassifier(); }
    @Bean public MemoryAlertEvaluator memoryAlertEvaluator() { return new MemoryAlertEvaluator(); }

    @Bean
    public IngestErrptUseCase ingestErrptUseCase(ErrptParser parser,
                                                 AixErrorClassifier classifier,
                                                 OtlpLogPublisher publisher) {
        return new ErrptIngestService(parser, classifier, publisher);
    }

    @Bean
    public IngestLparstatUseCase ingestLparstatUseCase(LparstatCommandExecutor executor,
                                                      MemoryAlertEvaluator evaluator,
                                                      OtlpMetricPublisher publisher) {
        return new LparstatIngestService(executor, evaluator, publisher);
    }
}