package dev.robgro.timesheet.invoice;

import java.time.LocalDate;

public interface InvoiceNumberGenerator {
    String generateInvoiceNumber(LocalDate issueDate);
}
