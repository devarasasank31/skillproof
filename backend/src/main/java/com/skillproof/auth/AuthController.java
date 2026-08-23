package com.skillproof.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    record RefreshRequest(@NotBlank String refreshToken) {}
    record VerifyRequest(@NotBlank String token) {}
    record ResendRequest(@NotBlank @Email String email) {}

    private final AuthService authService;
    private final VerificationService verificationService;

    public AuthController(AuthService authService, VerificationService verificationService) {
        this.authService = authService;
        this.verificationService = verificationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@jakarta.validation.Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    public java.util.Map<String, String> verify(@jakarta.validation.Valid @RequestBody VerifyRequest body) {
        verificationService.confirm(body.token());
        return java.util.Map.of("message", "Email verified. You can now sign in.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resend(@jakarta.validation.Valid @RequestBody ResendRequest body,
                                       HttpServletRequest http) {
        verificationService.resend(clientIp(http), body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public AuthResponse login(@jakarta.validation.Valid @RequestBody LoginRequest body,
                              HttpServletRequest http) {
        String ip = clientIp(http);
        return authService.loginRateLimited(ip, body.email(), body.password());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@jakarta.validation.Valid @RequestBody RefreshRequest body) {
        return authService.refresh(body.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest http) {
        String fwd = http.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
