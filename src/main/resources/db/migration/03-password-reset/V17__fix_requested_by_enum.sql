-- Fix requested_by column type to ENUM for Hibernate compatibility
-- V16 created VARCHAR(10), but entity uses @Enumerated(EnumType.STRING)
-- MariaDB/Hibernate requires ENUM type for proper validation

ALTER TABLE password_reset_tokens
MODIFY COLUMN requested_by ENUM('ADMIN', 'SELF') NOT NULL;
