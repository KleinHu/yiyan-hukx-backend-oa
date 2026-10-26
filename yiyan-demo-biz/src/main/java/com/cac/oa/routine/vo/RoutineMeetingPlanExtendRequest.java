package com.cac.oa.routine.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@ApiModel("行政例会计划延期")
public class RoutineMeetingPlanExtendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("延期至")
    @NotNull(message = "延期日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
