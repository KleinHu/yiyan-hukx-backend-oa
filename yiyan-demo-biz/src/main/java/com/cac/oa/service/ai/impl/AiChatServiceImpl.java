package com.cac.oa.service.ai.impl;

import com.cac.oa.entity.ai.AiUserConfigEntity;
import com.cac.oa.entity.ai.AiChatMessageEntity;
import com.cac.oa.entity.ai.AiChatSessionEntity;
import com.cac.oa.dao.ai.AiChatMessageMapper;
import com.cac.oa.dao.ai.AiChatSessionMapper;
import com.cac.oa.service.ai.AiChatService;
import com.cac.oa.service.ai.AiChatStreamContext;
import com.cac.oa.service.ai.AiChatStreamListener;
import com.cac.oa.service.ai.OpenAiGatewayClient;
import com.cac.oa.vo.ai.AiChatMessageRequest;
import com.cac.oa.vo.ai.AiChatRequest;
import com.cac.oa.vo.ai.AiChatVO;
import com.cac.oa.vo.ai.AiChatHistoryMessageVO;
import com.cac.oa.vo.ai.AiChatSessionDetailVO;
import com.cac.oa.vo.ai.AiChatSessionVO;
import com.cac.yiyan.mybatis.query.LambdaQueryWrapperX;
import com.cac.yiyan.security.core.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {
    private static final int MAX_MESSAGES = 30;
    private static final int MAX_CONTENT_LENGTH = 30_000;
    private static final Set<String> ALLOWED_ROLES = new HashSet<String>();

    static {
        ALLOWED_ROLES.add("system");
        ALLOWED_ROLES.add("user");
        ALLOWED_ROLES.add("assistant");
    }

    private final AiConfigServiceImpl configService;
    private final OpenAiGatewayClient gatewayClient;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatVO chat(AiChatRequest request) {
        PreparedChat prepared = prepareChat(request);
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", prepared.model);
        body.put("messages", prepared.messages);
        body.put("stream", false);
        JsonNode response = gatewayClient.post(prepared.config.getBaseUrl(), "/chat/completions", body,
                prepared.config.getApiKey());
        JsonNode choice = response.path("choices").path(0);
        String content = choice.path("message").path("content").asText();
        if (content.isEmpty()) {
            throw new IllegalStateException("The AI gateway returned an empty response");
        }
        AiChatSessionEntity session = saveChatHistory(request.getSessionId(), prepared.model,
                lastUserMessage(prepared.messages), content);
        AiChatVO result = new AiChatVO();
        result.setId(response.path("id").asText());
        result.setSessionId(String.valueOf(session.getId()));
        result.setModel(response.path("model").asText(prepared.model));
        result.setContent(content);
        result.setFinishReason(choice.path("finish_reason").asText());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatStreamContext startStream(AiChatRequest request) {
        PreparedChat prepared = prepareChat(request);
        boolean retry = Boolean.TRUE.equals(request.getRetry());
        if (retry && trimToNull(request.getSessionId()) == null) {
            throw new IllegalArgumentException("A saved chat session is required to regenerate a response");
        }
        AiChatSessionEntity session = persistStreamUserMessage(request.getSessionId(), prepared.model,
                lastUserMessage(prepared.messages), prepared.userId, retry);
        return new AiChatStreamContext(prepared.userId, session.getId(), prepared.model,
                prepared.config.getBaseUrl(), prepared.config.getApiKey(), prepared.messages);
    }

    @Override
    public AiChatVO streamChat(AiChatStreamContext context, AiChatStreamListener listener) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", context.getModel());
        body.put("messages", context.getMessages());
        body.put("stream", true);
        OpenAiGatewayClient.StreamResponse response = gatewayClient.streamPost(context.getBaseUrl(),
                "/chat/completions", body, context.getApiKey(), listener::onDelta);
        AiChatVO result = new AiChatVO();
        result.setId(response.getId());
        result.setSessionId(String.valueOf(context.getSessionId()));
        result.setModel(response.getModel() == null || response.getModel().isEmpty()
                ? context.getModel() : response.getModel());
        result.setFinishReason(response.getFinishReason());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeStream(AiChatStreamContext context, String assistantContent) {
        if (assistantContent == null || assistantContent.isEmpty()) {
            return;
        }
        AiChatSessionEntity session = requireMySession(context.getSessionId(), context.getUserId());
        int messageOrder = session.getMessageCount() == null ? 0 : session.getMessageCount();
        insertMessage(context.getUserId(), session.getId(), ++messageOrder, "assistant", assistantContent);
        session.setMessageCount(messageOrder);
        session.setLastMessageAt(new Date());
        sessionMapper.updateById(session);
    }

    @Override
    public List<AiChatSessionVO> listMySessions() {
        List<AiChatSessionVO> result = new ArrayList<AiChatSessionVO>();
        for (AiChatSessionEntity entity : sessionMapper.selectList(new LambdaQueryWrapperX<AiChatSessionEntity>()
                .eq(AiChatSessionEntity::getUserId, requireUserId())
                .orderByDesc(AiChatSessionEntity::getLastMessageAt))) {
            AiChatSessionVO session = new AiChatSessionVO();
            session.setId(String.valueOf(entity.getId()));
            session.setTitle(entity.getTitle());
            session.setModel(entity.getModel());
            session.setLastMessageAt(entity.getLastMessageAt());
            result.add(session);
        }
        return result;
    }

    @Override
    public AiChatSessionDetailVO getMySession(String sessionId) {
        AiChatSessionEntity session = requireMySession(sessionId);
        AiChatSessionDetailVO result = new AiChatSessionDetailVO();
        result.setId(String.valueOf(session.getId()));
        result.setModel(session.getModel());
        List<AiChatHistoryMessageVO> messages = new ArrayList<AiChatHistoryMessageVO>();
        for (AiChatMessageEntity entity : messageMapper.selectList(new LambdaQueryWrapperX<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getSessionId, session.getId())
                .eq(AiChatMessageEntity::getUserId, requireUserId())
                .orderByAsc(AiChatMessageEntity::getMessageOrder))) {
            AiChatHistoryMessageVO message = new AiChatHistoryMessageVO();
            message.setRole(entity.getRole());
            message.setContent(entity.getContent());
            messages.add(message);
        }
        result.setMessages(messages);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMySession(String sessionId) {
        AiChatSessionEntity session = requireMySession(sessionId);
        messageMapper.delete(new LambdaQueryWrapperX<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getSessionId, session.getId())
                .eq(AiChatMessageEntity::getUserId, requireUserId()));
        sessionMapper.deleteById(session.getId());
    }

    private PreparedChat prepareChat(AiChatRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()
                || request.getMessages().size() > MAX_MESSAGES) {
            throw new IllegalArgumentException("Send between 1 and " + MAX_MESSAGES + " chat messages");
        }
        AiUserConfigEntity config = configService.requireConfigured();
        String model = trimToNull(request.getModel());
        if (model == null) {
            model = trimToNull(config.getDefaultModel());
        }
        if (model == null || model.length() > 256) {
            throw new IllegalArgumentException("Select a model before sending a message");
        }
        if (!configService.isMyEnabledModel(model)) {
            throw new IllegalArgumentException(
                    "Select a model from your saved model list, then refresh it if the gateway has changed");
        }
        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        for (AiChatMessageRequest message : request.getMessages()) {
            if (message == null || !ALLOWED_ROLES.contains(message.getRole())
                    || trimToNull(message.getContent()) == null || message.getContent().length() > MAX_CONTENT_LENGTH) {
                throw new IllegalArgumentException("Each chat message needs a supported role and non-empty content");
            }
            Map<String, String> item = new HashMap<String, String>();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            messages.add(item);
        }
        return new PreparedChat(requireUserId(), config, model, messages);
    }

    private AiChatSessionEntity persistStreamUserMessage(String sessionId, String model,
            String userContent, Long userId, boolean retry) {
        Date now = new Date();
        AiChatSessionEntity session;
        if (trimToNull(sessionId) == null) {
            session = new AiChatSessionEntity();
            session.setUserId(userId);
            session.setTitle(toTitle(userContent));
            session.setModel(model);
            session.setMessageCount(0);
            session.setLastMessageAt(now);
            sessionMapper.insert(session);
        } else {
            session = requireMySession(sessionId);
            if (retry) {
                removeLastAssistantMessage(session, userId);
                if (lastMessageIsSameUser(session, userId, userContent)) {
                    session.setModel(model);
                    session.setLastMessageAt(now);
                    sessionMapper.updateById(session);
                    return session;
                }
            }
            session.setModel(model);
            session.setLastMessageAt(now);
        }
        int messageOrder = session.getMessageCount() == null ? 0 : session.getMessageCount();
        insertMessage(userId, session.getId(), ++messageOrder, "user", userContent);
        session.setMessageCount(messageOrder);
        session.setLastMessageAt(now);
        sessionMapper.updateById(session);
        return session;
    }

    private void removeLastAssistantMessage(AiChatSessionEntity session, Long userId) {
        AiChatMessageEntity message = lastMessage(session.getId(), userId);
        if (message == null || !"assistant".equals(message.getRole())) {
            return;
        }
        messageMapper.deleteById(message.getId());
        int messageCount = session.getMessageCount() == null ? 0 : session.getMessageCount();
        session.setMessageCount(Math.max(0, messageCount - 1));
    }

    private boolean lastMessageIsSameUser(AiChatSessionEntity session, Long userId, String content) {
        AiChatMessageEntity message = lastMessage(session.getId(), userId);
        return message != null && "user".equals(message.getRole()) && content.equals(message.getContent());
    }

    private AiChatMessageEntity lastMessage(Long sessionId, Long userId) {
        return messageMapper.selectOne(new LambdaQueryWrapperX<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getSessionId, sessionId)
                .eq(AiChatMessageEntity::getUserId, userId)
                .orderByDesc(AiChatMessageEntity::getMessageOrder)
                .last("LIMIT 1"));
    }

    private AiChatSessionEntity saveChatHistory(String sessionId, String model,
            String userContent, String assistantContent) {
        Long userId = requireUserId();
        AiChatSessionEntity session;
        Date now = new Date();
        if (trimToNull(sessionId) == null) {
            session = new AiChatSessionEntity();
            session.setUserId(userId);
            session.setTitle(toTitle(userContent));
            session.setModel(model);
            session.setMessageCount(0);
            session.setLastMessageAt(now);
            sessionMapper.insert(session);
        } else {
            session = requireMySession(sessionId);
            session.setModel(model);
            session.setLastMessageAt(now);
        }
        int messageOrder = session.getMessageCount() == null ? 0 : session.getMessageCount();
        insertMessage(userId, session.getId(), ++messageOrder, "user", userContent);
        insertMessage(userId, session.getId(), ++messageOrder, "assistant", assistantContent);
        session.setMessageCount(messageOrder);
        session.setLastMessageAt(now);
        sessionMapper.updateById(session);
        return session;
    }

    private void insertMessage(Long userId, Long sessionId, int messageOrder,
            String role, String content) {
        AiChatMessageEntity entity = new AiChatMessageEntity();
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setMessageOrder(messageOrder);
        entity.setRole(role);
        entity.setContent(content);
        messageMapper.insert(entity);
    }

    private String lastUserMessage(List<Map<String, String>> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            Map<String, String> message = messages.get(index);
            if ("user".equals(message.get("role"))) {
                return message.get("content");
            }
        }
        throw new IllegalArgumentException("A user message is required before starting a chat");
    }

    private AiChatSessionEntity requireMySession(String sessionId) {
        Long id;
        try {
            id = Long.valueOf(sessionId);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Chat history does not exist");
        }
        return requireMySession(id, requireUserId());
    }

    private AiChatSessionEntity requireMySession(Long sessionId, Long userId) {
        AiChatSessionEntity session = sessionMapper.selectOne(new LambdaQueryWrapperX<AiChatSessionEntity>()
                .eq(AiChatSessionEntity::getId, sessionId)
                .eq(AiChatSessionEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (session == null) {
            throw new IllegalArgumentException("Chat history does not exist");
        }
        return session;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new IllegalStateException("Unable to determine the current user");
        }
        return userId;
    }

    private String toTitle(String content) {
        String title = content.replaceAll("\\s+", " ").trim();
        return title.length() <= 60 ? title : title.substring(0, 60) + "…";
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static class PreparedChat {
        private final Long userId;
        private final AiUserConfigEntity config;
        private final String model;
        private final List<Map<String, String>> messages;

        private PreparedChat(Long userId, AiUserConfigEntity config, String model,
                List<Map<String, String>> messages) {
            this.userId = userId;
            this.config = config;
            this.model = model;
            this.messages = messages;
        }
    }
}
