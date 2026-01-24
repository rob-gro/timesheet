-- =====================================================
-- EMAIL TRACKING - SQL MIGRATION FOR TEST DATABASE
-- Database: robgro_test_invoices
-- =====================================================

START TRANSACTION;

-- Create email_tracking table
CREATE TABLE IF NOT EXISTS email_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    tracking_token VARCHAR(255) NOT NULL UNIQUE,
    opened_at DATETIME,
    last_opened_at DATETIME,
    open_count INT DEFAULT 0,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    device_type VARCHAR(50),
    email_client VARCHAR(100),
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_tracking_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(id)
        ON DELETE CASCADE,
    INDEX idx_tracking_token (tracking_token),
    INDEX idx_invoice_id (invoice_id),
    INDEX idx_opened_at (opened_at),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add tracking columns to invoices table
ALTER TABLE invoices
ADD COLUMN IF NOT EXISTS email_tracking_token VARCHAR(255) AFTER email_sent_at,
ADD COLUMN IF NOT EXISTS email_opened_at DATETIME AFTER email_tracking_token,
ADD COLUMN IF NOT EXISTS email_open_count INT DEFAULT 0 AFTER email_opened_at,
ADD COLUMN IF NOT EXISTS last_email_opened_at DATETIME AFTER email_open_count;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_email_tracking_token ON invoices(email_tracking_token);
CREATE INDEX IF NOT EXISTS idx_email_opened_at ON invoices(email_opened_at);

-- Verification
SELECT 'email_tracking table created:' AS status;
SHOW CREATE TABLE email_tracking;

SELECT 'New columns added to invoices:' AS status;
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'robgro_aga_invoices'
  AND TABLE_NAME = 'invoices'
  AND COLUMN_NAME IN ('email_tracking_token', 'email_opened_at', 'email_open_count', 'last_email_opened_at')
ORDER BY ORDINAL_POSITION;

SELECT '✅ Email tracking schema created successfully!' AS result;
SELECT 'Review the output above. If everything looks good, type: COMMIT;' AS next_step;
SELECT 'If there are errors, type: ROLLBACK;' AS alternative;

-- DO NOT AUTO-COMMIT
-- Manually review output and then execute:

COMMIT;
# -- OR
-- ROLLBACK; (if there are errors)
