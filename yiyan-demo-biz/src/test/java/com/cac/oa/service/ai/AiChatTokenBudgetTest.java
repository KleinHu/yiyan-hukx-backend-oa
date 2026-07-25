package com.cac.oa.service.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiChatTokenBudgetTest {
    @Test
    void estimatesUtf8BytesConservativelyWithMessageOverhead() {
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        Map<String, String> message = new HashMap<String, String>();
        message.put("role", "user");
        message.put("content", "你好"); // six UTF-8 bytes, rounded up to three tokens
        messages.add(message);

        assertEquals(15, AiChatTokenBudget.estimate(messages));
    }

    @Test
    void estimatesEnglishAndStructuredTableText() {
        assertEquals(15, AiChatTokenBudget.estimate(singleUserMessage("abcde")));

        String table = "姓名\\t数量\\n张三\\t3";
        int expected = 8 + 4 + (table.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1) / 2;
        assertEquals(expected, AiChatTokenBudget.estimate(singleUserMessage(table)));
    }

    @Test
    void staysAtAndThenExceedsTheDefaultSevenKInputBoundary() {
        int payloadBytesAtLimit = 2 * (AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS - 8 - 4);
        StringBuilder atLimit = new StringBuilder(payloadBytesAtLimit);
        for (int index = 0; index < payloadBytesAtLimit; index++) {
            atLimit.append('a');
        }
        assertEquals(AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS,
                AiChatTokenBudget.estimate(singleUserMessage(atLimit.toString())));
        assertEquals(AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS + 1,
                AiChatTokenBudget.estimate(singleUserMessage(atLimit.append('a').toString())));
    }

    private List<Map<String, String>> singleUserMessage(String content) {
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        Map<String, String> message = new HashMap<String, String>();
        message.put("role", "user");
        message.put("content", content);
        messages.add(message);
        return messages;
    }
}
