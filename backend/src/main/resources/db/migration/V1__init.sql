-- SkillProof initial schema
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    name VARCHAR(140) NOT NULL,
    headline VARCHAR(200),
    bio TEXT,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(140) NOT NULL UNIQUE,
    category VARCHAR(80) NOT NULL,
    aliases TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_skills (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    claim_source VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    knowledge_score INT NOT NULL DEFAULT 0,
    practical_score INT NOT NULL DEFAULT 0,
    activity_score INT NOT NULL DEFAULT 0,
    market_score INT NOT NULL DEFAULT 0,
    confidence INT NOT NULL DEFAULT 0,
    state VARCHAR(30) NOT NULL DEFAULT 'NEW',
    memory_strength DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    retention DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    last_activity_at TIMESTAMPTZ,
    last_reviewed_at TIMESTAMPTZ,
    next_review_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_skill UNIQUE (user_id, skill_id)
);
CREATE INDEX idx_user_skills_user ON user_skills(user_id);
CREATE INDEX idx_user_skills_next_review ON user_skills(next_review_at);

CREATE TABLE assessments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id),
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    source VARCHAR(20) NOT NULL DEFAULT 'BANK',
    score INT,
    question_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    assessment_id BIGINT REFERENCES assessments(id) ON DELETE CASCADE,
    bank_key VARCHAR(200),
    skill_id BIGINT REFERENCES skills(id),
    type VARCHAR(40) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    prompt TEXT NOT NULL,
    options TEXT,
    answer_key TEXT,
    keywords TEXT,
    explanation TEXT,
    order_index INT NOT NULL DEFAULT 0
);

CREATE TABLE answers (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    correct BOOLEAN,
    evaluation_source VARCHAR(20) NOT NULL DEFAULT 'DETERMINISTIC',
    feedback TEXT,
    missing_concepts TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_answer_per_question UNIQUE (question_id, user_id)
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    user_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DUE',
    score INT,
    interval_days INT,
    memory_strength DOUBLE PRECISION,
    retention_before DOUBLE PRECISION,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reviews_due ON reviews(due_at, status);

CREATE TABLE practical_challenges (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(160) NOT NULL UNIQUE,
    title VARCHAR(220) NOT NULL,
    skill_name VARCHAR(140) NOT NULL,
    type VARCHAR(40) NOT NULL,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    prompt TEXT NOT NULL,
    rubric TEXT,
    required_keywords TEXT,
    est_minutes INT NOT NULL DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE challenge_submissions (
    id BIGSERIAL PRIMARY KEY,
    challenge_id BIGINT NOT NULL REFERENCES practical_challenges(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_text TEXT NOT NULL,
    score INT NOT NULL,
    correctness INT NOT NULL,
    completeness INT NOT NULL,
    best_practices INT NOT NULL,
    checks_passed INT NOT NULL,
    checks_total INT NOT NULL,
    feedback TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_submissions_user ON challenge_submissions(user_id);

CREATE TABLE github_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(120) NOT NULL,
    public_repos INT NOT NULL DEFAULT 0,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE github_repositories (
    id BIGSERIAL PRIMARY KEY,
    profile_id BIGINT NOT NULL REFERENCES github_profiles(id) ON DELETE CASCADE,
    external_id BIGINT NOT NULL,
    name VARCHAR(220) NOT NULL,
    description TEXT,
    primary_language VARCHAR(80),
    languages TEXT,
    topics TEXT,
    pushed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_repo_per_profile UNIQUE (profile_id, external_id)
);

CREATE TABLE skill_evidence (
    id BIGSERIAL PRIMARY KEY,
    user_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    evidence_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_evidence_usk ON skill_evidence(user_skill_id, occurred_at DESC);

CREATE TABLE knowledge_events (
    id BIGSERIAL PRIMARY KEY,
    user_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
    initial_retention DOUBLE PRECISION NOT NULL,
    review_score INT,
    memory_strength DOUBLE PRECISION NOT NULL,
    elapsed_days INT NOT NULL DEFAULT 0,
    predicted_retention DOUBLE PRECISION NOT NULL,
    reviewed_at TIMESTAMPTZ,
    next_review_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE job_descriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(220) NOT NULL,
    company VARCHAR(180),
    raw_text TEXT NOT NULL,
    readiness INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE job_skills (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES job_descriptions(id) ON DELETE CASCADE,
    skill_id BIGINT REFERENCES skills(id),
    matched_name VARCHAR(140) NOT NULL,
    frequency INT NOT NULL DEFAULT 1,
    confidence INT
);

CREATE TABLE skill_scores (
    id BIGSERIAL PRIMARY KEY,
    user_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
    confidence INT NOT NULL,
    knowledge INT NOT NULL,
    practical INT NOT NULL,
    activity INT NOT NULL,
    market INT NOT NULL,
    state VARCHAR(30) NOT NULL,
    snapshot_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_scores_usk_time ON skill_scores(user_skill_id, snapshot_at);

CREATE TABLE recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(30) NOT NULL,
    title VARCHAR(240) NOT NULL,
    reason TEXT NOT NULL,
    priority INT NOT NULL DEFAULT 50,
    effort_minutes INT,
    skill_id BIGINT REFERENCES skills(id),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    generated_on DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE interview_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_role VARCHAR(180),
    skill_ids TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    overall_score INT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE interview_questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    skill_id BIGINT REFERENCES skills(id),
    category VARCHAR(80) NOT NULL,
    prompt TEXT NOT NULL,
    answer_key TEXT,
    keywords TEXT,
    order_index INT NOT NULL DEFAULT 0,
    answer_text TEXT,
    score INT,
    time_spent_seconds INT,
    evaluation_source VARCHAR(20)
);

CREATE TABLE skill_edges (
    id BIGSERIAL PRIMARY KEY,
    from_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
    to_skill_id BIGINT NOT NULL REFERENCES user_skills(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL
);
