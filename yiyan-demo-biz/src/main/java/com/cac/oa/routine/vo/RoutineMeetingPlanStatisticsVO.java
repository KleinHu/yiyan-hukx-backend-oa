package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("行政例会计划统计")
public class RoutineMeetingPlanStatisticsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("计划总数")
    private Long total;

    @ApiModelProperty("未开始")
    private Long notStarted;

    @ApiModelProperty("执行中")
    private Long active;

    @ApiModelProperty("已完成")
    private Long completed;

    @ApiModelProperty("年份列表")
    private List<Integer> years;
}
