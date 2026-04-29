package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.config.InvoicingSchedulerProperties;
import dev.robgro.timesheet.invoice.InvoiceCreationService;
import dev.robgro.timesheet.invoice.InvoiceDto;
import dev.robgro.timesheet.invoice.delivery.InvoiceDeliveryJobRepository;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for InvoicingTaskServiceImpl — per-seller invoicing with explicit (sellerId, period).
 *
 * CRITICAL notes:
 * - Constructor injection with @Qualifier means @InjectMocks doesn't work → manual construction.
 * - The service receives period from caller (scheduler), does NOT compute it internally.
 * - issueDate = period.atEndOfMonth() (e.g. 2026-01 → 2026-01-31).
 * - LENIENT strictness: uninvoicedTimesheet stubs in setUp are intentionally shared but not
 *   used in all tests (e.g. shouldSkipInactiveClients, shouldHandleNoActiveClients).
 * - Delivery is now async — InvoicingTaskServiceImpl only queues jobs, no sync PDF/send.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoicingTaskService - Per-Seller Monthly Invoicing")
class InvoicingTaskServiceImplTest {

    @Mock private InvoiceCreationService invoiceCreationService;
    @Mock private InvoiceDeliveryJobRepository deliveryJobRepository;
    @Mock private ClientService clientService;
    @Mock private TimesheetService timesheetService;
    @Mock private AdminNotificationService notificationService;
    @Mock private InvoicingSchedulerProperties properties;

    private InvoicingTaskServiceImpl invoicingTaskService;

    private static final Long SELLER_ID = 1L;
    private static final YearMonth PERIOD = YearMonth.of(2026, 1);

    private ClientDto activeClient;
    private InvoiceDto testInvoice;
    private TimesheetDto uninvoicedTimesheet;

    @BeforeEach
    void setUp() {
        // Manual construction because constructor uses @Qualifier — @InjectMocks can't inject it
        invoicingTaskService = new InvoicingTaskServiceImpl(
                invoiceCreationService, deliveryJobRepository, clientService,
                timesheetService, notificationService, properties);

        activeClient = new ClientDto(
                1L, "Test Client", 50.00, "123",
                "Test Street", "Test City", "12-345",
                "test@client.com", true);

        testInvoice = new InvoiceDto(
                1L, 1L, "Test Client", SELLER_ID, "Test Seller",
                "INV-2026-001", LocalDate.now(),
                new BigDecimal("1000.00"), "/path/to/invoice.pdf",
                Collections.emptyList(), null, null, null, 0, null, "NOT_SENT", null, null, null, false);

        uninvoicedTimesheet = mock(TimesheetDto.class);
        when(uninvoicedTimesheet.invoiced()).thenReturn(false);
        when(uninvoicedTimesheet.id()).thenReturn(10L);

        lenient().when(properties.isSendSummaryEmail()).thenReturn(false);
        lenient().when(properties.isSendEmptyClientWarning()).thenReturn(false);
    }

    @Nested
    @DisplayName("Normal Operation")
    class NormalOperationTests {

