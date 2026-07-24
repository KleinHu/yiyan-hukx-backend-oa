package com.cac.oa.vo.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiChatSessionDetailVO {
    private String id;
    private String model;
    private List<AiChatHistoryMessageVO> messages;
}
