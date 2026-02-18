-- Create Invoice Number Counters Table
-- Atomic counter management for invoice numbering
-- Prevents concurrency issues and ensures unique sequence numbers per scope

CREATE TABLE invoice_number_counters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL COMMENT 'Tenant isolation - each seller has separate counters',
    reset_period VARCHAR(20) NOT NULL COMMENT 'NEVER, MONTHLY, YEARLY, FISCAL_YEAR',
    period_key VARCHAR(32) NOT NULL COMMENT 'Scope key: NEVER, 2026, 2026-02, 2025-26, COMPANY-2026-02',
    last_value INT NOT NULL DEFAULT 0 COMMENT 'Last used sequence number (atomic increment)',
    fy_start_year INT DEFAULT NULL COMMENT 'Fiscal year start (for filtering/reports)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Unique constraint: one counter per (seller, reset_period, period_key)
    UNIQUE KEY unique_counter_scope (seller_id, reset_period, period_key),

    -- Index for SELECT queries
    INDEX idx_seller_period (seller_id, reset_period, period_key),

    -- Foreign key to sellers
    FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Atomic counters for invoice numbering - prevents concurrency issues';

-- Note: Backfill will be done in V27 after adding UNIQUE constraint to invoices
-- This ensures existing invoices don't cause duplicate numbers

-- Verification query
-- SELECT * FROM invoice_number_counters ORDER BY seller_id, reset_period, period_key;
