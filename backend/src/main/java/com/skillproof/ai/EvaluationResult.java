package com.skillproof.ai;

import java.util.List;

public record EvaluationResult(int score, String feedback, List<String> missingConcepts, boolean valid) {
    public static EvaluationResult invalid() {
        return new EvaluationResult(0, "", List.of(), false);
    }
}
