package com.cac.oa.vo.sso;

import com.cac.yiyan.common.page.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 外部系统跳转配置查询参数
 *
 * @author antigravity
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("外部系统查询")
public class ExternalSystemQuery extends PageParam {

    @ApiModelProperty("系统名称(模糊查询)")
    private String name;
}
