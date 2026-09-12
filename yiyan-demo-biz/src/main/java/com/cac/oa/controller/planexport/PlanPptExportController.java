package com.cac.oa.controller.planexport;

import com.cac.oa.service.planexport.PlanPptExportService;
import com.cac.oa.vo.planexport.PlanPptExportReqVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 计划管理 - 导出 PPT。
 * <p>
 * 响应为 pptx 二进制流（非 Result 包装），前端以 responseType:'blob' 接收。
 * 集成到内网工程后可放开权限注解，例如：
 * &#64;PreAuthorize("@ss.hasPermission('oa:plan-ppt:export')")
 */
@Api(tags = "计划管理-导出PPT")
@RestController
@RequestMapping("/api/240/oa/plan-ppt")
public class PlanPptExportController {

    private static final String PPTX_MIME =
        "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private final PlanPptExportService planPptExportService;

    public PlanPptExportController(PlanPptExportService planPptExportService) {
        this.planPptExportService = planPptExportService;
    }

    @PostMapping("/export")
    @ApiOperation("计划数据导出为 PPT（default=内置样式多页；template=按模板填充）")
    public void export(@RequestBody PlanPptExportReqVO req, HttpServletResponse response) throws Exception {
        byte[] data = planPptExportService.export(req);

        String base = req.getTitle() == null || req.getTitle().trim().isEmpty()
            ? "计划汇报" : req.getTitle().trim();
        String name = base + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pptx";
        String encoded = URLEncoder.encode(name, "UTF-8").replace("+", "%20");

        response.reset();
        response.setContentType(PPTX_MIME);
        response.setContentLength(data.length);
        response.setHeader("Content-Disposition",
            "attachment; filename=\"plan-export.pptx\"; filename*=UTF-8''" + encoded);
        OutputStream os = response.getOutputStream();
        os.write(data);
        os.flush();
    }
}
