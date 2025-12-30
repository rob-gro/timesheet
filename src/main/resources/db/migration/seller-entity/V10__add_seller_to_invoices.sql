-- Add seller_id to invoices table
-- Links each invoice to a seller

ALTER TABLE invoices
ADD COLUMN seller_id BIGINT AFTER client_id,
ADD CONSTRAINT fk_invoice_seller
    FOREIGN KEY (seller_id) REFERENCES sellers(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_invoice_seller ON invoices(seller_id);
