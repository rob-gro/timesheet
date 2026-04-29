ALTER TABLE invoices
    ADD COLUMN cancelled_at DATETIME NULL,
    ADD COLUMN cancelled_by VARCHAR(255) NULL;