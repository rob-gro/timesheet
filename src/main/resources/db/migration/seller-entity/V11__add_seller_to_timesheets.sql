-- Add seller_id to timesheets table
-- REQUIRED for automatic invoice generation via CRON
-- When CRON automatically generates monthly invoices, it needs to know which seller to assign

ALTER TABLE timesheets
ADD COLUMN seller_id BIGINT AFTER client_id,
ADD CONSTRAINT fk_timesheet_seller
    FOREIGN KEY (seller_id) REFERENCES sellers(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_timesheet_seller ON timesheets(seller_id);

-- Note: seller_id will be set to NOT NULL after data migration (V5)
