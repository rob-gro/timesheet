package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock
    private ClientService clientService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private TimesheetService timesheetService;

    @InjectMocks
    private BillingServiceImpl billingService;

    @Test
    void shouldGenerateMonthlyInvoicesForAllClients() {
        // given
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        ClientDto client1 = new ClientDto(1L, "Client 1", 50.0, 1L, "Street", "City", "12345", "email1@test.com");
        ClientDto client2 = new ClientDto(2L, "Client 2", 60.0, 2L, "Street", "City", "12345", "email2@test.com");

        TimesheetDto timesheet1 = new TimesheetDto(
                1L,
                client1.clientName(),
                LocalDate.of(2024, 1, 15),
                2.0,
                false,
                client1.id(),
                client1.hourlyRate(),
                null,
                null
        );

        TimesheetDto timesheet2 = new TimesheetDto(
                2L,
                client2.clientName(),
                LocalDate.of(2024, 1, 16),
                3.0,
                false,
                client2.id(),
                client2.hourlyRate(),
                null,
                null
        );

        List<TimesheetDto> timesheets1 = List.of(timesheet1);
        List<TimesheetDto> timesheets2 = List.of(timesheet2);

        InvoiceDto invoice1 = new InvoiceDto(
                1L,
                client1.id(),
                client1.clientName(),
                "INV-001",
                lastDayOfMonth,
                BigDecimal.valueOf(100),
                null,
                List.of(),
                null,
                null
        );

        InvoiceDto invoice2 = new InvoiceDto(
                2L,
                client2.id(),
                client2.clientName(),
                "INV-002",
                lastDayOfMonth,
                BigDecimal.valueOf(180),
                null,
                List.of(),
                null,
                null
        );

        // when
        when(clientService.getAllClients()).thenReturn(List.of(client1, client2));
        when(timesheetService.getMonthlyTimesheets(client1.id(), year, month))
                .thenReturn(timesheets1);
        when(timesheetService.getMonthlyTimesheets(client2.id(), year, month))
                .thenReturn(timesheets2);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);  // Dodane
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);  // Dodane
        when(clientService.getClientById(client1.id())).thenReturn(client1); // Dodane
        when(clientService.getClientById(client2.id())).thenReturn(client2); // Dodane
        when(invoiceService.createInvoiceFromTimesheets(eq(client1), anyList(), eq(lastDayOfMonth)))
                .thenReturn(invoice1);
        when(invoiceService.createInvoiceFromTimesheets(eq(client2), anyList(), eq(lastDayOfMonth)))
                .thenReturn(invoice2);

        List<InvoiceDto> result = billingService.generateMonthlyInvoices(year, month);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(InvoiceDto::id)
                .containsExactly(invoice1.id(), invoice2.id());

        verify(clientService).getAllClients();
        verify(timesheetService).getMonthlyTimesheets(client1.id(), year, month);
        verify(timesheetService).getMonthlyTimesheets(client2.id(), year, month);
        verify(timesheetService).getTimesheetById(1L); // Dodane
        verify(timesheetService).getTimesheetById(2L); // Dodane
    }

    @Test
    void shouldCreateMonthlyInvoice() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        ClientDto client = new ClientDto(clientId, "Test Client", 50.0, 1L, "Street", "City", "12345", "test@email.com");

        TimesheetDto timesheet = new TimesheetDto(
                1L,
                client.clientName(),
                LocalDate.of(2024, 1, 15),
                2.0,
                false,
                client.id(),
                client.hourlyRate(),
                null,
                null
        );

        InvoiceDto expectedInvoice = new InvoiceDto(
                1L,
                clientId,
                client.clientName(),
                "INV-001",
                lastDayOfMonth,
                BigDecimal.valueOf(100),
                null,
                List.of(),
                null,
                null
        );

        // when
        when(clientService.getClientById(clientId)).thenReturn(client);
        when(timesheetService.getMonthlyTimesheets(clientId, year, month))
                .thenReturn(List.of(timesheet));
        when(timesheetService.getTimesheetById(timesheet.id())).thenReturn(timesheet);  // Dodane - kluczowe!
        when(invoiceService.createInvoiceFromTimesheets(eq(client), anyList(), eq(lastDayOfMonth)))
                .thenReturn(expectedInvoice);

        InvoiceDto result = billingService.createMonthlyInvoice(clientId, year, month);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(expectedInvoice.id());
        assertThat(result.clientId()).isEqualTo(clientId);
        assertThat(result.invoiceNumber()).isEqualTo(expectedInvoice.invoiceNumber());

        verify(clientService).getClientById(clientId);
        verify(timesheetService).getMonthlyTimesheets(clientId, year, month);
        verify(timesheetService).getTimesheetById(timesheet.id());  // Dodane
        verify(invoiceService).createInvoiceFromTimesheets(eq(client), anyList(), eq(lastDayOfMonth));
    }

    @Test
    void shouldThrowExceptionWhenNoUnbilledTimesheetsFound() {
        // given
        Long clientId = 1L;
        int year = 2024;
        int month = 1;

        when(timesheetService.getMonthlyTimesheets(clientId, year, month))
                .thenReturn(List.of());

        // when/then
        assertThatThrownBy(() -> billingService.createMonthlyInvoice(clientId, year, month))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No uninvoiced timesheets found for this period");
    }

    @Test
    void shouldCreateInvoiceFromSelectedTimesheets() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto client = new ClientDto(clientId, "Test Client", 50.0, 1L, "Street", "City", "12345", "test@email.com");

        TimesheetDto timesheet1 = new TimesheetDto(
                1L,
                client.clientName(),
                LocalDate.of(2024, 1, 15),
                2.0,
                false,
                client.id(),
                client.hourlyRate(),
                null,
                null
        );

        TimesheetDto timesheet2 = new TimesheetDto(
                2L,
                client.clientName(),
                LocalDate.of(2024, 1, 16),
                3.0,
                false,
                client.id(),
                client.hourlyRate(),
                null,
                null
        );

        InvoiceDto expectedInvoice = new InvoiceDto(
                1L,
                clientId,
                client.clientName(),
                "INV-001",
                issueDate,
                BigDecimal.valueOf(250),
                null,
                List.of(),
                null,
                null
        );

        when(clientService.getClientById(clientId)).thenReturn(client);
        // Upewniamy się, że oba wywołania getTimesheetById są zmockowane
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);
        when(invoiceService.createInvoiceFromTimesheets(eq(client), anyList(), eq(issueDate)))
                .thenReturn(expectedInvoice);

        // when
        InvoiceDto result = billingService.createInvoice(clientId, issueDate, timesheetIds);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(expectedInvoice.id());
        verify(timesheetService).getTimesheetById(1L);  // Weryfikujemy wywołania
        verify(timesheetService).getTimesheetById(2L);
    }

    @Test
    void shouldThrowExceptionWhenNoTimesheetsSelectedForInvoice() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of();

        // when/then
        assertThatThrownBy(() -> billingService.createInvoice(clientId, issueDate, timesheetIds))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No timesheets selected for invoice");
    }

    @Test
    void shouldThrowExceptionWhenAllSelectedTimesheetsAreAlreadyInvoiced() {
        // given
        Long clientId = 1L;
        LocalDate issueDate = LocalDate.now();
        List<Long> timesheetIds = List.of(1L, 2L);

        ClientDto client = new ClientDto(clientId, "Test Client", 50.0, 1L, "Street", "City", "12345", "test@email.com");

        TimesheetDto timesheet1 = new TimesheetDto(
                1L,
                client.clientName(),
                LocalDate.of(2024, 1, 15),
                2.0,
                true, // already invoiced
                client.id(),
                client.hourlyRate(),
                "001-01-2024",
                null
        );

        TimesheetDto timesheet2 = new TimesheetDto(
                2L,
                client.clientName(),
                LocalDate.of(2024, 1, 16),
                3.0,
                true, // already invoiced
                client.id(),
                client.hourlyRate(),
                "001-01-2024",
                null
        );

        when(clientService.getClientById(clientId)).thenReturn(client);
        when(timesheetService.getTimesheetById(1L)).thenReturn(timesheet1);
        when(timesheetService.getTimesheetById(2L)).thenReturn(timesheet2);

        // when/then
        assertThatThrownBy(() -> billingService.createInvoice(clientId, issueDate, timesheetIds))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("All selected timesheets are already invoiced");
    }
}
