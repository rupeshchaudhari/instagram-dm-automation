-- ============================================
-- Instagram DM Automation — Bootstrap Schema
-- Executed on first PostgreSQL container start
-- ============================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================
-- USERS
-- ============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(255),
    plan_tier       VARCHAR(20)  NOT NULL DEFAULT 'free'
                        CHECK (plan_tier IN ('free', 'pro', 'enterprise')),
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================
-- INSTAGRAM_ACCOUNTS
-- ============================================
CREATE TABLE instagram_accounts (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ig_user_id              VARCHAR(100) NOT NULL UNIQUE,
    ig_username             VARCHAR(100),
    page_id                 VARCHAR(100),
    access_token_encrypted  TEXT NOT NULL,
    token_expires_at        TIMESTAMP WITH TIME ZONE,
    is_connected            BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ig_accounts_user ON instagram_accounts(user_id);
CREATE INDEX idx_ig_accounts_ig_user ON instagram_accounts(ig_user_id);

-- ============================================
-- AUTOMATION_RULES
-- ============================================
CREATE TABLE automation_rules (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ig_account_id     UUID NOT NULL REFERENCES instagram_accounts(id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    trigger_type      VARCHAR(30) NOT NULL DEFAULT 'keyword'
                          CHECK (trigger_type IN ('keyword', 'any_comment', 'first_comment')),
    trigger_keywords  JSONB DEFAULT '[]'::jsonb,
    dm_template       TEXT NOT NULL,
    is_active         BOOLEAN NOT NULL DEFAULT true,
    daily_dm_limit    INTEGER NOT NULL DEFAULT 100,
    dm_sent_today     INTEGER NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rules_ig_account ON automation_rules(ig_account_id);
CREATE INDEX idx_rules_active ON automation_rules(ig_account_id, is_active) WHERE is_active = true;

-- ============================================
-- INTERACTION_LOGS
-- ============================================
CREATE TABLE interaction_logs (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id                 UUID REFERENCES automation_rules(id) ON DELETE SET NULL,
    ig_comment_id           VARCHAR(100) NOT NULL UNIQUE,
    ig_commenter_id         VARCHAR(100) NOT NULL,
    ig_commenter_username   VARCHAR(100),
    comment_text            TEXT,
    media_id                VARCHAR(100),
    status                  VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
                                CHECK (status IN ('QUEUED','SENT','FAILED','SKIPPED','RATE_LIMITED')),
    dm_content_sent         TEXT,
    error_message           TEXT,
    retry_count             INTEGER NOT NULL DEFAULT 0,
    dm_sent_at              TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_logs_rule ON interaction_logs(rule_id);
CREATE INDEX idx_logs_commenter ON interaction_logs(ig_commenter_id);
CREATE INDEX idx_logs_status ON interaction_logs(status);
CREATE INDEX idx_logs_created ON interaction_logs(created_at DESC);
CREATE INDEX idx_logs_comment_id ON interaction_logs(ig_comment_id);

-- ============================================
-- TRIGGER: auto-update updated_at
-- ============================================
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_updated_at_users
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_ig_accounts
    BEFORE UPDATE ON instagram_accounts
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();

CREATE TRIGGER set_updated_at_rules
    BEFORE UPDATE ON automation_rules
    FOR EACH ROW
    EXECUTE FUNCTION trigger_set_updated_at();
