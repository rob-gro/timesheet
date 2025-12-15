-- ROLLBACK Script
-- Use this ONLY if you need to completely remove email tracking feature
-- WARNING: This will delete all tracking data permanently!

-- Step 1: Drop foreign key constraint first
ALTER TABLE email_tracking DROP FOREIGN KEY IF EXISTS fk_email_tracking_invoice;

-- Step 2: Drop email_tracking table
DROP TABLE IF EXISTS email_tracking;

-- Step 3: Remove tracking columns from invoices table
ALTER TABLE invoices
DROP COLUMN IF EXISTS last_email_opened_at,
DROP COLUMN IF EXISTS email_open_count,
DROP COLUMN IF EXISTS email_opened_at,
DROP COLUMN IF EXISTS email_tracking_token;

-- Step 4: Verify cleanup
SELECT 'Rollback completed. Email tracking schema removed.' AS status;
