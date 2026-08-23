package com.skillproof.job;

import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    private String company;

    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    private String rawText;

    private Integer readiness;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getCompany() { return company; }
    public void setCompany(String c) { this.company = c; }
    public String getRawText() { return rawText; }
    public void setRawText(String t) { this.rawText = t; }
    public Integer getReadiness() { return readiness; }
    public void setReadiness(Integer r) { this.readiness = r; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant c) { this.createdAt = c; }
}