        @Test
        @DisplayName("Should create invoice and queue for async delivery")
        void shouldCreateAndQueueInvoice_whenClientHasUninvoicedTimesheets() {
            // given
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1))
                    .thenReturn(List.of(uninvoicedTimesheet));
            when(invoiceCreationService.createInvoice(eq(1L), eq(SELLER_ID), any(LocalDate.class), anyList()))
                    .thenReturn(testInvoice);

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then — delivery is async, invoice creation counts as success
            assertThat(summary.successfulInvoices()).isEqualTo(1);
            assertThat(summary.failedInvoices()).isEqualTo(0);
            assertThat(summary.totalInvoices()).isEqualTo(1);
            verify(invoiceCreationService).createInvoice(
                    eq(1L), eq(SELLER_ID), any(LocalDate.class), eq(List.of(10L)));
            // runId=null via default method → deliveryJobRepository NOT called
            verifyNoInteractions(deliveryJobRepository);
        }

        @Test
        @DisplayName("Should skip client when all timesheets are already invoiced")
        void shouldSkipClient_whenAllTimesheetsAlreadyInvoiced() {
            // given
            TimesheetDto invoicedTs = mock(TimesheetDto.class);
            when(invoicedTs.invoiced()).thenReturn(true);
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1))
                    .thenReturn(List.of(invoicedTs));

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            assertThat(summary.totalInvoices()).isEqualTo(0);
            verifyNoInteractions(invoiceCreationService);
        }

        @Test
        @DisplayName("Should skip inactive clients entirely")
        void shouldSkipInactiveClients() {
            // given
            ClientDto inactiveClient = new ClientDto(
                    2L, "Inactive", 50.00, "456",
                    "Street", "City", "00-000", "x@test.com", false);
            when(clientService.getAllClients()).thenReturn(List.of(inactiveClient));

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            assertThat(summary.totalInvoices()).isEqualTo(0);
            verifyNoInteractions(invoiceCreationService);
            verifyNoInteractions(timesheetService);
        }

        @Test
        @DisplayName("Should use period.atEndOfMonth() as issueDate")
        void shouldUsePeriodAtEndOfMonthAsIssueDate() {
            // given
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1))
                    .thenReturn(List.of(uninvoicedTimesheet));
            when(invoiceCreationService.createInvoice(any(), any(), any(), any()))
                    .thenReturn(testInvoice);

            // when
            invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, YearMonth.of(2026, 1));

            // then - January 2026 → issueDate = 2026-01-31
            verify(invoiceCreationService).createInvoice(
                    any(), any(), eq(LocalDate.of(2026, 1, 31)), any());
        }

        @Test
        @DisplayName("Should handle no active clients gracefully")
        void shouldHandleNoActiveClients() {
            // given
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            assertThat(summary.totalInvoices()).isEqualTo(0);
            assertThat(summary.successfulInvoices()).isEqualTo(0);
            assertThat(summary.failedInvoices()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should continue processing remaining clients when one fails")
        void shouldContinueProcessing_whenOneClientFails() {
            // given
            ClientDto secondClient = new ClientDto(
                    2L, "Second Client", 50.00, "456",
                    "Street", "City", "00-000", "second@test.com", true);
            TimesheetDto ts2 = mock(TimesheetDto.class);
            when(ts2.invoiced()).thenReturn(false);
            when(ts2.id()).thenReturn(20L);
            InvoiceDto invoice2 = new InvoiceDto(
                    2L, 2L, "Second Client", SELLER_ID, "Seller",
                    "INV-2026-002", LocalDate.now(), BigDecimal.TEN, "/path2",
                    Collections.emptyList(), null, null, null, 0, null, "NOT_SENT", null, null, null, false);

            when(clientService.getAllClients()).thenReturn(List.of(activeClient, secondClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1)).thenReturn(List.of(uninvoicedTimesheet));
            when(timesheetService.getMonthlyTimesheets(2L, 2026, 1)).thenReturn(List.of(ts2));
            when(invoiceCreationService.createInvoice(eq(1L), any(), any(), any()))
                    .thenThrow(new RuntimeException("DB error for client 1"));
            when(invoiceCreationService.createInvoice(eq(2L), any(), any(), any()))
                    .thenReturn(invoice2);

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then — client 1 fails (creation), client 2 succeeds
            assertThat(summary.totalInvoices()).isEqualTo(1);
            assertThat(summary.successfulInvoices()).isEqualTo(1);
            assertThat(summary.failedInvoices()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should notify admin when invoice creation fails")
        void shouldNotifyAdmin_whenInvoiceCreationFails() {
            // given
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1))
                    .thenReturn(List.of(uninvoicedTimesheet));
            when(invoiceCreationService.createInvoice(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Creation failed"));

            // when
            invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            verify(notificationService).sendErrorNotification(
                    contains("Invoice Creation Error"),
                    anyString(),
                    any(RuntimeException.class));
        }
    }

    @Nested
    @DisplayName("Admin Notifications")
    class AdminNotificationTests {

        @Test
        @DisplayName("Should send summary email when enabled")
        void shouldSendSummaryEmail_whenEnabled() {
            // given
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());
            when(properties.isSendSummaryEmail()).thenReturn(true);

            // when
            invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            verify(notificationService).sendSummaryNotification(any(InvoicingSummary.class));
        }

        @Test
        @DisplayName("Should not send summary email when disabled")
        void shouldNotSendSummaryEmail_whenDisabled() {
            // given
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());
            when(properties.isSendSummaryEmail()).thenReturn(false);

            // when
            invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            verify(notificationService, never()).sendSummaryNotification(any());
        }

        @Test
        @DisplayName("Should warn admin about active client with no timesheets")
        void shouldWarnAdmin_whenActiveClientHasNoTimesheets() {
            // given
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1))
                    .thenReturn(Collections.emptyList());
            when(properties.isSendEmptyClientWarning()).thenReturn(true);

            // when
            invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            verify(notificationService).sendEmptyClientWarning(
                    argThat(list -> list.contains("Test Client")));
        }

        @Test
        @DisplayName("Should not warn when empty client warning is disabled")
        void shouldNotWarn_whenEmptyClientWarningDisabled() {
            // given
            when(clientService.getAllClients()).thenReturn(List.of(activeClient));
            when(timesheetService.getMonthlyTimesheets(1L, 2026, 1))
                    .thenReturn(Collections.emptyList());
            when(properties.isSendEmptyClientWarning()).thenReturn(false);

            // when
            invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            verify(notificationService, never()).sendEmptyClientWarning(any());
        }
    }

    @Nested
    @DisplayName("Summary Construction")
    class SummaryConstructionTests {

        @Test
        @DisplayName("Summary should reflect the period passed by caller")
        void summaryShouldReflectCallerPeriod() {
            // given
            YearMonth customPeriod = YearMonth.of(2025, 6);
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, customPeriod);

            // then — period is caller-provided, not computed internally
            assertThat(summary.previousMonth()).isEqualTo(customPeriod);
        }

        @Test
        @DisplayName("Summary should have non-null execution time")
        void summaryShouldHaveNonNullExecutionTime() {
            // given
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            assertThat(summary.executionTime()).isNotNull();
        }

        @Test
        @DisplayName("Summary should carry runId when provided")
        void summaryShouldCarryRunId_whenProvided() {
            // given
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());
            String runId = "test-run-id-123";

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD, runId);

            // then
            assertThat(summary.runId()).isEqualTo(runId);
        }

        @Test
        @DisplayName("Summary runId should be null when called via default method")
        void summaryShouldHaveNullRunId_whenCalledViaDefaultMethod() {
            // given
            when(clientService.getAllClients()).thenReturn(Collections.emptyList());

            // when
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(SELLER_ID, PERIOD);

            // then
            assertThat(summary.runId()).isNull();
        }
    }
}
