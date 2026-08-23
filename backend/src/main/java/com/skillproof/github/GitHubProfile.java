package com.skillproof.github;

import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "github_profiles")
public class GitHubProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false)
    private String username;

    @Column(name = "public_repos", nullable = false)
    private int publicRepos;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public int getPublicRepos() { return publicRepos; }
    public void setPublicRepos(int r) { this.publicRepos = r; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant f) { this.fetchedAt = f; }
}
