package com.skillproof.github;

import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.exception.ApiException;
import com.skillproof.skill.Skill;
import com.skillproof.skill.SkillRepository;
import com.skillproof.skill.UserSkillRepository;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.user.UserRepository;
import com.skillproof.common.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class GitHubImportService {

    public record RepoRow(String name, String description, String primaryLanguage, List<String> languages,
                          List<String> topics, Instant pushedAt) {}
    public record MappedSkill(String skillName, long repoCount) {}
    public record AnalyzeResult(String username, int publicRepos, List<RepoRow> repos,
                                List<MappedSkill> mappedSkills) {}

    private final GitHubProfileRepository profiles;
    private final GitHubRepoRepository repos;
    private final SkillRepository skills;
    private final UserSkillRepository userSkills;
    private final UserRepository users;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;
    private final RateLimiter rateLimiter;
    private final RestClient http;

    public GitHubImportService(GitHubProfileRepository profiles, GitHubRepoRepository repos,
                               SkillRepository skills, UserSkillRepository userSkills, UserRepository users,
                               SkillEvidenceRepository evidence, RecalculationService recalculation,
                               RateLimiter rateLimiter) {
        this.profiles = profiles;
        this.repos = repos;
        this.skills = skills;
        this.userSkills = userSkills;
        this.users = users;
        this.evidence = evidence;
        this.recalculation = recalculation;
        this.rateLimiter = rateLimiter;
        this.http = RestClient.create();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public AnalyzeResult analyze(Long userId, String usernameRaw) {
        if (!rateLimiter.tryAcquire("gh:" + userId, 10, Duration.ofHours(1).toMillis())) {
            throw ApiException.tooMany("GitHub analysis limit reached (10/hour). Try later.");
        }
        String username = usernameRaw == null ? "" : usernameRaw.trim();
        if (username.isEmpty() || !username.matches("[A-Za-z0-9-]{1,39}")) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Invalid GitHub username");
        }

        List<Map<String, Object>> ghRepos;
        try {
            ghRepos = http.get()
                    .uri("https://api.github.com/users/{u}/repos?per_page=100&sort=pushed", username)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(List.class);
        } catch (Exception e) {
            throw new ApiException(502, "GITHUB_UNAVAILABLE",
                    "Could not fetch repositories from GitHub. It may be down or rate-limited. Try again later.");
        }
        if (ghRepos == null || ghRepos.isEmpty()) {
            throw ApiException.notFound("No public repositories found for '" + username + "'");
        }

        GitHubProfile profile = profiles.findByUserId(userId).orElseGet(() -> {
            GitHubProfile p = new GitHubProfile();
            p.setUser(users.findById(userId).orElseThrow());
            return p;
        });
        profile.setUsername(username);
        profile.setPublicRepos(ghRepos.size());
        profile.setFetchedAt(Instant.now());
        profiles.save(profile);

        Map<String, Long> techCounts = new LinkedHashMap<>();
        List<RepoRow> rows = new ArrayList<>();

        for (Map<String, Object> r : ghRepos) {
            Number idNum = (Number) r.get("id");
            if (idNum == null) continue;
            String name = String.valueOf(r.getOrDefault("name", "repo"));
            String desc = r.get("description") == null ? "" : r.get("description").toString();
            String lang = r.get("language") == null ? "" : r.get("language").toString();

            Set<String> techs = new LinkedHashSet<>();
            if (!lang.isBlank()) techs.add(lang);
            for (String t : detectTech(desc)) techs.add(t);
            Object topicsObj = r.get("topics");
            List<String> topicList = new ArrayList<>();
            if (topicsObj instanceof List<?> tl) {
                for (Object t : tl) {
                    String ts = String.valueOf(t);
                    topicList.add(ts);
                    for (String tech : detectTech(ts)) techs.add(tech);
                }
            }
            Instant pushedAt = r.get("pushed_at") == null ? null : Instant.parse(r.get("pushed_at").toString());

            GitHubRepository entity = repos.findByProfileIdAndExternalId(profile.getId(), idNum.longValue())
                    .orElseGet(GitHubRepository::new);
            entity.setProfile(profile);
            entity.setExternalId(idNum.longValue());
            entity.setName(name);
            entity.setDescription(desc);
            entity.setPrimaryLanguage(lang);
            entity.setLanguages(String.join(",", techs));
            entity.setTopics(String.join(",", topicList));
            entity.setPushedAt(pushedAt);
            repos.save(entity);

            for (String t : techs) techCounts.merge(normalizeTech(t), 1L, Long::sum);
            rows.add(new RepoRow(name, desc, lang, List.copyOf(techs), topicList, pushedAt));

            if (pushedAt != null) touchActivity(userId, techs, pushedAt, name);
        }

        List<MappedSkill> mapped = techCounts.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new MappedSkill(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.repoCount(), a.repoCount()))
                .toList();

        recalculation.recalculateUser(userId);
        return new AnalyzeResult(username, ghRepos.size(), rows, mapped);
    }

    private void touchActivity(Long userId, Set<String> techs, Instant pushedAt, String repoName) {
        for (String tech : techs) {
            Skill s = skills.findByNameIgnoreCase(normalizeTech(tech)).orElse(null);
            if (s == null) continue;
            userSkills.findByUserIdAndSkillId(userId, s.getId()).ifPresent(us -> {
                boolean newer = us.getLastActivityAt() == null || pushedAt.isAfter(us.getLastActivityAt());
                if (newer) us.setLastActivityAt(pushedAt);
                SkillEvidence ev = new SkillEvidence();
                ev.setUserSkill(us);
                ev.setUserId(userId);
                ev.setEvidenceType(SkillEvidence.Type.GITHUB);
                ev.setDescription("Detected in GitHub repository: " + repoName);
                ev.setPoints(5);
                ev.setOccurredAt(pushedAt);
                evidence.save(ev);
            });
        }
    }

    static Set<String> detectTech(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return found;
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> known = List.of("java", "spring boot", "spring", "postgresql", "postgres", "docker",
                "kubernetes", "kafka", "react", "typescript", "javascript", "python", "aws", "redis",
                "mongodb", "mysql", "graphql", "terraform", "go", "rust", "kotlin", "node", "vue", "angular",
                "elasticsearch", "rabbitmq", "grpc", "rest api", "microservices");
        for (String k : known) {
            if (lower.contains(k)) found.add(titleTech(k));
        }
        return found;
    }

    static String normalizeTech(String tech) {
        String t = tech.trim();
        if (t.equalsIgnoreCase("postgres")) return "PostgreSQL";
        if (t.equalsIgnoreCase("go")) return "Go";
        if (t.equalsIgnoreCase("node")) return "Node.js";
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String titleTech(String s) {
        switch (s.toLowerCase(Locale.ROOT)) {
            case "aws": return "AWS";
            case "postgresql": case "postgres": return "PostgreSQL";
            case "graphql": return "GraphQL";
            case "grpc": return "gRPC";
            case "node": return "Node.js";
            default: return normalizeTech(s);
        }
    }
}

