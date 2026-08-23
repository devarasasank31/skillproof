package com.skillproof.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.decay")
public record DecayProperties(double defaultMemoryStrengthDays, double minRetentionFlag) {}
