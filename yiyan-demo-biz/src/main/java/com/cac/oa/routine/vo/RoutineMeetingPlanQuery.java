package com.cac.oa.routine.vo;

import com.cac.yiyan.common.page.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("行政例会计划查询")
public class RoutineMeetingPlanQuery extends PageParam {

    @ApiModelProperty("计划名称")
    private String name;

    @ApiModelProperty("计划状态")
    private String status;

    @ApiModelProperty("年度")
    private Integer year;

    @ApiModelProperty("计划周期开始")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @ApiModelProperty("计划周期结束")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
