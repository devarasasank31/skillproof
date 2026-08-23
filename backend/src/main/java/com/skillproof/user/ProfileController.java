package com.skillproof.user;

import com.skillproof.assessment.AssessmentRepository;
import com.skillproof.challenge.ChallengeSubmissionRepository;
import com.skillproof.exception.ApiException;
import com.skillproof.github.GitHubProfileRepository;
import com.skillproof.security.CurrentUserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    public record ProfileResponse(Long id, String name, String email, String headline, String bio,
                                  String visibility, Stats stats) {}
    public record Stats(long skills, long assessments, long challenges, long repos) {}
    public record UpdateProfileRequest(@NotBlank String name, String headline, String bio,
                                       @NotBlank String visibility) {}

    private final UserRepository users;
    private final AssessmentRepository assessments;
    private final ChallengeSubmissionRepository submissions;
    private final GitHubProfileRepository githubProfiles;
    private final com.skillproof.skill.UserSkillRepository userSkills;
    private final com.skillproof.ai.AiSettingsService aiSettings;

    public ProfileController(UserRepository users, AssessmentRepository assessments,
                             ChallengeSubmissionRepository submissions, GitHubProfileRepository githubProfiles,
                             com.skillproof.skill.UserSkillRepository userSkills,
                             com.skillproof.ai.AiSettingsService aiSettings) {
        this.users = users;
        this.assessments = assessments;
        this.submissions = submissions;
        this.githubProfiles = githubProfiles;
        this.userSkills = userSkills;
        this.aiSettings = aiSettings;
    }

    @GetMapping
    public ProfileResponse me(@CurrentUserId Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return toDto(u);
    }

    @GetMapping("/ai")
    public com.skillproof.ai.AiSettingsService.AiStatus aiStatus(@CurrentUserId Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        return aiSettings.status(u);
    }

    @PutMapping("/ai")
    public com.skillproof.ai.AiSettingsService.AiStatus updateAi(@CurrentUserId Long userId,
                                                                 @RequestBody com.skillproof.ai.AiSettingsService.AiSetup setup) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        aiSettings.apply(u, setup);
        users.save(u);
        return aiSettings.status(u);
    }

    @DeleteMapping("/ai")
    public com.skillproof.ai.AiSettingsService.AiStatus clearAi(@CurrentUserId Long userId) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        aiSettings.clear(u);
        users.save(u);
        return aiSettings.status(u);
    }

    @PutMapping
    public ProfileResponse update(@CurrentUserId Long userId, @Valid @RequestBody UpdateProfileRequest req) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (req.name() != null && req.name().trim().length() >= 2) {
            u.setName(req.name().trim());
        }
        u.setHeadline(req.headline());
        u.setBio(req.bio());
        try {
            u.setVisibility(req.visibility());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("VALIDATION_ERROR", "visibility must be PRIVATE or PUBLIC");
        }
        users.save(u);
        return toDto(u);
    }

    private ProfileResponse toDto(User u) {
        return new ProfileResponse(
                u.getId(), u.getName(), u.getEmail(), u.getHeadline(), u.getBio(), u.getVisibility(),
                new Stats(
                        userSkills.countByUserId(u.getId()),
                        assessments.countByUserId(u.getId()),
                        submissions.countByUserId(u.getId()),
                        githubProfiles.countByUserId(u.getId())));
    }
}
