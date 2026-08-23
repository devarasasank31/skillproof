package com.skillproof.challenge;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "challenge_submissions")
public class ChallengeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id")
    private PracticalChallenge challenge;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "submission_text", nullable = false, columnDefinition = "text")
    private String submissionText;

    @Column(nullable = false)
    private int score;

    @Column(name = "correctness", nullable = false)
    private int correctness;

    @Column(name = "completeness", nullable = false)
    private int completeness;

    @Column(name = "best_practices", nullable = false)
    private int bestPractices;

    @Column(name = "checks_passed", nullable = false)
    private int checksPassed;

    @Column(name = "checks_total", nullable = false)
    private int checksTotal;

    @Column(columnDefinition = "text")
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public PracticalChallenge getChallenge() { return challenge; }
    public void setChallenge(PracticalChallenge c) { this.challenge = c; }
    public Long getUserId() { return userId; }
    public void setUserId(Long u) { this.userId = u; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String t) { this.submissionText = t; }
    public int getScore() { return score; }
    public void setScore(int s) { this.score = Math.max(0, Math.min(100, s)); }
    public int getCorrectness() { return correctness; }
    public void setCorrectness(int v) { this.correctness = v; }
    public int getCompleteness() { return completeness; }
    public void setCompleteness(int v) { this.completeness = v; }
    public int getBestPractices() { return bestPractices; }
    public void setBestPractices(int v) { this.bestPractices = v; }
    public int getChecksPassed() { return checksPassed; }
    public void setChecksPassed(int v) { this.checksPassed = v; }
    public int getChecksTotal() { return checksTotal; }
    public void setChecksTotal(int v) { this.checksTotal = v; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String f) { this.feedback = f; }
    public Instant getCreatedAt() { return createdAt; }
}
