package dev.robgro.timesheet.scheduler;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Immutable summary of monthly invoicing execution
 * Thread-safe with defensive copies for collections
 */
public record InvoicingSummary(
        LocalDateTime executionTime,
        YearMonth previousMonth,
        int totalInvoices,
        int successfulInvoices,
        int failedInvoices,
        List<String> clientsWithoutTimesheets,
        List<InvoiceProcessingResult> processingResults
) {
    /**
     * Canonical constructor with defensive copies for lists
     * Ensures immutability and prevents external modification
     */
    public InvoicingSummary {
        clientsWithoutTimesheets = List.copyOf(clientsWithoutTimesheets);
        processingResults = List.copyOf(processingResults);
    }

    /**
     * Builder pattern for convenience
     * Maintains backward compatibility with existing code
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDateTime executionTime;
        private YearMonth previousMonth;
        private int totalInvoices;
        private int successfulInvoices;
        private int failedInvoices;
        private List<String> clientsWithoutTimesheets;
        private List<InvoiceProcessingResult> processingResults;

        public Builder executionTime(LocalDateTime executionTime) {
            this.executionTime = executionTime;
            return this;
        }

        public Builder previousMonth(YearMonth previousMonth) {
            this.previousMonth = previousMonth;
            return this;
        }

        public Builder totalInvoices(int totalInvoices) {
            this.totalInvoices = totalInvoices;
            return this;
        }

        public Builder successfulInvoices(int successfulInvoices) {
            this.successfulInvoices = successfulInvoices;
            return this;
        }

        public Builder failedInvoices(int failedInvoices) {
            this.failedInvoices = failedInvoices;
            return this;
        }

        public Builder clientsWithoutTimesheets(List<String> clientsWithoutTimesheets) {
            this.clientsWithoutTimesheets = clientsWithoutTimesheets;
            return this;
        }

        public Builder processingResults(List<InvoiceProcessingResult> processingResults) {
            this.processingResults = processingResults;
            return this;
        }

        public InvoicingSummary build() {
            return new InvoicingSummary(
                executionTime,
                previousMonth,
                totalInvoices,
                successfulInvoices,
                failedInvoices,
                clientsWithoutTimesheets,
                processingResults
            );
        }
    }
}
