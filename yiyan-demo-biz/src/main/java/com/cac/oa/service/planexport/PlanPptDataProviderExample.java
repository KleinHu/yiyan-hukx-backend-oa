package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptQueryVO;
import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link PlanPptDataProvider} 参考实现（路径 B）。
 *
 * <p><b>当前故意不生效</b>：类上没有 @Component，Spring 不会注册它，
 * 因此导出接口默认仍走"前端传 plans"（路径 A）。等计划的表/Service 就绪后：
 * <ol>
 *   <li>解开下面的 @Component 注解；</li>
 *   <li>把 listForExport 里的 TODO 换成本模块 Service 查询（不分页）；</li>
 *   <li>前端改为只传 query 即可，Controller 与生成引擎无需改动。</li>
 * </ol>
 *
 * <p>查询要点：
 * <ul>
 *   <li><b>不要分页</b>——导出要的是筛选全量；</li>
 *   <li>区间按 <b>startDate</b> 卡：{@code startDate >= startDateFrom and startDate <= startDateTo}；</li>
 *   <li>状态等枚举翻译成展示态中文后再放进 VO（生成引擎不做字典翻译）；</li>
 *   <li>达梦下注意空集合条件（不要生成 {@code IN ()}），与既有模块保持一致。</li>
 * </ul>
 */
// @Component
public class PlanPptDataProviderExample implements PlanPptDataProvider {

    @Override
    public List<PlanPptRowVO> listForExport(PlanPptQueryVO query) {
        // TODO 替换为真实的计划查询（示例仅说明映射关系）：
        //
        // LambdaQueryWrapper<OfficePlanEntity> w = new LambdaQueryWrapper<>();
        // w.like(StringUtils.hasText(query.getPlanName()), OfficePlanEntity::getPlanName, query.getPlanName());
        // w.eq(StringUtils.hasText(query.getCategory()), OfficePlanEntity::getPrimaryCategory, query.getCategory());
        // w.eq(StringUtils.hasText(query.getStatus()), OfficePlanEntity::getStatus, statusCodeOf(query.getStatus()));
        // // 时间区间按开始时间卡
        // w.ge(StringUtils.hasText(query.getStartDateFrom()), OfficePlanEntity::getStartDate, query.getStartDateFrom());
        // w.le(StringUtils.hasText(query.getStartDateTo()), OfficePlanEntity::getStartDate, query.getStartDateTo());
        // List<OfficePlanEntity> list = officePlanService.list(w);   // 注意：不要传分页对象
        // return list.stream().map(this::toRow).collect(Collectors.toList());
        throw new UnsupportedOperationException(
            "参考实现未启用：请接入真实计划 Service 后解开 @Component 并实现本方法");
    }
}
