package dev.robgro.timesheet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects global attributes into every Thymeleaf model.
 * Used for feature flags and shared UI state.
 */
@ControllerAdvice
public class GlobalViewModelAttributes {

    @Value("${internal.counters.observability.enabled:true}")
    private boolean counterObservabilityEnabled;

    @ModelAttribute("counterObservabilityEnabled")
    public boolean counterObservabilityEnabled() {
        return counterObservabilityEnabled;
    }
}