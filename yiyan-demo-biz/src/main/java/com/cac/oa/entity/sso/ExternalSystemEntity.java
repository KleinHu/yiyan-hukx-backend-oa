package com.cac.oa.entity.sso;

import com.baomidou.mybatisplus.annotation.*;
import com.cac.yiyan.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部系统跳转配置实体
 *
 * @author antigravity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_external_system")
public class ExternalSystemEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 系统名称
     */
    private String name;

    /**
     * 跳转URL
     */
    private String url;

    /**
     * 图标标识
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
