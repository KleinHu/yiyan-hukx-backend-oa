package com.cac.oa.routine.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
@ApiModel("行政例会计划")
public class RoutineMeetingPlanVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("ID")
    private Long id;

    @ApiModelProperty("计划名称")
    private String name;

    @ApiModelProperty("目标")
    private String goal;

    @ApiModelProperty("责任领导ID")
    private String leaderId;

    @ApiModelProperty("责任领导")
    private String leaderName;

    @ApiModelProperty("创建人ID")
    private String creatorId;

    @ApiModelProperty("创建人")
    private String creatorName;

    @ApiModelProperty("开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @ApiModelProperty("结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @ApiModelProperty("原结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate originalEndDate;

    @ApiModelProperty("计划下达时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issuedAt;

    @ApiModelProperty("完成进度")
    private Integer progress;

    @ApiModelProperty("实际完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completedAt;

    @ApiModelProperty("是否草稿")
    private Boolean draft;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("状态名称")
    private String statusName;

    @ApiModelProperty("计划时长")
    private Long durationDays;

    @ApiModelProperty("剩余天数")
    private Long remainDays;

    @ApiModelProperty("超期天数")
    private Long overdueDays;

    @ApiModelProperty("未反馈周数")
    private Integer pendingWeekCount;

    @ApiModelProperty("当前周开始")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate currentWeekStart;

    @ApiModelProperty("是否可编辑")
    private Boolean canEdit;

    @ApiModelProperty("是否可删除")
    private Boolean canDelete;

    @ApiModelProperty("是否可延期")
    private Boolean canExtend;

    @ApiModelProperty("是否可反馈")
    private Boolean canFeedback;

    @ApiModelProperty("周反馈")
    private List<RoutineMeetingPlanWeekVO> weeks;
}
