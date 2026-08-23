package com.skillproof.ai;

import java.util.List;

public interface AiClient {
    boolean isAvailable();
    String complete(String systemPrompt, String userPrompt);
}
