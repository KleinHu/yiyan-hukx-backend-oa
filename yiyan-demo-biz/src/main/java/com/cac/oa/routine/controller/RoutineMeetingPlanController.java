package com.cac.oa.routine.controller;

import com.cac.oa.routine.service.IRoutineMeetingPlanService;
import com.cac.oa.routine.vo.RoutineMeetingPlanExtendRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanFeedbackRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanQuery;
import com.cac.oa.routine.vo.RoutineMeetingPlanSaveRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanVO;
import com.cac.yiyan.common.page.PageData;
import com.cac.yiyan.common.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "OA-行政例会计划管理")
@RestController
@RequestMapping("/api/240/oa/routine-meeting-plan")
@RequiredArgsConstructor
public class RoutineMeetingPlanController {

    private final IRoutineMeetingPlanService planService;

    @GetMapping("/page")
    @ApiOperation("计划管理分页")
    public Result<PageData<RoutineMeetingPlanVO>> page(RoutineMeetingPlanQuery query) {
        return Result.ok(planService.page(query));
    }

    @GetMapping("/my-plans/page")
    @ApiOperation("我的计划分页")
    public Result<PageData<RoutineMeetingPlanVO>> myPlans(RoutineMeetingPlanQuery query) {
        return Result.ok(planService.myPlans(query));
    }

    @GetMapping("/my-feedback/page")
    @ApiOperation("我的反馈分页")
    public Result<PageData<RoutineMeetingPlanVO>> myFeedback(RoutineMeetingPlanQuery query) {
        return Result.ok(planService.myFeedback(query));
    }

    @GetMapping("/{id}")
    @ApiOperation("计划详情")
    public Result<RoutineMeetingPlanVO> detail(@PathVariable Long id) {
        return Result.ok(planService.detail(id));
    }

    @PostMapping
    @ApiOperation("新增计划")
    public Result<Long> save(@Valid @RequestBody RoutineMeetingPlanSaveRequest request) {
        return Result.ok(planService.savePlan(request));
    }

    @PutMapping("/{id}")
    @ApiOperation("修改计划")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody RoutineMeetingPlanSaveRequest request) {
        planService.updatePlan(id, request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除计划")
    public Result<Void> delete(@PathVariable Long id) {
        planService.deletePlan(id);
        return Result.ok();
    }

    @PutMapping("/{id}/extend")
    @ApiOperation("延期")
    public Result<Void> extend(@PathVariable Long id,
                               @Valid @RequestBody RoutineMeetingPlanExtendRequest request) {
        planService.extend(id, request);
        return Result.ok();
    }

    @PostMapping("/{id}/feedback")
    @ApiOperation("填写/修改周反馈")
    public Result<Void> feedback(@PathVariable Long id,
                                 @Valid @RequestBody RoutineMeetingPlanFeedbackRequest request) {
        planService.feedback(id, request);
        return Result.ok();
    }
}
