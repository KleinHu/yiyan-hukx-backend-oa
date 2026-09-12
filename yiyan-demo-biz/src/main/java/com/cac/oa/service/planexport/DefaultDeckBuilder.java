package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptExportReqVO;
import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import org.apache.poi.sl.usermodel.TableCell;
import org.apache.poi.sl.usermodel.TextParagraph.TextAlign;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

/**
 * 默认样式导出：封面 -> 汇总 -> 明细自动翻页 -> 关注清单。
 * 只使用 POI 基础能力，不依赖任何模板。
 */
final class DefaultDeckBuilder {

    /** 明细表列定义：{表头, 列宽pt}，共 930pt，占页面 960pt 的 96.9%，留左右边距 */
    private static final String[] HEADERS =
        {"计划名称", "分类", "节点", "科室责任人", "计划负责人", "等级", "开始日期", "结束日期", "状态", "进度"};
    private static final double[] COL_W = {190, 105, 70, 68, 68, 42, 80, 80, 64, 43};
    private static final double TABLE_X = 15;
    private static final double ROW_H = 26;

    private final XMLSlideShow ppt = new XMLSlideShow();
    private final PlanPptExportReqVO req;
    private final PptStats stats;
    private final int rowsPerPage;

    private final List<PlanPptRowVO> plans;

    DefaultDeckBuilder(PlanPptExportReqVO req, List<PlanPptRowVO> plans) {
        this.req = req;
        this.plans = plans;
        this.stats = new PptStats(plans);
        this.rowsPerPage = req.getRowsPerPage() == null || req.getRowsPerPage() < 1
            ? 12 : Math.min(req.getRowsPerPage(), 20);
        ppt.setPageSize(new java.awt.Dimension((int) PptKit.PAGE_W, (int) PptKit.PAGE_H));
    }

    XMLSlideShow build() {
        cover();
        summary();
        details();
        focus();
        return ppt;
    }

