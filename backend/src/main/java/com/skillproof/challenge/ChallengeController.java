package com.skillproof.challenge;

import com.skillproof.exception.ApiException;
import com.skillproof.security.CurrentUserId;
import com.skillproof.common.RateLimiter;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    record SubmitRequest(String submissionText) {}

    private final ChallengeService challenges;
    private final RateLimiter rateLimiter;

    public ChallengeController(ChallengeService challenges, RateLimiter rateLimiter) {
        this.challenges = challenges;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public Object list(@RequestParam(required = false) String skill, @RequestParam(required = false) String type) {
        return challenges.list(skill, type);
    }

    @GetMapping("/{id}")
    public PracticalChallenge get(@PathVariable Long id) {
        return challenges.get(id);
    }

    @PostMapping("/{id}/submit")
    public ChallengeSubmission submit(@CurrentUserId Long userId, @PathVariable Long id,
                                      @RequestBody SubmitRequest body) {
        if (!rateLimiter.tryAcquire("submit:" + userId, 30, Duration.ofHours(1).toMillis())) {
            throw ApiException.tooMany("Submission limit reached for this hour");
        }
        if (body == null || body.submissionText() == null || body.submissionText().isBlank()) {
            throw ApiException.badRequest("EMPTY_SUBMISSION", "Submission text is required");
        }
        return challenges.submit(userId, id, body.submissionText());
    }
}
