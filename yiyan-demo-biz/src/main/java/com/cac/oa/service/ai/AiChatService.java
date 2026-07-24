package com.cac.oa.service.ai;

import com.cac.oa.vo.ai.AiChatRequest;
import com.cac.oa.vo.ai.AiChatVO;
import com.cac.oa.vo.ai.AiChatSessionDetailVO;
import com.cac.oa.vo.ai.AiChatSessionVO;

import java.util.List;

public interface AiChatService {
    AiChatVO chat(AiChatRequest request);
    AiChatStreamContext startStream(AiChatRequest request);
    AiChatVO streamChat(AiChatStreamContext context, AiChatStreamListener listener);
    void completeStream(AiChatStreamContext context, String assistantContent);
    List<AiChatSessionVO> listMySessions();
    AiChatSessionDetailVO getMySession(String sessionId);
    void deleteMySession(String sessionId);
}
