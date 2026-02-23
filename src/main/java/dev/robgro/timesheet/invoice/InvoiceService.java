package dev.robgro.timesheet.invoice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceService {

    List<InvoiceDto> getAllInvoices();

    List<InvoiceDto> getAllInvoicesOrderByDateDesc();

    List<InvoiceDto> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate);

    InvoiceDto getInvoiceById(long id);

    Optional<InvoiceDto> findByInvoiceNumber(String invoiceNumber);

    List<InvoiceDto> searchAndSortInvoices(Long clientId, Integer year, Integer month, String sortBy, String sortDir);

    List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month);

    List<InvoiceDto> getYearlyInvoices(Long clientId, int year);

    byte[] getInvoicePdfContent(Long invoiceId);

    void savePdfAndSendInvoice(Long id, PrintMode printMode);

    List<InvoiceDto> searchInvoices(Long clientId, Integer year, Integer month);

    InvoiceDto createAndRedirectInvoice(CreateInvoiceRequest request);

    InvoiceDto buildInvoicePreview(CreateInvoiceRequest request);

    InvoiceDto updateInvoice(Long id, InvoiceUpdateRequest request);

    void deleteInvoice(Long id, boolean deleteTimesheets, boolean detachFromClient);

    Page<InvoiceDto> getAllInvoicesPageable(Long clientId, Integer year, Integer month, Pageable pageable);

    Page<InvoiceDto> searchInvoices(DateRangeRequest dateRange, Long clientId, Pageable pageable);

    InvoiceReportData generateReport(DateRangeRequest dateRange, Long clientId);
}
