package dev.robgro.timesheet.exception;

/**
 * Thrown when trying to generate invoice number but no numbering scheme is configured.
 * This should trigger seller to configure their numbering scheme.
 */
public class NoSchemeConfiguredException extends BusinessRuleViolationException {
    public NoSchemeConfiguredException(String message) {
        super(message);
    }
}
