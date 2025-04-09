package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a business rule is violated.
 * Maps to HTTP 409 Conflict response.
 */
public class BusinessRuleViolationException extends BaseApplicationException {

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", cause);
    }
}
