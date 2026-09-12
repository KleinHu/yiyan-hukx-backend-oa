package com.cac.oa.vo.planexport;

import java.io.Serializable;

/**
 * 导出筛选条件（路径 B：前端只传条件，由服务端查询全量后组装）。
 * <p>
 * 与前端列表页筛选栏一一对应。时间区间固定按【开始时间 startDate】卡，
 * 即 startDateFrom &lt;= startDate &lt;= startDateTo（跨月计划只会出现在开始月）。
 * <p>
 * 说明：本类只承载条件，不承载数据。服务端需要有一个
 * {@link com.cac.oa.service.planexport.PlanPptDataProvider} 实现来按条件取数；
 * 未注册实现类时，请改用 request.plans 直接传行数据（路径 A）。
 */
public class PlanPptQueryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划名称，模糊匹配 */
    private String planName;
    /** 分类（一级分类） */
    private String category;
    /** 计划状态，展示态文案，如"执行中" */
    private String status;
    /** 开始时间下界（含），yyyy-MM-dd */
    private String startDateFrom;
    /** 开始时间上界（含），yyyy-MM-dd */
    private String startDateTo;
    /** 科室责任人 */
    private String deptManager;
    /** 计划负责人 */
    private String owner;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDateFrom() {
        return startDateFrom;
    }

    public void setStartDateFrom(String startDateFrom) {
        this.startDateFrom = startDateFrom;
    }

    public String getStartDateTo() {
        return startDateTo;
    }

    public void setStartDateTo(String startDateTo) {
        this.startDateTo = startDateTo;
    }

    public String getDeptManager() {
        return deptManager;
    }

    public void setDeptManager(String deptManager) {
        this.deptManager = deptManager;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
