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
@TableName("routine_meeting_plan")
public class RoutineMeetingPlanEntity extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String goal;

    private String leaderId;

    private String leaderName;

    private String creatorId;

    private String creatorName;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate originalEndDate;

    private LocalDate issuedAt;

    private Integer progress;

    private Date completedAt;

    private Integer draft;
}
