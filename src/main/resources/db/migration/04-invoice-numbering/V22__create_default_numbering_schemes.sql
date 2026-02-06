-- Create Default Numbering Schemes for Existing Sellers
-- Assigns current format ({SEQ:3}-{MM}-{YYYY}, MONTHLY) to all active sellers

INSERT INTO invoice_numbering_schemes (
    seller_id,
    template,
    reset_period,
    effective_from,
    status
)
SELECT
    id AS seller_id,
    '{SEQ:3}-{MM}-{YYYY}' AS template,
    'MONTHLY' AS reset_period,
    '2020-01-01' AS effective_from,  -- Before first invoice (safe baseline)
    'ACTIVE' AS status
FROM sellers
WHERE active = TRUE;

-- Verification: Check schemes created
-- Expected: 1 scheme per active seller
-- SELECT
--     s.name as seller,
--     ins.template,
--     ins.reset_period,
--     ins.effective_from,
--     ins.status
-- FROM invoice_numbering_schemes ins
-- JOIN sellers s ON ins.seller_id = s.id
-- ORDER BY s.id;
