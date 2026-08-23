package com.skillproof.ai;

import com.skillproof.security.SecretCipher;
import com.skillproof.user.User;
import com.skillproof.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves the AI client for a user: their own BYOK configuration first,
 * falling back to the server-wide Ollama instance when configured.
 */
@Component
public class UserAiResolver {

    private final UserRepository users;
    private final SecretCipher cipher;
    private final OllamaAiClient globalFallback;

    public UserAiResolver(UserRepository users, SecretCipher cipher, OllamaAiClient globalFallback) {
        this.users = users;
        this.cipher = cipher;
        this.globalFallback = globalFallback;
    }

    public Optional<AiClient> forUserId(Long userId) {
        return users.findById(userId).flatMap(this::forUser);
    }

    public Optional<AiClient> forUser(User u) {
        String provider = u.getAiProvider() == null ? "" : u.getAiProvider().trim().toUpperCase();
        if (provider.isEmpty()) {
            return globalFallback.isAvailable() ? Optional.of(globalFallback) : Optional.empty();
        }
        String key = cipher.decrypt(u.getAiApiKeyEnc());
        switch (provider) {
            case "OPENAI" -> {
                if (blank(key)) return Optional.empty();
                return Optional.of(new OpenAiCompatClient("https://api.openai.com/v1",
                        orDefault(u.getAiModel(), "gpt-4o-mini"), key));
            }
            case "GROQ" -> {
                if (blank(key)) return Optional.empty();
                return Optional.of(new OpenAiCompatClient("https://api.groq.com/openai/v1",
                        orDefault(u.getAiModel(), "llama-3.3-70b-versatile"), key));
            }
            case "OPENROUTER" -> {
                if (blank(key)) return Optional.empty();
                return Optional.of(new OpenAiCompatClient("https://openrouter.ai/api/v1",
                        orDefault(u.getAiModel(), "openai/gpt-4o-mini"), key));
            }
            case "OLLAMA" -> {
                String base = orDefault(u.getAiBaseUrl(), "http://localhost:11434/v1");
                return Optional.of(new OpenAiCompatClient(base,
                        orDefault(u.getAiModel(), "llama3.2"), key));
            }
            case "CUSTOM" -> {
                if (blank(u.getAiBaseUrl())) return Optional.empty();
                return Optional.of(new OpenAiCompatClient(u.getAiBaseUrl(),
                        orDefault(u.getAiModel(), "default"), key));
            }
            default -> { return Optional.empty(); }
        }
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static String orDefault(String v, String dflt) {
        return blank(v) ? dflt : v.trim();
    }
}
