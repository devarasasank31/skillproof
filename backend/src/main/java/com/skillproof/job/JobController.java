package com.skillproof.job;

import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.exception.ApiException;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.resume.SkillCatalog;
import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.Skill;
import com.skillproof.skill.SkillRepository;
import com.skillproof.skill.UserSkill;
import com.skillproof.skill.UserSkillRepository;
import com.skillproof.common.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    public record AnalyzeRequest(@NotBlank String title, @NotBlank String text) {}
    public record JobSkillRow(String name, boolean required, Integer confidence) {}
    public record AnalyzeResponse(Long jobId, String title, int readiness,
                                  List<JobSkillRow> skills, List<JobSkillRow> gaps) {}
    public record JobSummary(Long jobId, String title, String company, Integer readiness, Instant createdAt) {}
    public record MarketResponse(int totalJobs, List<MarketRow> rows) {}
    public record MarketRow(String name, long frequency, int totalJobs, Integer yourConfidence) {}

    private final JobDescriptionRepository jobs;
    private final JobSkillRepository jobSkills;
    private final SkillCatalog catalog;
    private final SkillRepository skills;
    private final UserSkillRepository userSkills;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;
    private final RateLimiter rateLimiter;

    public JobController(JobDescriptionRepository jobs, JobSkillRepository jobSkills,
                         com.skillproof.resume.SkillCatalog catalog, SkillRepository skills,
                         UserSkillRepository userSkills, SkillEvidenceRepository evidence,
                         RecalculationService recalculation, RateLimiter rateLimiter) {
        this.jobs = jobs;
        this.jobSkills = jobSkills;
        this.catalog = catalog;
        this.skills = skills;
        this.userSkills = userSkills;
        this.evidence = evidence;
        this.recalculation = recalculation;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@CurrentUserId Long userId, @Valid @RequestBody AnalyzeRequest req) {
        if (!rateLimiter.tryAcquire("jobs:" + userId, 30, Duration.ofHours(1).toMillis())) {
            throw ApiException.tooMany("Job analysis limit reached (30/hour)");
        }
        if (req.text().length() < 40) {
            throw ApiException.badRequest("INVALID_JOB", "Paste the full job description (at least 40 characters)");
        }

        Set<String> detected = catalog.detect(req.text());
        if (detected.isEmpty()) {
            throw ApiException.badRequest("NO_SKILLS_FOUND",
                    "No known technical skills detected in this description");
        }

        JobDescription jd = new JobDescription();
        var u = new com.skillproof.user.User();
        u.setId(userId);
        jd.setUser(u);
        jd.setTitle(req.title().trim());
        jd.setRawText(req.text());
        jobs.save(jd);

        List<JobSkillRow> rows = new ArrayList<>();
        for (String name : detected) {
            Optional<Skill> s = skills.findByNameIgnoreCase(name);
            Long skillId = s.map(Skill::getId).orElse(null);
            Integer conf = userSkills.findByUserIdAndSkillName(userId, name)
                    .map(UserSkill::getConfidence).orElse(null);
            JobSkill js = new JobSkill();
            var ref = new JobDescription();
            ref.setId(jd.getId());
            js.setJob(ref);
            s.ifPresent(js::setSkill);
            js.setMatchedName(name);
            jobSkills.save(js);

            rows.add(new JobSkillRow(name, true, conf));

            if (s.isPresent()) {
                userSkills.findByUserIdAndSkillId(userId, skillId).ifPresent(us -> {
                    us.setMarketScore(100);
                    SkillEvidence ev = new SkillEvidence();
                    ev.setUserSkill(us);
                    ev.setUserId(userId);
                    ev.setEvidenceType(SkillEvidence.Type.MARKET);
                    ev.setDescription("Required in saved job: " + req.title().trim());
                    ev.setPoints(0);
                    evidence.save(ev);
                });
            }
        }
        jobs.save(jd);

        List<JobSkillRow> withConf = rows.stream().filter(r -> r.confidence() != null).toList();
        int readiness = withConf.isEmpty() ? 0
                : (int) Math.round(withConf.stream().mapToInt(JobSkillRow::confidence).average().orElse(0));
        jd.setReadiness(readiness);
        jobs.save(jd);

        recalculation.recalculateUser(userId);

        List<JobSkillRow> gaps = rows.stream()
                .filter(r -> r.confidence() == null || r.confidence() < 60)
                .sorted(Comparator.comparingInt(r -> r.confidence() == null ? -1 : r.confidence()))
                .collect(Collectors.toList());

        return new AnalyzeResponse(jd.getId(), jd.getTitle(), readiness, rows, gaps);
    }

    @GetMapping
    public List<JobSummary> list(@CurrentUserId Long userId) {
        return jobs.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(j -> new JobSummary(j.getId(), j.getTitle(), j.getCompany(), j.getReadiness(),
                        j.getCreatedAt()))
                .toList();
    }

    @GetMapping("/{id}")
    public Object get(@CurrentUserId Long userId, @PathVariable Long id) {
        JobDescription j = jobs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Job not found"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("jobId", j.getId());
        out.put("title", j.getTitle());
        out.put("company", j.getCompany());
        out.put("readiness", j.getReadiness());
        out.put("createdAt", j.getCreatedAt());
        out.put("skills", jobSkills.findByJobId(id).stream()
                .map(js -> {
                    Integer conf = js.getSkill() == null ? null :
                            userSkills.findByUserIdAndSkillName(userId, js.getMatchedName())
                                    .map(UserSkill::getConfidence).orElse(null);
                    return new JobSkillRow(js.getMatchedName(), true, conf);
                }).toList());
        return out;
    }

    @DeleteMapping("/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long id) {
        JobDescription j = jobs.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Job not found"));
        jobs.delete(j);
    }

    @GetMapping("/market")
    public MarketResponse market(@CurrentUserId Long userId) {
        long totalJobs = jobs.countByUserId(userId);
        if (totalJobs == 0) return new MarketResponse(0, List.of());

        Map<String, Long> freq = jobSkills.frequencyByUser(userId);
        List<MarketRow> rows = freq.entrySet().stream()
                .map(e -> new MarketRow(e.getKey(), e.getValue(), (int) totalJobs,
                        userSkills.findByUserIdAndSkillName(userId, e.getKey())
                                .map(UserSkill::getConfidence).orElse(null)))
                .sorted((a, b) -> Long.compare(b.frequency(), a.frequency()))
                .toList();
        return new MarketResponse((int) totalJobs, rows);
    }
}

