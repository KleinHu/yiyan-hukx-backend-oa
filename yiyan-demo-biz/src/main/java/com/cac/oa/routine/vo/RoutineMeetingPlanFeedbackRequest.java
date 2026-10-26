package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@ApiModel("行政例会计划周反馈")
public class RoutineMeetingPlanFeedbackRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("周开始日期")
    @NotNull(message = "周次不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate weekStart;

    @ApiModelProperty("完成进度")
    @NotNull(message = "完成进度不能为空")
    @Min(value = 0, message = "完成进度不能小于0")
    @Max(value = 100, message = "完成进度不能大于100")
    private Integer progress;

    @ApiModelProperty("反馈内容")
    @NotBlank(message = "反馈内容不能为空")
    private String content;
}
