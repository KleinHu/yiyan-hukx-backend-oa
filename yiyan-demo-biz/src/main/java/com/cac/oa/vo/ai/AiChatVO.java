package com.cac.oa.vo.ai;

import lombok.Data;

@Data
public class AiChatVO {
    private String id;
    private String sessionId;
    private String model;
    private String content;
    private String finishReason;
}
