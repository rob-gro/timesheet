package dev.robgro.timesheet.exception;

public class FtpException extends IntegrationException {

    public FtpException(String message, Throwable cause) {
        super("FTP operation failed: " + message, cause);
    }

    public FtpException(String message) {
        super("FTP operation failed: " + message);
    }
}
