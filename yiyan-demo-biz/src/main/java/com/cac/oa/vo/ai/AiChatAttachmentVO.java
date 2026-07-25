package com.cac.oa.vo.ai;

import lombok.Data;

/** Metadata for a file retained with a user AI chat message. */
@Data
public class AiChatAttachmentVO {
    private String name;
    private Long size;
    private String contentType;
}
