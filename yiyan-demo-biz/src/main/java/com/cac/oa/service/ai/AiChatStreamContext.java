package com.cac.oa.service.ai;

import java.util.List;
import java.util.Map;

/**
 * Immutable request state captured before the HTTP response switches to the
 * asynchronous streaming thread. It deliberately avoids looking up the
 * SecurityContext while a stream is being written.
 */
public class AiChatStreamContext {
    private final Long userId;
    private final Long sessionId;
    private final String model;
    private final String baseUrl;
    private final String apiKey;
    private final List<Map<String, String>> messages;

    public AiChatStreamContext(Long userId, Long sessionId, String model,
            String baseUrl, String apiKey, List<Map<String, String>> messages) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.model = model;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.messages = messages;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }
}
