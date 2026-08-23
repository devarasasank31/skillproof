package com.skillproof.assessment;

import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "answers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "user_id"}))
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "answer_text", nullable = false, columnDefinition = "text")
    private String answerText;

    @Column(nullable = false)
    private int score;

    private Boolean correct;

    @Column(name = "evaluation_source", nullable = false)
    private String evaluationSource = "DETERMINISTIC";

    @Column(columnDefinition = "text")
    private String feedback;

    @Column(name = "missing_concepts", columnDefinition = "text")
    private String missingConcepts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public void setQuestion(Question q) { this.question = q; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String t) { this.answerText = t; }
    public int getScore() { return score; }
    public void setScore(int s) { this.score = Math.max(0, Math.min(100, s)); }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean c) { this.correct = c; }
    public String getEvaluationSource() { return evaluationSource; }
    public void setEvaluationSource(String s) { this.evaluationSource = s; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String f) { this.feedback = f; }
    public String getMissingConcepts() { return missingConcepts; }
    public void setMissingConcepts(String m) { this.missingConcepts = m; }
    public Instant getCreatedAt() { return createdAt; }
}
