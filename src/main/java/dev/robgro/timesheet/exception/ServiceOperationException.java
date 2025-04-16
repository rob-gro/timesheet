package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class ServiceOperationException extends BaseApplicationException {

    public ServiceOperationException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "SERVICE_OPERATION_ERROR", cause);
    }

    public ServiceOperationException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "SERVICE_OPERATION_ERROR");
    }
}
