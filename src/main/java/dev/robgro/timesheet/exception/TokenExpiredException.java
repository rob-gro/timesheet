package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends BaseApplicationException {

    public TokenExpiredException(String message) {
        super(message, HttpStatus.NOT_FOUND, "TOKEN_EXPIRED");
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, HttpStatus.NOT_FOUND, "TOKEN_EXPIRED", cause);
    }
}
