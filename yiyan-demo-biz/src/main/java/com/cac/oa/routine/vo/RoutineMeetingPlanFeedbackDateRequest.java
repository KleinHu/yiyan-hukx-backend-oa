package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@ApiModel("行政例会计划周反馈日期校正")
public class RoutineMeetingPlanFeedbackDateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("周开始日期")
    @NotNull(message = "周次不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate weekStart;

    @ApiModelProperty("实际反馈日期")
    @NotNull(message = "实际反馈日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actualDate;
}
