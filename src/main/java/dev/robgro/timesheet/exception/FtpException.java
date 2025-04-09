package dev.robgro.timesheet.exception;

/**
 * Exception thrown when FTP operations fail.
 * Extends IntegrationException for FTP-specific errors.
 */
public class FtpException extends IntegrationException {

    public FtpException(String message, Throwable cause) {
        super("FTP operation failed: " + message, cause);
    }

    public FtpException(String message) {
        super("FTP operation failed: " + message);
    }
}
