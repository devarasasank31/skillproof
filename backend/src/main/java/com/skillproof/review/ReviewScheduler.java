package com.skillproof.review;

import com.skillproof.config.ReviewProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReviewScheduler {

    private final ReviewProperties props;

    public ReviewScheduler(ReviewProperties props) {
        this.props = props;
    }

    public int intervalDaysForScore(int score) {
        return props.intervalForScore(score);
    }

    public Instant nextReviewAt(int score, Instant from) {
        return from.plusSeconds(intervalDaysForScore(score) * 86400L);
    }
}
