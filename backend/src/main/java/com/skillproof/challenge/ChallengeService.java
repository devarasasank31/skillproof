package com.skillproof.challenge;

import com.skillproof.ai.AiEvaluationService;
import com.skillproof.common.RateLimiter;
import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.exception.ApiException;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.skill.SkillRepository;
import com.skillproof.skill.UserSkill;
import com.skillproof.skill.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChallengeService {

    private static final ExecutorService challengeGenerator =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "challenge-generator");
                t.setDaemon(true);
                return t;
            });

    private final ChallengeRepository challenges;
    private final ChallengeSubmissionRepository submissions;
    private final UserSkillRepository userSkills;
    private final SkillRepository skills;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;
    private final AiEvaluationService ai;
    private final RateLimiter rateLimiter;

    public ChallengeService(ChallengeRepository challenges, ChallengeSubmissionRepository submissions,
                            UserSkillRepository userSkills, SkillRepository skills,
                            SkillEvidenceRepository evidence, RecalculationService recalculation,
                            AiEvaluationService ai, RateLimiter rateLimiter) {
        this.challenges = challenges;
        this.submissions = submissions;
        this.userSkills = userSkills;
        this.skills = skills;
        this.evidence = evidence;
        this.recalculation = recalculation;
        this.ai = ai;
        this.rateLimiter = rateLimiter;
    }

    public List<PracticalChallenge> list(Long userId, String skill, String type) {
        // One query for the user's skills, one for the catalog - previously this ran
        // userSkills twice and challenges.findAll() three times per request.
        List<UserSkill> owned = userSkills.findByUserId(userId);
        Set<String> mine = owned.stream()
                .map(us -> us.getSkill().getName().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        List<PracticalChallenge> all = challenges.findAll();

        // Resume-first + BYOK: kick off generation for skills with no ready-made challenge,
        // but never let an AI call block the response.
        List<String> mySkills = owned.stream()
                .map(us -> us.getSkill().getName())
                .distinct()
                .toList();
        Set<String> covered = all.stream()
                .map(c -> c.getSkillName().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        generateMissingAsync(userId, mySkills, covered);

        Set<String> finalMine = mine;
        return all.stream()
                .filter(c -> skill == null || c.getSkillName().equalsIgnoreCase(skill))
                .filter(c -> type == null || c.getType().equalsIgnoreCase(type))
                .filter(c -> finalMine.contains(c.getSkillName().toLowerCase(Locale.ROOT)))
                .toList();
    }

    /**
     * Tailored challenges are generated in the background so an AI round-trip never blocks the
     * page. They are persisted, so they simply appear on a later load. Uses an explicit executor
     * because a self-invoked @Async method would be ignored by Spring's proxy.
     */
    private void generateMissingAsync(Long userId, List<String> mySkills, Set<String> covered) {
        List<String> missing = mySkills.stream()
                .filter(s -> !covered.contains(s.toLowerCase(Locale.ROOT)))
                .limit(3)
                .toList();
        if (missing.isEmpty() || !ai.available(userId)) return;
        Set<String> snapshot = Set.copyOf(covered);
        challengeGenerator.execute(() -> generateMissing(userId, missing, snapshot));
    }

    private void generateMissing(Long userId, List<String> missing, Set<String> covered) {
        for (String s : missing) {
            if (!rateLimiter.tryAcquire("aichal:" + userId, 12, java.time.Duration.ofHours(1).toMillis())) break;
            var gen = ai.generateChallenge(userId, s);
            if (gen == null) continue;
            PracticalChallenge c = new PracticalChallenge();
            c.setSlug("ai-" + s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                    + "-" + Long.toHexString(System.nanoTime()));
            c.setTitle(gen.title());
            c.setSkillName(s);
            c.setType(gen.type());
            c.setDifficulty(gen.difficulty());
            c.setPrompt(gen.prompt());
            c.setRubric(gen.rubric());
            c.setRequiredKeywords(String.join(",", gen.keywords()));
            c.setEstMinutes(gen.estMinutes());
            try {
                challenges.save(c);
            } catch (Exception ignored) {
                // slug collision or constraint issue - skip this round
            }
        }
    }

    public PracticalChallenge get(Long id) {
        return challenges.findById(id).orElseThrow(() -> ApiException.notFound("Challenge not found"));
    }

    @Transactional
    public ChallengeSubmission submit(Long userId, Long challengeId, String submissionText) {
        if (submissionText == null || submissionText.isBlank()) {
            throw ApiException.badRequest("EMPTY_SUBMISSION", "Submission cannot be empty");
        }
        PracticalChallenge challenge = get(challengeId);

        Evaluation eval = evaluate(challenge, submissionText);

        ChallengeSubmission sub = new ChallengeSubmission();
        sub.setChallenge(challenge);
        sub.setUserId(userId);
        sub.setSubmissionText(submissionText);
        sub.setScore(eval.score());
        sub.setCorrectness(eval.correctness());
        sub.setCompleteness(eval.completeness());
        sub.setBestPractices(eval.bestPractices());
        sub.setChecksPassed(eval.checksPassed());
        sub.setChecksTotal(eval.checksTotal());
        sub.setFeedback(eval.feedback());
        submissions.save(sub);

        userSkills.findByUserIdAndSkillName(userId, challenge.getSkillName())
                .ifPresent(us -> {
                    us.setPracticalScore(Math.max(us.getPracticalScore(), eval.score()));
                    us.setLastActivityAt(Instant.now());
                    SkillEvidence ev = new SkillEvidence();
                    ev.setUserSkill(us);
                    ev.setUserId(userId);
                    ev.setEvidenceType(SkillEvidence.Type.PRACTICAL);
                    ev.setDescription("Completed " + challenge.getType() + " challenge: " + challenge.getTitle()
                            + " (score " + eval.score() + "%)");
                    ev.setPoints(eval.score());
                    evidence.save(ev);
                    recalculation.recalculateUserSkill(us);
                });

        return sub;
    }

    record Evaluation(int score, int correctness, int completeness, int bestPractices,
                      int checksPassed, int checksTotal, String feedback) {}

    static Evaluation evaluate(PracticalChallenge challenge, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> keywords = splitCsv(challenge.getRequiredKeywords());

        List<String> passed = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        for (String kw : keywords) {
            boolean ok = kw.contains("|")
                    ? java.util.Arrays.stream(kw.split("\\|")).anyMatch(s -> lower.contains(s.trim()))
                    : lower.contains(kw);
            if (ok) passed.add(kw); else missed.add(kw);
        }

        int total = keywords.size();
        int checksPassed = passed.size();
        int correctness = total == 0 ? 70 : (int) Math.round(100.0 * checksPassed / total);
        int completeness = Math.min(100, 40 + (int) Math.min(60, text.length() / 20.0));
        int bestPractices = computeBestPractices(lower, challenge.getType());

        int score = (int) Math.round(correctness * 0.55 + completeness * 0.25 + bestPractices * 0.20);
        if (text.length() < 80) score = Math.min(score, 35);

        StringBuilder fb = new StringBuilder();
        fb.append("Deterministic checks: ").append(checksPassed).append("/").append(total).append(" key concepts detected.");
        if (!missed.isEmpty()) {
            fb.append(" Not found in your submission: ").append(String.join(", ", missed)).append(".");
        }
        fb.append(" Rubric-based review recommended for full depth.");

        return new Evaluation(score, correctness, completeness, bestPractices, checksPassed, total, fb.toString());
    }

    private static int computeBestPractices(String lower, String type) {
        int bp = 50;
        if (lower.contains("test") || lower.contains("assert")) bp += 12;
        if (lower.contains("error") || lower.contains("exception") || lower.contains("handle")) bp += 10;
        if (lower.contains("valid") || lower.contains("constraint")) bp += 8;
        if (lower.contains("complexity") || lower.contains("o(")) bp += 10;
        if (type != null && type.equals("REST_API") && (lower.contains("idempot") || lower.contains("status code"))) bp += 10;
        return Math.min(100, bp);
    }

    static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
