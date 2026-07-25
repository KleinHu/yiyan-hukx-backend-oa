package com.cac.oa.service.ai;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Conservative, tokenizer-independent request budget for mixed gateway models. */
public final class AiChatTokenBudget {
    public static final int DEFAULT_MAX_INPUT_TOKENS = 7168;
    public static final int DEFAULT_CONTEXT_WINDOW_TOKENS = 131072;
    private static final int REQUEST_OVERHEAD_TOKENS = 8;
    private static final int MESSAGE_OVERHEAD_TOKENS = 4;

    private AiChatTokenBudget() { }

    public static int estimate(List<Map<String, String>> messages) {
        long tokens = REQUEST_OVERHEAD_TOKENS;
        for (Map<String, String> message : messages) {
            String content = message.get("content");
            int bytes = content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
            tokens += MESSAGE_OVERHEAD_TOKENS + (bytes + 1L) / 2L;
            if (tokens >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) tokens;
    }
}
