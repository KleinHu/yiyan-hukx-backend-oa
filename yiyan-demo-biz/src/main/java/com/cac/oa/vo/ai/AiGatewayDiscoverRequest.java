package com.cac.oa.vo.ai;

import lombok.Data;

/** Connection details used to retrieve models before a configuration is saved. */
@Data
public class AiGatewayDiscoverRequest {
    private String baseUrl;
    /** Required for a new gateway; optional when reusing the saved gateway key. */
    private String apiKey;
}
