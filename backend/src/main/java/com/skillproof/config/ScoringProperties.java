package com.skillproof.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.scoring")
public record ScoringProperties(
        double claimWeight,
        double knowledgeWeight,
        double practicalWeight,
        double activityWeight,
        double marketWeight,
        int overclaimKnowledgeThreshold) {

    public ScoringProperties {
        double total = claimWeight + knowledgeWeight + practicalWeight + activityWeight + marketWeight;
        if (total <= 0 || Math.abs(total - 1.0) > 0.01) {
            throw new IllegalStateException("app.scoring weights must sum to 1.0 (got " + total + ")");
        }
    }
}
