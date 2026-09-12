package com.cac.oa.service.planexport;

import com.cac.oa.vo.planexport.PlanPptExportReqVO;
import com.cac.oa.vo.planexport.PlanPptRowVO;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 计划导出 PPT 服务。
 * <p>
 * 数据流：前端提交筛选条件 → 本服务通过 {@link PlanPptDataProvider} 查询
 * <b>完整列表（不分页）</b> → 生成 PPT。
 * 前端不传行数据，因此列表页分页不影响导出内容。
 * <p>
 * 要接入真实数据，只需实现 {@link PlanPptDataProvider} 并注册为 Spring Bean；
 * Controller 与两个生成引擎都无需改动。
 */
@Service
public class PlanPptExportService {

    /** 单次导出上限，防止条件过宽导致内存/耗时失控 */
    private static final int MAX_ROWS = 2000;

    private final ObjectProvider<PlanPptDataProvider> dataProvider;

    public PlanPptExportService(ObjectProvider<PlanPptDataProvider> dataProvider) {
        this.dataProvider = dataProvider;
    }

    public byte[] export(PlanPptExportReqVO req) throws Exception {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        // 1) 按条件查询全量（不含分页）
        List<PlanPptRowVO> rows = queryRows(req);

        // 2) 校验（行数据显式传入引擎，不落回请求体）
        validate(rows, req);

        // 3) 出片
        XMLSlideShow ppt;
        if ("template".equalsIgnoreCase(req.getMode())) {
            byte[] tpl = decodeTemplate(req.getTemplateBase64());
            ppt = new TemplateFiller(req, rows, tpl).fill();
        } else {
            ppt = new DefaultDeckBuilder(req, rows).build();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 20);
        try {
            ppt.write(bos);
        } finally {
            ppt.close();
        }
        return bos.toByteArray();
    }

    private List<PlanPptRowVO> queryRows(PlanPptExportReqVO req) {
        PlanPptDataProvider provider = dataProvider.getIfAvailable();
        if (provider == null) {
            throw new IllegalStateException(
                "导出服务缺少计划数据源：请实现 PlanPptDataProvider 并注册为 Spring Bean（@Component）。"
                    + "参考实现见 PlanPptDataProviderExample，其中需按筛选条件查询完整列表、不要分页。");
        }
        List<PlanPptRowVO> rows = provider.listForExport(req.getQuery());
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("按当前筛选条件没有查询到计划，已取消导出");
        }
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException(
                "当前条件命中 " + rows.size() + " 条，超过单次导出上限 " + MAX_ROWS + " 条，请收窄筛选条件");
        }
        return rows;
    }

    private void validate(List<PlanPptRowVO> rows, PlanPptExportReqVO req) {
        if ("template".equalsIgnoreCase(req.getMode())
            && (req.getTemplateBase64() == null || req.getTemplateBase64().trim().isEmpty())) {
            throw new IllegalArgumentException("template 模式必须提供 templateBase64");
        }
        for (PlanPptRowVO row : rows) {
            if (row.getName() == null || row.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("存在计划名称为空的行");
            }
        }
    }

    private byte[] decodeTemplate(String base64) {
        String pure = base64;
        int idx = pure.indexOf("base64,");
        if (idx >= 0) {
            pure = pure.substring(idx + 7);
        }
        byte[] bytes = Base64.getMimeDecoder().decode(pure);
        if (bytes.length < 1024) {
            throw new IllegalArgumentException("模板文件过小或 Base64 解码失败");
        }
        return bytes;
    }
}
