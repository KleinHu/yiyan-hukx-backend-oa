package com.cac.oa.routine.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

@Data
@ApiModel("行政例会计划周反馈")
public class RoutineMeetingPlanWeekVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("周ID")
    private Long id;

    @ApiModelProperty("计划ID")
    private Long planId;

    @ApiModelProperty("周开始")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate weekStart;

    @ApiModelProperty("周结束")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate weekEnd;

    @ApiModelProperty("周显示")
    private String weekLabel;

    @ApiModelProperty("反馈人ID")
    private String leaderId;

    @ApiModelProperty("反馈人")
    private String leaderName;

    @ApiModelProperty("完成进度")
    private Integer progress;

    @ApiModelProperty("反馈内容")
    private String content;

    @ApiModelProperty("首次反馈时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date firstFeedbackAt;

    @ApiModelProperty("周状态")
    private String status;

    @ApiModelProperty("周状态名称")
    private String statusName;

    @ApiModelProperty("是否可以反馈")
    private Boolean canFeedback;
}
