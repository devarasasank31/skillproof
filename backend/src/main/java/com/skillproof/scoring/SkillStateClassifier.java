package com.skillproof.scoring;

import com.skillproof.skill.UserSkill;
import org.springframework.stereotype.Service;

@Service
public class SkillStateClassifier {

    private static final int STRONG_MIN = 70;
    private static final int WEAK_MAX = 39;
    private static final int MASTERED_KNOWLEDGE = 90;
    private static final int MASTERED_PRACTICAL = 75;
    private static final int MASTERED_CONFIDENCE = 85;
    private static final double STALE_RETENTION = 0.60;
    private static final double AT_RISK_RETENTION = 0.80;

    /**
     * Deterministic state rules, evaluated top-down:
     * OVERCLAIMED: measured knowledge exists but is below the overclaim threshold and there is
     *              no practical or activity evidence backing the claim.
     * NEW:         nothing measured yet (no assessment, no challenge, no activity).
     * STALE:       predicted retention collapsed below 60%.
     * WEAK:        confidence at or below 39%.
     * MASTERED:    knowledge >= 90, practical >= 75, confidence >= 85.
     * STRONG:      confidence >= 70 with healthy retention.
     * AT_RISK:     retention below 80% or confidence below 55%.
     * LEARNING:    everything else.
     */
    public UserSkill.State classify(UserSkill us, double predictedRetention, boolean hasAnyAssessment) {
        boolean hasMeasuredKnowledge = us.getKnowledgeScore() > 0;
        boolean lowEvidenceClaim = hasMeasuredKnowledge
                && us.getKnowledgeScore() < overclaimThreshold(us)
                && us.getPracticalScore() == 0
                && us.getActivityScore() == 0;
        if (lowEvidenceClaim) {
            return UserSkill.State.OVERCLAIMED;
        }
        if (!hasAnyAssessment && us.getPracticalScore() == 0 && us.getActivityScore() == 0) {
            return UserSkill.State.NEW;
        }
        if (predictedRetention < STALE_RETENTION) {
            return UserSkill.State.STALE;
        }
        if (us.getConfidence() <= WEAK_MAX) {
            return UserSkill.State.WEAK;
        }
        if (us.getKnowledgeScore() >= MASTERED_KNOWLEDGE
                && us.getPracticalScore() >= MASTERED_PRACTICAL
                && us.getConfidence() >= MASTERED_CONFIDENCE) {
            return UserSkill.State.MASTERED;
        }
        if (us.getConfidence() >= STRONG_MIN && predictedRetention >= AT_RISK_RETENTION) {
            return UserSkill.State.STRONG;
        }
        if (predictedRetention < AT_RISK_RETENTION || us.getConfidence() < 55) {
            return UserSkill.State.AT_RISK;
        }
        return UserSkill.State.LEARNING;
    }

    private int overclaimThreshold(UserSkill us) {
        return 40;
    }
}
