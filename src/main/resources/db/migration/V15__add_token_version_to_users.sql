-- Add token_version column to users table for JWT token invalidation
-- Part of password reset flow implementation

ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 1;
