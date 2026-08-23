package com.skillproof.seed;

import com.skillproof.assessment.Assessment;
import com.skillproof.assessment.AssessmentRepository;
import com.skillproof.challenge.PracticalChallenge;
import com.skillproof.challenge.ChallengeRepository;
import com.skillproof.challenge.ChallengeSubmission;
import com.skillproof.challenge.ChallengeSubmissionRepository;
import com.skillproof.challenge.ChallengeSubmission;
import com.skillproof.challenge.ChallengeSubmissionRepository;
import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.job.JobDescription;
import com.skillproof.job.JobDescriptionRepository;
import com.skillproof.job.JobSkill;
import com.skillproof.job.JobSkillRepository;
import com.skillproof.review.DecayEngine;
import com.skillproof.review.Review;
import com.skillproof.review.ReviewRepository;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.skill.*;
import com.skillproof.user.User;
import com.skillproof.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class DemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    public static final String DEMO_EMAIL = "demo@skillproof.dev";
    public static final String DEMO_PASSWORD = "Demo1234!";

    private final UserRepository users;
    private final SkillRepository skills;
    private final UserSkillRepository userSkills;
    private final AssessmentRepository assessments;
    private final ReviewRepository reviews;
    private final ChallengeRepository challenges;
    private final ChallengeSubmissionRepository submissions;
    private final SkillEvidenceRepository evidence;
    private final JobDescriptionRepository jobs;
    private final JobSkillRepository jobSkills;
    private final PasswordEncoder encoder;
    private final RecalculationService recalculation;
    private final boolean enabled;

    public DemoSeeder(UserRepository users, SkillRepository skills, UserSkillRepository userSkills,
                      AssessmentRepository assessments, ReviewRepository reviews,
                      ChallengeRepository challenges, ChallengeSubmissionRepository submissions,
                      SkillEvidenceRepository evidence, JobDescriptionRepository jobs,
                      JobSkillRepository jobSkills, PasswordEncoder encoder,
                      RecalculationService recalculation,
                      @Value("${app.seed.demo-user:true}") boolean enabled) {
        this.users = users;
        this.skills = skills;
        this.userSkills = userSkills;
        this.assessments = assessments;
        this.reviews = reviews;
        this.challenges = challenges;
        this.submissions = submissions;
        this.evidence = evidence;
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.encoder = encoder;
        this.recalculation = recalculation;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (users.existsByEmailIgnoreCaseAndDeletedFalse(DEMO_EMAIL)) return;
        log.info("Seeding demo account {} ...", DEMO_EMAIL);

        User demo = new User();
        demo.setEmail(DEMO_EMAIL);
        demo.setName("Sasank");
        demo.setHeadline("Backend Software Engineer");
        demo.setBio("Demo profile with realistic history: strong Java/Spring, decaying AWS, an overclaimed Kafka.");
        demo.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        demo.setEmailVerifiedAt(Instant.now());
        users.save(demo);

        Instant now = Instant.now();

        seedJava(demo, now);
        seedSpringBoot(demo, now);
        seedPostgres(demo, now);
        seedDocker(demo, now);
        seedAws(demo, now);
        seedKafka(demo, now);
        seedReact(demo, now);
        seedDsa(demo, now);
        seedSystemDesign(demo, now);

        seedJob(demo, "Senior Backend Engineer - Payments", "Stripe-like fintech",
                """
                        We are hiring a Senior Backend Engineer. Requirements:
                        - 5+ years Java and Spring Boot experience
                        - Strong PostgreSQL and SQL tuning skills
                        - Experience with Kafka event streaming
                        - Docker and Kubernetes in production
                        - AWS cloud experience (EC2, S3, RDS)
                        - Solid DSA fundamentals and system design skills
                        """,
                List.of("Java", "Spring Boot", "PostgreSQL", "Kafka", "Docker", "Kubernetes", "AWS", "DSA",
                        "System Design"), now.minus(3, ChronoUnit.DAYS));

        seedJob(demo, "Full Stack Engineer", "SaaS startup",
                """
                        Full Stack Engineer role:
                        - React and TypeScript frontend
                        - Node.js or Java backend
                        - REST API design
                        - PostgreSQL
                        - Redis caching
                        """,
                List.of("React", "TypeScript", "Node.js", "REST API", "PostgreSQL", "Redis"),
                now.minus(10, ChronoUnit.DAYS));

        for (UserSkill us : userSkills.findByUserId(demo.getId())) {
            recalculation.recalculateUserSkill(us);
        }
        log.info("Demo seed complete. Login with {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
    }

    private void seedJava(User u, Instant now) {
        UserSkill us = claim(u, "Java", now.minus(120, ChronoUnit.DAYS));
        assessment(u, us, 92, now.minus(60, ChronoUnit.DAYS), "MEDIUM");
        assessment(u, us, 95, now.minus(20, ChronoUnit.DAYS), "HARD");
        challenge(us, u, "lru-cache", 88, now.minus(45, ChronoUnit.DAYS));
        activity(us, now.minus(5, ChronoUnit.DAYS));
        evidence(us, SkillEvidence.Type.ACTIVITY, "Used in GitHub project: payment-api", 5,
                now.minus(12, ChronoUnit.DAYS));
    }

    private void seedSpringBoot(User u, Instant now) {
        UserSkill us = claim(u, "Spring Boot", now.minus(100, ChronoUnit.DAYS));
        assessment(u, us, 84, now.minus(30, ChronoUnit.DAYS), "MEDIUM");
        challenge(us, u, "rest-validation-endpoint", 82, now.minus(15, ChronoUnit.DAYS));
        activity(us, now.minus(8, ChronoUnit.DAYS));
    }

    private void seedPostgres(User u, Instant now) {
        UserSkill us = claim(u, "PostgreSQL", now.minus(200, ChronoUnit.DAYS));
        assessment(u, us, 81, now.minus(25, ChronoUnit.DAYS), "MEDIUM");
        challenge(us, u, "top-customers-sql", 90, now.minus(40, ChronoUnit.DAYS));
        activity(us, now.minus(18, ChronoUnit.DAYS));
    }

    private void seedDocker(User u, Instant now) {
        UserSkill us = claim(u, "Docker", now.minus(90, ChronoUnit.DAYS));
        assessment(u, us, 58, now.minus(50, ChronoUnit.DAYS), "EASY");
        activity(us, now.minus(70, ChronoUnit.DAYS));
    }

    private void seedAws(User u, Instant now) {
        UserSkill us = claim(u, "AWS", now.minus(150, ChronoUnit.DAYS));
        assessment(u, us, 62, now.minus(80, ChronoUnit.DAYS), "MEDIUM");
        activity(us, now.minus(85, ChronoUnit.DAYS));
    }

    private void seedKafka(User u, Instant now) {
        UserSkill us = claim(u, "Kafka", now.minus(30, ChronoUnit.DAYS));
        assessment(u, us, 29, now.minus(7, ChronoUnit.DAYS), "MEDIUM");
        evidence(us, SkillEvidence.Type.CLAIM, "Listed on resume as 'experienced with Kafka'", 0,
                now.minus(30, ChronoUnit.DAYS));
    }

    private void seedReact(User u, Instant now) {
        UserSkill us = claim(u, "React", now.minus(80, ChronoUnit.DAYS));
        assessment(u, us, 77, now.minus(22, ChronoUnit.DAYS), "MEDIUM");
        activity(us, now.minus(14, ChronoUnit.DAYS));
    }

    private void seedDsa(User u, Instant now) {
        UserSkill us = claim(u, "DSA", now.minus(180, ChronoUnit.DAYS));
        assessment(u, us, 86, now.minus(35, ChronoUnit.DAYS), "MEDIUM");
        activity(us, now.minus(28, ChronoUnit.DAYS));
    }

    private void seedSystemDesign(User u, Instant now) {
        UserSkill us = claim(u, "System Design", now.minus(60, ChronoUnit.DAYS));
        assessment(u, us, 48, now.minus(12, ChronoUnit.DAYS), "HARD");
        activity(us, now.minus(12, ChronoUnit.DAYS));
    }

    private UserSkill claim(User u, String skillName, Instant at) {
        Skill s = skills.findByNameIgnoreCase(skillName).orElseThrow(
                () -> new IllegalStateException("Catalog skill missing: " + skillName + " (run migrations first)"));
        UserSkill us = new UserSkill();
        us.setUser(u);
        us.setSkill(s);
        us.setClaimSource(skillName.equals("Kafka") ? "RESUME" : "MANUAL");
        userSkills.save(us);

        SkillEvidence ev = new SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(u.getId());
        ev.setEvidenceType(SkillEvidence.Type.CLAIM);
        ev.setDescription(skillName.equals("Kafka")
                ? "Detected in uploaded resume" : "Claimed manually");
        ev.setOccurredAt(at);
        evidence.save(ev);
        return us;
    }

    private void assessment(User u, UserSkill us, int score, Instant at, String difficulty) {
        Assessment a = new Assessment();
        a.setUser(u);
        a.setSkill(us.getSkill());
        a.setDifficulty(difficulty);
        a.setStatus(Assessment.Status.COMPLETED);
        a.setScore(score);
        a.setQuestionCount(4);
        a.setCreatedAt(at);
        a.setCompletedAt(at.plusSeconds(540));
        assessments.save(a);

        SkillEvidence ev = new SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(u.getId());
        ev.setEvidenceType(SkillEvidence.Type.KNOWLEDGE);
        ev.setDescription("Passed " + difficulty + " assessment with score " + score + "%");
        ev.setPoints(score);
        ev.setOccurredAt(at);
        evidence.save(ev);

        Review r = new Review();
        r.setUserSkill(us);
        r.setDueAt(at);
        r.setStatus(Review.Status.COMPLETED);
        r.setScore(score);
        r.setCompletedAt(at);
        reviews.save(r);

        us.setLastReviewedAt(at);
        double strength = Math.max(1.0, Math.min(365.0, 20 * factorFor(score)));
        us.setMemoryStrength(strength);
        us.setRetention(score / 100.0);
        us.setNextReviewAt(at.plusSeconds(intervalFor(score) * 86400L));
        userSkills.save(us);
    }

    private double factorFor(int s) {
        if (s >= 90) return 1.4;
        if (s >= 75) return 1.2;
        if (s >= 60) return 1.05;
        if (s >= 40) return 0.9;
        return 0.7;
    }

    private int intervalFor(int s) {
        if (s <= 39) return 1;
        if (s <= 59) return 2;
        if (s <= 74) return 4;
        if (s <= 89) return 7;
        return 14;
    }

    private void challenge(UserSkill us, User u, String slug, int score, Instant at) {
        PracticalChallenge c = challenges.findBySlug(slug).orElseThrow(
                () -> new IllegalStateException("Challenge missing: " + slug));
        ChallengeSubmission sub = new ChallengeSubmission();
        sub.setChallenge(c);
        sub.setUserId(u.getId());
        sub.setSubmissionText("Demo submission for " + c.getTitle() + ". Implemented the core approach with "
                + "appropriate data structures, handled edge cases and validated behavior with tests.");
        sub.setScore(score);
        sub.setCorrectness(Math.min(100, score + 4));
        sub.setCompleteness(score);
        sub.setBestPractices(Math.max(40, score - 6));
        sub.setChecksPassed(2);
        sub.setChecksTotal(3);
        sub.setFeedback("Deterministic checks passed for key concepts; rubric review recommended.");
        submissions.save(sub);

        SkillEvidence ev = new SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(u.getId());
        ev.setEvidenceType(SkillEvidence.Type.PRACTICAL);
        ev.setDescription("Completed " + c.getType() + " challenge: " + c.getTitle() + " (score " + score + "%)");
        ev.setPoints(score);
        ev.setOccurredAt(at);
        evidence.save(ev);
        if (us.getLastActivityAt() == null || at.isAfter(us.getLastActivityAt())) {
            us.setLastActivityAt(at);
            userSkills.save(us);
        }
    }

    private void activity(UserSkill us, Instant at) {
        us.setLastActivityAt(at);
        userSkills.save(us);
        SkillEvidence ev = new SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(us.getUser().getId());
        ev.setEvidenceType(SkillEvidence.Type.GITHUB);
        ev.setDescription("Recent GitHub activity detected");
        ev.setPoints(5);
        ev.setOccurredAt(at);
        evidence.save(ev);
    }

    private void evidence(UserSkill us, SkillEvidence.Type type, String description, int points, Instant at) {
        SkillEvidence ev = new SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(us.getUser().getId());
        ev.setEvidenceType(type);
        ev.setDescription(description);
        ev.setPoints(points);
        ev.setOccurredAt(at);
        evidence.save(ev);
    }

    private void seedJob(User u, String title, String company, String text, List<String> required,
                         Instant at) {
        JobDescription jd = new JobDescription();
        jd.setUser(u);
        jd.setTitle(title);
        jd.setCompany(company);
        jd.setRawText(text);
        jd.setCreatedAt(at);
        jobs.save(jd);
        for (String name : required) {
            JobSkill js = new JobSkill();
            js.setJob(jd);
            js.setMatchedName(name);
            skills.findByNameIgnoreCase(name).ifPresent(js::setSkill);
            jobSkills.save(js);
        }
    }
}

