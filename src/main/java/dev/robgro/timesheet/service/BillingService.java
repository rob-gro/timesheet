package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.InvoiceDto;

import java.time.LocalDate;
import java.util.List;

public interface BillingService {

    List<InvoiceDto> generateMonthlyInvoices(int year, int month);

    InvoiceDto createMonthlyInvoice(Long clientId, int year, int month);

    InvoiceDto createInvoice(Long clientId, LocalDate issueDate, List<Long> timesheetIds);

    List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month);

    List<InvoiceDto> getYearlyInvoices(Long clientId, int year);
}
