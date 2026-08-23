package com.skillproof.challenge;

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

@Service
public class ChallengeService {

    private final ChallengeRepository challenges;
    private final ChallengeSubmissionRepository submissions;
    private final UserSkillRepository userSkills;
    private final SkillRepository skills;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;

    public ChallengeService(ChallengeRepository challenges, ChallengeSubmissionRepository submissions,
                            UserSkillRepository userSkills, SkillRepository skills,
                            SkillEvidenceRepository evidence, RecalculationService recalculation) {
        this.challenges = challenges;
        this.submissions = submissions;
        this.userSkills = userSkills;
        this.skills = skills;
        this.evidence = evidence;
        this.recalculation = recalculation;
    }

    public List<PracticalChallenge> list(String skill, String type) {
        List<PracticalChallenge> all = challenges.findAll();
        return all.stream()
                .filter(c -> skill == null || c.getSkillName().equalsIgnoreCase(skill))
                .filter(c -> type == null || c.getType().equalsIgnoreCase(type))
                .toList();
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
