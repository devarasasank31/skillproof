package com.skillproof.github;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "github_repositories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "external_id"}))
public class GitHubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private GitHubProfile profile;

    @Column(name = "external_id", nullable = false)
    private long externalId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "primary_language")
    private String primaryLanguage;

    @Column(columnDefinition = "text")
    private String languages;

    @Column(columnDefinition = "text")
    private String topics;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public GitHubProfile getProfile() { return profile; }
    public void setProfile(GitHubProfile p) { this.profile = p; }
    public long getExternalId() { return externalId; }
    public void setExternalId(long e) { this.externalId = e; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String l) { this.primaryLanguage = l; }
    public String getLanguages() { return languages; }
    public void setLanguages(String l) { this.languages = l; }
    public String getTopics() { return topics; }
    public void setTopics(String t) { this.topics = t; }
    public Instant getPushedAt() { return pushedAt; }
    public void setPushedAt(Instant p) { this.pushedAt = p; }
    public Instant getCreatedAt() { return createdAt; }
}
