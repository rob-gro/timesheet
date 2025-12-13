package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.IntegrationException;
import dev.robgro.timesheet.exception.ResourceAlreadyExistsException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.invoice.*;
import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.invoice.InvoiceItem;
import dev.robgro.timesheet.timesheet.Timesheet;
import dev.robgro.timesheet.client.ClientRepository;
import dev.robgro.timesheet.invoice.InvoiceRepository;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetRepository;
import dev.robgro.timesheet.timesheet.TimesheetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TimesheetRepository timesheetRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private InvoiceDtoMapper invoiceDtoMapper;

    @Mock
    private FtpService ftpService;

    @Mock
    private TimesheetService timesheetService;

    @Mock
    private InvoiceDocumentService invoiceDocumentService;

    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @Mock
    private InvoiceCreationService invoiceCreationService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // ----- Basic Invoice Retrieval -----

    @Test
    void shouldGetAllInvoices() {
        // given
        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);

        InvoiceDto dto1 = new InvoiceDto(1L, 1L, "Client 1", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);
        InvoiceDto dto2 = new InvoiceDto(2L, 2L, "Client 2", "002-01-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findAll()).thenReturn(List.of(invoice1, invoice2));
        when(invoiceDtoMapper.apply(invoice1)).thenReturn(dto1);
        when(invoiceDtoMapper.apply(invoice2)).thenReturn(dto2);

        // when
        List<InvoiceDto> result = invoiceService.getAllInvoices();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(dto1, dto2);
        verify(invoiceRepository).findAll();
    }

    @Test
    void shouldGetAllInvoicesOrderByDateDesc() {
        // given
        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);

        InvoiceDto dto1 = new InvoiceDto(1L, 1L, "Client 1", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);
        InvoiceDto dto2 = new InvoiceDto(2L, 2L, "Client 2", "002-01-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findAllByOrderByIssueDateDesc()).thenReturn(List.of(invoice1, invoice2));
        when(invoiceDtoMapper.apply(invoice1)).thenReturn(dto1);
        when(invoiceDtoMapper.apply(invoice2)).thenReturn(dto2);

        // when
        List<InvoiceDto> result = invoiceService.getAllInvoicesOrderByDateDesc();

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(dto1, dto2);
        verify(invoiceRepository).findAllByOrderByIssueDateDesc();
    }

    @Test
    void shouldGetInvoicesByDateRange() {
        // given
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);

        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);

        InvoiceDto dto1 = new InvoiceDto(1L, 1L, "Client 1", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);
        InvoiceDto dto2 = new InvoiceDto(2L, 2L, "Client 2", "002-01-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findByIssueDateBetweenOrderByIssueDateDesc(startDate, endDate))
                .thenReturn(List.of(invoice1, invoice2));
        when(invoiceDtoMapper.apply(invoice1)).thenReturn(dto1);
        when(invoiceDtoMapper.apply(invoice2)).thenReturn(dto2);

        // when
        List<InvoiceDto> result = invoiceService.getInvoicesByDateRange(startDate, endDate);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).contains(dto1, dto2);
        verify(invoiceRepository).findByIssueDateBetweenOrderByIssueDateDesc(startDate, endDate);
    }

    @Test
    void shouldGetInvoiceById() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);

        InvoiceDto dto = new InvoiceDto(invoiceId, 1L, "Client 1", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceDtoMapper.apply(invoice)).thenReturn(dto);

        // when
        InvoiceDto result = invoiceService.getInvoiceById(invoiceId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(invoiceId);
        verify(invoiceRepository).findById(invoiceId);
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFound() {
        // given
        Long invoiceId = 1L;
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> invoiceService.getInvoiceById(invoiceId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice with id 1 not found");
    }

    @Test
    void shouldFindByInvoiceNumber() {
        // given
        String invoiceNumber = "001-01-2023";
        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber(invoiceNumber);

        InvoiceDto dto = new InvoiceDto(1L, 1L, "Client 1", invoiceNumber, LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findByInvoiceNumber(invoiceNumber)).thenReturn(Optional.of(invoice));
        when(invoiceDtoMapper.apply(invoice)).thenReturn(dto);

        // when
        Optional<InvoiceDto> result = invoiceService.findByInvoiceNumber(invoiceNumber);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().invoiceNumber()).isEqualTo(invoiceNumber);
    }

    // ----- Invoice Filtering and Sorting -----

    @Test
    void shouldSearchAndSortInvoices() {
        // given
        Long clientId = 1L;
        Integer year = 2023;
        Integer month = 1;
        String sortBy = "invoiceNumber";
        String sortDir = "asc";

        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        invoice1.setInvoiceNumber("001-01-2023");

        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);
        invoice2.setInvoiceNumber("002-02-2023");

        InvoiceDto dto1 = new InvoiceDto(1L, 1L, "Client 1", "001-01-2023", LocalDate.of(2023, 1, 15), null, null, List.of(), null, null);
        InvoiceDto dto2 = new InvoiceDto(2L, 1L, "Client 1", "002-02-2023", LocalDate.of(2023, 2, 15), null, null, List.of(), null, null);

        when(invoiceRepository.findFilteredInvoices(clientId, year, month)).thenReturn(List.of(invoice1, invoice2));
        when(invoiceDtoMapper.apply(invoice1)).thenReturn(dto1);
        when(invoiceDtoMapper.apply(invoice2)).thenReturn(dto2);

        // when
        List<InvoiceDto> result = invoiceService.searchAndSortInvoices(clientId, year, month, sortBy, sortDir);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).invoiceNumber()).isEqualTo("001-01-2023");
        assertThat(result.get(1).invoiceNumber()).isEqualTo("002-02-2023");
    }

    @Test
    void shouldGetMonthlyInvoices() {
        // given
        Long clientId = 1L;
        int year = 2023;
        int month = 1;

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();

        Invoice invoice = new Invoice();
        invoice.setId(1L);

        InvoiceDto dto = new InvoiceDto(1L, clientId, "Client 1", "001-01-2023", startDate, null, null, List.of(), null, null);

        when(invoiceRepository.findByClientIdAndIssueDateBetween(clientId, startDate, endDate))
                .thenReturn(List.of(invoice));
        when(invoiceDtoMapper.apply(invoice)).thenReturn(dto);

        // when
        List<InvoiceDto> result = invoiceService.getMonthlyInvoices(clientId, year, month);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        verify(invoiceRepository).findByClientIdAndIssueDateBetween(clientId, startDate, endDate);
    }

    @Test
    void shouldGetYearlyInvoices() {
        // given
        Long clientId = 1L;
        int year = 2023;

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        Invoice invoice = new Invoice();
        invoice.setId(1L);

        InvoiceDto dto = new InvoiceDto(1L, clientId, "Client 1", "001-01-2023", startDate, null, null, List.of(), null, null);

        when(invoiceRepository.findByClientIdAndIssueDateBetween(clientId, startDate, endDate))
                .thenReturn(List.of(invoice));
        when(invoiceDtoMapper.apply(invoice)).thenReturn(dto);

        // when
        List<InvoiceDto> result = invoiceService.getYearlyInvoices(clientId, year);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        verify(invoiceRepository).findByClientIdAndIssueDateBetween(clientId, startDate, endDate);
    }

    // ----- Test for sorting by different criteria -----

    @Test
    void shouldSortInvoicesByIssueDate() {
        // given
        Long clientId = 1L;
        LocalDate date1 = LocalDate.of(2023, 1, 1);
        LocalDate date2 = LocalDate.of(2023, 2, 1);

        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        invoice1.setIssueDate(date1);

        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);
        invoice2.setIssueDate(date2);

        InvoiceDto dto1 = new InvoiceDto(1L, clientId, "Client 1", "001-01-2023", date1, BigDecimal.valueOf(100), null, List.of(), null, null);
        InvoiceDto dto2 = new InvoiceDto(2L, clientId, "Client 1", "002-02-2023", date2, BigDecimal.valueOf(200), null, List.of(), null, null);

        when(invoiceRepository.findFilteredInvoices(eq(clientId), isNull(), isNull())).thenReturn(List.of(invoice2, invoice1));
        when(invoiceDtoMapper.apply(invoice1)).thenReturn(dto1);
        when(invoiceDtoMapper.apply(invoice2)).thenReturn(dto2);

        // when
        List<InvoiceDto> result = invoiceService.searchAndSortInvoices(clientId, null, null, "issueDate", "asc");

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result.get(0).issueDate()).isEqualTo(date1);
        assertThat(result.get(1).issueDate()).isEqualTo(date2);
    }

    @Test
    void shouldSortInvoicesByClientName() {
        // given
        Invoice inv1 = new Invoice();
        inv1.setId(1L);

        Invoice inv2 = new Invoice();
        inv2.setId(2L);

        InvoiceDto invoice1 = new InvoiceDto(1L, 1L, "B Client", "001-01-2023", LocalDate.now(), BigDecimal.valueOf(100), null, List.of(), null, null);
        InvoiceDto invoice2 = new InvoiceDto(2L, 2L, "A Client", "002-01-2023", LocalDate.now(), BigDecimal.valueOf(200), null, List.of(), null, null);

        when(invoiceRepository.findFilteredInvoices(isNull(), isNull(), isNull())).thenReturn(List.of(inv1, inv2));
        when(invoiceDtoMapper.apply(inv1)).thenReturn(invoice1);
        when(invoiceDtoMapper.apply(inv2)).thenReturn(invoice2);

        // when
        List<InvoiceDto> result = invoiceService.searchAndSortInvoices(null, null, null, "clientName", "asc");

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        assertThat(result.get(0).clientName()).isEqualTo("A Client");
        assertThat(result.get(1).clientName()).isEqualTo("B Client");
    }

    @Test
    void shouldSortInvoicesByTotalAmount() {
        // given
        InvoiceDto invoice1 = new InvoiceDto(1L, 1L, "Client", "001-01-2023", LocalDate.now(), BigDecimal.valueOf(100), null, List.of(), null, null);
        InvoiceDto invoice2 = new InvoiceDto(2L, 1L, "Client", "002-01-2023", LocalDate.now(), BigDecimal.valueOf(200), null, List.of(), null, null);

        List<Invoice> invoices = new ArrayList<>();
        Invoice inv1 = new Invoice(); inv1.setId(1L);
        Invoice inv2 = new Invoice(); inv2.setId(2L);
        invoices.add(inv2); invoices.add(inv1); // Reversed order

        when(invoiceRepository.findFilteredInvoices(isNull(), isNull(), isNull())).thenReturn(invoices);
        when(invoiceDtoMapper.apply(inv1)).thenReturn(invoice1);
        when(invoiceDtoMapper.apply(inv2)).thenReturn(invoice2);

        // when
        List<InvoiceDto> result = invoiceService.searchAndSortInvoices(null, null, null, "totalAmount", "desc");

        // then
        assertThat(result).isNotNull();
    }

    // ----- Invoice Creation and Update -----

    @Test
    void shouldCreateAndRedirectInvoice() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);
        String invoiceNumber = null;

        CreateInvoiceRequest request = new CreateInvoiceRequest(
                clientId,      // clientId
                issueDate,     // issueDate
                timesheetIds,  // timesheetIds
                invoiceNumber  // invoiceNumber
        );

        InvoiceDto invoiceDto = new InvoiceDto(
                1L, clientId, "Client 1", "001-01-2023", issueDate, null, null, List.of(), null, null
        );

        when(invoiceCreationService.createInvoice(clientId, issueDate, timesheetIds)).thenReturn(invoiceDto);

        // when
        InvoiceDto result = invoiceService.createAndRedirectInvoice(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        verify(invoiceCreationService).createInvoice(clientId, issueDate, timesheetIds);
    }

    @Test
    void shouldUpdateInvoice() {
        // given
        Long invoiceId = 1L;
        Long clientId = 2L;
        String invoiceNumber = "001-01-2023";
        LocalDate issueDate = LocalDate.now();

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setItemsList(new ArrayList<>());
        invoice.setTimesheets(new ArrayList<>());

        Client client = new Client();
        client.setId(clientId);

        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);

        InvoiceItemUpdateRequest itemRequest = new InvoiceItemUpdateRequest(
                1L,
                1L,
                issueDate,
                "Service description",
                2.0,
                BigDecimal.valueOf(100.0),
                50.0  // hourlyRate
        );

        InvoiceUpdateRequest updateRequest = new InvoiceUpdateRequest(
                clientId,
                issueDate,
                invoiceNumber,
                List.of(itemRequest)
        );

        InvoiceDto updatedDto = new InvoiceDto(
                invoiceId, clientId, "Client 2", invoiceNumber, issueDate,
                BigDecimal.valueOf(100.0), null, List.of(), null, null
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(timesheetRepository.findById(1L)).thenReturn(Optional.of(timesheet));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
        when(invoiceDtoMapper.apply(invoice)).thenReturn(updatedDto);

        // when
        InvoiceDto result = invoiceService.updateInvoice(invoiceId, updateRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(invoiceId);
        assertThat(result.clientId()).isEqualTo(clientId);

        verify(invoiceRepository, atLeastOnce()).findById(invoiceId);
        verify(clientRepository, atLeastOnce()).findById(clientId);
        verify(invoiceRepository, atLeastOnce()).save(any(Invoice.class));
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNumberAlreadyExists() {
        // given
        Long invoiceId = 1L;
        Long clientId = 2L;
        String existingInvoiceNumber = "001-01-2023";
        String newInvoiceNumber = "002-01-2023";
        LocalDate issueDate = LocalDate.now();

        Invoice currentInvoice = new Invoice();
        currentInvoice.setId(invoiceId);
        currentInvoice.setInvoiceNumber(existingInvoiceNumber);

        Invoice existingInvoice = new Invoice();
        existingInvoice.setId(2L);
        existingInvoice.setInvoiceNumber(newInvoiceNumber);

        Client client = new Client();
        client.setId(clientId);

        InvoiceUpdateRequest updateRequest = new InvoiceUpdateRequest(
                clientId,           // clientId
                issueDate,          // issueDate
                newInvoiceNumber,   // invoiceNumber
                List.of()           // items
        );

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(currentInvoice));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceRepository.findByInvoiceNumber(newInvoiceNumber)).thenReturn(Optional.of(existingInvoice));

        // when/then
        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, updateRequest))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    // ----- Invoice PDF Operations -----

    @Test
    void shouldGetInvoicePdfContent() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber("001-01-2023");
        invoice.setPdfPath("/path/to/pdf");

        byte[] pdfContent = "PDF content".getBytes();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(ftpService.downloadPdfInvoice("001-01-2023.pdf")).thenReturn(pdfContent);

        // when
        byte[] result = invoiceService.getInvoicePdfContent(invoiceId);

        // then
        assertThat(result).isEqualTo(pdfContent);
        verify(ftpService).downloadPdfInvoice("001-01-2023.pdf");
    }

    @Test
    void shouldThrowExceptionWhenPdfNotFound() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setPdfPath(null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // when/then
        assertThatThrownBy(() -> invoiceService.getInvoicePdfContent(invoiceId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("PDF for invoice");
    }

    @Test
    void shouldThrowExceptionWhenPdfDownloadFails() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber("001-01-2023");
        invoice.setPdfPath("/path/to/pdf");

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        doThrow(new RuntimeException("FTP error"))
                .when(ftpService).downloadPdfInvoice(eq("001-01-2023.pdf"));

        // when/then
        assertThatThrownBy(() -> invoiceService.getInvoicePdfContent(invoiceId))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Could not download PDF");
    }

    @Test
    void shouldSavePdfAndSendInvoice() {
        // given
        Long invoiceId = 1L;

        // when
        invoiceService.savePdfAndSendInvoice(invoiceId);

        // then
        verify(invoiceDocumentService).savePdfAndSendInvoice(invoiceId);
    }

    // ----- Invoice Deletion -----

    @Test
    void shouldDeleteInvoice() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber("001-01-2023");
        invoice.setTimesheets(new ArrayList<>());

        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setInvoice(invoice);
        invoice.getTimesheets().add(timesheet);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // when
        invoiceService.deleteInvoice(invoiceId, false, false);

        // then
        verify(timesheetRepository).flush();
        verify(invoiceRepository).deleteInvoiceItemsByInvoiceId(invoiceId);
        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void shouldDeleteInvoiceAndTimesheets() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber("001-01-2023");
        invoice.setTimesheets(new ArrayList<>());

        Timesheet timesheet = new Timesheet();
        timesheet.setId(1L);
        timesheet.setInvoice(invoice);
        invoice.getTimesheets().add(timesheet);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // when
        invoiceService.deleteInvoice(invoiceId, true, false);

        // then
        verify(timesheetRepository, never()).flush();
        verify(invoiceRepository).deleteInvoiceItemsByInvoiceId(invoiceId);
        verify(invoiceRepository).delete(invoice);
    }

    // ----- Report Generation -----

    @Test
    void shouldGenerateReport() {
        // given
        DateRangeRequest dateRange = new DateRangeRequest(2023, 1, 2023, 12);
        Long clientId = 1L;

        LocalDate fromDate = LocalDate.of(2023, 1, 1);
        LocalDate toDate = LocalDate.of(2023, 12, 31);

        Invoice invoice1 = new Invoice();
        invoice1.setId(1L);
        invoice1.setTotalAmount(BigDecimal.valueOf(100));

        Invoice invoice2 = new Invoice();
        invoice2.setId(2L);
        invoice2.setTotalAmount(BigDecimal.valueOf(200));

        Client client = new Client();
        client.setId(clientId);
        client.setClientName("Test Client");

        InvoiceDto dto1 = new InvoiceDto(1L, clientId, "Test Client", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);
        InvoiceDto dto2 = new InvoiceDto(2L, clientId, "Test Client", "002-02-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findForReporting(eq(clientId), any(), any(), any()))
                .thenReturn(List.of(invoice1, invoice2));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(invoiceDtoMapper.apply(invoice1)).thenReturn(dto1);
        when(invoiceDtoMapper.apply(invoice2)).thenReturn(dto2);

        // when
        InvoiceReportData result = invoiceService.generateReport(dateRange, clientId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.invoices()).hasSize(2);
        assertThat(result.totalAmount()).isEqualTo(BigDecimal.valueOf(300));
        assertThat(result.clientName()).isEqualTo("Test Client");
        assertThat(result.period()).contains("2023");
    }

    // ----- Pagination -----

    @Test
    void shouldGetAllInvoicesPageable() {
        // given
        Long clientId = 1L;
        Integer year = 2023;
        Integer month = 1;
        Pageable pageable = mock(Pageable.class);

        Invoice invoice = new Invoice();
        invoice.setId(1L);

        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice));
        InvoiceDto invoiceDto = new InvoiceDto(1L, clientId, "Client 1", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findFilteredInvoices(clientId, year, month, pageable)).thenReturn(invoicePage);
        when(invoiceDtoMapper.apply(invoice)).thenReturn(invoiceDto);

        // when
        Page<InvoiceDto> result = invoiceService.getAllInvoicesPageable(clientId, year, month, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(invoiceRepository).findFilteredInvoices(clientId, year, month, pageable);
    }

    @Test
    void shouldSearchInvoicesWithDateRange() {
        // given
        DateRangeRequest dateRange = new DateRangeRequest(2023, 1, 2023, 12);
        Long clientId = 1L;
        Pageable pageable = mock(Pageable.class);

        Invoice invoice = new Invoice();
        invoice.setId(1L);

        Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice));
        InvoiceDto invoiceDto = new InvoiceDto(1L, clientId, "Client 1", "001-01-2023", LocalDate.now(), null, null, List.of(), null, null);

        when(invoiceRepository.findByDateRangeAndClient(any(), any(), eq(clientId), eq(pageable))).thenReturn(invoicePage);
        when(invoiceDtoMapper.apply(invoice)).thenReturn(invoiceDto);

        // when
        Page<InvoiceDto> result = invoiceService.searchInvoices(dateRange, clientId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(invoiceRepository).findByDateRangeAndClient(any(), any(), eq(clientId), eq(pageable));
    }

    @Test
    void shouldValidateDateRange() {
        // given
        DateRangeRequest invalidDateRange = new DateRangeRequest(2023, 12, 2023, 1); // end before start
        Long clientId = 1L;
        Pageable pageable = mock(Pageable.class);

        // when/then
        assertThatThrownBy(() -> invoiceService.searchInvoices(invalidDateRange, clientId, pageable))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    // ----- Utility Methods -----

    @Test
    void shouldValidateDateRangeWithNullDates() {
        // given
        Method validateMethod = ReflectionUtils.findMethod(InvoiceServiceImpl.class, "validateDateRange", LocalDate.class, LocalDate.class)
                .orElseThrow(() -> new EntityNotFoundException("Method", "ValidateDateRange not found"));

        // when & then
        assertThatCode(() -> {
            ReflectionUtils.invokeMethod(validateMethod, invoiceService, null, null);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            ReflectionUtils.invokeMethod(validateMethod, invoiceService, LocalDate.now(), null);
        }).doesNotThrowAnyException();

        assertThatCode(() -> {
            ReflectionUtils.invokeMethod(validateMethod, invoiceService, null, LocalDate.now());
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldGeneratePeriodLabelForCompleteDates() {
        // given
        DateRangeRequest request = new DateRangeRequest(2023, 1, 2023, 12);

        Method generateLabelMethod = ReflectionUtils.findMethod(InvoiceServiceImpl.class, "generatePeriodLabel", DateRangeRequest.class)
            .orElseThrow(() -> new EntityNotFoundException("Method", "GeneratePeriodLabel not found"));

        // when
        String result = (String) ReflectionUtils.invokeMethod(generateLabelMethod, invoiceService, request);

        // then
        assertThat(result).contains("2023");
        assertThat(result).contains("JANUARY");
        assertThat(result).contains("DECEMBER");
    }

    @Test
    void shouldGeneratePeriodLabelForNullDates() {
        // given
        DateRangeRequest request = new DateRangeRequest(null, null, null, null);

        Method generateLabelMethod = ReflectionUtils.findMethod(InvoiceServiceImpl.class, "generatePeriodLabel", DateRangeRequest.class)
            .orElseThrow(() -> new EntityNotFoundException("Method", "GeneratePeriodLabel not found"));

        // when
        String result = (String) ReflectionUtils.invokeMethod(generateLabelMethod, invoiceService, request);

        // then
        assertThat(result).isEqualTo("all dates");
    }
}
