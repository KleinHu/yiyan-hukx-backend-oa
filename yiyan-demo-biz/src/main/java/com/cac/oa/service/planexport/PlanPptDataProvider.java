package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptQueryVO;
import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.util.List;

/**
 * 导出数据提供者（SPI）：路径 B 的服务端取数入口。
 * <p>
 * 计划表/Service 就绪后，实现本接口并注册为 Spring Bean（加 @Component），
 * 导出接口即可支持"前端只传 query 条件、服务端查全量"的模式，
 * 无需改动 Controller / 生成引擎。
 * <p>
 * 实现约定：
 * 1. 返回**当前筛选条件下的全量行**（不分页！这是分页问题的关键）；
 * 2. 时间区间按 startDate 过滤（与列表页口径一致）；
 * 3. 状态等枚举需翻译成展示态中文（"执行中"），生成引擎不做字典翻译；
 * 4. 返回 null 或空集合时，导出接口会明确报错而不是产出空 PPT。
 */
public interface PlanPptDataProvider {

    /**
     * 按条件查询导出所需的全部计划行。
     *
     * @param query 前端传来的筛选条件，字段可能为 null（表示不限制）
     * @return 全量计划行；不要分页
     */
    List<PlanPptRowVO> listForExport(PlanPptQueryVO query);
}
