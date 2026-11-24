-- PAYWELL core schema. Money columns are NUMERIC(19,2) with an explicit currency; never floating point.
-- Everything here is synthetic-development oriented: no raw card data, no real bank data.

CREATE TABLE users (
    id               UUID PRIMARY KEY,
    email            VARCHAR(254) NOT NULL,
    password_hash    VARCHAR(100) NOT NULL,
    full_name        VARCHAR(120) NOT NULL,
    phone            VARCHAR(20),
    role             VARCHAR(16)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','ADMIN')),
    status           VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
    failed_logins    INT          NOT NULL DEFAULT 0,
    locked_until     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE user_profiles (
    user_id               UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    handle                VARCHAR(40) NOT NULL,
    currency              CHAR(3)     NOT NULL DEFAULT 'INR',
    language              VARCHAR(8)  NOT NULL DEFAULT 'en',
    notify_payments       BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_rewards        BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_bills          BOOLEAN     NOT NULL DEFAULT TRUE,
    notify_security       BOOLEAN     NOT NULL DEFAULT TRUE,
    hide_balance_default  BOOLEAN     NOT NULL DEFAULT FALSE,
    share_analytics       BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX ux_profiles_handle ON user_profiles (lower(handle));

CREATE TABLE auth_sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id           UUID        NOT NULL,
    refresh_token_hash  CHAR(64)    NOT NULL,   -- SHA-256 of the opaque refresh token; the token itself is never stored
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ,
    rotated_at          TIMESTAMPTZ,
    user_agent          VARCHAR(200)
);
CREATE UNIQUE INDEX ux_sessions_token ON auth_sessions (refresh_token_hash);
CREATE INDEX ix_sessions_user ON auth_sessions (user_id);
CREATE INDEX ix_sessions_family ON auth_sessions (family_id);

CREATE TABLE accounts (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    currency    CHAR(3)       NOT NULL,
    balance     NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    kind        VARCHAR(24)   NOT NULL DEFAULT 'DEMO_WALLET',   -- simulated funds; NOT a bank balance
    version     BIGINT        NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE recipients (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    handle      VARCHAR(60)  NOT NULL,
    favorite    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, handle)
);

CREATE TABLE payment_methods (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind        VARCHAR(16) NOT NULL CHECK (kind IN ('BANK_ACCOUNT','CARD')),
    label       VARCHAR(60) NOT NULL,
    last4       VARCHAR(4)  CHECK (last4 ~ '^[0-9]{0,4}$'),     -- metadata only: raw card/account numbers are never stored
    status      VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED' CHECK (status IN ('UNVERIFIED','VERIFIED','DISABLED')),
    is_default  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_pm_user ON payment_methods (user_id);

CREATE TABLE transactions (
    id                       UUID PRIMARY KEY,
    reference                VARCHAR(32)   NOT NULL UNIQUE,         -- reconciliation-ready reference, e.g. PW2611AB12CD34
    user_id                  UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id               UUID          NOT NULL REFERENCES accounts(id),
    direction                VARCHAR(8)    NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    type                     VARCHAR(24)   NOT NULL CHECK (type IN ('P2P_SEND','P2P_RECEIVE','BILL','REWARD','TOPUP','REVERSAL')),
    category                 VARCHAR(24)   NOT NULL,
    amount                   NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency                 CHAR(3)       NOT NULL,
    status                   VARCHAR(16)   NOT NULL CHECK (status IN ('PENDING','SUCCESS','FAILED','REVERSED')),
    counterparty_name        VARCHAR(120),
    counterparty_handle      VARCHAR(60),
    recipient_id             UUID REFERENCES recipients(id) ON DELETE SET NULL,
    note                     VARCHAR(140),
    simulated                BOOLEAN       NOT NULL DEFAULT TRUE,
    failure_reason           VARCHAR(60),
    reverses_transaction_id  UUID REFERENCES transactions(id),
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT now(),
    completed_at             TIMESTAMPTZ
);
CREATE INDEX ix_txn_user_created ON transactions (user_id, created_at DESC);
CREATE INDEX ix_txn_user_status ON transactions (user_id, status);
CREATE INDEX ix_txn_user_category ON transactions (user_id, category);
CREATE INDEX ix_txn_recipient ON transactions (recipient_id);

-- One row per provider attempt. Insert-only: an attempt is history, not state.
CREATE TABLE payment_attempts (
    id                  UUID PRIMARY KEY,
    transaction_id      UUID        NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    provider            VARCHAR(32) NOT NULL,
    provider_reference  VARCHAR(64),
    outcome             VARCHAR(16) NOT NULL,                -- SUCCESS | FAILED | SKIPPED
    failure_code        VARCHAR(60),
    risk_score          INT,
    risk_category       VARCHAR(8),
    risk_action         VARCHAR(12),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_attempt_txn ON payment_attempts (transaction_id);

CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    idem_key        VARCHAR(80) NOT NULL,
    request_hash    CHAR(64)    NOT NULL,
    transaction_id  UUID        NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, idem_key)                               -- the database, not the application, enforces uniqueness
);

-- Immutable ledger: every balance change has exactly one row.
CREATE TABLE ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    account_id      UUID          NOT NULL REFERENCES accounts(id),
    transaction_id  UUID          NOT NULL REFERENCES transactions(id),
    direction       VARCHAR(8)    NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    balance_after   NUMERIC(19,2) NOT NULL CHECK (balance_after >= 0),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_ledger_account ON ledger_entries (account_id, id);

CREATE TABLE rewards (
    id                     UUID PRIMARY KEY,
    user_id                UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind                   VARCHAR(16) NOT NULL CHECK (kind IN ('CASHBACK','WELCOME')),
    title                  VARCHAR(120) NOT NULL,
    description            VARCHAR(300),
    amount                 NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    status                 VARCHAR(16) NOT NULL CHECK (status IN ('AVAILABLE','REDEEMED','EXPIRED')),
    source_transaction_id  UUID REFERENCES transactions(id) ON DELETE SET NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    redeemed_at            TIMESTAMPTZ,
    expires_at             TIMESTAMPTZ
);
CREATE INDEX ix_rewards_user ON rewards (user_id, status);

CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind        VARCHAR(16) NOT NULL,
    title       VARCHAR(120) NOT NULL,
    body        VARCHAR(300) NOT NULL,
    is_read     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_notif_user ON notifications (user_id, created_at DESC);

CREATE TABLE budgets (
    id             UUID PRIMARY KEY,
    user_id        UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category       VARCHAR(24)   NOT NULL,
    monthly_limit  NUMERIC(19,2) NOT NULL CHECK (monthly_limit >= 0),
    UNIQUE (user_id, category)
);

CREATE TABLE recurring_bills (
    id             UUID PRIMARY KEY,
    user_id        UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           VARCHAR(80)   NOT NULL,
    category       VARCHAR(24)   NOT NULL,
    amount         NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    frequency      VARCHAR(8)    NOT NULL CHECK (frequency IN ('WEEKLY','MONTHLY','YEARLY')),
    next_due_date  DATE          NOT NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_bills_user ON recurring_bills (user_id, next_due_date);

CREATE TABLE audit_events (
    id           BIGSERIAL PRIMARY KEY,
    user_id      UUID,
    actor        VARCHAR(40)  NOT NULL,
    action       VARCHAR(60)  NOT NULL,
    entity_type  VARCHAR(40),
    entity_id    VARCHAR(64),
    detail       VARCHAR(500),            -- short, non-sensitive context only
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_user ON audit_events (user_id, id DESC);

-- Immutability: audit and ledger rows can be inserted, never updated or deleted.
CREATE FUNCTION forbid_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% on % is not allowed (insert-only table)', TG_OP, TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_immutable  BEFORE UPDATE OR DELETE ON audit_events   FOR EACH ROW EXECUTE FUNCTION forbid_mutation();
CREATE TRIGGER trg_ledger_immutable BEFORE UPDATE OR DELETE ON ledger_entries FOR EACH ROW EXECUTE FUNCTION forbid_mutation();
