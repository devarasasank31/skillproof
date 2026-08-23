package com.skillproof.scoring;

import com.skillproof.config.ReviewProperties;
import com.skillproof.review.DecayEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecayEngineTest {

    private final DecayEngine engine = new DecayEngine(new ReviewProperties(
            new ReviewProperties.Intervals(1, 2, 4, 7, 14)));

    @Test
    void decaySpecExample() {
        double r0 = 0.9;
        double strength = 20.0;
        Instant last = Instant.now().minusSeconds(10L * 86400);
        double retention = engine.predictRetention(r0, strength, last, Instant.now());
        assertEquals(0.9 * Math.exp(-0.5), retention, 0.0001);
    }

    @Test
    void zeroElapsedKeepsRetention() {
        assertEquals(1.0, engine.predictRetention(1.0, 20.0, Instant.now(), Instant.now()), 0.000001);
    }

    @Test
    void highScoreGrowsMemoryStrength() {
        assertTrue(engine.newMemoryStrength(20, 95) > 20);
    }

    @Test
    void lowScoreShrinksMemoryStrength() {
        assertTrue(engine.newMemoryStrength(20, 30) < 20);
    }

    @Test
    void memoryStrengthClamped() {
        assertEquals(365.0, engine.newMemoryStrength(360, 100), 0.0001);
        assertEquals(1.4, engine.newMemoryStrength(2, 5), 0.0001);
        assertEquals(1.0, engine.clampStrength(0.2), 0.0001);
    }
}
