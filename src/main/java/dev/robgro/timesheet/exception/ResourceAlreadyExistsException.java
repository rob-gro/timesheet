package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class ResourceAlreadyExistsException extends BaseApplicationException {

    public ResourceAlreadyExistsException(String entityName, String identifierName, Object identifierValue) {
        super(
                String.format("%s with %s %s already exists", entityName, identifierName, identifierValue),
                HttpStatus.CONFLICT,
                "RESOURCE_ALREADY_EXISTS"
        );
    }

    public ResourceAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT, "RESOURCE_ALREADY_EXISTS");
    }
}
