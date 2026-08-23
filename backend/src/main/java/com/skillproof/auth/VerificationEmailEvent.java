package com.skillproof.auth;

public record VerificationEmailEvent(Long userId, String email, String name, String link) {}
