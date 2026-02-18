package dev.robgro.timesheet.invoice;

import lombok.Builder;
import lombok.Value;

/**
 * Template context - Value Object (NOT JPA entity!)
 * Holds values for token replacement during invoice number generation.
 *
 * IMPORTANT: This class must NOT depend on JPA entities to keep
 * template rendering logic separate from persistence concerns.
 */
@Value
@Builder
public class TemplateContext {
    /**
     * Sequence number within period (e.g., 1, 2, 3...)
     */
    Integer sequenceNumber;

    /**
     * Year component (e.g., 2026)
     */
    Integer year;

    /**
     * Month component: 1-12 for MONTHLY, 0 for YEARLY/NEVER
     */
    Integer month;

    /**
     * Optional department for multi-department numbering
     * NOTE: Currently accepts JPA entity, but could be replaced with
     * DepartmentVO in future to fully decouple from persistence
     */
    Department department;
}
