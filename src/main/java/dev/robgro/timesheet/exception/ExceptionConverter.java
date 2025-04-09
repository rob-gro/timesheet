package dev.robgro.timesheet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility class for converting between ResponseStatusException and application exceptions.
 * Helpful for migrating code from using ResponseStatusException to the new exceptions framework.
 */
public final class ExceptionConverter {

    private ExceptionConverter() {
    }

    public static BaseApplicationException convertResponseStatusException(ResponseStatusException ex) {
        if (ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            return new EntityNotFoundException(ex.getReason());
        } else if (ex.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
            return new ValidationException(ex.getReason());
        } else if (ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
            if (ex.getReason() != null && ex.getReason().contains("already exists")) {
                return new ResourceAlreadyExistsException(ex.getReason());
            } else {
                return new BusinessRuleViolationException(ex.getReason());
            }
        } else if (ex.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
            return new IntegrationException(ex.getReason(), ex);
        } else {
            // Fallback - create a BaseApplicationException with the same status
            return new BaseApplicationException(ex.getReason(), HttpStatus.valueOf(ex.getStatusCode().value()), "API_ERROR") {};
        }
    }

    public static EntityNotFoundException extractEntityNotFound(ResponseStatusException ex) {
        if (!ex.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            throw new IllegalArgumentException("Exception is not a NOT_FOUND status: " + ex.getStatusCode());
        }

        String message = ex.getReason();
        if (message == null) {
            return new EntityNotFoundException("Entity not found");
        }
        String[] parts = message.split(" with id ");
        if (parts.length == 2) {
            String entityName = parts[0];
            String idPart = parts[1].split(" not found")[0];
            try {
                 Long id = Long.parseLong(idPart);
                return new EntityNotFoundException(entityName, id);
            } catch (NumberFormatException e) {
                return new EntityNotFoundException(entityName, idPart);
            }
        }
        return new EntityNotFoundException(message);
    }
}
