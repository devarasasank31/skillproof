-- Performance indexes for the hottest read paths (dashboard, analytics, challenges).
-- These back the queries that run on every page load; without them Postgres does
-- sequential scans as skill_scores/snapshots grow.

-- Dashboard/analytics read every snapshot for one user ordered by time.
CREATE INDEX IF NOT EXISTS idx_scores_user_time ON skill_scores(user_skill_id, snapshot_at DESC);

-- Assessment scoring aggregates per user+skill (averageScore / countBy...).
CREATE INDEX IF NOT EXISTS idx_assessments_user_skill_status ON assessments(user_id, skill_id, status);

-- Practical evidence lookups by user + challenge.
CREATE INDEX IF NOT EXISTS idx_submissions_user_challenge ON challenge_submissions(user_id, challenge_id);

-- Challenges are always filtered by skill name (case-insensitive).
CREATE INDEX IF NOT EXISTS idx_challenges_skill_name ON practical_challenges(lower(skill_name));

-- Job-skill matching joins on skill_id.
CREATE INDEX IF NOT EXISTS idx_job_skills_skill ON job_skills(skill_id);

-- Open recommendation lookups per user.
CREATE INDEX IF NOT EXISTS idx_recs_user_status ON recommendations(user_id, status);

-- User skill listing is ordered by confidence.
CREATE INDEX IF NOT EXISTS idx_user_skills_user_conf ON user_skills(user_id, confidence DESC);

-- Question pools are always fetched per skill.
CREATE INDEX IF NOT EXISTS idx_questions_skill ON questions(skill_id);

-- Interview session history per user.
CREATE INDEX IF NOT EXISTS idx_interviews_user ON interview_sessions(user_id, started_at DESC);

-- Saved job listings per user.
CREATE INDEX IF NOT EXISTS idx_job_descriptions_user ON job_descriptions(user_id);

-- Login looks users up by lower(email); the plain UNIQUE(email) index cannot serve that.
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users(lower(email));