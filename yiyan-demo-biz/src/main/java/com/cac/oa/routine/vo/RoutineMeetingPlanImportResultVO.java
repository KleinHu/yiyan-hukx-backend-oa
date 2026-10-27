package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("行政例会计划导入结果")
public class RoutineMeetingPlanImportResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("导入成功条数")
    private Integer successCount;
}
