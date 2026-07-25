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
import com.cac.oa.service.ai.AiAttachmentTextExtractor;
import com.cac.oa.service.ai.AiChatTokenBudget;
import com.cac.oa.service.MinIO.MinIOService;
import com.cac.oa.vo.ai.AiChatAttachmentVO;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {
    private static final int MAX_MESSAGES = 30;
    private static final int MAX_HISTORY_MESSAGES = 6;
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
    private final AiAttachmentTextExtractor attachmentTextExtractor;
    private final MinIOService minIOService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatVO chat(AiChatRequest request) {
        PreparedChat prepared = prepareChat(request, null);
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
                prepared.rawUserContent, content);
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
        return startPreparedStream(request, prepareChat(request, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatStreamContext startStreamWithAttachment(AiChatRequest request, MultipartFile file) {
        if (Boolean.TRUE.equals(request.getRetry())) {
            throw new IllegalArgumentException("附件消息不支持重新生成");
        }
        AiAttachmentTextExtractor.ExtractedAttachment attachment = attachmentTextExtractor.extract(file);
        PreparedChat prepared = prepareChat(request, attachment);
        PersistedStreamUserMessage persisted = persistStreamUserMessage(request.getSessionId(), prepared.model,
                prepared.rawUserContent, prepared.userId, Boolean.TRUE.equals(request.getRetry()));
        persistAttachment(file, attachment, persisted.message, prepared.userId, persisted.session.getId());
        return createStreamContext(prepared, persisted.session);
    }

    private AiChatStreamContext startPreparedStream(AiChatRequest request, PreparedChat prepared) {
        boolean retry = Boolean.TRUE.equals(request.getRetry());
        if (retry && trimToNull(request.getSessionId()) == null) {
            throw new IllegalArgumentException("A saved chat session is required to regenerate a response");
        }
        PersistedStreamUserMessage persisted = persistStreamUserMessage(request.getSessionId(), prepared.model,
                prepared.rawUserContent, prepared.userId, retry);
        return createStreamContext(prepared, persisted.session);
    }

    private AiChatStreamContext createStreamContext(PreparedChat prepared, AiChatSessionEntity session) {
        return new AiChatStreamContext(prepared.userId, session.getId(), prepared.model,
                prepared.config.getBaseUrl(), prepared.config.getApiKey(), prepared.messages,
                prepared.attachmentRequest);
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
            message.setAttachment(toAttachmentVO(entity));
            messages.add(message);
        }
        result.setMessages(messages);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMySession(String sessionId) {
        AiChatSessionEntity session = requireMySession(sessionId);
        List<AiChatMessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapperX<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getSessionId, session.getId())
                .eq(AiChatMessageEntity::getUserId, requireUserId()));
        for (AiChatMessageEntity message : messages) {
            if (trimToNull(message.getAttachmentPath()) != null) {
                minIOService.deleteFile(message.getAttachmentPath());
            }
        }
        messageMapper.delete(new LambdaQueryWrapperX<AiChatMessageEntity>()
                .eq(AiChatMessageEntity::getSessionId, session.getId())
                .eq(AiChatMessageEntity::getUserId, requireUserId()));
        sessionMapper.deleteById(session.getId());
    }

    private PreparedChat prepareChat(AiChatRequest request, AiAttachmentTextExtractor.ExtractedAttachment attachment) {
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
        int currentUserIndex = lastUserIndex(messages);
        if (currentUserIndex != messages.size() - 1) {
            throw new IllegalArgumentException("The current chat message must be a user message");
        }
        String rawUserContent = messages.get(currentUserIndex).get("content");
        int historyStart = Math.max(0, currentUserIndex - MAX_HISTORY_MESSAGES);
        List<Map<String, String>> gatewayMessages = new ArrayList<Map<String, String>>();
        for (int index = historyStart; index <= currentUserIndex; index++) {
            Map<String, String> source = messages.get(index);
            Map<String, String> item = new HashMap<String, String>();
            item.put("role", source.get("role"));
            item.put("content", source.get("content"));
            gatewayMessages.add(item);
        }
        if (attachment != null) {
            Map<String, String> currentUser = gatewayMessages.get(gatewayMessages.size() - 1);
            currentUser.put("content", appendAttachment(rawUserContent, attachment));
        }
        int maxInputTokens = effectiveMaxInputTokens(config);
        while (AiChatTokenBudget.estimate(gatewayMessages) > maxInputTokens && gatewayMessages.size() > 1) {
            gatewayMessages.remove(0);
        }
        if (AiChatTokenBudget.estimate(gatewayMessages) > maxInputTokens) {
            throw new IllegalArgumentException("内容超过网关单次输入限制，请缩小文件、缩短问题或新建对话");
        }
        return new PreparedChat(requireUserId(), config, model, gatewayMessages, rawUserContent,
                attachment != null);
    }

    private int effectiveMaxInputTokens(AiUserConfigEntity config) {
        int contextWindow = config.getContextWindowTokens() == null
                ? AiChatTokenBudget.DEFAULT_CONTEXT_WINDOW_TOKENS : config.getContextWindowTokens();
        int maxInput = config.getMaxInputTokens() == null
                ? AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS : config.getMaxInputTokens();
        if (maxInput > contextWindow) {
            throw new IllegalStateException("AI 网关 Token 配置无效，请重新保存配置");
        }
        return maxInput;
    }

    private String appendAttachment(String userContent, AiAttachmentTextExtractor.ExtractedAttachment attachment) {
        return userContent + "\n\n--- 附件内容开始 ---\n文件名：" + attachment.getFileName()
                + "\n类型：" + attachment.getExtension() + "\n" + attachment.getText()
                + "\n--- 附件内容结束 ---";
    }

    private int lastUserIndex(List<Map<String, String>> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            if ("user".equals(messages.get(index).get("role"))) {
                return index;
            }
        }
        throw new IllegalArgumentException("A user message is required before starting a chat");
    }

    private PersistedStreamUserMessage persistStreamUserMessage(String sessionId, String model,
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
                    return new PersistedStreamUserMessage(session, lastMessage(session.getId(), userId));
                }
            }
            session.setModel(model);
            session.setLastMessageAt(now);
        }
        int messageOrder = session.getMessageCount() == null ? 0 : session.getMessageCount();
        AiChatMessageEntity message = insertMessage(userId, session.getId(), ++messageOrder, "user", userContent);
        session.setMessageCount(messageOrder);
        session.setLastMessageAt(now);
        sessionMapper.updateById(session);
        return new PersistedStreamUserMessage(session, message);
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

    private AiChatMessageEntity insertMessage(Long userId, Long sessionId, int messageOrder,
            String role, String content) {
        AiChatMessageEntity entity = new AiChatMessageEntity();
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setMessageOrder(messageOrder);
        entity.setRole(role);
        entity.setContent(content);
        messageMapper.insert(entity);
        return entity;
    }

    private void persistAttachment(MultipartFile file, AiAttachmentTextExtractor.ExtractedAttachment attachment,
            AiChatMessageEntity message, Long userId, Long sessionId) {
        String objectPath = "/oa/ai-chat/" + userId + "/" + sessionId + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + attachment.getExtension();
        boolean uploaded = false;
        try {
            minIOService.uploadFileWithFullPath(file, objectPath);
            uploaded = true;
            message.setAttachmentName(attachment.getFileName());
            message.setAttachmentPath(objectPath);
            message.setAttachmentSize(file.getSize());
            message.setAttachmentContentType(file.getContentType());
            if (messageMapper.updateById(message) != 1) {
                throw new IllegalStateException("附件记录保存失败");
            }
        } catch (RuntimeException exception) {
            if (uploaded) {
                try {
                    minIOService.deleteFile(objectPath);
                } catch (RuntimeException ignored) {
                    // Keep the original database or upload failure as the client-visible error.
                }
            }
            throw exception;
        }
    }

    private AiChatAttachmentVO toAttachmentVO(AiChatMessageEntity entity) {
        if (trimToNull(entity.getAttachmentName()) == null) {
            return null;
        }
        AiChatAttachmentVO attachment = new AiChatAttachmentVO();
        attachment.setName(entity.getAttachmentName());
        attachment.setSize(entity.getAttachmentSize());
        attachment.setContentType(entity.getAttachmentContentType());
        return attachment;
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
        private final String rawUserContent;
        private final boolean attachmentRequest;

        private PreparedChat(Long userId, AiUserConfigEntity config, String model,
                List<Map<String, String>> messages, String rawUserContent, boolean attachmentRequest) {
            this.userId = userId;
            this.config = config;
            this.model = model;
            this.messages = messages;
            this.rawUserContent = rawUserContent;
            this.attachmentRequest = attachmentRequest;
        }
    }

    private static class PersistedStreamUserMessage {
        private final AiChatSessionEntity session;
        private final AiChatMessageEntity message;

        private PersistedStreamUserMessage(AiChatSessionEntity session, AiChatMessageEntity message) {
            this.session = session;
            this.message = message;
        }
    }
}
