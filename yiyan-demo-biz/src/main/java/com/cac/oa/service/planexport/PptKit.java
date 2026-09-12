package com.cac.oa.service.planexport;

import java.awt.Color;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.common.usermodel.fonts.FontGroup;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TableCell;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

/**
 * PPT 绘制工具（纯 POI 静态方法集合）。单位说明：XSLF 使用"点"（1/72 英寸），
 * 16:9 页面 = 960 x 540 pt。
 */
final class PptKit {

    static final double PAGE_W = 960d;
    static final double PAGE_H = 540d;

    static final Color INK = new Color(0x0F, 0x1B, 0x2D);
    static final Color PAPER = new Color(0xF7, 0xF8, 0xFA);
    static final Color WHITE = Color.WHITE;
    static final Color TEXT = new Color(0x1F, 0x2A, 0x37);
    static final Color MUTED = new Color(0x6B, 0x72, 0x80);
    static final Color ACCENT = new Color(0x25, 0x63, 0xEB);
    static final Color BAND = new Color(0xF1, 0xF4, 0xF8);

    static final String FONT_CN = "微软雅黑";
    static final String FONT_EN = "Segoe UI";

    /** 状态色（按状态文案关键字匹配，未匹配返回墨色） */
    static final Map<String, Color> STATUS_COLORS = new LinkedHashMap<String, Color>();
    static {
        STATUS_COLORS.put("已完成", new Color(0x16, 0xA3, 0x4A));
        STATUS_COLORS.put("执行中", ACCENT);
        STATUS_COLORS.put("进行中", ACCENT);
        STATUS_COLORS.put("已审批", ACCENT);
        STATUS_COLORS.put("待办结", new Color(0x0E, 0xA5, 0xE9));
        STATUS_COLORS.put("待审核", new Color(0xF5, 0x9E, 0x0B));
        STATUS_COLORS.put("已暂停", new Color(0xF5, 0x9E, 0x0B));
        STATUS_COLORS.put("已驳回", new Color(0xEF, 0x44, 0x44));
        STATUS_COLORS.put("已取消", new Color(0x94, 0xA3, 0xB8));
        STATUS_COLORS.put("草稿", new Color(0x94, 0xA3, 0xB8));
    }

    private PptKit() {
    }

    static Color statusColor(String status) {
        if (status == null) {
            return TEXT;
        }
        for (Map.Entry<String, Color> e : STATUS_COLORS.entrySet()) {
            if (status.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return TEXT;
    }

    /** 全页底色矩形（无边框：描边色与填充色一致） */
    static XSLFAutoShape fillRect(XSLFSlide slide, double x, double y, double w, double h, Color color) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.RECT);
        shape.setAnchor(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        shape.setFillColor(color);
        shape.setLineColor(color);
        return shape;
    }

    /** 圆角卡片 */
    static XSLFAutoShape card(XSLFSlide slide, double x, double y, double w, double h, Color fill, Color line) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.ROUND_RECT);
        shape.setAnchor(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        shape.setFillColor(fill);
        shape.setLineColor(line);
        shape.setLineWidth(0.75d);
        return shape;
    }

    /** 文本框；lines 为多个段落 */
    static XSLFTextBox text(XSLFSlide slide, double x, double y, double w, double h,
                            String[] lines, double size, boolean bold, Color color,
                            TextParagraph.TextAlign align) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
        box.setWordWrap(true);
        // createTextBox 自带一个空段落，pptxjs 渲染器会把空段落显示成 [object Object]，先清掉
        box.clearText();
        for (int i = 0; i < lines.length; i++) {
            XSLFTextParagraph p = box.addNewTextParagraph();
            p.setTextAlign(align);
            p.setSpaceBefore(0d);
            p.setSpaceAfter(size * 0.25d);
            XSLFTextRun r = p.addNewTextRun();
            r.setText(lines[i]);
            styleRun(r, size, bold, color);
        }
        return box;
    }

    static void styleRun(XSLFTextRun r, double size, boolean bold, Color color) {
        r.setFontSize(Double.valueOf(size));
        r.setBold(bold);
        r.setFontColor(color);
        r.setFontFamily(FONT_EN);
        r.setFontFamily(FONT_CN, FontGroup.EAST_ASIAN);
    }

    /** 单行单元格文本（清掉默认空段，避免首行空段撑高） */
    static void cellText(XSLFTableCell cell, String s, double size, boolean bold, Color color) {
        cell.clearText();
        XSLFTextParagraph p = cell.addNewTextParagraph();
        p.setTextAlign(TextParagraph.TextAlign.LEFT);
        p.setSpaceBefore(0d);
        p.setSpaceAfter(0d);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(s == null ? "" : s);
        styleRun(r, size, bold, color);
    }

    /** 表格所有单元格加浅色边框 */
    static void thinBorder(XSLFTableCell cell) {
        TableCell.BorderEdge[] edges = TableCell.BorderEdge.values();
        for (TableCell.BorderEdge edge : edges) {
            cell.setBorderColor(edge, new Color(0xD9, 0xDF, 0xE7));
            cell.setBorderWidth(edge, 0.5d);
        }
    }

    /** 嵌入 PNG（自动去掉 dataURL 前缀）；失败返回 null 不中断导出 */
    static XSLFPictureShape picture(XMLSlideShow ppt, XSLFSlide slide, String base64,
                                    double x, double y, double w, double h) {
        try {
            String pure = base64;
            int idx = pure.indexOf("base64,");
            if (idx >= 0) {
                pure = pure.substring(idx + 7);
            }
            byte[] bytes = Base64.getMimeDecoder().decode(pure);
            XSLFPictureData pd = ppt.addPicture(bytes, PictureData.PictureType.PNG);
            XSLFPictureShape pic = slide.createPicture(pd);
            pic.setAnchor(new java.awt.geom.Rectangle2D.Double(x, y, w, h));
            return pic;
        } catch (Exception e) {
            return null;
        }
    }

    /** 递归收集文本形状（含组合形状） */
    static void collectTextShapes(List<XSLFShape> shapes, List<XSLFTextShape> out) {
        for (XSLFShape s : shapes) {
            if (s instanceof XSLFTextShape) {
                out.add((XSLFTextShape) s);
            }
            if (s instanceof org.apache.poi.xslf.usermodel.XSLFGroupShape) {
                collectTextShapes(((org.apache.poi.xslf.usermodel.XSLFGroupShape) s).getShapes(), out);
            }
        }
    }

    /** 递归收集表格 */
    static void collectTables(List<XSLFShape> shapes, List<XSLFTable> out) {
        for (XSLFShape s : shapes) {
            if (s instanceof XSLFTable) {
                out.add((XSLFTable) s);
            }
            if (s instanceof org.apache.poi.xslf.usermodel.XSLFGroupShape) {
                collectTables(((org.apache.poi.xslf.usermodel.XSLFGroupShape) s).getShapes(), out);
            }
        }
    }

    static String safe(String s) {
        return s == null ? "" : s;
    }
}
