package com.skillproof.interview;

import com.skillproof.ai.AiEvaluationService;
import com.skillproof.assessment.EvaluationService;
import com.skillproof.assessment.QuestionBank;
import com.skillproof.exception.ApiException;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.Skill;
import com.skillproof.skill.SkillRepository;
import com.skillproof.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    public record StartRequest(String targetRole, List<Long> skillIds) {}
    public record QuestionRow(Long id, String category, String skillName, String prompt, Integer score) {}
    public record StartResponse(Long sessionId, String status, List<QuestionRow> questions,
                                List<String> skippedSkills) {}
    public record AnswerRequest(Long questionId, String answerText, Integer timeSpentSeconds) {}
    public record AnswerResponse(int score, String evaluationSource, String feedback, List<String> missingConcepts) {}
    public record SkillScoreRow(String skillName, int score) {}
    public record Report(Long sessionId, int overallScore, List<SkillScoreRow> perSkill,
                         String weakest, List<String> plan) {}

    private final InterviewSessionRepository sessions;
    private final InterviewQuestionRepository questions;
    private final com.skillproof.assessment.QuestionContentService content;
    private final EvaluationService evaluation;
    private final AiEvaluationService ai;
    private final SkillRepository skills;
    private final UserRepository users;
    private final com.skillproof.evidence.SkillEvidenceRepository evidence;
    private final com.skillproof.skill.UserSkillRepository userSkillsRepo;
    private final RecalculationService recalculation;

    public InterviewController(InterviewSessionRepository sessions, InterviewQuestionRepository questions,
                               com.skillproof.assessment.QuestionContentService content, EvaluationService evaluation,
                               AiEvaluationService ai,
                               SkillRepository skills, UserRepository users,
                               com.skillproof.evidence.SkillEvidenceRepository evidence,
                               com.skillproof.skill.UserSkillRepository userSkillsRepo,
                               RecalculationService recalculation) {
        this.sessions = sessions;
        this.questions = questions;
        this.content = content;
        this.evaluation = evaluation;
        this.ai = ai;
        this.skills = skills;
        this.users = users;
        this.evidence = evidence;
        this.userSkillsRepo = userSkillsRepo;
        this.recalculation = recalculation;
    }

    @PostMapping
    @Transactional
    public StartResponse start(@CurrentUserId Long userId, @Valid @RequestBody StartRequest req) {
        if (req.skillIds() == null || req.skillIds().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Select at least one skill to be interviewed on");
        }
        if (req.targetRole() == null || req.targetRole().isBlank()) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Target role is required");
        }

        InterviewSession session = new InterviewSession();
        session.setUser(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
        session.setTargetRole(req.targetRole().trim());
        session.setSkillIds(req.skillIds().stream().map(String::valueOf)
                .collect(Collectors.joining(",")));
        sessions.save(session);

        List<QuestionRow> rows = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int order = 0;
        for (Long skillId : req.skillIds()) {
            Skill s = skills.findById(skillId)
                    .orElseThrow(() -> ApiException.notFound("Skill " + skillId + " not found"));
            List<QuestionBank.BankQuestion> pool =
                    content.ensurePool(userId, s.getName(), true).stream()
                            .sorted(Comparator.comparingInt(q ->
                                    q.type().equalsIgnoreCase("INTERVIEW") ? 0 : 1))
                            .limit(2)
                            .toList();
            if (pool.isEmpty()) {
                skipped.add(s.getName());
                continue;
            }
            for (QuestionBank.BankQuestion bq : pool) {
                InterviewQuestion iq = new InterviewQuestion();
                iq.setSession(session);
                iq.setSkill(s);
                iq.setCategory(s.getName());
                iq.setPrompt(bq.prompt());
                iq.setAnswerKey(bq.answerKey());
                iq.setKeywords(String.join(",", bq.keywords() == null ? List.<String>of() : bq.keywords()));
                iq.setOrderIndex(order++);
                questions.save(iq);
                rows.add(new QuestionRow(iq.getId(), iq.getCategory(), s.getName(), bq.prompt(), null));
            }
        }
        if (rows.isEmpty()) {
            throw com.skillproof.assessment.QuestionContentService.noQuestions(
                    String.join(", ", skipped));
        }
        return new StartResponse(session.getId(), session.getStatus(), rows,
                skipped.isEmpty() ? null : skipped);
    }

    @PostMapping("/{id}/answer")
    @Transactional
    public AnswerResponse answer(@CurrentUserId Long userId, @PathVariable Long id,
                                 @Valid @RequestBody AnswerRequest req) {
        InterviewSession session = sessions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Interview not found"));
        if ("COMPLETED".equals(session.getStatus())) {
            throw ApiException.conflict("SESSION_CLOSED", "Interview already completed");
        }
        InterviewQuestion iq = questions.findByIdAndSession_Id(req.questionId(), id)
                .orElseThrow(() -> ApiException.notFound("Question not in this interview"));
        if (iq.getAnswerText() != null) {
            throw ApiException.conflict("ALREADY_ANSWERED", "Question already answered");
        }
        if (req.answerText() == null || req.answerText().isBlank()) {
            throw ApiException.badRequest("EMPTY_ANSWER", "Answer cannot be empty");
        }

        var pseudo = new com.skillproof.assessment.Question();
        pseudo.setType("SUBJECTIVE");
        pseudo.setKeywords(iq.getKeywords());
        pseudo.setAnswerKey(iq.getAnswerKey());

        com.skillproof.ai.EvaluationResult aiEval =
                ai.available(userId) ? ai.evaluateSubjective(userId, iq.getPrompt(),
                        iq.getKeywords() == null ? "" : iq.getKeywords(), req.answerText()) : null;
        var r = evaluation.evaluate(pseudo, req.answerText(), aiEval);

        iq.setAnswerText(req.answerText());
        iq.setScore(r.score());
        iq.setEvaluationSource(r.source());
        if (req.timeSpentSeconds() != null && req.timeSpentSeconds() >= 0) {
            iq.setTimeSpentSeconds(Math.min(req.timeSpentSeconds(), 3600));
        }

        return new AnswerResponse(r.score(), r.source(), r.feedback(), r.missingConcepts());
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public Report complete(@CurrentUserId Long userId, @PathVariable Long id) {
        InterviewSession session = sessions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Interview not found"));
        if ("COMPLETED".equals(session.getStatus())) {
            throw ApiException.conflict("SESSION_CLOSED", "Interview already completed");
        }
        List<InterviewQuestion> qs = questions.findBySessionIdOrderByOrderIndexAsc(id);
        Map<String, IntSummaryStatistics> byCat = new LinkedHashMap<>();
        for (InterviewQuestion q : qs) {
            if (q.getScore() == null) continue;
            byCat.computeIfAbsent(q.getCategory(), k -> new IntSummaryStatistics()).accept(q.getScore());

            Skill s = q.getSkill();
            if (s != null) {
                recalculationNote(userId, s, q);
            }
        }
        if (byCat.isEmpty()) {
            throw ApiException.badRequest("EMPTY_SESSION", "Answer at least one question before completing");
        }

        int overall = (int) Math.round(byCat.values().stream()
                .mapToDouble(IntSummaryStatistics::getAverage).average().orElse(0));
        session.setStatus("COMPLETED");
        session.setOverallScore(overall);
        session.setCompletedAt(java.time.Instant.now());

        List<SkillScoreRow> perSkill = byCat.entrySet().stream()
                .map(e -> new SkillScoreRow(e.getKey(), (int) Math.round(e.getValue().getAverage())))
                .toList();
        SkillScoreRow weakest = perSkill.stream()
                .min(Comparator.comparingInt(SkillScoreRow::score)).orElse(null);

        List<String> plan = new ArrayList<>();
        if (weakest != null) {
            plan.add("Review core concepts of " + weakest.skillName() + " (scored "
                    + weakest.score() + "%)");
            plan.add("Take a focused assessment on " + weakest.skillName() + " tomorrow");
            plan.add("Attempt a practical challenge in your weakest category this week");
        }
        plan.add("Re-run this interview in 7 days to measure improvement");

        return new Report(session.getId(), overall, perSkill,
                weakest == null ? null : weakest.skillName(), plan);
    }

    private void recalculationNote(Long userId, Skill s, InterviewQuestion q) {
        var usOpt = userSkillsRepo.findByUserIdAndSkillId(userId, s.getId());
        if (usOpt.isEmpty()) return;
        var us = usOpt.get();
        us.setLastActivityAt(java.time.Instant.now());
        var ev = new com.skillproof.evidence.SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(userId);
        ev.setEvidenceType(com.skillproof.evidence.SkillEvidence.Type.KNOWLEDGE);
        ev.setDescription("Interview question attempted in category " + q.getCategory()
                + " (score " + q.getScore() + "%)");
        ev.setPoints(q.getScore());
        evidence.save(ev);
        recalculation.recalculateUserSkill(us);
    }
}
