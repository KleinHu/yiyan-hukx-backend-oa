package com.cac.oa.service.ai;

import com.cac.oa.vo.ai.AiConfigSaveRequest;
import com.cac.oa.vo.ai.AiConfigVO;
import com.cac.oa.vo.ai.AiGatewayDiscoverRequest;
import com.cac.oa.vo.ai.AiModelVO;

import java.util.List;

public interface AiConfigService {
    AiConfigVO getMyConfig();
    AiConfigVO saveMyConfig(AiConfigSaveRequest request);
    List<AiModelVO> discoverModels(AiGatewayDiscoverRequest request);
    /** Returns the persisted cache; the gateway is contacted only before the first cache exists. */
    List<AiModelVO> listMyModels();
    /** Explicitly refreshes the current user's persisted model cache from their gateway. */
    List<AiModelVO> refreshMyModels();
    boolean isMyEnabledModel(String modelId);
}
