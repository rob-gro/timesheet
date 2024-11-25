package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.InvoiceDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {
    List<InvoiceDto> getAllInvoices();
    InvoiceDto getInvoiceById(long id);
    List<InvoiceDto> generateMonthlyInvoices(int year, int month);
    InvoiceDto createInvoice(Long clientId, int year, int month, List<Long> timesheetsIds);
    InvoiceDto createInvoice(Long clientId, LocalDate issueDate, List<Long> timesheetIds);

    InvoiceDto createMonthlyInvoice(Long clientId, int year, int month);
    Optional<InvoiceDto> findByInvoiceNumber(String invoiceNumber);

    List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month);
    List<InvoiceDto> getYearlyInvoices(Long clientId, int year);
}
