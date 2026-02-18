-- Add UNIQUE constraint on (seller_id, invoice_number)
-- Ensures no duplicate invoice numbers per tenant (SaaS multi-tenant isolation)
-- This is the "gold standard" for SaaS invoice numbering

-- Add UNIQUE constraint
ALTER TABLE invoices
ADD UNIQUE KEY unique_seller_invoice_number (seller_id, invoice_number);

-- Verification: Check for any existing duplicates (should return 0 rows)
-- SELECT seller_id, invoice_number, COUNT(*) as count
-- FROM invoices
-- GROUP BY seller_id, invoice_number
-- HAVING count > 1;
