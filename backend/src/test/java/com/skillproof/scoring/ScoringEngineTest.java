package com.skillproof.scoring;

import com.skillproof.config.ScoringProperties;
import com.skillproof.review.DecayEngine;
import com.skillproof.review.ReviewScheduler;
import com.skillproof.config.ReviewProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringEngineTest {

    private final ScoringEngine engine = new ScoringEngine(new ScoringProperties(0.10, 0.30, 0.30, 0.20, 0.10, 40));

    @Test
    void specWeightsProduceExpectedScore() {
        ScoringEngine.Breakdown b = engine.compute(80, 90, 70, 80);
        double expected = 0.10 * 100 + 0.30 * 80 + 0.30 * 90 + 0.20 * 70 + 0.10 * 80;
        assertEquals(Math.round(expected), b.confidence());
        assertEquals(83, b.confidence());
    }

    @Test
    void zeroEvidenceCapsClaimOnly() {
        ScoringEngine.Breakdown b = engine.compute(0, 0, 0, 0);
        assertEquals(10, b.confidence());
    }

    @Test
    void perfectEvidenceIs100() {
        assertEquals(100, engine.compute(100, 100, 100, 100).confidence());
    }

    @Test
    void valuesAreClamped() {
        assertEquals(70, engine.compute(150, -20, 100, 100).confidence());
    }

    @Test
    void invalidWeightsRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new ScoringProperties(0.5, 0.5, 0.5, 0.5, 0.5, 40));
    }
}
