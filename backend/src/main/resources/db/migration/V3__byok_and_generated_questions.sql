-- BYOK: per-user AI provider configuration (key stored encrypted)
ALTER TABLE users ADD COLUMN ai_provider VARCHAR(20);
ALTER TABLE users ADD COLUMN ai_api_key_enc TEXT;
ALTER TABLE users ADD COLUMN ai_base_url VARCHAR(255);
ALTER TABLE users ADD COLUMN ai_model VARCHAR(100);

-- Cache of AI-generated questions so a skill is billed once, reused forever
CREATE TABLE generated_questions (
    id BIGSERIAL PRIMARY KEY,
    skill_name VARCHAR(140) NOT NULL,
    prompt_hash VARCHAR(64) NOT NULL,
    type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    prompt TEXT NOT NULL,
    options TEXT,
    answer_key TEXT,
    keywords TEXT,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_gq_skill_prompt UNIQUE (skill_name, prompt_hash)
);
CREATE INDEX idx_gq_skill_lower ON generated_questions (lower(skill_name));
