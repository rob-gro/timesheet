package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class IntegrationException extends BaseApplicationException {

    public IntegrationException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_ERROR", cause);
    }

    public IntegrationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_ERROR");
    }
}
