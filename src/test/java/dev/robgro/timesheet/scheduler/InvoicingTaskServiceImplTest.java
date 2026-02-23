package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.config.InvoicingSchedulerProperties;
import dev.robgro.timesheet.invoice.BillingService;
import dev.robgro.timesheet.invoice.InvoiceDto;
import dev.robgro.timesheet.invoice.InvoiceService;
import dev.robgro.timesheet.invoice.PrintMode;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Tests for InvoicingTaskServiceImpl
 *
 * CRITICAL: Tests edge case of year boundary (January → December of previous year)
 * when calculating previous month for invoice generation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvoicingTaskService - Automated Monthly Invoicing")
class InvoicingTaskServiceImplTest {

    @Mock
    private BillingService billingService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private ClientService clientService;

    @Mock
    private TimesheetService timesheetService;

    @Mock
    private AdminNotificationService notificationService;

    @Mock
    private InvoicingSchedulerProperties properties;

    @InjectMocks
    private InvoicingTaskServiceImpl invoicingTaskService;

    private ClientDto activeClient;
    private InvoiceDto testInvoice;

    @BeforeEach
    void setUp() {
        // Setup common test data
        activeClient = new ClientDto(
                1L,
                "Test Client",
                50.00,
                "123",
                "Test Street",
                "Test City",
                "12-345",
                "test@client.com",
                true
        );

        testInvoice = new InvoiceDto(
                1L,
                1L,
                "Test Client",
                1L,
                "Test Seller",
                "INV-2025-001",
                LocalDate.now(),
                new BigDecimal("1000.00"),
                "/path/to/invoice.pdf",
                Collections.emptyList(),
                null, null, null, 0, null, "NOT_SENT"
        );

        // Default properties behavior (lenient - not all tests need these)
        lenient().when(properties.isSendSummaryEmail()).thenReturn(false);
        lenient().when(properties.isSendEmptyClientWarning()).thenReturn(false);
    }

    @Nested
    @DisplayName("CRITICAL: Year Boundary Handling (January → December)")
    class YearBoundaryTests {

        @Test
        @DisplayName("CRITICAL: YearMonth.minusMonths(1) must handle year boundaries correctly")
        void yearMonthMinusMonthsMustHandleYearBoundaries() {
            // This test verifies the CRITICAL year boundary logic used by CRON
            // When CRON runs on January 1st, it MUST look back to December of PREVIOUS year

            // CRITICAL CASE: January → December (year change!)
            YearMonth january = YearMonth.of(2026, 1);
            YearMonth previousMonth = january.minusMonths(1);

            assertThat(previousMonth.getYear())
                    .as("CRITICAL: January 2026 minus 1 month MUST be year 2025, NOT 2026!")
                    .isEqualTo(2025);

            assertThat(previousMonth.getMonthValue())
                    .as("CRITICAL: Month MUST be 12 (December), NOT 0 or 13!")
                    .isEqualTo(12);

            assertThat(previousMonth)
                    .as("January 2026 - 1 month = December 2025")
                    .isEqualTo(YearMonth.of(2025, 12));

            // Verify other months work correctly (no year change)
            assertThat(YearMonth.of(2026, 2).minusMonths(1))
                    .as("February → January (same year)")
                    .isEqualTo(YearMonth.of(2026, 1));

            assertThat(YearMonth.of(2026, 3).minusMonths(1))
                    .as("March → February (same year)")
                    .isEqualTo(YearMonth.of(2026, 2));

            assertThat(YearMonth.of(2025, 12).minusMonths(1))
                    .as("December → November (same year)")
                    .isEqualTo(YearMonth.of(2025, 11));
        }

        @Test
        @DisplayName("Service should calculate previous month correctly")
        void serviceShouldCalculatePreviousMonthCorrectly() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            YearMonth expectedPreviousMonth = YearMonth.now().minusMonths(1);
            assertThat(summary.previousMonth())
                    .as("Summary should contain correct previous month")
                    .isEqualTo(expectedPreviousMonth);

