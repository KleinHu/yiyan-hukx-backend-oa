package com.cac.oa.vo.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private String sessionId;
    private String model;
    private Boolean retry;
    private List<AiChatMessageRequest> messages;
}
