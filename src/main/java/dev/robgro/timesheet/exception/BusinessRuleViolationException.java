package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleViolationException extends BaseApplicationException {

    public BusinessRuleViolationException(String message) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleViolationException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", cause);
    }
}
