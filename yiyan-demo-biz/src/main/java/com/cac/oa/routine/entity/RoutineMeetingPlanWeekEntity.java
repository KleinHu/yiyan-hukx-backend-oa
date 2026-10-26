package com.cac.oa.routine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cac.yiyan.mybatis.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("routine_meeting_plan_week")
public class RoutineMeetingPlanWeekEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long planId;

    private LocalDate weekStart;

    private LocalDate weekEnd;

    private String leaderId;

    private String leaderName;

    private Integer progress;

    private String content;

    private Date firstFeedbackAt;
}
