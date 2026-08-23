package com.skillproof.auth;

import com.skillproof.common.RateLimiter;
import com.skillproof.exception.ApiException;
import com.skillproof.user.User;
import com.skillproof.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final long TOKEN_TTL_HOURS = 24;

    private final VerificationTokenRepository tokens;
    private final UserRepository users;
    private final RateLimiter rateLimiter;
    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final String appUrl;
    private final SecureRandom random = new SecureRandom();

    public VerificationService(VerificationTokenRepository tokens,
                               UserRepository users,
                               RateLimiter rateLimiter,
                               JavaMailSender mailSender,
                               @Value("${app.mail.enabled:false}") boolean mailEnabled,
                               @Value("${app.mail.from}") String mailFrom,
                               @Value("${app.app-url:http://localhost:5173}") String appUrl) {
        this.tokens = tokens;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.appUrl = appUrl;
    }

    @Transactional
    public void sendLink(User user) {
        if (user.getEmailVerifiedAt() != null) return;
        String rawToken = newToken();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setTokenHash(sha256(rawToken));
        vt.setExpiresAt(Instant.now().plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        tokens.save(vt);

        String link = appUrl + "/verify?token=" + rawToken;
        if (mailEnabled) {
            try {
                sendEmail(user.getEmail(), user.getName(), link);
            } catch (Exception e) {
                log.error("Failed to send verification email to {}", user.getEmail(), e);
                log.info("Verification link for {} (email delivery failed): {}", user.getEmail(), link);
            }
        } else {
            log.info("Mail disabled - verification link for {}: {}", user.getEmail(), link);
        }
    }

    @Transactional
    public void resend(String ip, String email) {
        if (!rateLimiter.tryAcquire("resend-verification:" + ip, 5, 60_000)) {
            throw ApiException.tooMany("Too many verification emails requested. Try again shortly.");
        }
        User user = users.findByEmailIgnoreCaseAndDeletedFalse(email).orElse(null);
        // Always succeed from the caller's perspective; never reveal whether the account exists.
        if (user == null || user.isDeleted() || user.getEmailVerifiedAt() != null) return;
        sendLink(user);
    }

    @Transactional
    public void confirm(String rawToken) {
        VerificationToken vt = tokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new ApiException(400, "INVALID_TOKEN", "This verification link is invalid."));
        if (vt.getUsedAt() != null || vt.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(400, "TOKEN_EXPIRED",
                    "This link has expired or was already used. Request a new one from the login page.");
        }
        vt.setUsedAt(Instant.now());
        User user = vt.getUser();
        user.setEmailVerifiedAt(Instant.now());
        users.save(user);
        log.info("Email verified for user {}", user.getEmail());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void sendEmail(String to, String name, String link) throws Exception {
        var message = mailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(to);
        helper.setSubject("Verify your SkillProof account");
        String safeName = name == null ? "" : name.replace("<", "&lt;");
        helper.setText("""
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:auto;padding:24px">
                  <h2 style="color:#4f46e5;margin:0 0 8px">SkillProof</h2>
                  <p>Hi %s,</p>
                  <p>Confirm your email address to activate your account:</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#4f46e5;color:#fff;text-decoration:none;
                       padding:12px 28px;border-radius:8px;font-weight:bold">Verify my email</a>
                  </p>
                  <p style="color:#64748b;font-size:13px">Or paste this link into your browser:<br>%s</p>
                  <p style="color:#64748b;font-size:13px">This link expires in %d hours.
                     If you didn't sign up, you can ignore this email.</p>
                </div>
                """.formatted(safeName, link, link, TOKEN_TTL_HOURS), true);
        mailSender.send(message);
    }
}
