package com.cac.oa.vo.article;

import com.cac.yiyan.common.page.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章专栏查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("专栏查询")
public class ArticleColumnQuery extends PageParam {

    @ApiModelProperty("专栏名称(模糊查询)")
    private String name;

    @ApiModelProperty("状态(1-启用 0-停用)")
    private Integer status;

    @ApiModelProperty("是否首页展示(1-展示 0-不展示)")
    private Integer isHomeShow;
}
