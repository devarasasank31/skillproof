package com.skillproof.review;

import com.skillproof.config.ReviewProperties;
import com.skillproof.skill.UserSkill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DecayEngine {

    private final ReviewProperties reviewProps;

    public DecayEngine(ReviewProperties reviewProps) {
        this.reviewProps = reviewProps;
    }

    public int intervalDaysForScore(int score) {
        return reviewProps.intervalForScore(score);
    }

    public double predictRetention(double retentionAfterLastReview, double memoryStrengthDays, Instant lastEvent, Instant now) {
        if (lastEvent == null) return 1.0;
        double days = Math.max(0, Duration.between(lastEvent, now).toSeconds() / 86400.0);
        return retentionAfterLastReview * Math.exp(-days / memoryStrengthDays);
    }

    public double newMemoryStrength(double currentStrength, int reviewScorePercent) {
        double factor;
        if (reviewScorePercent >= 90) factor = 1.40;
        else if (reviewScorePercent >= 75) factor = 1.20;
        else if (reviewScorePercent >= 60) factor = 1.05;
        else if (reviewScorePercent >= 40) factor = 0.90;
        else factor = 0.70;
        return clampStrength(currentStrength * factor);
    }

    public double clampStrength(double s) {
        return Math.max(1.0, Math.min(365.0, s));
    }

    @Transactional
    public void applyReview(UserSkill us, int score) {
        Instant now = Instant.now();
        us.setMemoryStrength(newMemoryStrength(us.getMemoryStrength(), score));
        us.setRetention(score / 100.0);
        us.setLastReviewedAt(now);
        us.setNextReviewAt(now.plusSeconds(reviewProps.intervalForScore(score) * 86400L));
        us.setUpdatedAt(now);
    }

    @Transactional
    public void refreshPredictedRetention(UserSkill us) {
        Instant anchor = us.getLastReviewedAt() != null ? us.getLastReviewedAt() : us.getCreatedAt();
        double predicted = predictRetention(us.getRetention(), us.getMemoryStrength(), anchor, Instant.now());
        us.setRetention(predicted);
        us.setUpdatedAt(Instant.now());
    }
}
