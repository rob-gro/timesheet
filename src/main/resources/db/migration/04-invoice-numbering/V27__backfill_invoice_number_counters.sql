-- Backfill invoice_number_counters from existing invoices
-- CRITICAL: Without this, first new invoice will have duplicate number!
-- Derives reset_period and period_key from existing invoice data

-- Backfill counters for MONTHLY reset (period_month > 0)
INSERT INTO invoice_number_counters (seller_id, reset_period, period_key, last_value)
SELECT
    seller_id,
    'MONTHLY' AS reset_period,
    CONCAT(period_year, '-', LPAD(period_month, 2, '0')) AS period_key,
    MAX(sequence_number) AS last_value
FROM invoices
WHERE period_month > 0
GROUP BY seller_id, period_year, period_month
ON DUPLICATE KEY UPDATE last_value = VALUES(last_value);

-- Backfill counters for YEARLY reset (period_month = 0, period_year > 0)
INSERT INTO invoice_number_counters (seller_id, reset_period, period_key, last_value)
SELECT
    seller_id,
    'YEARLY' AS reset_period,
    CAST(period_year AS CHAR) AS period_key,
    MAX(sequence_number) AS last_value
FROM invoices
WHERE period_month = 0 AND period_year > 0
GROUP BY seller_id, period_year
ON DUPLICATE KEY UPDATE last_value = VALUES(last_value);

-- Backfill counters for NEVER reset (continuous numbering)
-- Note: period_year and period_month should be 0 for NEVER
INSERT INTO invoice_number_counters (seller_id, reset_period, period_key, last_value)
SELECT
    seller_id,
    'NEVER' AS reset_period,
    'NEVER' AS period_key,
    MAX(sequence_number) AS last_value
FROM invoices
WHERE period_month = 0 AND period_year = 0
GROUP BY seller_id
ON DUPLICATE KEY UPDATE last_value = VALUES(last_value);

-- Verification: Check counters created
-- SELECT
--     seller_id,
--     reset_period,
--     period_key,
--     last_value,
--     created_at
-- FROM invoice_number_counters
-- ORDER BY seller_id, reset_period, period_key;

-- Verification: Compare with actual invoices
-- SELECT
--     i.seller_id,
--     i.period_year,
--     i.period_month,
--     MAX(i.sequence_number) as max_seq_in_invoices,
--     inc.last_value as counter_value
-- FROM invoices i
-- LEFT JOIN invoice_number_counters inc
--     ON i.seller_id = inc.seller_id
--     AND (
--         (i.period_month > 0 AND inc.reset_period = 'MONTHLY'
--          AND inc.period_key = CONCAT(i.period_year, '-', LPAD(i.period_month, 2, '0')))
--         OR
--         (i.period_month = 0 AND i.period_year > 0 AND inc.reset_period = 'YEARLY'
--          AND inc.period_key = CAST(i.period_year AS CHAR))
--         OR
--         (i.period_month = 0 AND i.period_year = 0 AND inc.reset_period = 'NEVER')
--     )
-- GROUP BY i.seller_id, i.period_year, i.period_month, inc.last_value;
