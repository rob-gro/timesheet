package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.seller.Seller;

import java.time.YearMonth;
import java.time.ZonedDateTime;

/**
 * Immutable data carrier for a reserved invoicing run.
 * Created by {@link InvoicingReservationService} after atomic reservation; consumed by
 * {@link InvoicingExecutionService} to execute and record the run.
 */
public record RunContext(
        Long configId,
        Seller seller,
        YearMonth period,
        String runId,
        boolean isRetry,
        ZonedDateTime scheduledTime
) {}