package com.cac.oa.service.ai;

import java.io.IOException;

@FunctionalInterface
public interface AiChatStreamListener {
    void onDelta(String content) throws IOException;
}
