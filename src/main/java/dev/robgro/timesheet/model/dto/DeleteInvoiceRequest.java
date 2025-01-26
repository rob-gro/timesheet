package dev.robgro.timesheet.model.dto;

public record DeleteInvoiceRequest(
        boolean deleteTimesheets,
        boolean detachFromClient
) {
}
