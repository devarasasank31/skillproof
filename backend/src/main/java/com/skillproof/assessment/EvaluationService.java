package com.skillproof.assessment;

import com.skillproof.ai.EvaluationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class EvaluationService {

    public record Result(int score, Boolean correct, String source, String feedback, List<String> missingConcepts) {}

    public Result evaluate(Question q, String answerText, EvaluationResult aiEval) {
        if (aiEval != null && aiEval.valid()) {
            return new Result(aiEval.score(), aiEval.score() >= 60, "AI", aiEval.feedback(),
                    aiEval.missingConcepts());
        }
        if ("MCQ".equalsIgnoreCase(q.getType())) {
            return evaluateMcq(q, answerText);
        }
        return evaluateKeywords(q, answerText);
    }

    private Result evaluateMcq(Question q, String answerText) {
        boolean correct = q.getAnswerKey() != null
                && q.getAnswerKey().trim().equalsIgnoreCase(safe(answerText).trim());
        int score = correct ? 100 : 0;
        String fb = correct ? "Correct." : "Incorrect. Correct answer: " + q.getAnswerKey()
                + (q.getExplanation() != null && !q.getExplanation().isBlank() ? ". " + q.getExplanation() : "");
        return new Result(score, correct, "DETERMINISTIC", fb, List.of());
    }

    private Result evaluateKeywords(Question q, String answerText) {
        String lower = safe(answerText).toLowerCase(Locale.ROOT);
        List<String> keywords = parseKeywords(q.getKeywords());
        if (keywords.isEmpty()) {
            return new Result(0, null, "MANUAL_REQUIRED",
                    "No deterministic key available for this question. Marked for manual review.",
                    List.of());
        }
        List<String> hit = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        for (String kw : keywords) {
            boolean ok = kw.contains("|")
                    ? Arrays.stream(kw.split("\\|")).map(String::trim).anyMatch(lower::contains)
                    : lower.contains(kw.toLowerCase(Locale.ROOT));
            if (ok) hit.add(kw); else missed.add(kw);
        }
        int score = (int) Math.round(100.0 * hit.size() / keywords.size());
        if (safe(answerText).length() < 40) score = (int) (score * 0.6);
        String fb = "Detected " + hit.size() + "/" + keywords.size() + " expected concepts."
                + (missed.isEmpty() ? "" : " Missing: " + String.join(", ", missed) + ".");
        return new Result(score, score >= 60, "DETERMINISTIC", fb, missed);
    }

    static List<String> parseKeywords(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
