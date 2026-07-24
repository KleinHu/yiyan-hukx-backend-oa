package com.cac.oa.entity.article;

import com.baomidou.mybatisplus.annotation.*;
import com.cac.yiyan.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专栏实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_column")
public class ArticleColumnEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 父专栏ID
     */
    private Long parentId;

    /**
     * 专栏名称
     */
    private String name;

    /**
     * 专栏简介
     */
    private String description;

    /**
     * 专栏封面图
     */
    private String coverUrl;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态 (1-启用 0-停用)
     */
    private Integer status;

    /**
     * 是否首页展示 (1-展示 0-不展示)
     */
    private Integer isHomeShow;

    /**
     * 首页排序号
     */
    private Integer homeSort;
}
