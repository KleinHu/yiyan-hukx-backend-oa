package com.cac.oa.service.ai.impl;

import com.cac.oa.entity.ai.AiUserConfigEntity;
import com.cac.oa.service.ai.AiChatTokenBudget;
import com.cac.oa.vo.ai.AiConfigVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiConfigServiceImplTest {
    @Test
    void returnsEffectiveDefaultsWhenNoGatewayConfigurationExists() {
        AiConfigVO config = AiConfigServiceImpl.toConfigVO(null);

        assertFalse(config.getApiKeyConfigured());
        assertEquals(Integer.valueOf(AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS), config.getMaxInputTokens());
        assertEquals(Integer.valueOf(AiChatTokenBudget.DEFAULT_CONTEXT_WINDOW_TOKENS),
                config.getContextWindowTokens());
    }

    @Test
    void returnsEffectiveDefaultsForSavedConfigurationWithBlankTokenFields() {
        AiUserConfigEntity entity = new AiUserConfigEntity();
        entity.setApiKey("key");

        AiConfigVO config = AiConfigServiceImpl.toConfigVO(entity);

        assertEquals(Integer.valueOf(AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS), config.getMaxInputTokens());
        assertEquals(Integer.valueOf(AiChatTokenBudget.DEFAULT_CONTEXT_WINDOW_TOKENS),
                config.getContextWindowTokens());
    }
}
