package dev.robgro.timesheet.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseApplicationException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    protected BaseApplicationException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    protected BaseApplicationException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
}
