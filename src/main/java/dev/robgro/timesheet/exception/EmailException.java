package dev.robgro.timesheet.exception;

/**
 * Exception thrown when email operations fail.
 * Extends IntegrationException for email-specific errors.
 */
public class EmailException extends IntegrationException {

  public EmailException(String message, Throwable cause) {
    super("Email operation failed: " + message, cause);
  }

  public EmailException(String message) {
    super("Email operation failed: " + message);
  }
}
