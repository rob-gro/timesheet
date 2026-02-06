-- Backfill Invoice Number Components
-- Populates sequence_number, period_year, period_month from existing invoice_number
-- Handles standard format (NNN-MM-YYYY) + fallback for edge cases

-- ========================================
-- Step 1: Populate display field
-- ========================================
UPDATE invoices
SET invoice_number_display = invoice_number
WHERE invoice_number_display IS NULL
  AND invoice_number IS NOT NULL;

-- ========================================
-- Step 2: Parse standard NNN-MM-YYYY format
-- ========================================
-- Example: "001-01-2026" → seq=1, month=1, year=2026
UPDATE invoices
SET
    sequence_number = CAST(SUBSTRING(invoice_number, 1, 3) AS UNSIGNED),
    period_month = CAST(SUBSTRING(invoice_number, 5, 2) AS UNSIGNED),
    period_year = CAST(SUBSTRING(invoice_number, 8, 4) AS UNSIGNED)
WHERE invoice_number IS NOT NULL
  AND invoice_number REGEXP '^[0-9]{3}-[0-9]{2}-[0-9]{4}$'
  AND sequence_number IS NULL;

-- ========================================
-- Step 3: Fallback for non-standard formats
-- ========================================
-- For invoices that don't match NNN-MM-YYYY (manual edits, imports)
-- Use issue_date + ranking to generate components safely
UPDATE invoices i
JOIN (
    SELECT
        id,
        YEAR(issue_date) as fallback_year,
        MONTH(issue_date) as fallback_month,
        ROW_NUMBER() OVER (
            PARTITION BY YEAR(issue_date), MONTH(issue_date)
            ORDER BY issue_date, id
        ) as fallback_seq
    FROM invoices
    WHERE sequence_number IS NULL
      AND issue_date IS NOT NULL
) fallback ON i.id = fallback.id
SET
    i.sequence_number = fallback.fallback_seq,
    i.period_year = fallback.fallback_year,
    i.period_month = fallback.fallback_month,
    i.invoice_number_display = CONCAT(
        LPAD(fallback.fallback_seq, 3, '0'), '-',
        LPAD(fallback.fallback_month, 2, '0'), '-',
        fallback.fallback_year
    )
WHERE i.sequence_number IS NULL
  AND i.issue_date IS NOT NULL;

-- ========================================
-- Step 4: Verification Query
-- ========================================
-- MANUAL CHECK: Run this query before executing V23
-- Expected result: 0 rows (all invoices have components)

-- SELECT COUNT(*) as remaining_nulls
-- FROM invoices
-- WHERE invoice_number IS NOT NULL
--   AND (sequence_number IS NULL OR period_year IS NULL OR period_month IS NULL);

-- If COUNT > 0: DO NOT proceed to V23! Investigate and fix manually.
