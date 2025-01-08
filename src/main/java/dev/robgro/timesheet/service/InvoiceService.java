package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {

    List<InvoiceDto> getAllInvoices();
    List<InvoiceDto> getAllInvoicesOrderByDateDesc();
    List<InvoiceDto> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate);
    InvoiceDto getInvoiceById(long id);
    Optional<InvoiceDto> findByInvoiceNumber(String invoiceNumber);
    List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month);
    List<InvoiceDto> getYearlyInvoices(Long clientId, int year);
    InvoiceDto createInvoiceFromTimesheets(ClientDto client, List<TimesheetDto> timesheets, LocalDate issueDate);
    void savePdfAndSendInvoice(Long id);
    List<InvoiceDto> searchInvoices(Long clientId, Integer year, Integer month, Boolean emailSent);
    void deleteInvoice(Long id);
}
