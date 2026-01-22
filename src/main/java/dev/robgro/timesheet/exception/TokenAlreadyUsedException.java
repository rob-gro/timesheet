package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class TokenAlreadyUsedException extends BaseApplicationException {

    public TokenAlreadyUsedException(String message) {
        super(message, HttpStatus.CONFLICT, "TOKEN_ALREADY_USED");
    }

    public TokenAlreadyUsedException(String message, Throwable cause) {
        super(message, HttpStatus.CONFLICT, "TOKEN_ALREADY_USED", cause);
    }
}
