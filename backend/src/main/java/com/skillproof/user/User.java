package com.skillproof.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    private String headline;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(nullable = false)
    private String visibility = "PRIVATE";

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "ai_provider", length = 20)
    private String aiProvider;

    @Column(name = "ai_api_key_enc", columnDefinition = "text")
    private String aiApiKeyEnc;

    @Column(name = "ai_base_url")
    private String aiBaseUrl;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) {
        if (!"PRIVATE".equals(visibility) && !"PUBLIC".equals(visibility)) {
            throw new IllegalArgumentException("visibility must be PRIVATE or PUBLIC");
        }
        this.visibility = visibility;
    }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
    public String getAiApiKeyEnc() { return aiApiKeyEnc; }
    public void setAiApiKeyEnc(String aiApiKeyEnc) { this.aiApiKeyEnc = aiApiKeyEnc; }
    public String getAiBaseUrl() { return aiBaseUrl; }
    public void setAiBaseUrl(String aiBaseUrl) { this.aiBaseUrl = aiBaseUrl; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
