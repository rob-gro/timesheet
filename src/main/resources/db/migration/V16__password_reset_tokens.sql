-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 hash = 64 hex chars
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    requested_by VARCHAR(10) NOT NULL CHECK (requested_by IN ('ADMIN', 'SELF')),
    request_ip VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,
    reset_version INT NULL,
    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used_at ON password_reset_tokens(used_at);
CREATE INDEX idx_password_reset_tokens_cleanup ON password_reset_tokens(expires_at, used_at);

-- Add audit field to users
ALTER TABLE users ADD COLUMN last_password_reset_at TIMESTAMP NULL;

-- Note: temp_password_expires_at will be removed in a future migration (Step 16)
-- after the new password reset flow is fully implemented and tested
-- For now, keeping it for backward compatibility with existing functionality
