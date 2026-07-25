package com.cac.oa.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cac.yiyan.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** Per-user connection details for an OpenAI-compatible gateway. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_user_config")
public class AiUserConfigEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String userCode;

    private String baseUrl;

    /**
     * Plaintext provider key for the internal-network deployment. Never return
     * this value from an API response.
     */
    @TableField("api_key_encrypted")
    private String apiKey;

    private String defaultModel;

    /** The last time the locally persisted model cache was refreshed from the gateway. */
    private Date modelCacheUpdatedAt;

    /** Maximum input tokens accepted by the configured gateway for one request. */
    private Integer maxInputTokens;

    /** Model context window (input plus output), kept as a gateway-level guardrail. */
    private Integer contextWindowTokens;
}
