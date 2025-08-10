-- Flyway migration: create sessions table
-- Note: Using IF NOT EXISTS for idempotency in environments where the table may already exist

CREATE TABLE IF NOT EXISTS sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT,
    session_data TEXT,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    last_accessed TIMESTAMP
);
