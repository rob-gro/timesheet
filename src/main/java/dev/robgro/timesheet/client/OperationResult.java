package dev.robgro.timesheet.client;

public record OperationResult(
        boolean success,
        String message
) {
}
