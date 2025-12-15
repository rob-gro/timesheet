-- =====================================================
-- EMAIL TRACKING - COMPLETE DATABASE MIGRATION
-- Execute this on BOTH databases:
-- 1. robgro_test_invoices (TEST)
-- 2. robgro_aga_invoices (PROD)
-- =====================================================

START TRANSACTION;

-- =====================================================
-- STEP 1: Create email_tracking table
-- =====================================================

CREATE TABLE IF NOT EXISTS email_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    tracking_token VARCHAR(255) NOT NULL UNIQUE,

    -- Tracking data
    opened_at DATETIME,              -- First email open timestamp
    last_opened_at DATETIME,         -- Most recent email open timestamp
    open_count INT DEFAULT 0,        -- Number of times email was opened

    -- Metadata
    ip_address VARCHAR(50),          -- Client IP address (last open)
    user_agent VARCHAR(500),         -- Browser/email client (last open)
    device_type VARCHAR(50),         -- Mobile/Desktop/Tablet (detected from user agent)
    email_client VARCHAR(100),       -- Email client name (Gmail, Outlook, etc.)

    -- Token expiry
    expires_at DATETIME NOT NULL,    -- Token expiration date (90 days from creation)

    -- Audit
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key
    CONSTRAINT fk_email_tracking_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE CASCADE,

    -- Indexes
    INDEX idx_tracking_token (tracking_token),
    INDEX idx_invoice_id (invoice_id),
    INDEX idx_opened_at (opened_at),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- STEP 2: Alter invoices table - add tracking fields
-- =====================================================

ALTER TABLE invoices
ADD COLUMN IF NOT EXISTS email_tracking_token VARCHAR(255) AFTER email_sent_at,
ADD COLUMN IF NOT EXISTS email_opened_at DATETIME AFTER email_tracking_token,
ADD COLUMN IF NOT EXISTS email_open_count INT DEFAULT 0 AFTER email_opened_at,
ADD COLUMN IF NOT EXISTS last_email_opened_at DATETIME AFTER email_open_count;

-- =====================================================
-- STEP 3: Create indexes on invoices table
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_email_tracking_token ON invoices(email_tracking_token);
CREATE INDEX IF NOT EXISTS idx_email_opened_at ON invoices(email_opened_at);

-- =====================================================
-- STEP 4: Verification
-- =====================================================

-- Show email_tracking table structure
SELECT 'email_tracking table created:' AS status;
SHOW CREATE TABLE email_tracking;

-- Show new columns in invoices table
SELECT 'New columns added to invoices:' AS status;
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'invoices'
  AND COLUMN_NAME IN ('email_tracking_token', 'email_opened_at', 'email_open_count', 'last_email_opened_at')
ORDER BY ORDINAL_POSITION;

-- Show foreign key constraint
SELECT 'Foreign key constraint:' AS status;
SELECT
    CONSTRAINT_NAME,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND CONSTRAINT_NAME = 'fk_email_tracking_invoice';

SELECT '✅ Email tracking schema created successfully!' AS result;
SELECT 'Review the output above. If everything looks good, type: COMMIT;' AS next_step;
SELECT 'If there are errors, type: ROLLBACK;' AS alternative;

-- DO NOT AUTO-COMMIT
-- Manually review output and then execute:
-- COMMIT;   (if everything is OK)
-- OR
-- ROLLBACK; (if there are errors)
