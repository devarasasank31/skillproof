package com.skillproof.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AiEvaluationService.class);

    public record GeneratedQuestion(String type, String difficulty, String prompt,
                                    List<String> options, String answerKey, List<String> keywords, String explanation) {}

    private static final String EVAL_SYSTEM = """
            You are a strict technical interviewer. Evaluate the candidate's answer.
            Respond with ONLY a JSON object, no markdown, in this exact schema:
            {"score": <integer 0-100>, "feedback": "<2-3 sentences>", "missingConcepts": ["<concept>", "..."]}
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserAiResolver resolver;
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{.*}", Pattern.DOTALL);
    private static final Pattern ARRAY_BLOCK = Pattern.compile("\\[.*]", Pattern.DOTALL);

    public AiEvaluationService(UserAiResolver resolver) {
        this.resolver = resolver;
    }

    public boolean available(Long userId) {
        return resolver.forUserId(userId).map(AiClient::isAvailable).orElse(false);
    }

    public EvaluationResult evaluateSubjective(Long userId, String questionPrompt, String expectedKeywords, String answer) {
        AiClient client = resolver.forUserId(userId).orElse(null);
        if (client == null || !client.isAvailable()) return EvaluationResult.invalid();
        String user = "Question: " + questionPrompt + "\nExpected concepts: " + expectedKeywords
                + "\nCandidate answer: " + answer + "\nJSON only:";
        try {
            String raw = client.complete(EVAL_SYSTEM, user);
            if (raw == null) return EvaluationResult.invalid();
            Matcher m = JSON_BLOCK.matcher(raw);
            if (!m.find()) return EvaluationResult.invalid();
            JsonNode node = mapper.readTree(m.group());
            int score = node.path("score").asInt(-1);
            if (score < 0 || score > 100) return EvaluationResult.invalid();
            String feedback = node.path("feedback").asText("");
            List<String> missing = new ArrayList<>();
            node.withArray("missingConcepts").forEach(n -> {
                String s = n.asText("").trim();
                if (!s.isEmpty()) missing.add(s);
            });
            return new EvaluationResult(score, feedback, missing, true);
        } catch (Exception e) {
            log.debug("AI evaluation failed: {}", e.getMessage());
            return EvaluationResult.invalid();
        }
    }

    public List<GeneratedQuestion> generateQuestions(Long userId, String skillName, String difficulty,
                                                     int count, boolean preferInterview) {
        AiClient client = resolver.forUserId(userId).orElse(null);
        if (client == null || !client.isAvailable()) return List.of();
        String types = preferInterview ? "INTERVIEW|SUBJECTIVE" : "SUBJECTIVE|MCQ|DEBUGGING";
        String system = """
                You generate technical interview questions. Respond with ONLY a JSON array, each item:
                {"type": "%s", "difficulty": "%s", "prompt": "...",
                 "options": ["A","B","C","D"] (MCQ only), "answerKey": "..." (MCQ only),
                 "keywords": ["concept1", "concept2"], "explanation": "..."}
                INTERVIEW type means an open-ended spoken-interview-style question.
                Generate %d questions about %s at %s difficulty. No markdown.
                """.formatted(types, difficulty, count, skillName, difficulty);
        try {
            String raw = client.complete(system, "Generate the questions now.");
            if (raw == null) return List.of();
            Matcher m = ARRAY_BLOCK.matcher(raw);
            if (!m.find()) return List.of();
            JsonNode arr = mapper.readTree(m.group());
            List<GeneratedQuestion> out = new ArrayList<>();
            for (JsonNode n : arr) {
                String prompt = n.path("prompt").asText("");
                if (prompt.isBlank()) continue;
                List<String> opts = new ArrayList<>();
                n.withArray("options").forEach(o -> opts.add(o.asText()));
                List<String> kws = new ArrayList<>();
                n.withArray("keywords").forEach(o -> kws.add(o.asText()));
                out.add(new GeneratedQuestion(
                        n.path("type").asText("SUBJECTIVE"),
                        n.path("difficulty").asText(difficulty),
                        prompt, opts, n.path("answerKey").asText(null), kws,
                        n.path("explanation").asText("")));
                if (out.size() >= count) break;
            }
            return out;
        } catch (Exception e) {
            log.debug("AI generation failed: {}", e.getMessage());
            return List.of();
        }
    }

    private static final String EXTRACT_SYSTEM = """
            You extract skills from resumes. Read the ENTIRE resume - the skills section AND every
            project, internship, job, certification, coursework, hackathon and achievement.
            Extract every tool, technology, framework, programming language, and design / marketing /
            business / finance / domain skill that a person could practice or be interviewed on,
            even if only implied by a project description.
            Respond with ONLY a JSON array of short skill names (1-3 words each), maximum 40 items,
            no duplicates, no markdown. Example: ["React","Figma","User Research","Financial Modeling"]
            """;

    /** Deep AI extraction: finds skills across the whole resume, not just keyword matches. */
    public List<String> extractSkills(Long userId, String resumeText) {
        AiClient client = resolver.forUserId(userId).orElse(null);
        if (client == null || !client.isAvailable()) return List.of();
        String text = resumeText.length() > 12000 ? resumeText.substring(0, 12000) : resumeText;
        try {
            String raw = client.complete(EXTRACT_SYSTEM, "Resume text:\n" + text + "\nJSON array of skills:");
            if (raw == null) return List.of();
            Matcher m = ARRAY_BLOCK.matcher(raw);
            if (!m.find()) return List.of();
            JsonNode arr = mapper.readTree(m.group());
            java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
            arr.forEach(n -> {
                String s = n.asText("").trim().replaceAll("\\s+", " ");
                if (!s.isEmpty() && s.length() <= 40 && out.size() < 40) out.add(s);
            });
            log.info("AI resume extraction found {} skills", out.size());
            return new ArrayList<>(out);
        } catch (Exception e) {
            log.warn("AI resume extraction failed: {}", e.getMessage());
            return List.of();
        }
    }
}
