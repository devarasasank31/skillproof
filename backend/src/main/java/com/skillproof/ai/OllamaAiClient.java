package com.skillproof.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiClient.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.ai.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${app.ai.model:llama3.2}")
    private String model;

    private volatile long lastCheckMillis = 0;
    private volatile boolean cachedAvailable = false;

    @Override
    public boolean isAvailable() {
        long now = System.currentTimeMillis();
        if (now - lastCheckMillis < 60_000) return cachedAvailable;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            cachedAvailable = resp.statusCode() == 200;
        } catch (Exception e) {
            cachedAvailable = false;
        }
        lastCheckMillis = now;
        log.debug("Ollama availability check: {}", cachedAvailable);
        return cachedAvailable;
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
            HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.debug("Ollama returned {}", resp.statusCode());
                return null;
            }
            JsonNode node = mapper.readTree(resp.body());
            JsonNode content = node.path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            log.debug("Ollama call failed: {}", e.getMessage());
            return null;
        }
    }
}
