package com.skillproof.ai;

import com.skillproof.exception.ApiException;
import com.skillproof.security.SecretCipher;
import com.skillproof.user.User;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Validates, encrypts and reports a user's own AI provider configuration.
 */
@Service
public class AiSettingsService {

    public static final Set<String> PROVIDERS = Set.of("OPENAI", "GROQ", "OPENROUTER", "OLLAMA", "CUSTOM");

    public record AiSetup(String provider, String apiKey, String baseUrl, String model) {}
    public record AiStatus(String provider, String maskedKey, String baseUrl, String model) {}

    private final SecretCipher cipher;

    public AiSettingsService(SecretCipher cipher) {
        this.cipher = cipher;
    }

    /** Applies the setup to the user. Passing null provider clears the configuration. */
    public void apply(User u, AiSetup setup) {
        if (setup == null || setup.provider() == null || setup.provider().isBlank()) {
            clear(u);
            return;
        }
        String provider = setup.provider().trim().toUpperCase();
        if (!PROVIDERS.contains(provider)) {
            throw ApiException.badRequest("VALIDATION_ERROR",
                    "Unknown AI provider. Use one of: " + String.join(", ", PROVIDERS));
        }
        if ("CUSTOM".equals(provider)
                && (setup.baseUrl() == null || !setup.baseUrl().trim().startsWith("http"))) {
            throw ApiException.badRequest("VALIDATION_ERROR",
                    "Custom providers need an OpenAI-compatible base URL (e.g. https://host/v1)");
        }
        String key = setup.apiKey() == null ? null : setup.apiKey().trim();
        if (key != null && key.length() > 300) {
            throw ApiException.badRequest("VALIDATION_ERROR", "API key too long");
        }
        u.setAiProvider(provider);
        if (key != null && !key.isEmpty()) {
            u.setAiApiKeyEnc(cipher.encrypt(key));
        } else if (u.getAiApiKeyEnc() == null && requiresKey(provider)) {
            throw ApiException.badRequest("VALIDATION_ERROR",
                    provider + " requires an API key");
        }
        u.setAiBaseUrl(setup.baseUrl() == null || setup.baseUrl().isBlank() ? null : setup.baseUrl().trim());
        u.setAiModel(setup.model() == null || setup.model().isBlank() ? null : setup.model().trim());
    }

    public void clear(User u) {
        u.setAiProvider(null);
        u.setAiApiKeyEnc(null);
        u.setAiBaseUrl(null);
        u.setAiModel(null);
    }

    public boolean hasKey(User u) {
        return u.getAiProvider() != null && !u.getAiProvider().isBlank();
    }

    public AiStatus status(User u) {
        return new AiStatus(u.getAiProvider(), SecretCipher.mask(cipher.decrypt(u.getAiApiKeyEnc())),
                u.getAiBaseUrl(), u.getAiModel());
    }

    static boolean requiresKey(String provider) {
        return "OPENAI".equals(provider) || "GROQ".equals(provider) || "OPENROUTER".equals(provider);
    }
}
