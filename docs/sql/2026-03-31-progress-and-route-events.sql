-- Manual SQL migration for production environments.
-- Creates tables for server-side user progress and route reward flow.

CREATE TABLE IF NOT EXISTS user_progress_profiles (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    snapshot_json TEXT,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_progress_profiles_email
    ON user_progress_profiles (lower(email));

CREATE TABLE IF NOT EXISTS route_completion_events (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    route_id BIGINT NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_route_completion_email
    ON route_completion_events (lower(email));
CREATE INDEX IF NOT EXISTS idx_route_completion_route
    ON route_completion_events (route_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_route_completion_email_route
    ON route_completion_events (lower(email), route_id);

CREATE TABLE IF NOT EXISTS reward_redemption_events (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    route_id BIGINT NOT NULL,
    used_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reward_redemption_email
    ON reward_redemption_events (lower(email));
CREATE INDEX IF NOT EXISTS idx_reward_redemption_route
    ON reward_redemption_events (route_id);
