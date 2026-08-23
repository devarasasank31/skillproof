package com.skillproof.auth;

import com.skillproof.common.RateLimiter;
import com.skillproof.exception.ApiException;
import com.skillproof.user.User;
import com.skillproof.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    private final ApplicationEventPublisher events;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final String brevoApiKey;
    private final String appUrl;
    private final SecureRandom random = new SecureRandom();

    public VerificationService(VerificationTokenRepository tokens,
                               UserRepository users,
                               RateLimiter rateLimiter,
                               JavaMailSender mailSender,
                               ApplicationEventPublisher events,
                               @Value("${app.mail.enabled:false}") boolean mailEnabled,
                               @Value("${app.mail.from}") String mailFrom,
                               @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
                               @Value("${app.app-url:http://localhost:5173}") String appUrl) {
        this.tokens = tokens;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.mailSender = mailSender;
        this.events = events;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        // Trim guards against invisible whitespace/newlines pasted into the Render env editor,
        // which otherwise produce a header Brevo rejects as "Key not found".
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.appUrl = appUrl;
    }

    /**
     * Persists the token and publishes the email event. Fast, DB-only work happens
     * synchronously; the SMTP round-trip is dispatched after the transaction commits
     * so a slow/broken mail server can never block or fail registration.
     */
    @Transactional
    public void sendLink(User user) {
        if (user.getEmailVerifiedAt() != null) return;
        String rawToken = newToken();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setTokenHash(sha256(rawToken));
        vt.setExpiresAt(Instant.now().plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        tokens.save(vt);
        events.publishEvent(new VerificationEmailEvent(
                user.getId(), user.getEmail(), user.getName(),
                appUrl + "/verify?token=" + rawToken));
    }

    /** Logs a clear verdict about mail config at startup so bad keys are obvious in Render logs. */
    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void checkMailSetup() {
        if (!brevoApiKey.isBlank()) {
            String masked = brevoApiKey.length() > 12
                    ? brevoApiKey.substring(0, 8) + "..." + brevoApiKey.substring(brevoApiKey.length() - 4)
                    : brevoApiKey;
            boolean looksLikeV3Key = brevoApiKey.startsWith("xkeysib-");
            log.info("MAIL CHECK: testing Brevo key {} (length {}, v3-format={})",
                    masked, brevoApiKey.length(), looksLikeV3Key);
            if (!looksLikeV3Key) {
                log.error("MAIL CHECK: key does NOT start with 'xkeysib-'. You copied the wrong credential. "
                        + "In Brevo go to SMTP & API -> API Keys (not SMTP keys) -> Generate new key.");
                return;
            }
            try {
                var req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.brevo.com/v3/account"))
                        .header("api-key", brevoApiKey)
                        .GET().build();
                var resp = java.net.http.HttpClient.newHttpClient()
                        .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    log.info("MAIL CHECK: Brevo API key valid - verification emails will send over HTTPS");
                } else {
                    log.error("MAIL CHECK FAILED: Brevo returned {} {} - replace BREVO_API_KEY with a fresh key",
                            resp.statusCode(), resp.body());
                }
            } catch (Exception e) {
                log.error("MAIL CHECK ERROR: {}", e.toString());
            }
        } else if (mailEnabled) {
            log.warn("MAIL CHECK: BREVO_API_KEY empty - using SMTP, which Render's free tier BLOCKS");
        } else {
            log.info("MAIL CHECK: email sending disabled - verification links go to the console log");
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliverEmail(VerificationEmailEvent e) {
        User user = users.findById(e.userId()).orElse(null);
        if (user == null || user.isDeleted() || user.getEmailVerifiedAt() != null) return;
        long start = System.currentTimeMillis();
        try {
            if (!brevoApiKey.isBlank()) {
                // Render free tier blocks outbound SMTP (25/465/587); Brevo's HTTPS API works everywhere.
                sendViaBrevo(e.email(), e.name(), e.link());
            } else if (mailEnabled) {
                sendEmail(e.email(), e.name(), e.link());
            } else {
                log.info("Mail disabled - verification link for {}: {}", e.email(), e.link());
                return;
            }
            log.info("Verification email sent to {} in {}ms", e.email(), System.currentTimeMillis() - start);
        } catch (Exception ex) {
            log.error("Failed to send verification email to {} (took {}ms): {}",
                    e.email(), System.currentTimeMillis() - start, ex.toString());
            log.info("Fallback verification link for {}: {}", e.email(), e.link());
        }
    }

    private void sendViaBrevo(String to, String name, String link) throws Exception {
        String fromAddress = extractAddress(mailFrom);
        var om = new com.fasterxml.jackson.databind.ObjectMapper();
        var root = om.createObjectNode();
        root.putObject("sender").put("name", "SkillProof").put("email", fromAddress);
        root.putArray("to").addObject().put("email", to);
        root.put("subject", "Verify your SkillProof account");
        root.put("htmlContent", buildHtml(name, link));
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", brevoApiKey)
                .header("content-type", "application/json")
                .header("accept", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(om.writeValueAsString(root)))
                .build();
        var response = java.net.http.HttpClient.newHttpClient()
                .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Brevo API returned " + response.statusCode()
                    + ": " + response.body());
        }
    }

    private String extractAddress(String from) {
        var m = java.util.regex.Pattern.compile("<([^>]+)>").matcher(from);
        return m.find() ? m.group(1) : from.trim();
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
        helper.setText(buildHtml(name, link), true);
        mailSender.send(message);
    }

    private String buildHtml(String name, String link) {
        String safeName = name == null ? "" : name.replace("<", "&lt;");
        return """
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
                """.formatted(safeName, link, link, TOKEN_TTL_HOURS);
    }
}
