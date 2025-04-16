package dev.robgro.timesheet.exception;

import dev.robgro.timesheet.exception.model.ErrorResponse;
import dev.robgro.timesheet.exception.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    private boolean isProductionEnvironment() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    @ExceptionHandler(BaseApplicationException.class)
    public ResponseEntity<ErrorResponse> handleBaseApplicationException(
            BaseApplicationException ex, HttpServletRequest request) {

        log.error("Application exception: {}", ex.getMessage(), ex);

        ErrorResponse.ErrorResponseBuilder responseBuilder = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .details(isProductionEnvironment() ? null : ex.toString())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now());

        if (ex instanceof ValidationException) {
            ValidationException validationEx = (ValidationException) ex;
            responseBuilder
                    .message("Validation failed: " + validationEx.getMessage())
                    .details(isProductionEnvironment() ? null :
                            validationEx.getValidationErrors().stream()
                                    .map(error -> error.getField() + ": " + error.getMessage())
                                    .collect(Collectors.joining(", ")));
        }
        return new ResponseEntity<>(responseBuilder.build(), ex.getStatus());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {

        log.error("Status exception: {}", ex.getReason(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("API_ERROR")
                .message(ex.getReason())
                .details(isProductionEnvironment() ? null : ex.toString())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            Exception ex, HttpServletRequest request) {

        log.error("Validation exception: {}", ex.getMessage(), ex);

        List<ValidationError> validationErrors;

        if (ex instanceof MethodArgumentNotValidException validationEx) {
            validationErrors = validationEx.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::convertToValidationError)
                    .collect(Collectors.toList());
        } else if (ex instanceof BindException bindException) {
            validationErrors = bindException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(this::convertToValidationError)
                    .collect(Collectors.toList());
        } else if (ex instanceof dev.robgro.timesheet.exception.ValidationException validationException) {
            validationErrors = validationException.getValidationErrors();
        } else {
            validationErrors = Collections.emptyList();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed")
                .details(isProductionEnvironment() ? null : ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ValidationError convertToValidationError(FieldError fieldError) {
        return ValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, HttpServletRequest request) {

        String path = request.getRequestURI();
        if (isStaticResource(path)) {
            return null;
        }

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .details(isProductionEnvironment() ? null : ex.toString())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean isStaticResource(String path) {
        return path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".html") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".gif") ||
                path.endsWith(".ico") ||
                path.endsWith(".woff") ||
                path.endsWith(".map") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/webjars");
    }
}
