package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@ApiModel("行政例会计划保存")
public class RoutineMeetingPlanSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("计划名称")
    @Size(max = 60, message = "计划名称不能超过60字")
    private String name;

    @ApiModelProperty("目标")
    private String goal;

    @ApiModelProperty("责任领导ID")
    private String leaderId;

    @ApiModelProperty("责任领导")
    private String leaderName;

    @ApiModelProperty("开始日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @ApiModelProperty("结束日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @ApiModelProperty("是否草稿")
    private Boolean draft;
}