    // ------------------------------------------------ 封面
    private void cover() {
        XSLFSlide s = ppt.createSlide();
        PptKit.fillRect(s, 0, 0, PptKit.PAGE_W, PptKit.PAGE_H, PptKit.INK);
        PptKit.fillRect(s, 0, 0, 8, PptKit.PAGE_H, PptKit.ACCENT);
        PptKit.text(s, 60, 150, 840, 40, new String[] {"PLAN REVIEW · 计划管控"},
            12, true, new Color(0x7D, 0xA7, 0xFF), TextAlign.LEFT);
        PptKit.text(s, 60, 190, 840, 80, new String[] {PptKit.safe(req.getTitle())},
            44, true, PptKit.WHITE, TextAlign.LEFT);
        PptKit.text(s, 60, 280, 840, 40, new String[] {PptKit.safe(req.getSubtitle())},
            18, false, new Color(0xB6, 0xC2, 0xD4), TextAlign.LEFT);
        PptKit.fillRect(s, 60, 330, 840, 1, new Color(0x2C, 0x3E, 0x5C));
        kpi(s, 60, 350, String.valueOf(stats.total), "计划总数");
        kpi(s, 300, 350, String.valueOf(stats.doneCount), "已完成");
        kpi(s, 540, 350, String.valueOf(stats.overdueCount), "已逾期");
        kpi(s, 780, 350, PptStats.fmt1(stats.avgProgress) + "%", "平均进度");
        PptKit.text(s, 60, 470, 840, 24,
            new String[] {PptKit.safe(req.getOrgName()) + " · " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())},
            11, false, new Color(0x64, 0x74, 0x8E), TextAlign.LEFT);
    }

    private void kpi(XSLFSlide s, double x, double y, String val, String label) {
        PptKit.text(s, x, y, 200, 52, new String[] {val}, 30, true, PptKit.WHITE, TextAlign.LEFT);
        PptKit.text(s, x, y + 50, 200, 24, new String[] {label}, 12, false,
            new Color(0x8E, 0x9D, 0xB5), TextAlign.LEFT);
    }

    // ------------------------------------------------ 汇总
    private void summary() {
        XSLFSlide s = ppt.createSlide();
        PptKit.fillRect(s, 0, 0, PptKit.PAGE_W, PptKit.PAGE_H, PptKit.PAPER);
        header(s, "计划状态与分类汇总");

        // 左：状态分布表
        PptKit.text(s, TABLE_X, 84, 300, 26, new String[] {"状态分布"}, 15, true, PptKit.INK, TextAlign.LEFT);
        int sn = stats.statusCount.size();
        XSLFTable st = table(s, TABLE_X, 118, sn + 1, 3, new double[] {140, 70, 70});
        setRow(st, 0, new String[] {"状态", "数量", "占比"}, true, PptKit.WHITE);
        int ri = 1;
        for (Map.Entry<String, Integer> e : stats.statusCount.entrySet()) {
            setRow(st, ri, new String[] {e.getKey(), String.valueOf(e.getValue()),
                PptStats.fmt1(100d * e.getValue() / stats.total) + "%"}, false, PptKit.statusColor(e.getKey()));
            ri++;
        }

        // 中：分类平均进度表
        double midX = TABLE_X + 300;
        PptKit.text(s, midX, 84, 300, 26, new String[] {"分类与平均进度"}, 15, true, PptKit.INK, TextAlign.LEFT);
        int cn = stats.categoryCount.size();
        XSLFTable ct = table(s, midX, 118, cn + 1, 3, new double[] {150, 70, 80});
        setRow(ct, 0, new String[] {"分类", "数量", "平均进度"}, true, PptKit.WHITE);
        ri = 1;
        for (Map.Entry<String, Integer> e : stats.categoryCount.entrySet()) {
            setRow(ct, ri, new String[] {e.getKey(), String.valueOf(e.getValue()),
                PptStats.fmt1(stats.categoryAvgProgress(e.getKey())) + "%"}, false, PptKit.TEXT);
            ri++;
        }

        // 右：可选图表（前端 ECharts 截图）
        double chartX = midX + 300;
        PptKit.text(s, chartX, 84, 330, 26, new String[] {"图表"}, 15, true, PptKit.INK, TextAlign.LEFT);
        boolean hasChart = false;
        if (req.getChartImages() != null && !req.getChartImages().isEmpty()) {
            double cy = 118;
            for (String b64 : req.getChartImages().values()) {
                if (PptKit.picture(ppt, s, b64, chartX, cy, 330, 180) != null) {
                    cy += 195;
                    hasChart = true;
                }
                if (cy > 480) {
                    break;
                }
            }
        }
        if (!hasChart) {
            PptKit.card(s, chartX, 118, 330, 300, PptKit.WHITE, new Color(0xE2, 0xE6, 0xEB));
            PptKit.text(s, chartX + 20, 200, 290, 80,
                new String[] {"未提供图表图片。", "前端可在导出请求中携带 chartImages（ECharts 截图 dataURL），", "将自动放置于此。"},
                12, false, PptKit.MUTED, TextAlign.LEFT);
        }
        footer(s, 0, 1);
    }

    // ------------------------------------------------ 明细（自动翻页）
    private void details() {
        int pages = (plans.size() + rowsPerPage - 1) / rowsPerPage;
        for (int page = 0; page < pages; page++) {
            XSLFSlide s = ppt.createSlide();
            PptKit.fillRect(s, 0, 0, PptKit.PAGE_W, PptKit.PAGE_H, PptKit.PAPER);
            header(s, "计划明细");
            int from = page * rowsPerPage;
            int to = Math.min(from + rowsPerPage, plans.size());
            XSLFTable t = table(s, TABLE_X, 74, 1 + (to - from), HEADERS.length, COL_W);
            setRow(t, 0, HEADERS, true, PptKit.WHITE);
            for (int r = from; r < to; r++) {
                PlanPptRowVO p = plans.get(r);
                String[] row = new String[] {
                    PptKit.safe(p.getName()), PptKit.safe(p.getCategory()), PptKit.safe(p.getNode()),
                    PptKit.safe(p.getDeptManager()), PptKit.safe(p.getOwner()), PptKit.safe(p.getLevel()),
                    PptKit.safe(p.getStartDate()), PptKit.safe(p.getEndDate()),
                    PptKit.safe(p.getStatus()),
                    p.getProgress() == null ? "-" : p.getProgress() + "%",
                };
                setRow(t, 1 + r - from, row, false, PptKit.TEXT);
                // 状态列着色 + 名称列加粗
                XSLFTableRow dataRow = t.getRows().get(1 + r - from);
                styleCell(dataRow.getCells().get(0), true, PptKit.TEXT);
                styleCell(dataRow.getCells().get(8), true, PptKit.statusColor(PptKit.safe(p.getStatus())));
            }
            footer(s, page + 1, pages);
        }
    }

    // ------------------------------------------------ 关注清单
    private void focus() {
        if (stats.focusRows.isEmpty()) {
            return;
        }
        XSLFSlide s = ppt.createSlide();
        PptKit.fillRect(s, 0, 0, PptKit.PAGE_W, PptKit.PAGE_H, PptKit.PAPER);
        header(s, "重点关注计划");
        List<PlanPptRowVO> rows = stats.focusRows;
        XSLFTable t = table(s, TABLE_X, 74, rows.size() + 1, 6,
            new double[] {250, 120, 70, 90, 100, 300});
        setRow(t, 0, new String[] {"计划名称", "状态", "进度", "截止日期", "负责人", "备注"}, true, PptKit.WHITE);
        for (int i = 0; i < rows.size(); i++) {
            PlanPptRowVO p = rows.get(i);
            setRow(t, i + 1, new String[] {
                PptKit.safe(p.getName()), PptKit.safe(p.getStatus()),
                p.getProgress() == null ? "-" : p.getProgress() + "%",
                PptKit.safe(p.getEndDate()), PptKit.safe(p.getOwner()), PptKit.safe(p.getRemark())},
                false, PptKit.TEXT);
            XSLFTableCell statusCell = t.getRows().get(i + 1).getCells().get(1);
            styleCell(statusCell, true, PptKit.statusColor(PptKit.safe(p.getStatus())));
        }
        footer(s, 0, 1);
    }

    // ------------------------------------------------ 页眉页脚
    private void header(XSLFSlide s, String title) {
        PptKit.text(s, 15, 22, 600, 34, new String[] {PptKit.safe(req.getTitle())},
            20, true, PptKit.INK, TextAlign.LEFT);
        PptKit.text(s, 610, 30, 330, 20, new String[] {title}, 12, false, PptKit.MUTED, TextAlign.RIGHT);
        PptKit.fillRect(s, 15, 58, 60, 3, PptKit.ACCENT);
    }

    private void footer(XSLFSlide s, int page, int pages) {
        String right = page == 0 ? "" : "第 " + page + " / " + pages + " 页";
        PptKit.text(s, 15, 524, 600, 14, new String[] {PptKit.safe(req.getOrgName())}, 9, false, PptKit.MUTED, TextAlign.LEFT);
        PptKit.text(s, 610, 524, 330, 14, new String[] {right}, 9, false, PptKit.MUTED, TextAlign.RIGHT);
    }

    // ------------------------------------------------ 表格构造
    private XSLFTable table(XSLFSlide s, double x, double y, int rows, int cols, double[] colW) {
        XSLFTable t = s.createTable(rows, cols);
        double sumW = 0;
        for (int i = 0; i < cols && i < colW.length; i++) {
            t.setColumnWidth(i, colW[i]);
            sumW += colW[i];
        }
        // 最后落锚点：列宽会重算 frame extent，先设会被覆盖
        t.setAnchor(new java.awt.geom.Rectangle2D.Double(x, y, sumW, rows * ROW_H));
        return t;
    }

    private void setRow(XSLFTable t, int rowIdx, String[] values, boolean header, Color fg) {
        XSLFTableRow row = t.getRows().get(rowIdx);
        row.setHeight(ROW_H);
        List<XSLFTableCell> cells = row.getCells();
        for (int c = 0; c < cells.size() && c < values.length; c++) {
            XSLFTableCell cell = cells.get(c);
            cell.setFillColor(header ? PptKit.INK : (rowIdx % 2 == 0 ? PptKit.WHITE : PptKit.BAND));
            PptKit.cellText(cell, values[c], 10, header, header ? PptKit.WHITE : fg);
            PptKit.thinBorder(cell);
        }
    }

    private void styleCell(XSLFTableCell cell, boolean bold, Color color) {
        for (XSLFTextParagraph p : cell.getTextParagraphs()) {
            for (XSLFTextRun r : p.getTextRuns()) {
                r.setBold(bold);
                r.setFontColor(color);
            }
        }
    }
}
