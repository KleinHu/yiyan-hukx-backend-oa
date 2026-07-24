package com.cac.oa.vo.ai;

import lombok.Data;

import java.util.Date;

@Data
public class AiChatSessionVO {
    private String id;
    private String title;
    private String model;
    private Date lastMessageAt;
}
