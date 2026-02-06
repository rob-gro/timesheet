-- Add Component Columns to Invoices
-- Enables component-based invoice numbering (sortable, resetable)

ALTER TABLE invoices
-- Component fields (nullable initially - backfill in V21, NOT NULL in V23)
ADD COLUMN sequence_number INT COMMENT 'Sequence within period (e.g., 1, 2, 3...)',
ADD COLUMN period_year SMALLINT COMMENT 'Year component for sorting/reset (e.g., 2026)',
ADD COLUMN period_month TINYINT DEFAULT 0 COMMENT 'Month: 1-12 for MONTHLY reset, 0 for YEARLY/NEVER',
ADD COLUMN department_id BIGINT COMMENT 'Department (v1 feature, FK added in V20)',
ADD COLUMN invoice_number_display VARCHAR(64) COMMENT 'Human-readable formatted number (e.g., 001-01-2026)';

-- Performance indexes
-- CRITICAL: Composite index for MAX(sequence_number) queries
CREATE INDEX idx_seller_period_seq ON invoices(seller_id, period_year, period_month, sequence_number);

-- Search by display number
CREATE INDEX idx_invoice_number_display ON invoices(invoice_number_display);

-- NOTE: UNIQUE constraint added in V23 (after backfill ensures no NULLs)
-- NOTE: FK for department_id added in V20 (after departments table created)
