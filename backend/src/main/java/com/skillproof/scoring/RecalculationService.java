package com.skillproof.scoring;

import com.skillproof.assessment.Assessment;
import com.skillproof.assessment.AssessmentRepository;
import com.skillproof.challenge.ChallengeSubmissionRepository;
import com.skillproof.config.DecayProperties;
import com.skillproof.evidence.KnowledgeEventRepository;
import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.job.JobSkillRepository;
import com.skillproof.review.Review;
import com.skillproof.review.DecayEngine;
import com.skillproof.review.ReviewRepository;
import com.skillproof.review.ReviewScheduler;
import com.skillproof.skill.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class RecalculationService {

    private final ScoringEngine scoringEngine;
    private final SkillStateClassifier classifier;
    private final DecayEngine decayEngine;
    private final ReviewScheduler reviewScheduler;
    private final AssessmentRepository assessments;
    private final ChallengeSubmissionRepository submissions;
    private final JobSkillRepository jobSkills;
    private final com.skillproof.job.JobDescriptionRepository jobDescriptions;
    private final SkillScoreRepository snapshots;
    private final ReviewRepository reviews;
    private final UserSkillRepository userSkills;
    private final DecayProperties decayProps;

    public RecalculationService(ScoringEngine scoringEngine, SkillStateClassifier classifier,
                                DecayEngine decayEngine, ReviewScheduler reviewScheduler,
                                AssessmentRepository assessments,
                                ChallengeSubmissionRepository submissions,
                                JobSkillRepository jobSkills,
                                com.skillproof.job.JobDescriptionRepository jobDescriptions,
                                SkillScoreRepository snapshots,
                                ReviewRepository reviews, UserSkillRepository userSkills,
                                DecayProperties decayProps) {
        this.scoringEngine = scoringEngine;
        this.classifier = classifier;
        this.decayEngine = decayEngine;
        this.reviewScheduler = reviewScheduler;
        this.assessments = assessments;
        this.submissions = submissions;
        this.jobSkills = jobSkills;
        this.jobDescriptions = jobDescriptions;
        this.snapshots = snapshots;
        this.reviews = reviews;
        this.userSkills = userSkills;
        this.decayProps = decayProps;
    }

    @Transactional
    public void recalculateUserSkill(UserSkill us) {
        Long userId = us.getUser().getId();
        Long skillId = us.getSkill().getId();

        int knowledge = (int) Math.round(assessments.averageScore(userId, skillId));
        long completedAssessments = assessments.countByUserIdAndSkillIdAndStatus(
                userId, skillId, Assessment.Status.COMPLETED);
        int practical = practicalEvidence(us);
        int activity = activityEvidence(us);
        int market = marketEvidence(userId, skillId);

        ScoringEngine.Breakdown b = scoringEngine.compute(knowledge, practical, activity, market);

        Instant anchor = us.getLastReviewedAt() != null ? us.getLastReviewedAt() : us.getLastActivityAt() != null ? us.getLastActivityAt() : us.getCreatedAt();
        double r0 = completedAssessments > 0 || us.getPracticalScore() > 0 ? Math.max(0.35, knowledge / 100.0) : 1.0;
        double predictedRetention = decayEngine.predictRetention(r0, us.getMemoryStrength(), anchor, Instant.now());
        if (us.getLastReviewedAt() != null && us.getRetention() > 0 && us.getRetention() < 1.0) {
            predictedRetention = decayEngine.predictRetention(us.getRetention(), us.getMemoryStrength(),
                    us.getLastReviewedAt(), Instant.now());
        }
        us.setRetention(predictedRetention);

        boolean hasAnyAssessment = completedAssessments > 0;
        UserSkill.State newState = classifier.classify(us, predictedRetention, hasAnyAssessment);

        us.setKnowledgeScore(knowledge);
        us.setPracticalScore(practical);
        us.setActivityScore(activity);
        us.setMarketScore(market);
        us.setConfidence(b.confidence());
        us.setState(newState);
        us.setUpdatedAt(Instant.now());

        if (us.getNextReviewAt() == null && hasAnyAssessment) {
            us.setNextReviewAt(reviewScheduler.nextReviewAt(knowledge == 0 ? 50 : knowledge, us.getCreatedAt()));
            Review review = new Review();
            review.setUserSkill(us);
            review.setDueAt(us.getNextReviewAt());
            reviews.save(review);
        } else if (us.getNextReviewAt() != null && !reviews.existsByUserSkillIdAndStatus(us.getId(), Review.Status.DUE)) {
            Review review = new Review();
            review.setUserSkill(us);
            review.setDueAt(us.getNextReviewAt());
            reviews.save(review);
        }

        SkillScore snap = new SkillScore();
        snap.setUserSkill(us);
        snap.setConfidence(b.confidence());
        snap.setKnowledge(knowledge);
        snap.setPractical(practical);
        snap.setActivity(activity);
        snap.setMarket(market);
        snap.setState(newState.name());
        snapshots.save(snap);
    }

    @Transactional
    public void recalculateUser(Long userId) {
        for (UserSkill us : userSkills.findByUserId(userId)) {
            recalculateUserSkill(us);
        }
    }

    private int activityEvidence(UserSkill us) {
        Instant last = us.getLastActivityAt();
        if (last == null) return 0;
        long days = Duration.between(last, Instant.now()).toDays();
        if (days <= 7) return 100;
        if (days <= 14) return 90;
        if (days <= 30) return 75;
        if (days <= 60) return 55;
        if (days <= 90) return 35;
        if (days <= 180) return 15;
        return 0;
    }

    private int marketEvidence(Long userId, Long skillId) {
        long totalJobs = jobDescriptions.countByUserId(userId);
        if (totalJobs == 0) return 0;
        long requiring = jobSkills.countJobsRequiring(userId, skillId);
        return (int) Math.min(100, Math.round(requiring * 100.0 / totalJobs));
    }

    private int practicalEvidence(UserSkill us) {
        String skillName = us.getSkill().getName();
        Integer avgBest = submissions.avgBestSubmissionScore(us.getUser().getId(), skillName);
        Integer coverage = submissions.practicalCoverage(us.getUser().getId(), skillName);
        int a = avgBest == null ? 0 : avgBest;
        int b = coverage == null ? 0 : coverage;
        return Math.min(100, (int) Math.round(a * 0.6 + b * 0.4));
    }
}
