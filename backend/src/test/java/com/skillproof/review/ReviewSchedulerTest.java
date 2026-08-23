package com.skillproof.review;

import com.skillproof.config.ReviewProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewSchedulerTest {

    private final ReviewProperties props =
            new ReviewProperties(new ReviewProperties.Intervals(1, 2, 4, 7, 14));
    private final ReviewScheduler scheduler = new ReviewScheduler(props);

    @Test
    void score95GetsLongerIntervalThanScore30() {
        assertTrue(scheduler.intervalDaysForScore(95) > scheduler.intervalDaysForScore(30));
    }

    @Test
    void bandBoundaries() {
        assertEquals(1, scheduler.intervalDaysForScore(39));
        assertEquals(2, scheduler.intervalDaysForScore(40));
        assertEquals(2, scheduler.intervalDaysForScore(59));
        assertEquals(4, scheduler.intervalDaysForScore(60));
        assertEquals(4, scheduler.intervalDaysForScore(74));
        assertEquals(7, scheduler.intervalDaysForScore(75));
        assertEquals(7, scheduler.intervalDaysForScore(89));
        assertEquals(14, scheduler.intervalDaysForScore(90));
        assertEquals(14, scheduler.intervalDaysForScore(100));
    }

    @Test
    void nextReviewDateIsFromNow() {
        Instant now = Instant.now();
        Instant next = scheduler.nextReviewAt(50, now);
        assertEquals(now.plus(2, ChronoUnit.DAYS), next);
    }
}
