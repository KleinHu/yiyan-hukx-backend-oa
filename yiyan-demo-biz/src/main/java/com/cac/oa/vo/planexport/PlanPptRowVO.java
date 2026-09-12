package com.cac.oa.vo.planexport;

import java.io.Serializable;

/**
 * 导出 PPT 的单条计划行。
 * <p>
 * 所有字段为"展示态字符串/数字"：调用方（或后端查询层）负责把枚举值翻译成中文
 * （如 status 直接传"进行中"，level 传"重要"），导出引擎不参与业务字典翻译。
 */
public class PlanPptRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划编号，如 科室-2026-001 */
    private String code;
    /** 计划名称 */
    private String name;
    /** 计划分类（一级/二级合并后的展示文案） */
    private String category;
    /** 计划节点（需求分析/方案设计/...） */
    private String node;
    /** 科室责任人 */
    private String deptManager;
    /** 计划负责人 */
    private String owner;
    /** 计划等级（重要/一般） */
    private String level;
    /** 开始日期 yyyy-MM-dd */
    private String startDate;
    /** 结束日期 yyyy-MM-dd */
    private String endDate;
    /** 计划状态（草稿/待审核/已审批/执行中/已完成/待办结审批/已驳回/已取消） */
    private String status;
    /** 完成进度 0-100 */
    private Integer progress;
    /** 备注/延期原因等补充说明 */
    private String remark;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
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

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
