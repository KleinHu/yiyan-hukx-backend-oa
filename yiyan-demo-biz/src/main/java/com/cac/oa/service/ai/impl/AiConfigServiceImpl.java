package com.cac.oa.service.ai.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cac.oa.dao.ai.AiUserConfigMapper;
import com.cac.oa.dao.ai.AiUserModelMapper;
import com.cac.oa.entity.ai.AiUserConfigEntity;
import com.cac.oa.entity.ai.AiUserModelEntity;
import com.cac.oa.service.ai.AiConfigService;
import com.cac.oa.service.ai.OpenAiGatewayClient;
import com.cac.oa.service.ai.AiChatTokenBudget;
import com.cac.oa.vo.ai.AiConfigSaveRequest;
import com.cac.oa.vo.ai.AiConfigVO;
import com.cac.oa.vo.ai.AiGatewayDiscoverRequest;
import com.cac.oa.vo.ai.AiModelVO;
import com.cac.yiyan.mybatis.query.LambdaQueryWrapperX;
import com.cac.yiyan.security.core.util.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl extends ServiceImpl<AiUserConfigMapper, AiUserConfigEntity> implements AiConfigService {
    private final AiUserConfigMapper mapper;
    private final AiUserModelMapper modelMapper;
    private final OpenAiGatewayClient gatewayClient;

    @Override
    public AiConfigVO getMyConfig() {
        return toConfigVO(findMyConfig());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConfigVO saveMyConfig(AiConfigSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AI configuration is required");
        }
        Long userId = requireUserId();
        AiUserConfigEntity entity = findMyConfig();
        boolean isNew = entity == null;
        if (isNew) {
            entity = new AiUserConfigEntity();
            entity.setUserId(userId);
            entity.setUserCode(SecurityUtils.getUserCode());
        }
        String baseUrl = gatewayClient.normalizeBaseUrl(request.getBaseUrl());
        String apiKey = trimToNull(request.getApiKey());
        entity.setBaseUrl(baseUrl);
        int contextWindowTokens = resolveContextWindowTokens(request.getContextWindowTokens());
        resolveMaxInputTokens(request.getMaxInputTokens(), contextWindowTokens);
        entity.setContextWindowTokens(request.getContextWindowTokens());
        entity.setMaxInputTokens(request.getMaxInputTokens());
        List<AiModelVO> selectedModels = normalizeSelectedModels(request.getModels());
        entity.setDefaultModel(selectDefaultModel(request.getDefaultModel(), selectedModels));
        if (apiKey != null) {
            entity.setApiKey(apiKey);
        }
        if (trimToNull(entity.getApiKey()) == null) {
            throw new IllegalArgumentException("Provider key is required when first saving this configuration");
        }
        if (isNew) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        replaceMyModels(userId, selectedModels);
        entity.setModelCacheUpdatedAt(new Date());
        mapper.updateById(entity);
        return toConfigVO(entity);
    }

    @Override
    public List<AiModelVO> discoverModels(AiGatewayDiscoverRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Gateway configuration is required");
        }
        String baseUrl = gatewayClient.normalizeBaseUrl(request.getBaseUrl());
        String apiKey = trimToNull(request.getApiKey());
        if (apiKey == null) {
            AiUserConfigEntity currentConfig = findMyConfig();
            if (currentConfig != null && Objects.equals(currentConfig.getBaseUrl(), baseUrl)) {
                apiKey = trimToNull(currentConfig.getApiKey());
            }
        }
        if (apiKey == null) {
            throw new IllegalArgumentException("Provider key is required before retrieving gateway models");
        }
        return fetchGatewayModels(baseUrl, apiKey);
    }

    @Override
    public List<AiModelVO> listMyModels() {
        AiUserConfigEntity config = requireConfigured();
        if (config.getModelCacheUpdatedAt() != null) {
            return toModelVOs(findEnabledModels(requireUserId()));
        }
        return refreshMyModels();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AiModelVO> refreshMyModels() {
        AiUserConfigEntity config = requireConfigured();
        List<AiModelVO> discoveredModels = fetchGatewayModels(config.getBaseUrl(), config.getApiKey());
        Long userId = requireUserId();
        Date now = new Date();
        Map<String, AiUserModelEntity> existingById = new HashMap<String, AiUserModelEntity>();
        for (AiUserModelEntity item : modelMapper.selectList(new LambdaQueryWrapperX<AiUserModelEntity>()
                .eq(AiUserModelEntity::getUserId, userId))) {
            existingById.put(item.getModelId(), item);
        }
        Set<String> seenIds = new HashSet<String>();
        for (AiModelVO item : discoveredModels) {
            String modelId = item.getId();
            if (seenIds.add(modelId)) {
                AiUserModelEntity model = existingById.get(modelId);
                String modelName = item.getName();
                if (model == null) {
                    model = new AiUserModelEntity();
                    model.setUserId(userId);
                    model.setModelId(modelId);
                    model.setEnabled(1);
                    model.setModelName(modelName == null ? modelId : modelName);
                    model.setLastSeenTime(now);
                    modelMapper.insert(model);
                } else {
                    model.setModelName(modelName == null ? modelId : modelName);
                    model.setLastSeenTime(now);
                    modelMapper.updateById(model);
                }
            }
        }
        for (AiUserModelEntity model : existingById.values()) {
            if (!seenIds.contains(model.getModelId()) && !Integer.valueOf(0).equals(model.getEnabled())) {
                model.setEnabled(0);
                modelMapper.updateById(model);
            }
        }
        config.setModelCacheUpdatedAt(now);
        mapper.updateById(config);
        return toModelVOs(findEnabledModels(userId));
    }

    @Override
    public boolean isMyEnabledModel(String modelId) {
        String normalizedModelId = trimToNull(modelId);
        return normalizedModelId != null && modelMapper.selectCount(new LambdaQueryWrapperX<AiUserModelEntity>()
                .eq(AiUserModelEntity::getUserId, requireUserId())
                .eq(AiUserModelEntity::getModelId, normalizedModelId)
                .eq(AiUserModelEntity::getEnabled, 1)) > 0;
    }

    public AiUserConfigEntity requireConfigured() {
        AiUserConfigEntity config = findMyConfig();
        if (config == null || trimToNull(config.getApiKey()) == null) {
            throw new IllegalStateException("Configure an AI gateway and provider key before starting a chat");
        }
        return config;
    }

    private AiUserConfigEntity findMyConfig() {
        return mapper.selectOne(new LambdaQueryWrapperX<AiUserConfigEntity>()
                .eq(AiUserConfigEntity::getUserId, requireUserId())
                .last("LIMIT 1"));
    }

    private List<AiUserModelEntity> findEnabledModels(Long userId) {
        return modelMapper.selectList(new LambdaQueryWrapperX<AiUserModelEntity>()
                .eq(AiUserModelEntity::getUserId, userId)
                .eq(AiUserModelEntity::getEnabled, 1)
                .orderByAsc(AiUserModelEntity::getModelName));
    }

    private List<AiModelVO> fetchGatewayModels(String baseUrl, String apiKey) {
        JsonNode response = gatewayClient.get(baseUrl, "/models", apiKey);
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            throw new IllegalStateException("The AI gateway did not return an OpenAI-compatible model list");
        }
        List<AiModelVO> result = new ArrayList<AiModelVO>();
        Set<String> seenIds = new HashSet<String>();
        for (JsonNode item : data) {
            String modelId = trimToNull(item.path("id").asText());
            if (modelId != null && modelId.length() <= 256 && seenIds.add(modelId)) {
                String modelName = trimToNull(item.path("name").asText());
                String normalizedName = modelName == null ? modelId : modelName;
                result.add(new AiModelVO(modelId,
                        normalizedName.substring(0, Math.min(normalizedName.length(), 512))));
            }
        }
        return result;
    }

    private List<AiModelVO> normalizeSelectedModels(List<AiModelVO> models) {
        if (models == null || models.isEmpty()) {
            throw new IllegalArgumentException("Select at least one gateway model before saving");
        }
        List<AiModelVO> result = new ArrayList<AiModelVO>();
        Set<String> seenIds = new HashSet<String>();
        for (AiModelVO model : models) {
            String id = model == null ? null : trimToNull(model.getId());
            String name = model == null ? null : trimToNull(model.getName());
            if (id == null || id.length() > 256 || !seenIds.add(id)) {
                continue;
            }
            result.add(new AiModelVO(id, name == null ? id : name.substring(0, Math.min(name.length(), 512))));
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Select at least one valid gateway model before saving");
        }
        return result;
    }

    private String selectDefaultModel(String requestedDefault, List<AiModelVO> models) {
        String defaultModel = trimToNull(requestedDefault);
        for (AiModelVO model : models) {
            if (Objects.equals(model.getId(), defaultModel)) {
                return defaultModel;
            }
        }
        return models.get(0).getId();
    }

    private void replaceMyModels(Long userId, List<AiModelVO> selectedModels) {
        modelMapper.delete(new LambdaQueryWrapperX<AiUserModelEntity>()
                .eq(AiUserModelEntity::getUserId, userId));
        Date now = new Date();
        for (AiModelVO selectedModel : selectedModels) {
            AiUserModelEntity entity = new AiUserModelEntity();
            entity.setUserId(userId);
            entity.setModelId(selectedModel.getId());
            entity.setModelName(selectedModel.getName());
            entity.setEnabled(1);
            entity.setLastSeenTime(now);
            modelMapper.insert(entity);
        }
    }

    private List<AiModelVO> toModelVOs(List<AiUserModelEntity> entities) {
        List<AiModelVO> models = new ArrayList<AiModelVO>();
        for (AiUserModelEntity entity : entities) {
            models.add(new AiModelVO(entity.getModelId(), entity.getModelName()));
        }
        return models;
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new IllegalStateException("Unable to determine the current user");
        }
        return userId;
    }

    static AiConfigVO toConfigVO(AiUserConfigEntity entity) {
        AiConfigVO result = new AiConfigVO();
        if (entity == null) {
            result.setApiKeyConfigured(false);
            result.setMaxInputTokens(AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS);
            result.setContextWindowTokens(AiChatTokenBudget.DEFAULT_CONTEXT_WINDOW_TOKENS);
            return result;
        }
        result.setBaseUrl(entity.getBaseUrl());
        result.setDefaultModel(entity.getDefaultModel());
        result.setApiKeyConfigured(trimToNull(entity.getApiKey()) != null);
        result.setApiKeySuffix(result.getApiKeyConfigured() ? "已保存" : null);
        result.setMaxInputTokens(resolveMaxInputTokens(entity.getMaxInputTokens(),
                resolveContextWindowTokens(entity.getContextWindowTokens())));
        result.setContextWindowTokens(resolveContextWindowTokens(entity.getContextWindowTokens()));
        return result;
    }

    private static int resolveContextWindowTokens(Integer value) {
        int result = value == null ? AiChatTokenBudget.DEFAULT_CONTEXT_WINDOW_TOKENS : value;
        if (result < 1024 || result > 1048576) {
            throw new IllegalArgumentException("模型上下文大小需在 1024 到 1048576 Token 之间");
        }
        return result;
    }

    private static int resolveMaxInputTokens(Integer value, int contextWindowTokens) {
        int result = value == null ? AiChatTokenBudget.DEFAULT_MAX_INPUT_TOKENS : value;
        if (result < 256 || result > contextWindowTokens) {
            throw new IllegalArgumentException("单次请求最大输入 Token 需在 256 到模型上下文大小之间");
        }
        return result;
    }

    private static String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
