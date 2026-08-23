package com.skillproof.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.review")
public record ReviewProperties(Intervals intervals) {

    public record Intervals(int below40, int s40to59, int s60to74, int s75to89, int s90plus) {}

    public int intervalForScore(int score) {
        Map<Integer, Integer> bands = new LinkedHashMap<>();
        bands.put(39, intervals.below40());
        bands.put(59, intervals.s40to59());
        bands.put(74, intervals.s60to74());
        bands.put(89, intervals.s75to89());
        return bands.entrySet().stream()
                .filter(e -> score <= e.getKey())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(intervals.s90plus());
    }
}
