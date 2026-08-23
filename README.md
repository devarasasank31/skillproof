<div align="center">

# 🛡️ SkillProof

### **Do you actually know it?**

[![Live Demo](https://img.shields.io/badge/LIVE_DEMO-skillproof-bice.vercel.app-6366f1?style=for-the-badge&logo=vercel)](https://skillproof-bice.vercel.app)
[![Java](https://img.shields.io/badge/Java_21-Spring_Boot_3.4-6DB33F?style=flat-square&logo=spring)](https://spring.io)
[![React](https://img.shields.io/badge/React_18-TypeScript_Vite-61DAFB?style=flat-square&logo=react)](https://vitejs.dev)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-Flyway_Migrations-4169E1?style=flat-square&logo=postgresql)](https://www.postgresql.org)

**Claim a skill → prove it → watch it decay if you ignore it.**
A continuously-verified skill profile instead of a static resume line.

</div>

---

## ⚡ The Idea

Resumes say *"Skills: Java, Kubernetes, AWS"*. Interviews are the only filter that checks — and they don't scale.

**SkillProof flips the model:** you don't list skills, you *defend* them.
Every claim starts weak and earns trust through assessments, challenges, mock interviews,
GitHub activity and spaced repetition. Skip a skill for a month and its confidence visibly rots.
What survives is honest.

```
confidence = 10% claim + 30% knowledge + 30% practical + 20% activity + 10% market
```

| Signal | Where it comes from |
|---|---|
| 📝 Claim · 10% | Saying it counts almost nothing |
| 🧠 Knowledge · 30% | Timed assessments, graded strictly |
| 🔨 Practical · 30% | Rubric-scored coding & design challenges |
| 📈 Activity · 20% | GitHub commits, reviews done on time |
| 💼 Market · 10% | How hot the skill is in job descriptions you analyze |

### Every skill lives in a state machine

`NEW` → `LEARNING` → `STRONG` → `MASTERED` …or fall into:

> ⚠️ **OVERCLAIMED** — you said you know it; the measurements disagree. The app will tell you. Repeatedly.

Plus `AT_RISK`, `STALE`, `WEAK` states driven by a **memory model**: retention decays
exponentially and review sessions are scheduled SM-2-style —
score low → review tomorrow; score high → see you in two weeks.

---

## 🚀 Try It Now

**👉 [skillproof-bice.vercel.app](https://skillproof-bice.vercel.app)** — live, free, no install.

1. **Create account** (optionally paste your AI API key — see [BYOK](#-bring-your-own-key-byok))
2. **Add skills** — type anything ("AWS Lambda", "LLMs", "Flutter"…) or upload your resume PDF
3. **Assess** — answer questions; every score updates confidence + schedules a review
4. **Interview yourself** — multi-skill mock interview with instant per-answer feedback
5. **Analyze a job posting** → get your readiness % and exactly what to study tonight

<details>
<summary>🧪 Prefer running locally?</summary>

```bash
# 1. Database (host port 5433 -> container 5432; avoids clashing with a local Postgres)
docker compose up -d

# 2. Backend (Java 21+)
cd backend
DATABASE_URL=jdbc:postgresql://localhost:5433/skillproof ./mvnw spring-boot:run   # :8080

# 3. Frontend
cd frontend
npm install && npm run dev   # :5173
```

Windows PowerShell: set `$env:DATABASE_URL="jdbc:postgresql://localhost:5433/skillproof"` first.
Demo account (auto-seeded): `demo@skillproof.dev` / `Demo1234!` · Swagger: <http://localhost:8080/swagger-ui.html>
</details>

---

## ✨ Feature Tour

| | Feature | What actually happens |
|---|---|---|
| 📄 | **Resume import** | Upload a PDF; ~120-term detector spans AI/ML, cloud, data, DevOps, mobile streams; detected skills become one-click claims |
| 🎯 | **Any skill, any stream** | Claim literally anything not in the catalog — full-stack, cloud, ML, whatever |
| 🤖 | **AI question generation** | No bank content for your niche skill? Your API key generates interview questions once; cached forever after |
| 🗣️ | **Mock interviews** | Multi-skill sessions, instant grading per answer, final report names your weakest area + study plan |
| 🧩 | **Practical challenges** | Real tasks scored on correctness / completeness / best practices rubrics |
| 🔁 | **Spaced repetition** | Reviews appear when your memory is about to slip — retention curve per skill |
| 💼 | **Job readiness** | Paste any JD → required skills matched against proof → readiness % + gap list |
| 🐙 | **GitHub evidence** | Live repo/language/topic analysis feeds activity signal into scores |
| 📊 | **Analytics** | Category averages, per-skill confidence trends over time, strongest/weakest |

---

## 🔑 Bring Your Own Key (BYOK)

SkillProof runs fully **without any AI key** — the built-in bank plus deterministic
keyword grader handle everything. Add your own key to unlock:

- **AI grading** of open-ended answers (holistic score + feedback, takes precedence when valid)
- **Auto-generated questions for ANY skill** — generated once with your key, cached in the DB forever

| Provider | Notes |
|---|---|
| OpenAI | `gpt-4o-mini` default |
| Groq | fast + generous free tier |
| OpenRouter | route to hundreds of models |
| Ollama | local & free, nothing leaves your machine |
| Custom | any OpenAI-compatible base URL |

Keys are stored **AES-GCM encrypted**, shown back only masked (`****abcd`),
and calls go directly to your provider — SkillProof never sees your usage.
Configure at signup or in **Settings → AI key**.

---

## ☁️ Deploy Your Own (all free)

Architecture: `Vercel (SPA) → Render (Spring Boot in Docker) → Neon (Postgres)`.

1. **Database — [neon.tech](https://neon.tech)**: create project, copy connection string.
2. **Backend — [render.com](https://render.com)**: New Web Service → this repo →
   Root Directory `backend` (Dockerfile included) → env vars:

   | Key | Value |
   |---|---|
   | `DATABASE_URL` | `jdbc:postgresql://<neon-host>/<db>?sslmode=require` |
   | `DATABASE_USERNAME` / `DATABASE_PASSWORD` | from Neon string |
   | `JWT_SECRET` | 60+ random chars |
   | `FRONTEND_URL` | your Vercel URL (set after step 3) |

3. **Frontend — [vercel.com](https://vercel.com)**: import repo → Root Directory `frontend` →
   env var `VITE_API_URL` = `https://<your-service>.onrender.com` → **Redeploy** (required!).
4. Back in Render set `FRONTEND_URL` = `https://<your-app>.vercel.app` → redeploy (unlocks CORS).

> Free-tier note: Render sleeps after ~15 min idle; first request takes ~60 s to wake.

---

## 🧪 Tests

```bash
cd backend && ./mvnw test        # unit tests (no DB needed)
cd frontend && npm run build     # type-checks + builds
```

## 🔐 Security Notes

- bcrypt password hashing; JWT access tokens (30 min) + rotating refresh tokens (14 days)
- All endpoints authenticated except `/api/auth/**` and Swagger
- BYOK keys encrypted at rest (AES-GCM, derived from `JWT_SECRET`); never returned in full
- CORS: localhost dev by default; lock to your domain via `FRONTEND_URL` in production
- Rate limiting on login, resume analysis and GitHub imports

## 🏗️ Project Layout

```
skillproof/
├── backend/            Spring Boot service (Dockerfile included)
│   └── src/main/java/com/skillproof/
│       ├── auth/       register/login/refresh/logout, JWT filter
│       ├── user/       profile, stats, BYOK settings
│       ├── skill/      claims, catalog, detail, graph edges
│       ├── assessment/ question bank + cache, scoring, evidence
│       ├── challenge/  rubric-scored practical tasks
│       ├── interview/  mock interviews + reports
│       ├── review/     SM-2-style spaced repetition scheduler
│       ├── scoring/    weighted confidence engine + state classifier
│       ├── resume/     PDF extraction -> ~120-term skill detection
│       ├── github/     public repo analysis -> activity evidence
│       ├── job/        JD parsing -> readiness + market signal
│       └── ai/         BYOK resolver, OpenAI-compatible client, AES cipher
├── frontend/           React SPA (Vercel-ready)
├── docker-compose.yml  PostgreSQL for local dev
└── .env.example
```
