package dev.robgro.timesheet.invoice;

/**
 * Status of invoice numbering scheme.
 * Determines if scheme can be used for new invoices or only for backdated ones.
 */
public enum SchemeStatus {
    /**
     * Active scheme - used for new invoices created today or in the future
     */
    ACTIVE,

    /**
     * Archived scheme - no longer used for new invoices,
     * but still available for backdated invoices to maintain historical accuracy
     */
    ARCHIVED,

    /**
     * Draft scheme - not yet effective, used for planning future numbering changes
     */
    DRAFT;

    /**
     * Returns human-readable description of the status
     */
    public String getDescription() {
        return switch (this) {
            case ACTIVE -> "Active - used for new invoices";
            case ARCHIVED -> "Archived - available for backdated invoices";
            case DRAFT -> "Draft - not yet effective";
        };
    }
}
