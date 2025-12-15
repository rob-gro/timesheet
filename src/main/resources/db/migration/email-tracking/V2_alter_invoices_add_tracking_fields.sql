-- Alter Invoices Table
-- Add email tracking denormalized fields for quick access

ALTER TABLE invoices
ADD COLUMN IF NOT EXISTS email_tracking_token VARCHAR(255) AFTER email_sent_at,
ADD COLUMN IF NOT EXISTS email_opened_at DATETIME AFTER email_tracking_token,
ADD COLUMN IF NOT EXISTS email_open_count INT DEFAULT 0 AFTER email_opened_at,
ADD COLUMN IF NOT EXISTS last_email_opened_at DATETIME AFTER email_open_count;

-- Add indexes for tracking fields
CREATE INDEX IF NOT EXISTS idx_email_tracking_token ON invoices(email_tracking_token);
CREATE INDEX IF NOT EXISTS idx_email_opened_at ON invoices(email_opened_at);
