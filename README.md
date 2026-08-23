# SkillProof

**Do you actually know it?**

SkillProof is a developer skill intelligence platform. You claim skills; the platform
measures whether you can defend them — through spaced assessments, practical challenges,
interview simulation, activity evidence, and a memory model that decays confidence over
time. The result is an honest, continuously-verified skill profile instead of a static list.

## How confidence works

```
confidence = 10% claim + 30% knowledge + 30% practical + 20% activity + 10% market
```

| Signal | Source |
|---|---|
| Claim (10%) | Self-declared, resume-imported or GitHub-derived |
| Knowledge (30%) | MCQ + open-ended assessments, keyword/AI graded |
| Practical (30%) | Rubric-scored coding & system-design challenges |
| Activity (20%) | GitHub repo analysis, interview practice, reviews on time |
| Market (10%) | Frequency of the skill in job postings you analyze |

### Skill states

`NEW` → claimed, unmeasured · `LEARNING` → building evidence · `STRONG` / `MASTERED` →
proven · `AT_RISK` / `STALE` → retention slipping · `WEAK` → low confidence ·
`OVERCLAIMED` → you claim it, but measured knowledge is low with zero proof.
Resume imports are marked as **claims only** until verified.

### Memory model (spaced repetition)

Retention decays exponentially: `R(t) = e^(-t / S)` with per-skill memory strength `S`
that grows when you review on time and shrinks when assessments go badly.
Review intervals by confidence: `<40% → 1d`, `40-59% → 2d`, `60-74% → 4d`, `75-89% → 7d`, `90%+ → 14d`.

## Stack

- **Backend**: Java 21, Spring Boot 3.4, PostgreSQL 16, Flyway migrations, JWT auth (access + refresh), springdoc OpenAPI
- **Frontend**: React 18, TypeScript, Vite, Tailwind CSS, TanStack Query v5, React Router, Recharts
- **AI**: optional local LLM via Ollama-compatible API for grading open answers; falls back to deterministic keyword grader

## Quick start

```bash
# 1. Database (host port 5433 -> container 5432; avoids clashing with a local Postgres)
docker compose up -d

# 2. Backend (Java 21+)
cd backend
DATABASE_URL=jdbc:postgresql://localhost:5433/skillproof ./mvnw spring-boot:run   # http://localhost:8080

# 3. Frontend
cd frontend
npm install
npm run dev                     # http://localhost:5173
```

> Windows PowerShell: `$env:DATABASE_URL="jdbc:postgresql://localhost:5433/skillproof"` before starting the backend.

Demo account (auto-seeded): `demo@skillproof.dev` / `Demo1234!`

Swagger UI: <http://localhost:8080/swagger-ui.html>

## Tests

```bash
cd backend && ./mvnw test       # unit tests (no DB needed)
RUN_INTEGRATION=true ./mvnw test # adds Testcontainers integration suite (Docker required)
cd frontend && npm run build    # type-checks + builds
```

## Project layout

```
skillproof/
├── backend/            Spring Boot service (single module)
│   └── src/main/java/com/skillproof/
│       ├── auth/       register/login/refresh/logout, JWT filter
│       ├── user/       profile + stats
│       ├── skill/      claims, catalog, detail, graph edges
│       ├── assessment/ question bank, scoring, evidence
│       ├── challenge/  rubric-scored practical tasks
│       ├── interview/  mock interviews + reports
│       ├── review/     SM-2-style spaced repetition scheduler
│       ├── decay/      exponential retention memory model
│       ├── scoring/    weighted confidence engine + state classifier
│       ├── resume/     PDF text extraction -> detected skills
│       ├── github/     public repo analysis -> activity evidence
│       ├── job/        JD parsing -> readiness score + market signal
│       ├── dashboard/  daily briefing + next best action
│       ├── analytics/  category averages + trends
│       └── ai/         optional local-LLM grader adapter
├── frontend/           React SPA
├── docker-compose.yml  PostgreSQL
└── .env.example
```

## Security notes

- Passwords hashed with bcrypt; JWT access tokens (30 min) + rotating refresh tokens (14 days)
- All endpoints authenticated except `/api/auth/**` and Swagger
- Global `ExceptionHandler` returns `{timestamp, status, code, message, path}`
- Rate limiting on challenge submissions and GitHub analysis
