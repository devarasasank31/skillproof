package com.skillproof.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Tiny unauthenticated liveness probe. Used by the login page to detect/measure a cold start
 * and by the keep-alive workflow to prevent Render's free instance from sleeping.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "time", Instant.now().toString());
    }
}