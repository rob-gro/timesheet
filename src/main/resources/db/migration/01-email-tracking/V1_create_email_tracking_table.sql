-- Email Tracking Table
-- Stores tracking information for invoice emails

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
