package com.skillproof.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Talks to any OpenAI-compatible chat completions endpoint
 * (OpenAI, Groq, OpenRouter, Ollama's /v1, LM Studio, vLLM...).
 */
public class OpenAiCompatClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String model;
    private final String apiKey;

    public OpenAiCompatClient(String baseUrl, String model, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.apiKey = (apiKey == null || apiKey.isBlank()) ? null : apiKey.trim();
    }

    @Override
    public boolean isAvailable() {
        return baseUrl != null && !baseUrl.isBlank() && model != null && !model.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) return null;
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "stream", false,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)));
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            if (apiKey != null) rb.header("Authorization", "Bearer " + apiKey);
            HttpResponse<String> resp = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.debug("AI endpoint returned {}: {}", resp.statusCode(),
                        resp.body() == null ? "" : resp.body().substring(0, Math.min(200, resp.body().length())));
                return null;
            }
            JsonNode node = mapper.readTree(resp.body());
            JsonNode content = node.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            log.debug("AI call failed: {}", e.getMessage());
            return null;
        }
    }
}
