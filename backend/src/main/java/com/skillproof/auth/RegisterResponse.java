package com.skillproof.auth;

public record RegisterResponse(boolean needsVerification, String message) {}
