package com.aoa.aix.domain.service;

import com.aoa.aix.domain.model.LparstatSnapshot;
import com.aoa.aix.domain.model.MemoryAlert;
import com.aoa.aix.domain.model.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoryAlertEvaluator {

    @Value("${aix.memory.paging-pct.warn:70.0}")
    private double pagingPctWarn;

    @Value("${aix.memory.paging-pct.critical:85.0}")
    private double pagingPctCritical;

    @Value("${aix.memory.paging-rate.warn:5.0}")
    private double pagingRateWarn;

    @Value("${aix.memory.paging-rate.critical:10.0}")
    private double pagingRateCritical;

    public List<MemoryAlert> evaluate(LparstatSnapshot s) {
        List<MemoryAlert> alerts = new ArrayList<>();
        if (s == null) return alerts;

        double pct  = s.getPagingSpaceUsedPct();
        double rate = s.getPagingRate();

        if (pct >= pagingPctCritical) {
            alerts.add(build(s, "PAGING_SPACE_CRITICAL", pct, pagingPctCritical,
                             "paging.used_pct", Severity.CRITICAL));
        } else if (pct >= pagingPctWarn) {
            alerts.add(build(s, "PAGING_SPACE_HIGH", pct, pagingPctWarn,
                             "paging.used_pct", Severity.WARN));
        }

        if (rate >= pagingRateCritical) {
            alerts.add(build(s, "PAGING_RATE_CRITICAL", rate, pagingRateCritical,
                             "paging.rate", Severity.CRITICAL));
        } else if (rate >= pagingRateWarn) {
            alerts.add(build(s, "PAGING_RATE_HIGH", rate, pagingRateWarn,
                             "paging.rate", Severity.WARN));
        }
        return alerts;
    }

    private MemoryAlert build(LparstatSnapshot s, String reason, double value,
                              double threshold, String metric, Severity sev) {
        return MemoryAlert.builder()
                .host(s.getHost())
                .reason(reason)
                .value(value)
                .threshold(threshold)
                .metric(metric)
                .severity(sev)
                .build();
    }
}