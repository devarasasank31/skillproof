package com.skillproof.assessment;

import com.skillproof.ai.AiEvaluationService;
import com.skillproof.ai.EvaluationResult;
import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.exception.ApiException;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.skill.Skill;
import com.skillproof.skill.UserSkill;
import com.skillproof.skill.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AssessmentService {

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final UserSkillRepository userSkills;
    private final QuestionContentService content;
    private final AiEvaluationService ai;
    private final EvaluationService evaluation;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;

    public AssessmentService(AssessmentRepository assessments, QuestionRepository questions,
                             AnswerRepository answers, UserSkillRepository userSkills, QuestionContentService content,
                             AiEvaluationService ai, EvaluationService evaluation,
                             SkillEvidenceRepository evidence, RecalculationService recalculation) {
        this.assessments = assessments;
        this.questions = questions;
        this.answers = answers;
        this.userSkills = userSkills;
        this.content = content;
        this.ai = ai;
        this.evaluation = evaluation;
        this.evidence = evidence;
        this.recalculation = recalculation;
    }

    public record StartRequest(String difficulty, Integer count) {}
    public record QuestionDto(Long id, String type, String difficulty, String prompt, List<String> options) {}
    public record StartedAssessment(Long assessmentId, String source, List<QuestionDto> questions) {}
    public record AnswerRequest(Long questionId, String answerText) {}
    public record AnswerResult(int score, Boolean correct, String evaluationSource, String feedback,
                               List<String> missingConcepts, List<String> keyConcepts,
                               String answerKey, String explanation) {}
    public record CompletedResult(Long assessmentId, int score, int answered, int total) {}

    @Transactional
    public StartedAssessment start(Long userId, Long skillId, StartRequest req) {
        UserSkill us = userSkills.findByIdAndUserId(skillId, userId)
                .orElseThrow(() -> ApiException.notFound("Skill not found in your profile"));
        Skill skill = us.getSkill();

        String difficulty = req.difficulty() == null ? "MEDIUM" : req.difficulty().toUpperCase();
        int count = req.count() == null ? 5 : Math.max(1, Math.min(15, req.count()));

        Assessment assessment = new Assessment();
        assessment.setUser(us.getUser());
        assessment.setSkill(skill);
        assessment.setDifficulty(difficulty);
        assessments.save(assessment);

        List<Question> created = new ArrayList<>();
        String source = "BANK";

        // Free content first: static bank + previously generated (cached) questions.
        List<QuestionBank.BankQuestion> pool = new ArrayList<>(content.pool(userId, skill.getName()));

        // Top up with fresh AI-generated questions on the user's own key when short.
        if (pool.size() < count && ai.available(userId)) {
            int made = content.generateAndCache(userId, skill.getName(), count - pool.size(), false);
            if (made > 0) {
                source = "AI";
                pool = new ArrayList<>(content.pool(userId, skill.getName()));
            }
        }

        Collections.shuffle(pool, ThreadLocalRandom.current());
        for (QuestionBank.BankQuestion bq : pool) {
            if (created.size() >= count) break;
            created.add(newQuestion(assessment, skill,
                    skill.getName() + ":" + bq.prompt().hashCode(),
                    bq.type(), bq.difficulty(), bq.prompt(),
                    bq.options() == null ? null : String.join("||", bq.options()),
                    bq.answerKey(), bq.keywords() == null ? null : String.join(",", bq.keywords()),
                    bq.explanation(), created.size()));
        }

        if (created.isEmpty()) {
            throw QuestionContentService.noQuestions(skill.getName());
        }

        assessment.setSource(source);
        assessment.setQuestionCount(created.size());
        questions.saveAll(created);

        List<QuestionDto> dtos = created.stream()
                .map(q -> new QuestionDto(q.getId(), q.getType(), q.getDifficulty(), q.getPrompt(),
                        q.getOptions() == null || q.getOptions().isBlank()
                                ? List.of() : List.of(q.getOptions().split("\\|\\|"))))
                .toList();
        return new StartedAssessment(assessment.getId(), source, dtos);
    }

    private Question newQuestion(Assessment a, Skill s, String bankKey, String type, String difficulty,
                                 String prompt, String options, String answerKey, String keywords,
                                 String explanation, int order) {
        Question q = new Question();
        q.setAssessment(a);
        q.setSkill(s);
        q.setBankKey(bankKey);
        q.setType(type.toUpperCase());
        q.setDifficulty(difficulty);
        q.setPrompt(prompt);
        q.setOptions(options);
        q.setAnswerKey(answerKey);
        q.setKeywords(keywords);
        q.setExplanation(explanation);
        q.setOrderIndex(order);
        return q;
    }

    @Transactional
    public AnswerResult answer(Long userId, Long assessmentId, AnswerRequest req) {
        if (req.answerText() == null || req.answerText().isBlank()) {
            throw ApiException.badRequest("EMPTY_ANSWER", "Answer cannot be empty");
        }
        Assessment assessment = assessments.findByIdAndUserId(assessmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Assessment not found"));
        if (assessment.getStatus() != Assessment.Status.IN_PROGRESS) {
            throw ApiException.conflict("ASSESSMENT_CLOSED", "This assessment is already completed");
        }
        Question q = questions.findById(req.questionId())
                .filter(qq -> qq.getAssessment() != null && qq.getAssessment().getId().equals(assessmentId))
                .orElseThrow(() -> ApiException.notFound("Question not in this assessment"));
        answers.findByQuestionIdAndUserId(q.getId(), userId)
                .ifPresent(a -> { throw ApiException.conflict("ALREADY_ANSWERED", "Question already answered"); });

        EvaluationResult aiEval = null;
        if (!"MCQ".equalsIgnoreCase(q.getType()) && ai.available(userId)) {
            aiEval = ai.evaluateSubjective(userId, q.getPrompt(),
                    q.getKeywords() == null ? "" : q.getKeywords(), req.answerText());
        }
        EvaluationService.Result r = evaluation.evaluate(q, req.answerText(), aiEval);

        Answer a = new Answer();
        a.setQuestion(q);
        var userRef = new com.skillproof.user.User();
        userRef.setId(userId);
        a.setUser(userRef);
        a.setAnswerText(req.answerText());
        a.setScore(r.score());
        a.setCorrect(r.correct());
        a.setEvaluationSource(r.source());
        a.setFeedback(r.feedback());
        a.setMissingConcepts(r.missingConcepts().isEmpty() ? null : String.join(",", r.missingConcepts()));
        answers.save(a);

        // Always give the user something to learn from: the literal answer when we have one,
        // otherwise a reference answer built from the expected key concepts.
        List<String> keyConcepts = q.getKeywords() == null || q.getKeywords().isBlank()
                ? List.of() : List.of(q.getKeywords().split(","));
        String reveal = (q.getAnswerKey() == null || q.getAnswerKey().isBlank())
                ? (keyConcepts.isEmpty() ? null : "A strong answer covers: " + String.join("; ", keyConcepts) + ".")
                : q.getAnswerKey();

        return new AnswerResult(r.score(), r.correct(), r.source(), r.feedback(), r.missingConcepts(),
                keyConcepts, reveal, q.getExplanation());
    }

    @Transactional
    public CompletedResult complete(Long userId, Long assessmentId) {
        Assessment assessment = assessments.findByIdAndUserId(assessmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Assessment not found"));
        if (assessment.getStatus() == Assessment.Status.COMPLETED) {
            throw ApiException.conflict("ASSESSMENT_CLOSED", "Already completed");
        }
        List<Question> qs = questions.findByAssessmentIdOrderByOrderIndexAsc(assessmentId);
        List<Answer> as = qs.stream()
                .map(q -> answers.findByQuestionIdAndUserId(q.getId(), userId).orElse(null))
                .filter(a -> a != null)
                .toList();
        if (as.isEmpty()) {
            throw ApiException.badRequest("EMPTY_ASSESSMENT", "Answer at least one question before completing");
        }
        int score = (int) Math.round(as.stream().mapToInt(Answer::getScore).average().orElse(0));
        assessment.setScore(score);
        assessment.setStatus(Assessment.Status.COMPLETED);
        assessment.setCompletedAt(Instant.now());

        UserSkill us = userSkills.findByUserIdAndSkillId(userId, assessment.getSkill().getId()).orElse(null);
        if (us != null) {
            us.setLastActivityAt(Instant.now());
            SkillEvidence ev = new SkillEvidence();
            ev.setUserSkill(us);
            ev.setUserId(userId);
            ev.setEvidenceType(SkillEvidence.Type.KNOWLEDGE);
            ev.setDescription("Passed " + assessment.getDifficulty() + " assessment with score " + score + "%");
            ev.setPoints(score);
            evidence.save(ev);
            recalculation.recalculateUserSkill(us);
        }
        return new CompletedResult(assessment.getId(), score, as.size(), qs.size());
    }

    public Assessment getOwned(Long userId, Long assessmentId) {
        return assessments.findByIdAndUserId(assessmentId, userId)
                .orElseThrow(() -> ApiException.notFound("Assessment not found"));
    }

    public List<Question> getQuestions(Long assessmentId) {
        return questions.findByAssessmentIdOrderByOrderIndexAsc(assessmentId);
    }
}
