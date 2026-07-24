package com.cac.oa.vo.ai;

import lombok.Data;

@Data
public class AiConfigVO {
    private String baseUrl;
    private String defaultModel;
    private Boolean apiKeyConfigured;
    private String apiKeySuffix;
}
