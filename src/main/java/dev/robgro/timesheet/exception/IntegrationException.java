package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for integration issues with external systems.
 * Maps to HTTP 500 Internal Server Error response.
 */
public class IntegrationException extends BaseApplicationException {

    public IntegrationException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_ERROR", cause);
    }

    public IntegrationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_ERROR");
    }
}
