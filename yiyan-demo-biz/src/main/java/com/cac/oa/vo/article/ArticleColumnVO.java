package com.cac.oa.vo.article;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 文章专栏 VO
 */
@Data
@ApiModel("文章专栏")
public class ArticleColumnVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("父级ID")
    private Long parentId;

    @ApiModelProperty("子级列表")
    private java.util.List<ArticleColumnVO> children;

    @ApiModelProperty("专栏名称")
    private String name;

    @ApiModelProperty("专栏简介")
    private String description;

    @ApiModelProperty("专栏封面图")
    private String coverUrl;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("状态(1-启用 0-停用)")
    private Integer status;

    @ApiModelProperty("是否首页展示(1-展示 0-不展示)")
    private Integer isHomeShow;

    @ApiModelProperty("首页排序号")
    private Integer homeSort;

    @ApiModelProperty("创建时间")
    private Date createTime;
}
