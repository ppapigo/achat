-- Additive migration. Run once on a backed-up AChat database, after staging rehearsal.
-- No DROP/TRUNCATE/UPDATE of existing user/game data.
CREATE TABLE auth_sessions (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    password_fingerprint VARCHAR(64) NOT NULL,
    refresh_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BIT NOT NULL,
    version BIGINT,
    PRIMARY KEY (id),
    INDEX idx_auth_session_user (user_id),
    INDEX idx_auth_session_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
