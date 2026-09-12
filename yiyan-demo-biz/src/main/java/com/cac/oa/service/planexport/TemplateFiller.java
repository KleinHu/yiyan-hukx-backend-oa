package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptExportReqVO;
import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTable;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTableRow;

/**
 * 模板填充引擎（mode=template）。
 * <p>
 * 模板规范（详见 README）：
 * 1. 全局占位符：文字中写 {{title}} {{subtitle}} {{date}} {{planCount}} {{doneCount}}
 *    {{inProgressCount}} {{overdueCount}} {{avgProgress}} {{orgName}}，以及请求 extra 里的自定义 key；
 * 2. 明细表：模板表格中放一行"标记行"，任意单元格含 &lt;&lt;ROW&gt;&gt;，其余单元格用
 *    &lt;&lt;字段&gt;&gt;（code/name/category/node/deptManager/owner/level/startDate/endDate/status/progress/remark）；
 * 3. 含标记行的那一页作为"明细母版页"：引擎按 rowsPerPage 分片克隆整页（importContent），
 *    每页可用 {{page}} {{pages}}；母版页本身会被移除；
 * 4. 模板中明细母版页请放在文件末尾（克隆页会追加在末尾），母版页上的表格必须是 PowerPoint 原生表格。
 */
final class TemplateFiller {

    private static final String TAG = "{{";
    private static final String TAG_END = "}}";
    private static final String ROW_MARK = "<<ROW>>";

    private final XMLSlideShow ppt;
    private final PlanPptExportReqVO req;
    private final PptStats stats;
    private final int rowsPerPage;

    private final List<PlanPptRowVO> plans;

    TemplateFiller(PlanPptExportReqVO req, List<PlanPptRowVO> plans, byte[] templateBytes) throws Exception {
        this.req = req;
        this.plans = plans;
        this.ppt = new XMLSlideShow(new ByteArrayInputStream(templateBytes));
        this.stats = new PptStats(plans);
        this.rowsPerPage = req.getRowsPerPage() == null || req.getRowsPerPage() < 1
            ? 12 : Math.min(req.getRowsPerPage(), 20);
    }

    XMLSlideShow fill() throws Exception {
        Map<String, String> globals = stats.globalTags(req.getTitle(), req.getSubtitle(), req.getOrgName());
        if (req.getExtra() != null) {
            globals.putAll(req.getExtra());
        }

        // 1) 找明细母版页（含 <<ROW>> 标记行的页）
        List<XSLFSlide> detailSlides = new ArrayList<XSLFSlide>();
        for (XSLFSlide slide : ppt.getSlides()) {
            if (containsRowMark(slide)) {
                detailSlides.add(slide);
            }
        }

        // 2) 非明细页：只做全局替换
        for (XSLFSlide slide : ppt.getSlides()) {
            if (!detailSlides.contains(slide)) {
                replaceSlide(slide, globals);
            }
        }

        // 3) 明细页：先从"未填充的母版页"克隆出所需份数，再逐页填充/扩行
        int pages = (plans.size() + rowsPerPage - 1) / rowsPerPage;
        for (int d = 0; d < detailSlides.size(); d++) {
            XSLFSlide master = detailSlides.get(d);
            // 每张明细母版页均摊承载的分片数
            int share = (int) Math.ceil((double) pages / detailSlides.size());
            share = Math.max(share, plans.isEmpty() ? 0 : 1);
            List<XSLFSlide> targets = new ArrayList<XSLFSlide>();
            targets.add(master);
            for (int k = 1; k < share; k++) {
                targets.add(cloneSlide(master));
            }
            for (int k = 0; k < targets.size(); k++) {
                int pageNo = d * share + k;
                if (pageNo >= pages && k > 0) {
                    continue;
                }
                int from = pageNo * rowsPerPage;
                int to = Math.min(from + rowsPerPage, plans.size());
                XSLFSlide target = targets.get(k);
                Map<String, String> ctx = new LinkedHashMap<String, String>(globals);
                ctx.put("page", String.valueOf(pageNo + 1));
                ctx.put("pages", String.valueOf(pages));
                replaceSlide(target, ctx);
                expandTables(target, plans.subList(from, to));
            }
        }

        // 4) 母版页承载第 1 片（保留）；模板原明细页如果完全没数据才移除
        if (plans.isEmpty()) {
            for (XSLFSlide detail : detailSlides) {
                int idx = ppt.getSlides().indexOf(detail);
                if (idx >= 0) {
                    ppt.removeSlide(idx);
                }
            }
        }
        return ppt;
    }

    // ------------------------------------------------ 标记检测与替换

