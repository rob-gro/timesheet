package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseApplicationException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.NOT_FOUND, "INVALID_TOKEN");
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, "INVALID_TOKEN", cause);
    }
}
