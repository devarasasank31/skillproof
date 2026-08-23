package com.skillproof.scheduler;

import com.skillproof.recommendation.RecommendationEngine;
import com.skillproof.user.User;
import com.skillproof.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyJob {

    private static final Logger log = LoggerFactory.getLogger(DailyJob.class);

    private final UserRepository users;
    private final RecommendationEngine engine;

    public DailyJob(UserRepository users, RecommendationEngine engine) {
        this.users = users;
        this.engine = engine;
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "UTC")
    public void refreshRecommendations() {
        for (User u : users.findAll()) {
            if (u.isDeleted()) continue;
            try {
                engine.refreshDaily(u.getId());
            } catch (Exception e) {
                log.warn("Failed to refresh recommendations for user {}: {}", u.getId(), e.getMessage());
            }
        }
    }
}
