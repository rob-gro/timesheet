package dev.robgro.timesheet.model.dto;

public record OperationResult(
        boolean success,
        String message
) {
}
