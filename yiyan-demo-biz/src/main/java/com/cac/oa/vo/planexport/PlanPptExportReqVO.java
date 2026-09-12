package com.cac.oa.vo.planexport;

import java.io.Serializable;
import java.util.Map;

/**
 * 计划导出 PPT 请求体。
 * <p>
 * <b>数据流设计</b>：前端只负责"确认导出条件"，把条件交给后端；
 * 后端按条件查询**完整列表**（不分页）并生成 PPT。
 * 因此本类<b>只承载条件与版式参数，不承载行数据</b>——
 * 前端不需要、也不应该把列表数据搬到导出接口里。
 * <p>
 * mode=default：内置样式（封面 + 汇总 + 明细自动翻页 + 关注清单）。
 * mode=template：按客户模板填充（占位符与标记行规范见 README）。
 */
public class PlanPptExportReqVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** default | template */
    private String mode = "default";
    /** 汇报标题 */
    private String title;
    /** 副标题（建议带筛选口径，便于 PPT 自解释） */
    private String subtitle;
    /** 落款单位 */
    private String orgName;
    /** 明细每页行数，默认 12，上限 20 */
    private Integer rowsPerPage;
    /** template 模式必填：模板 .pptx 文件内容的 Base64（可带 dataURL 前缀） */
    private String templateBase64;
    /** 模板额外占位符取值，key 不带花括号，如 {"reportor":"张三"} 对应模板 {{reportor}} */
    private Map<String, String> extra;
    /** 可选图表图片：name -> dataURL 或纯 Base64(PNG)。default 模式会放在汇总页 */
    private Map<String, String> chartImages;
    /**
     * 导出筛选条件（必填）。后端据此查询全量计划——这是本接口的数据入口。
     * 条件为空表示导出全部。
     */
    private PlanPptQueryVO query;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public Integer getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(Integer rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public String getTemplateBase64() {
        return templateBase64;
    }

    public void setTemplateBase64(String templateBase64) {
        this.templateBase64 = templateBase64;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }

    public Map<String, String> getChartImages() {
        return chartImages;
    }

    public void setChartImages(Map<String, String> chartImages) {
        this.chartImages = chartImages;
    }

    public PlanPptQueryVO getQuery() {
        return query;
    }

    public void setQuery(PlanPptQueryVO query) {
        this.query = query;
    }
}
