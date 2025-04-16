package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;

public class EntityNotFoundException extends BaseApplicationException {

    public EntityNotFoundException(String entityName, Object id) {
        super(
                String.format("%s with id %s not found", entityName, id),
                HttpStatus.NOT_FOUND,
                "ENTITY_NOT_FOUND"
        );
    }

    public EntityNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND");
    }
}