    private boolean containsRowMark(XSLFSlide slide) {
        for (XSLFTable table : tablesOf(slide)) {
            for (org.apache.poi.xslf.usermodel.XSLFTableRow row : table.getRows()) {
                for (org.apache.poi.xslf.usermodel.XSLFTableCell cell : row.getCells()) {
                    if (cell.getText().contains(ROW_MARK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<XSLFTable> tablesOf(XSLFSlide slide) {
        List<XSLFTable> out = new ArrayList<XSLFTable>();
        PptKit.collectTables(slide.getShapes(), out);
        return out;
    }

    /** 对单页所有文本形状 + 所有表格单元格做 {{key}} 替换（合并被拆散的 run） */
    private void replaceSlide(XSLFSlide slide, Map<String, String> ctx) {
        List<XSLFTextShape> shapes = new ArrayList<XSLFTextShape>();
        PptKit.collectTextShapes(slide.getShapes(), shapes);
        for (XSLFTable table : tablesOf(slide)) {
            for (org.apache.poi.xslf.usermodel.XSLFTableRow row : table.getRows()) {
                shapes.addAll(row.getCells());
            }
        }
        for (XSLFTextShape shape : shapes) {
            replaceInTextShape(shape, ctx);
        }
    }

    /**
     * 表格标记行扩行：删除含 &lt;&lt;ROW&gt;&gt; 的行，按其样式模板在原地逐条插入数据行。
     */
    private void expandTables(XSLFSlide slide, List<PlanPptRowVO> slice) {
        for (XSLFTable table : tablesOf(slide)) {
            CTTable ct = table.getCTTable();
            int markIdx = -1;
            for (int i = 0; i < table.getRows().size(); i++) {
                for (org.apache.poi.xslf.usermodel.XSLFTableCell cell : table.getRows().get(i).getCells()) {
                    if (cell.getText().contains(ROW_MARK)) {
                        markIdx = i;
                        break;
                    }
                }
                if (markIdx >= 0) {
                    break;
                }
            }
            if (markIdx < 0 || markIdx >= ct.sizeOfTrArray()) {
                continue;
            }
            CTTableRow proto = (CTTableRow) ct.getTrArray(markIdx).copy();
            ct.removeTr(markIdx);
            for (int k = 0; k < slice.size(); k++) {
                CTTableRow tr = ct.insertNewTr(markIdx + k);
                tr.set(proto);
                replaceRowTokens(tr, rowToken(slice.get(k)));
            }
            // 扩行后同步表格 frame 高度：否则渲染器按旧 extent 裁掉溢出行（LibreOffice/PowerPoint 行为不一）
            double totalHpt = 0;
            for (int i = 0; i < ct.sizeOfTrArray(); i++) {
                Object h = ct.getTrArray(i).getH(); // a:tr 的 h 属性（EMU），XmlBeans 返回 Number
                long emuH = h instanceof Number ? ((Number) h).longValue() : 0;
                totalHpt += (emuH <= 0 ? 320040 : emuH) / 12700d; // 无声明行高时按 25.2pt 估
            }
            java.awt.geom.Rectangle2D anchor = table.getAnchor();
            table.setAnchor(new java.awt.geom.Rectangle2D.Double(
                anchor.getX(), anchor.getY(), anchor.getWidth(), totalHpt));
        }
    }

    private void replaceRowTokens(CTTableRow tr, Map<String, String> rowCtx) {
        for (org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell tc : tr.getTcList()) {
            org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody body = tc.getTxBody();
            if (body == null) {
                continue;
            }
            for (org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph p : body.getPList()) {
                for (org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun run : p.getRList()) {
                    String t = run.getT();
                    String nt = applyTokens(t, rowCtx);
                    if (!nt.equals(t)) {
                        run.setT(nt);
                    }
                }
            }
        }
    }

    private Map<String, String> rowToken(PlanPptRowVO p) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("ROW", "");
        m.put("code", PptKit.safe(p.getCode()));
        m.put("name", PptKit.safe(p.getName()));
        m.put("category", PptKit.safe(p.getCategory()));
        m.put("node", PptKit.safe(p.getNode()));
        m.put("deptManager", PptKit.safe(p.getDeptManager()));
        m.put("owner", PptKit.safe(p.getOwner()));
        m.put("level", PptKit.safe(p.getLevel()));
        m.put("startDate", PptKit.safe(p.getStartDate()));
        m.put("endDate", PptKit.safe(p.getEndDate()));
        m.put("status", PptKit.safe(p.getStatus()));
        m.put("progress", p.getProgress() == null ? "-" : p.getProgress() + "%");
        m.put("remark", PptKit.safe(p.getRemark()));
        return m;
    }

    // ------------------------------------------------ run 级文本替换

    private void replaceInTextShape(XSLFTextShape shape, Map<String, String> ctx) {
        for (org.apache.poi.xslf.usermodel.XSLFTextParagraph p : shape.getTextParagraphs()) {
            List<org.apache.poi.xslf.usermodel.XSLFTextRun> runs = p.getTextRuns();
            if (runs.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (org.apache.poi.xslf.usermodel.XSLFTextRun r : runs) {
                sb.append(r.getRawText());
            }
            String merged = sb.toString();
            if (!merged.contains(TAG) && !merged.contains("<<")) {
                continue;
            }
            String replaced = applyTokens(merged, ctx);
            if (replaced.equals(merged)) {
                continue;
            }
            runs.get(0).setText(replaced);
            for (int i = 1; i < runs.size(); i++) {
                runs.get(i).setText("");
            }
        }
    }

    private String applyTokens(String text, Map<String, String> ctx) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String out = text;
        for (Map.Entry<String, String> e : ctx.entrySet()) {
            out = out.replace(TAG + e.getKey() + TAG_END, e.getValue());
            out = out.replace("<<" + e.getKey() + ">>", e.getValue());
        }
        return out;
    }

    // ------------------------------------------------ 页克隆

    private XSLFSlide cloneSlide(XSLFSlide source) throws Exception {
        org.apache.poi.xslf.usermodel.XSLFSlideLayout layout = source.getSlideLayout();
        XSLFSlide dup = layout != null ? ppt.createSlide(layout) : ppt.createSlide();
        dup.importContent(source);
        return dup;
    }
}
