-- Add default_seller_id to users table
-- Allows users to have a default seller that auto-populates when creating timesheets

ALTER TABLE users
ADD COLUMN default_seller_id BIGINT AFTER email,
ADD CONSTRAINT fk_user_default_seller
    FOREIGN KEY (default_seller_id) REFERENCES sellers(id)
    ON DELETE SET NULL;

CREATE INDEX idx_user_default_seller ON users(default_seller_id);
