package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptExportReqVO;
import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/**
 * 本地自测工具（不依赖 Spring/数据库/HTTP）。
 * <p>
 * 用法：
 *   javac + java 本包所有类后：
 *   java -cp "<poi等依赖>:classes" com.cac.oa.service.planexport.PlanPptLocalTool <输出目录> [模板.pptx]
 * <p>
 * 生成 sample-default.pptx（内置样式）与 sample-template.pptx（模板填充，需提供模板）。
 * 集成完成后可删除本类，或留着当回归测试入口。
 */
public final class PlanPptLocalTool {

    public static void main(String[] args) throws Exception {
        File outDir = new File(args.length > 0 ? args[0] : ".");
        outDir.mkdirs();
        PlanPptExportReqVO req = sampleReq();
        List<PlanPptRowVO> rows = samplePlans();

        XMLSlideShow defaultDeck = new DefaultDeckBuilder(req, rows).build();
        write(defaultDeck, new File(outDir, "sample-default.pptx"));
        System.out.println("OK sample-default.pptx 页数=" + defaultDeck.getSlides().size());

        if (args.length > 1 && new File(args[1]).isFile()) {
            byte[] tpl = readAll(new FileInputStream(new File(args[1])));
            String b64 = new String(Base64.getEncoder().encode(tpl), StandardCharsets.US_ASCII);
            PlanPptExportReqVO tplReq = sampleReq();
            tplReq.setMode("template");
            tplReq.setTemplateBase64(b64);
            Map<String, String> extra = new LinkedHashMap<String, String>();
            extra.put("reportor", "流化部 · 李工");
            tplReq.setExtra(extra);
            XMLSlideShow filled = new TemplateFiller(tplReq, rows, tpl).fill();
            write(filled, new File(outDir, "sample-template.pptx"));
            System.out.println("OK sample-template.pptx 页数=" + filled.getSlides().size());
        }
    }

    static PlanPptExportReqVO sampleReq() {
        PlanPptExportReqVO req = new PlanPptExportReqVO();
        req.setTitle("科室计划管理汇报");
        req.setSubtitle("2026 年度 · 计划执行与风险分析");
        req.setOrgName("数字化办公平台 · 计划管控");
        req.setRowsPerPage(12);
        // 自测工具直接给数据（正式流程里由 Service 按 query 查询后传给引擎）
        req.setQuery(new com.cac.oa.vo.planexport.PlanPptQueryVO());
        return req;
    }

    public static List<PlanPptRowVO> samplePlans() {
        List<PlanPptRowVO> list = new ArrayList<PlanPptRowVO>();
        String[] nodes = {"需求分析", "方案设计", "开发实施", "测试验证", "上线部署", "运维监控"};
        String[] cats = {"质量管理", "安全管理", "业务发展", "人员管理"};
        String[] persons = {"张三", "李四", "王五", "赵六", "陈静", "刘洋"};
        String[] status = {"草稿", "待审核", "已审批", "执行中", "已完成", "执行中", "已取消", "执行中"};
        String[] levels = {"重要", "一般", "一般"};
        for (int i = 1; i <= 37; i++) {
            PlanPptRowVO p = new PlanPptRowVO();
            p.setCode("科室-2026-" + String.format("%03d", i));
            p.setName(cats[i % cats.length] + "专项" + i + "号计划");
            p.setCategory(cats[i % cats.length]);
            p.setNode(nodes[i % nodes.length]);
            p.setDeptManager(persons[i % persons.length]);
            p.setOwner(persons[(i + 2) % persons.length]);
            p.setLevel(levels[i % levels.length]);
            int sm = 1 + i % 6; // 1-6 月
            p.setStartDate(String.format("2026-%02d-%02d", sm, 1 + i % 27));
            p.setEndDate(String.format("2026-%02d-%02d", sm + 1 + i % 3, 1 + i % 27));
            p.setStatus(status[i % status.length]);
            p.setProgress(i % 4 == 0 ? 100 : (i * 37) % 95);
            p.setRemark(i % 5 == 0 ? "资源到位，按计划推进" : "");
            list.add(p);
        }
        return list;
    }

    private static void write(XMLSlideShow ppt, File file) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            ppt.write(fos);
        } finally {
            fos.close();
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        in.close();
        return bos.toByteArray();
    }
}