            verify(billingService).generateMonthlyInvoices(
                    expectedPreviousMonth.getYear(),
                    expectedPreviousMonth.getMonthValue()
            );
        }
    }

    @Nested
    @DisplayName("Normal Operation")
    class NormalOperationTests {

        @Test
        @DisplayName("Should successfully generate and process invoices")
        void shouldSuccessfullyGenerateAndProcessInvoices() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(List.of(testInvoice));
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());
            doNothing().when(invoiceService).savePdfAndSendInvoice(anyLong(), any());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            assertThat(summary.totalInvoices()).isEqualTo(1);
            assertThat(summary.successfulInvoices()).isEqualTo(1);
            assertThat(summary.failedInvoices()).isEqualTo(0);

            verify(billingService).generateMonthlyInvoices(anyInt(), anyInt());
            verify(invoiceService).savePdfAndSendInvoice(eq(1L), eq(PrintMode.ORIGINAL));
        }

        @Test
        @DisplayName("Should handle invoice processing failure gracefully")
        void shouldHandleInvoiceProcessingFailure() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(List.of(testInvoice));
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            doThrow(new RuntimeException("PDF generation failed"))
                    .when(invoiceService).savePdfAndSendInvoice(anyLong(), any());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            assertThat(summary.totalInvoices()).isEqualTo(1);
            assertThat(summary.successfulInvoices()).isEqualTo(0);
            assertThat(summary.failedInvoices()).isEqualTo(1);

            verify(notificationService).sendErrorNotification(
                    contains("Invoice Processing Error"),
                    anyString(),
                    any(Exception.class)
            );
        }

        @Test
        @DisplayName("Should handle no invoices to generate")
        void shouldHandleNoInvoicesToGenerate() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            assertThat(summary.totalInvoices()).isEqualTo(0);
            assertThat(summary.successfulInvoices()).isEqualTo(0);
            assertThat(summary.failedInvoices()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Empty Client Detection")
    class EmptyClientDetectionTests {

        @Test
        @DisplayName("Should detect active clients without timesheets")
        void shouldDetectActiveClientsWithoutTimesheets() {
            // given
            ClientDto clientWithoutTimesheets = new ClientDto(
                    2L, "Empty Client", 50.00, "456",
                    "Empty Street", "Empty City", "67-890",
                    "empty@client.com", true
            );

            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(clientService.getAllClients())
                    .thenReturn(List.of(activeClient, clientWithoutTimesheets));

            // Client 1 has timesheets
            when(timesheetService.getMonthlyTimesheets(eq(1L), anyInt(), anyInt()))
                    .thenReturn(List.of(mock(TimesheetDto.class)));

            // Client 2 has NO timesheets
            when(timesheetService.getMonthlyTimesheets(eq(2L), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());

            when(properties.isSendEmptyClientWarning()).thenReturn(true);

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            assertThat(summary.clientsWithoutTimesheets())
                    .hasSize(1)
                    .contains("Empty Client");

            verify(notificationService).sendEmptyClientWarning(
                    argThat(list -> list.contains("Empty Client"))
            );
        }

        @Test
        @DisplayName("Should not check inactive clients for timesheets")
        void shouldNotCheckInactiveClientsForTimesheets() {
            // given
            ClientDto inactiveClient = new ClientDto(
                    3L, "Inactive Client", 50.00, "789",
                    "Inactive Street", "Inactive City", "11-222",
                    "inactive@client.com", false  // inactive
            );

            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(clientService.getAllClients())
                    .thenReturn(List.of(inactiveClient));

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            assertThat(summary.clientsWithoutTimesheets()).isEmpty();

            // Inactive client should not be checked for timesheets
            verify(timesheetService, never()).getMonthlyTimesheets(eq(3L), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("Admin Notifications")
    class AdminNotificationTests {

        @Test
        @DisplayName("Should send summary email when enabled")
        void shouldSendSummaryEmailWhenEnabled() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(List.of(testInvoice));
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());
            when(properties.isSendSummaryEmail()).thenReturn(true);

            // when
            invoicingTaskService.executeMonthlyInvoicing();

            // then
            verify(notificationService).sendSummaryNotification(any(InvoicingSummary.class));
        }

        @Test
        @DisplayName("Should not send summary email when disabled")
        void shouldNotSendSummaryEmailWhenDisabled() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(List.of(testInvoice));
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());
            when(properties.isSendSummaryEmail()).thenReturn(false);

            // when
            invoicingTaskService.executeMonthlyInvoicing();

            // then
            verify(notificationService, never()).sendSummaryNotification(any());
        }

        @Test
        @DisplayName("Should send empty client warning when enabled and clients found")
        void shouldSendEmptyClientWarningWhenEnabledAndClientsFound() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(anyLong(), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(properties.isSendEmptyClientWarning()).thenReturn(true);

            // when
            invoicingTaskService.executeMonthlyInvoicing();

            // then
            verify(notificationService).sendEmptyClientWarning(anyList());
        }
    }

    @Nested
    @DisplayName("Summary Construction")
    class SummaryConstructionTests {

        @Test
        @DisplayName("Should build correct summary with execution details")
        void shouldBuildCorrectSummaryWithExecutionDetails() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(List.of(testInvoice));
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            assertThat(summary.executionTime()).isNotNull();
            assertThat(summary.previousMonth()).isNotNull();
            assertThat(summary.totalInvoices()).isEqualTo(1);
            assertThat(summary.processingResults()).hasSize(1);
        }

        @Test
        @DisplayName("Summary should contain correct previous month calculation")
        void summaryShouldContainCorrectPreviousMonthCalculation() {
            // given
            when(billingService.generateMonthlyInvoices(anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            // then
            YearMonth expectedPreviousMonth = YearMonth.now().minusMonths(1);
            assertThat(summary.previousMonth())
                    .as("Summary should contain correct previous month")
                    .isEqualTo(expectedPreviousMonth);
        }
    }
}
