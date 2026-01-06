package dev.robgro.timesheet.service;

import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.invoice.BillingServiceImpl;
import dev.robgro.timesheet.invoice.InvoiceCreationService;
import dev.robgro.timesheet.invoice.InvoiceDto;
import dev.robgro.timesheet.invoice.InvoiceService;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock
    private ClientService clientService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceCreationService invoiceCreationService;

    @Mock
    private TimesheetService timesheetService;

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private BillingServiceImpl billingService;

    private Seller testSeller;

    @BeforeEach
    void setUp() {
        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setName("Test Seller");
        testSeller.setActive(true);

        // Mock seller repository to return test seller (lenient for tests that don't need it)
        lenient().when(sellerRepository.findByActiveTrue()).thenReturn(List.of(testSeller));
    }

    // ----- Monthly Invoice Generation -----

    @Test
    void shouldGenerateMonthlyInvoices() {
        // given
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        ClientDto client = new ClientDto(1L, "Client 1", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);
        TimesheetDto timesheet = new TimesheetDto(1L, "Client 1", LocalDate.of(2024, 1, 15), 2.0, false, 1L, 50.0, null, null, BigDecimal.valueOf(100.0));
        InvoiceDto invoice = new InvoiceDto(1L, 1L, "Client 1", 1L, "Test Seller", "INV-001", lastDayOfMonth, null, null, List.of(), null, null, null, 0, null, "NOT_SENT");

        when(clientService.getAllClients()).thenReturn(List.of(client));
        when(timesheetService.getMonthlyTimesheets(client.id(), year, month)).thenReturn(List.of(timesheet));
        when(invoiceCreationService.createInvoice(client.id(), testSeller.getId(), lastDayOfMonth, List.of(timesheet.id()))).thenReturn(invoice);

        // when
        List<InvoiceDto> result = billingService.generateMonthlyInvoices(year, month);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(invoice.id());
        verify(clientService).getAllClients();
        verify(timesheetService).getMonthlyTimesheets(client.id(), year, month);
        verify(invoiceCreationService).createInvoice(client.id(), testSeller.getId(), lastDayOfMonth, List.of(timesheet.id()));
    }

    @Test
    void shouldFilterInvoicedTimesheetsWhenGeneratingInvoice() {
        // given
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        ClientDto client = new ClientDto(1L, "Client 1", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);
        TimesheetDto invoicedTimesheet = new TimesheetDto(1L, "Client 1", LocalDate.of(2024, 1, 15), 2.0, true, 1L, 50.0, null, null, BigDecimal.valueOf(100.0));
        TimesheetDto uninvoicedTimesheet = new TimesheetDto(2L, "Client 1", LocalDate.of(2024, 1, 16), 3.0, false, 1L, 50.0, null, null, BigDecimal.valueOf(150.0));
        InvoiceDto invoice = new InvoiceDto(1L, 1L, "Client 1", 1L, "Test Seller", "INV-001", lastDayOfMonth, null, null, List.of(), null, null, null, 0, null, "NOT_SENT");

        when(clientService.getAllClients()).thenReturn(List.of(client));
        when(timesheetService.getMonthlyTimesheets(client.id(), year, month)).thenReturn(List.of(invoicedTimesheet, uninvoicedTimesheet));
        when(invoiceCreationService.createInvoice(client.id(), testSeller.getId(), lastDayOfMonth, List.of(uninvoicedTimesheet.id()))).thenReturn(invoice);

        // when
        List<InvoiceDto> result = billingService.generateMonthlyInvoices(year, month);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(invoice.id());
        verify(invoiceCreationService).createInvoice(client.id(), testSeller.getId(), lastDayOfMonth, List.of(uninvoicedTimesheet.id()));
    }

    @Test
    void shouldNotGenerateInvoiceWhenAllTimesheetsAreInvoiced() {
        // given
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        ClientDto client = new ClientDto(1L, "Client 1", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);
        TimesheetDto invoicedTimesheet1 = new TimesheetDto(1L, "Client 1", LocalDate.of(2024, 1, 15), 2.0, true, 1L, 50.0, null, null, BigDecimal.valueOf(100.0));
        TimesheetDto invoicedTimesheet2 = new TimesheetDto(2L, "Client 1", LocalDate.of(2024, 1, 16), 3.0, true, 1L, 50.0, null, null, BigDecimal.valueOf(150.0));

        when(clientService.getAllClients()).thenReturn(List.of(client));
        when(timesheetService.getMonthlyTimesheets(client.id(), year, month)).thenReturn(List.of(invoicedTimesheet1, invoicedTimesheet2));

        // when
        List<InvoiceDto> result = billingService.generateMonthlyInvoices(year, month);

        // then
        assertThat(result).isEmpty();
        verify(timesheetService).getMonthlyTimesheets(client.id(), year, month);
        verifyNoInteractions(invoiceCreationService);
    }

    @Test
    void shouldReturnEmptyListWhenNoClients() {
        // given
        int year = 2024;
        int month = 1;

        when(clientService.getAllClients()).thenReturn(List.of());

        // when
        List<InvoiceDto> result = billingService.generateMonthlyInvoices(year, month);

        // then
        assertThat(result).isEmpty();
        verify(clientService).getAllClients();
        verifyNoInteractions(timesheetService, invoiceCreationService);
    }

    @Test
    void shouldNotGenerateInvoiceForClientWithoutUnbilledTimesheets() {
        // given
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        ClientDto client = new ClientDto(1L, "Client 1", 50.0, 1L, "Street", "City", "12345", "email@test.com", true);

        when(clientService.getAllClients()).thenReturn(List.of(client));
        when(timesheetService.getMonthlyTimesheets(client.id(), year, month)).thenReturn(List.of());

        // when
        List<InvoiceDto> result = billingService.generateMonthlyInvoices(year, month);

        // then
        assertThat(result).isEmpty();
        verify(clientService).getAllClients();
        verify(timesheetService).getMonthlyTimesheets(client.id(), year, month);
        verifyNoInteractions(invoiceCreationService);
    }

    // ----- Creating Single Monthly Invoice -----

    @Test
    void shouldCreateMonthlyInvoiceForClient() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        TimesheetDto timesheet = new TimesheetDto(1L, "Client 1", LocalDate.of(2024, 1, 15), 2.0, false, 1L, 50.0, null, null, BigDecimal.valueOf(100.0));
        InvoiceDto invoice = new InvoiceDto(1L, clientId, "Client 1", 1L, "Test Seller", "INV-001", lastDayOfMonth, null, null, List.of(), null, null, null, 0, null, "NOT_SENT");

        when(timesheetService.getMonthlyTimesheets(clientId, year, month)).thenReturn(List.of(timesheet));
        when(invoiceCreationService.createInvoice(clientId, testSeller.getId(), lastDayOfMonth, List.of(timesheet.id()))).thenReturn(invoice);

        // when
        InvoiceDto result = billingService.createMonthlyInvoice(clientId, year, month);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(invoice.id());
        verify(timesheetService).getMonthlyTimesheets(clientId, year, month);
        verify(invoiceCreationService).createInvoice(clientId, testSeller.getId(), lastDayOfMonth, List.of(timesheet.id()));
    }

    @Test
    void shouldThrowExceptionWhenNoUnbilledTimesheetsFound() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;

        when(timesheetService.getMonthlyTimesheets(clientId, year, month)).thenReturn(List.of());

        // when/then
        assertThatThrownBy(() -> billingService.createMonthlyInvoice(clientId, year, month))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("No uninvoiced timesheets found for this period");
    }

    @Test
    void shouldThrowExceptionWhenAllTimesheetsAreInvoicedForMonthlyInvoice() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;

        TimesheetDto invoicedTimesheet = new TimesheetDto(1L, "Client 1", LocalDate.of(2024, 1, 15), 2.0, true, 1L, 50.0, null, null, BigDecimal.valueOf(100.0));

        when(timesheetService.getMonthlyTimesheets(clientId, year, month)).thenReturn(List.of(invoicedTimesheet));

        // when/then
        assertThatThrownBy(() -> billingService.createMonthlyInvoice(clientId, year, month))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("No uninvoiced timesheets found for this period");

        verify(timesheetService).getMonthlyTimesheets(clientId, year, month);
        verifyNoInteractions(invoiceCreationService);
    }

    // ----- Single Invoice Operations -----

    @Test
    void shouldCreateInvoice() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);
        InvoiceDto invoice = new InvoiceDto(1L, clientId, "Client 1", 1L, "Test Seller", "INV-001", issueDate, null, null, List.of(), null, null, null, 0, null, "NOT_SENT");

        when(invoiceCreationService.createInvoice(clientId, testSeller.getId(), issueDate, timesheetIds)).thenReturn(invoice);

        // when
        InvoiceDto result = billingService.createInvoice(clientId, issueDate, timesheetIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(invoice.id());
        verify(invoiceCreationService).createInvoice(clientId, testSeller.getId(), issueDate, timesheetIds);
    }

    // ----- Retrieving Invoices -----

    @Test
    void shouldGetMonthlyInvoices() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;
        InvoiceDto invoice = new InvoiceDto(1L, clientId, "Client 1", 1L, "Test Seller", "INV-001", LocalDate.now(), null, null, List.of(), null, null, null, 0, null, "NOT_SENT");

        when(invoiceService.getMonthlyInvoices(clientId, year, month)).thenReturn(List.of(invoice));

        // when
        List<InvoiceDto> result = billingService.getMonthlyInvoices(clientId, year, month);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(invoice.id());
        verify(invoiceService).getMonthlyInvoices(clientId, year, month);
    }

    @Test
    void shouldGetYearlyInvoices() {
        // given
        Long clientId = 1L;
        int year = 2024;
        InvoiceDto invoice = new InvoiceDto(1L, clientId, "Client 1", 1L, "Test Seller", "INV-001", LocalDate.now(), null, null, List.of(), null, null, null, 0, null, "NOT_SENT");

        when(invoiceService.getYearlyInvoices(clientId, year)).thenReturn(List.of(invoice));

        // when
        List<InvoiceDto> result = billingService.getYearlyInvoices(clientId, year);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(invoice.id());
        verify(invoiceService).getYearlyInvoices(clientId, year);
    }

    // ----- Exception Handling -----

    @Test
    void shouldHandleExceptionFromClientService() {
        // given
        int year = 2024;
        int month = 1;

        when(clientService.getAllClients()).thenThrow(new RuntimeException("Database error"));

        // when/then
        assertThatThrownBy(() -> billingService.generateMonthlyInvoices(year, month))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database error");

        verify(clientService).getAllClients();
        verifyNoInteractions(timesheetService, invoiceCreationService);
    }
}
