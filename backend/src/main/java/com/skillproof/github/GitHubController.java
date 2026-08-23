package com.skillproof.github;

import com.skillproof.security.CurrentUserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    public record AnalyzeRequest(@NotBlank String username) {}

    private final GitHubImportService service;

    public GitHubController(GitHubImportService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public GitHubImportService.AnalyzeResult analyze(@CurrentUserId Long userId,
                                                     @Valid @RequestBody AnalyzeRequest req) {
        return service.analyze(userId, req.username());
    }
}
