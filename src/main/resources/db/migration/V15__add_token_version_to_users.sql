-- Add token_version column to users table for JWT token invalidation
-- Part of password reset flow implementation

ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 1;

-- Update existing users to have token_version = 1
UPDATE users SET token_version = 1 WHERE token_version IS NULL;

-- Add comment for documentation
COMMENT ON COLUMN users.token_version IS 'Token version for JWT invalidation. Incremented on password change.';
