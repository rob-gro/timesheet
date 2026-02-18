package dev.robgro.timesheet.invoice;

import java.time.LocalDate;

/**
 * Internal command object carrying all data needed by TransactionalWorker.
 * Decouples the worker from Spring Security context (sellerId resolved by orchestrator).
 */
public record CreateSchemeCommand(
    Long sellerId,
    String template,
    ResetPeriod resetPeriod,
    LocalDate effectiveFrom
) {}
