-- One-time-code challenges. Only a salted hash of the code is stored; codes are single use and expire.
CREATE TABLE otp_challenges (
    id           UUID PRIMARY KEY,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose      VARCHAR(24) NOT NULL CHECK (purpose IN ('PASSWORD_RESET')),
    code_hash    CHAR(64)    NOT NULL,
    attempts     INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ NOT NULL,
    consumed_at  TIMESTAMPTZ
);
CREATE INDEX ix_otp_user_purpose ON otp_challenges (user_id, purpose, created_at DESC);

-- "Request money" (the RECEIVE side of a payment flow). Settling one goes through the normal payment path.
CREATE TABLE payment_requests (
    id                   UUID PRIMARY KEY,
    requester_user_id    UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    payer_handle         VARCHAR(60)   NOT NULL,
    payer_user_id        UUID REFERENCES users(id) ON DELETE SET NULL,   -- null when the payer is not a PAYWELL user
    amount               NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency             CHAR(3)       NOT NULL,
    note                 VARCHAR(140),
    status               VARCHAR(16)   NOT NULL CHECK (status IN ('PENDING','PAID','DECLINED','CANCELLED')),
    paid_transaction_id  UUID REFERENCES transactions(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    resolved_at          TIMESTAMPTZ
);
CREATE INDEX ix_preq_requester ON payment_requests (requester_user_id, created_at DESC);
CREATE INDEX ix_preq_payer ON payment_requests (payer_user_id, status);
