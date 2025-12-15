-- Verification Script
-- Run this to verify that email tracking schema was created correctly

-- Check email_tracking table exists
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'email_tracking';

-- Check email_tracking columns
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'email_tracking'
ORDER BY ORDINAL_POSITION;

-- Check invoices tracking columns
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'invoices'
  AND COLUMN_NAME IN ('email_tracking_token', 'email_opened_at', 'email_open_count', 'last_email_opened_at')
ORDER BY ORDINAL_POSITION;

-- Check foreign key constraint
SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'fk_email_tracking_invoice';
