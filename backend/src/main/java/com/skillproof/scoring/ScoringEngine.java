package com.skillproof.scoring;

import com.skillproof.config.ScoringProperties;
import org.springframework.stereotype.Service;

@Service
public class ScoringEngine {

    public record Breakdown(int claim, int knowledge, int practical, int activity, int market, int confidence) {}

    private final ScoringProperties props;

    public ScoringEngine(ScoringProperties props) {
        this.props = props;
    }

    public int claimEvidence(boolean claimed) {
        return claimed ? 100 : 0;
    }

    public Breakdown compute(int knowledge, int practical, int activity, int market) {
        double raw = props.claimWeight() * 100
                + props.knowledgeWeight() * clamp(knowledge)
                + props.practicalWeight() * clamp(practical)
                + props.activityWeight() * clamp(activity)
                + props.marketWeight() * clamp(market);
        return new Breakdown(100, clamp(knowledge), clamp(practical), clamp(activity), clamp(market),
                (int) Math.round(raw));
    }

    public int overclaimKnowledgeThreshold() {
        return props.overclaimKnowledgeThreshold();
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
