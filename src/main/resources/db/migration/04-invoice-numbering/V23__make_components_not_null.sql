-- Make Component Columns NOT NULL
-- ⚠️ EXECUTE ONLY AFTER VERIFICATION that V21 backfill succeeded!

-- ========================================
-- STOP! Run verification query first:
-- ========================================
-- SELECT COUNT(*) FROM invoices
-- WHERE invoice_number IS NOT NULL
--   AND (sequence_number IS NULL OR period_year IS NULL OR period_month IS NULL);
--
-- Expected result: 0 rows
-- If result > 0: DO NOT run this migration! Fix data first.
-- ========================================

-- Make components NOT NULL (INT to match Entity)
ALTER TABLE invoices
MODIFY COLUMN sequence_number INT NOT NULL COMMENT 'Sequence within period',
MODIFY COLUMN period_year INT NOT NULL COMMENT 'Year component',
MODIFY COLUMN period_month INT NOT NULL DEFAULT 0 COMMENT 'Month: 1-12 or 0';

-- ========================================
-- Verification: Check seller_id has no NULLs
-- ========================================
-- MUST be 0 before making seller_id NOT NULL
-- SELECT COUNT(*) FROM invoices WHERE seller_id IS NULL;
--
-- Expected: 0 rows
-- If result > 0: ASSIGN seller_id FIRST before running V23!
-- ========================================

-- Enforce seller_id NOT NULL before adding UNIQUE (prevents NULL holes)
ALTER TABLE invoices
MODIFY COLUMN seller_id BIGINT NOT NULL;

-- ========================================
-- Verification: Check for duplicate sequences
-- ========================================
-- SELECT seller_id, period_year, period_month, sequence_number, COUNT(*) as duplicates
-- FROM invoices
-- WHERE sequence_number IS NOT NULL
-- GROUP BY seller_id, period_year, period_month, sequence_number
-- HAVING duplicates > 1;
--
-- Expected: 0 rows
-- If duplicates exist: FIX MANUALLY before adding UNIQUE!
-- ========================================

-- Add UNIQUE constraint to prevent duplicate sequences
-- CRITICAL: Ensures no two invoices have same (seller, year, month, sequence)
ALTER TABLE invoices
ADD UNIQUE KEY unique_invoice_sequence (seller_id, period_year, period_month, sequence_number);

-- Verification: Check constraint exists
-- SELECT
--     CONSTRAINT_NAME,
--     CONSTRAINT_TYPE
-- FROM information_schema.TABLE_CONSTRAINTS
-- WHERE TABLE_NAME = 'invoices'
--   AND CONSTRAINT_NAME = 'unique_invoice_sequence';
