-- Email verification: accounts must confirm their address before logging in.
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;

-- Existing users are grandfathered in as verified.
UPDATE users SET email_verified_at = now() WHERE email_verified_at IS NULL;

CREATE TABLE verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_verification_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_verification_user ON verification_tokens (user_id);
