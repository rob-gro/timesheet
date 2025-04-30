package dev.robgro.timesheet.invoice;

public record DeleteInvoiceRequest(
        boolean deleteTimesheets,
        boolean detachFromClient
) {
}
