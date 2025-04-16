package dev.robgro.timesheet.service;

import java.time.LocalDate;

public interface InvoiceNumberGenerator {
    String generateInvoiceNumber(LocalDate issueDate);
}
