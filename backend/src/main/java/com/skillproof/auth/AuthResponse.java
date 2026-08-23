package com.skillproof.auth;

public record AuthResponse(Long userId, String name, String email, boolean hasAiKey,
                           String accessToken, String refreshToken) {}
