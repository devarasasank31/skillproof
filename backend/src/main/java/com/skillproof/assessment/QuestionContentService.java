package com.skillproof.assessment;

import com.skillproof.ai.AiEvaluationService;
import com.skillproof.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Single source of question content for assessments and interviews:
 * static bank first, then the per-skill AI-generated cache, then live
 * generation on the user's own API key (cached for everyone afterwards).
 */
@Service
public class QuestionContentService {

    private static final Logger log = LoggerFactory.getLogger(QuestionContentService.class);

    private final QuestionBank bank;
    private final GeneratedQuestionRepository generated;
    private final AiEvaluationService ai;

    public QuestionContentService(QuestionBank bank, GeneratedQuestionRepository generated, AiEvaluationService ai) {
        this.bank = bank;
        this.generated = generated;
        this.ai = ai;
    }

    public List<QuestionBank.BankQuestion> pool(Long userId, String skillName) {
        List<QuestionBank.BankQuestion> out = new ArrayList<>(bank.forSkill(skillName));
        for (GeneratedQuestionEntity e : generated.findBySkillNameIgnoreCase(skillName)) {
            out.add(new QuestionBank.BankQuestion(e.getSkillName(), e.getType(), e.getDifficulty(),
                    e.getPrompt(),
                    e.getOptions() == null ? null : List.of(e.getOptions().split("\\|\\|")),
                    e.getAnswerKey(),
                    e.getKeywords() == null ? null : List.of(e.getKeywords().split(",")),
                    e.getExplanation()));
        }
        return out;
    }

    /**
     * Generates fresh questions with the user's AI key and caches them.
     * Returns how many were newly persisted.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int generateAndCache(Long userId, String skillName, int count, boolean preferInterview) {
        if (!ai.available(userId)) return 0;
        List<AiEvaluationService.GeneratedQuestion> fresh =
                ai.generateQuestions(userId, skillName, "MEDIUM", count, preferInterview);
        int saved = 0;
        for (var gq : fresh) {
            String hash = sha256(gq.prompt());
            if (generated.existsBySkillNameIgnoreCaseAndPromptHash(skillName, hash)) continue;
            try {
                GeneratedQuestionEntity e = new GeneratedQuestionEntity();
                e.setSkillName(skillName);
                e.setPromptHash(hash);
                e.setType(gq.type() == null ? "SUBJECTIVE" : gq.type().toUpperCase());
                e.setDifficulty(gq.difficulty() == null ? "MEDIUM" : gq.difficulty().toUpperCase());
                e.setPrompt(gq.prompt());
                e.setOptions(gq.options() == null || gq.options().isEmpty() ? null : String.join("||", gq.options()));
                e.setAnswerKey(gq.answerKey());
                e.setKeywords(gq.keywords() == null ? null : String.join(",", gq.keywords()));
                e.setExplanation(gq.explanation());
                generated.save(e);
                saved++;
            } catch (DataIntegrityViolationException dup) {
                log.debug("Duplicate generated question skipped for {}", skillName);
            }
        }
        return saved;
    }

    /**
     * Pool for a skill; when empty and the user has an API key, generates content
     * on demand so ANY skill can be assessed/interviewed.
     */
    public List<QuestionBank.BankQuestion> ensurePool(Long userId, String skillName,
                                                      boolean preferInterview) {
        List<QuestionBank.BankQuestion> p = pool(userId, skillName);
        if (!p.isEmpty()) return p;
        generateAndCache(userId, skillName, preferInterview ? 3 : 4, preferInterview);
        return pool(userId, skillName);
    }

    public static ApiException noQuestions(String skillName) {
        return ApiException.badRequest("NO_QUESTIONS",
                "No question content for '" + skillName + "' yet. Add your AI API key in Settings > AI key"
                        + " to auto-generate assessments and interviews for any skill.");
    }

    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256")
                    .digest((s == null ? "" : s.trim().toLowerCase()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf((s == null ? "" : s).hashCode());
        }
    }
}
