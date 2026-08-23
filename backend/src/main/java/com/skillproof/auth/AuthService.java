package com.skillproof.auth;

import com.skillproof.ai.AiSettingsService;
import com.skillproof.common.RateLimiter;
import com.skillproof.exception.ApiException;
import com.skillproof.security.JwtService;
import com.skillproof.user.User;
import com.skillproof.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final AiSettingsService aiSettings;
    private final VerificationService verification;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService,
                       RateLimiter rateLimiter, AiSettingsService aiSettings,
                       VerificationService verification) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.aiSettings = aiSettings;
        this.verification = verification;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCaseAndDeletedFalse(req.email())) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = new User();
        user.setEmail(req.email().trim().toLowerCase());
        user.setName(req.name().trim());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setVisibility("PRIVATE");
        if (req.ai() != null) {
            aiSettings.apply(user, new AiSettingsService.AiSetup(
                    req.ai().provider(), req.ai().apiKey(), req.ai().baseUrl(), req.ai().model()));
        }
        users.save(user);
        verification.sendLink(user);
        return new RegisterResponse(true,
                "Account created. Check " + user.getEmail() + " for a verification link.");
    }

    public AuthResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCaseAndDeletedFalse(email.trim())
                .orElseThrow(() -> new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password"));
        boolean ok = encoder.matches(password, user.getPasswordHash());
        if (!ok) throw new ApiException(401, "INVALID_CREDENTIALS", "Invalid email or password");
        if (user.getEmailVerifiedAt() == null) {
            throw new ApiException(403, "EMAIL_NOT_VERIFIED",
                    "Please verify your email first - check your inbox for the verification link.");
        }
        return tokens(user);
    }

    public AuthResponse loginRateLimited(String ip, String email, String password) {
        String key = "login:" + ip + ":" + email.toLowerCase();
        if (!rateLimiter.tryAcquire(key, 5, Duration.ofMinutes(1).toMillis())) {
            log.info("Login rate limit hit for {} / {}", ip, email);
            throw ApiException.tooMany("Too many login attempts. Try again in a minute.");
        }
        return login(email, password);
    }

    public AuthResponse refresh(String refreshToken) {
        Long userId = jwtService.parseUserId(refreshToken, "refresh");
        User user = users.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> ApiException.unauthorized("Account not found"));
        return tokens(user);
    }

    private AuthResponse tokens(User user) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                aiSettings.hasKey(user),
                jwtService.issueAccessToken(user.getId()),
                jwtService.issueRefreshToken(user.getId()));
    }
}
