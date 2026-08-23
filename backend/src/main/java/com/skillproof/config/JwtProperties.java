package com.skillproof.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, int accessTokenMinutes, int refreshTokenDays) {}
