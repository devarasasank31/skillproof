package com.skillproof.resume;

import com.skillproof.ai.AiEvaluationService;
import com.skillproof.common.RateLimiter;
import com.skillproof.exception.ApiException;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.SkillService;
import com.skillproof.user.UserRepository;
import jakarta.validation.Valid;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    public record DetectedSkill(String name, Long skillId) {}
    public record AnalyzeResponse(String fileName, int pages, List<DetectedSkill> detected) {}
    public record ConfirmRequest(List<String> skillNames) {}
    public record ConfirmResponse(int added, List<String> addedNames, List<String> skipped) {}

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    private final SkillCatalog catalog;
    private final SkillService skillService;
    private final UserRepository users;
    private final RateLimiter rateLimiter;
    private final RecalculationService recalculation;
    private final AiEvaluationService ai;

    public ResumeController(SkillCatalog catalog, SkillService skillService, UserRepository users,
                            RateLimiter rateLimiter, RecalculationService recalculation,
                            AiEvaluationService ai) {
        this.catalog = catalog;
        this.skillService = skillService;
        this.users = users;
        this.rateLimiter = rateLimiter;
        this.recalculation = recalculation;
        this.ai = ai;
    }

    @PostMapping("/analyze")
    public AnalyzeResponse analyze(@CurrentUserId Long userId,
                                   @RequestParam("file") MultipartFile file) throws IOException {
        if (!rateLimiter.tryAcquire("resume:" + userId, 10, Duration.ofHours(1).toMillis())) {
            throw ApiException.tooMany("Resume analysis limit reached (10/hour)");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Please upload a PDF file");
        }
        String name = sanitize(file.getOriginalFilename());
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw ApiException.badRequest("INVALID_FILE", "Only PDF files are supported");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_BYTES) {
            throw new ApiException(413, "FILE_TOO_LARGE", "PDF must be under 10MB");
        }

        String text;
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        } catch (IOException e) {
            throw ApiException.badRequest("CORRUPT_PDF", "Could not read the PDF. It may be corrupt or password-protected.");
        }
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest("NO_TEXT",
                    "No extractable text found. The PDF may be a scan; scanned resumes are not supported.");
        }
        int pages = Math.max(1, text.length() / 1800);

        // 1) Fast deterministic dictionary scan.
        Set<String> detected = new LinkedHashSet<>(catalog.detect(text));
        // 2) Deep AI pass (users with their own API key): reads projects, internships,
        //    certifications - every corner of the resume, not just keyword matches.
        if (ai.available(userId)) {
            for (String n : ai.extractSkills(userId, text)) {
                boolean dup = detected.stream().anyMatch(d -> d.equalsIgnoreCase(n));
                if (!dup) detected.add(n);
            }
        }
        List<DetectedSkill> out = new ArrayList<>();
        for (String d : detected.stream().limit(40).toList()) {
            var s = catalog.findOrCreate(d, catalog.categoryOf(d));
            out.add(new DetectedSkill(s.getName(), s.getId()));
        }
        return new AnalyzeResponse(name, pages, out);
    }

    @PostMapping("/confirm")
    public ConfirmResponse confirm(@CurrentUserId Long userId, @Valid @RequestBody ConfirmRequest req) {
        if (req.skillNames() == null || req.skillNames().isEmpty()) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Select at least one skill to add");
        }
        int added = 0;
        List<String> names = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (String n : req.skillNames()) {
            try {
                var existing = users.findById(userId).orElseThrow();
                boolean has = skillService.listForUser(userId).stream()
                        .anyMatch(r -> r.name().equalsIgnoreCase(n.trim()));
                if (has) { skipped.add(n); continue; }
                skillService.claim(userId, new SkillService.ClaimRequest(n));
                names.add(n.trim());
                added++;
            } catch (Exception e) {
                skipped.add(n);
            }
        }
        recalculation.recalculateUser(userId);
        return new ConfirmResponse(added, names, skipped);
    }

    private static String sanitize(String original) {
        String s = original == null ? "resume.pdf" : original;
        return s.replaceAll("[^A-Za-z0-9._ ()-]", "_");
    }
}
