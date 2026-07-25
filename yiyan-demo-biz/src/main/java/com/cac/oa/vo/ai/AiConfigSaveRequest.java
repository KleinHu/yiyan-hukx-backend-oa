package com.cac.oa.vo.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiConfigSaveRequest {
    private String baseUrl;
    /** Optional on update. An omitted key keeps the value already stored. */
    private String apiKey;
    private String defaultModel;
    private Integer maxInputTokens;
    private Integer contextWindowTokens;
    /** The models this user explicitly enabled after discovering the gateway list. */
    private List<AiModelVO> models;
}
