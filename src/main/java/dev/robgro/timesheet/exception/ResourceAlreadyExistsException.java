package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when trying to create a resource that already exists.
 * Maps to HTTP 409 Conflict response.
 */
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
