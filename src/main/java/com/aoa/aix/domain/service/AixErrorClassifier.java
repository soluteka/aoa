package com.aoa.aix.domain.service;

import com.aoa.aix.domain.model.ErrptEvent;
import com.aoa.aix.domain.model.Severity;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AixErrorClassifier {

    private static final Set<String> CRITICAL_IDS = Set.of(
        "C5C09FFA", "369D049B", "BFE4C025", "AA8AB241"
    );

    private static final Set<String> CRITICAL_KEYWORDS = Set.of(
        "FAILURE", "CRASH", "DOWN", "DEAD", "FATAL", "PANIC"
    );

    private static final Set<String> WARN_KEYWORDS = Set.of(
        "ERROR", "PREDICTED", "WARNING", "DEGRADED", "TIMEOUT", "RECOVERY"
    );

    /** Compat: firma antigua. */
    public Severity classify(String identifier, String description) {
        return classify(identifier, description, null, null);
    }

    /** Firma nueva: usa class (P/T/I) y type (H/S/O) para refinar. */
    public Severity classify(ErrptEvent e) {
        if (e == null) return Severity.INFO;
        return classify(e.getIdentifier(), e.getDescription(),
                        e.getErrorClass(), e.getErrorType());
    }

    public Severity classify(String identifier, String description,
                             String errorClass, String errorType) {

        if (identifier != null && CRITICAL_IDS.contains(identifier)) {
            return Severity.CRITICAL;
        }

        String desc = description == null ? "" : description.toUpperCase();

        // PERMANENT + HARDWARE = casi siempre crítico
        if ("P".equalsIgnoreCase(errorClass) && "H".equalsIgnoreCase(errorType)) {
            return Severity.CRITICAL;
        }

        if (containsAny(desc, CRITICAL_KEYWORDS)) return Severity.CRITICAL;
        if (containsAny(desc, WARN_KEYWORDS))     return Severity.WARN;

        if ("T".equalsIgnoreCase(errorClass)) return Severity.WARN;   // Temporary
        if ("I".equalsIgnoreCase(errorClass)) return Severity.INFO;   // Informational

        return Severity.INFO;
    }

    private boolean containsAny(String text, Set<String> kws) {
        for (String k : kws) if (text.contains(k)) return true;
        return false;
    }
}