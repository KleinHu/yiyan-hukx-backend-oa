package com.cac.oa.vo.sso;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 外部系统跳转配置 VO
 *
 * @author antigravity
 */
@Data
@ApiModel("外部系统跳转配置")
public class ExternalSystemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("系统名称")
    private String name;

    @ApiModelProperty("跳转URL")
    private String url;

    @ApiModelProperty("图标标识")
    private String icon;

    @ApiModelProperty("排序号")
    private Integer sortOrder;

    @ApiModelProperty("创建时间")
    private Date createTime;
}
