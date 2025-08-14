-- Flyway migration: create sessions table
-- Note: Using IF NOT EXISTS for idempotency in environments where the table may already exist

CREATE TABLE IF NOT EXISTS sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    permissions TEXT[] NOT NULL DEFAULT '{}'
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_last_activity ON sessions(last_activity);
CREATE INDEX idx_sessions_login_time ON sessions(login_time);
