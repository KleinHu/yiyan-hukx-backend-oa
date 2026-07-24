package com.cac.oa.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cac.yiyan.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** A model discovered from one user's OpenAI-compatible gateway. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_ai_user_model")
public class AiUserModelEntity extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String modelId;

    private String modelName;

    /** 1 when the model is selectable; 0 when it disappeared from the latest gateway refresh. */
    private Integer enabled;

    private Date lastSeenTime;
}
