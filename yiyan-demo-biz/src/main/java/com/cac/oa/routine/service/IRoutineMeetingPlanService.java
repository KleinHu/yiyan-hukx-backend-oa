package com.cac.oa.routine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cac.oa.routine.entity.RoutineMeetingPlanEntity;
import com.cac.oa.routine.vo.RoutineMeetingPlanCompletedDateRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanExtendRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanFeedbackDateRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanFeedbackRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanQuery;
import com.cac.oa.routine.vo.RoutineMeetingPlanSaveRequest;
import com.cac.oa.routine.vo.RoutineMeetingPlanVO;
import com.cac.yiyan.common.page.PageData;

public interface IRoutineMeetingPlanService extends IService<RoutineMeetingPlanEntity> {

    PageData<RoutineMeetingPlanVO> page(RoutineMeetingPlanQuery query);

    PageData<RoutineMeetingPlanVO> myPlans(RoutineMeetingPlanQuery query);

    PageData<RoutineMeetingPlanVO> myFeedback(RoutineMeetingPlanQuery query);

    RoutineMeetingPlanVO detail(Long id);

    Long savePlan(RoutineMeetingPlanSaveRequest request);

    void updatePlan(Long id, RoutineMeetingPlanSaveRequest request);

    void deletePlan(Long id);

    void extend(Long id, RoutineMeetingPlanExtendRequest request);

    void feedback(Long id, RoutineMeetingPlanFeedbackRequest request);

    void correctFeedbackDate(Long id, RoutineMeetingPlanFeedbackDateRequest request);

    void correctCompletedDate(Long id, RoutineMeetingPlanCompletedDateRequest request);
}
