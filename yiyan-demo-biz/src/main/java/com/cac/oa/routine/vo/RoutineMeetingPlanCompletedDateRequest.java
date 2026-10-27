package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@ApiModel("行政例会计划完成日期校正")
public class RoutineMeetingPlanCompletedDateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("实际完成日期")
    @NotNull(message = "实际完成日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate completedDate;
}
