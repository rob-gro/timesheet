-- Add company_code to sellers table
-- Used for {COMPANY} token in invoice numbering templates

ALTER TABLE sellers
ADD COLUMN company_code VARCHAR(10) DEFAULT NULL
COMMENT 'Short company code for invoice numbering (e.g., ACME, CLN, TS)';

-- Optional: Set default company codes for existing sellers
-- Uses first 4 characters of company name, uppercase
UPDATE sellers
SET company_code = UPPER(LEFT(name, 4))
WHERE company_code IS NULL;

-- Verification
-- SELECT id, name, company_code FROM sellers;
