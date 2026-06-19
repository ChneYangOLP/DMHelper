package com.DMHelper.basic;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PDF 生成基础工具类，封装 PDFBox 3.0.1 常用操作。
 * 提供颜色常量、字体加载、绘制工具方法等。
 */
public class PDF_Util {

    private static final Logger LOGGER = Logger.getLogger(PDF_Util.class.getName());

    // ------------------------------------------------------------------ //
    //  颜色常量
    // ------------------------------------------------------------------ //

    public static final Color DARK_BG = new Color(30, 30, 40);
    public static final Color ACCENT_GREEN = new Color(46, 125, 50);
    public static final Color ACCENT_GOLD = new Color(183, 149, 11);
    public static final Color TEXT_WHITE = Color.WHITE;
    public static final Color TEXT_LIGHT_GRAY = new Color(200, 200, 200);
    public static final Color TEXT_DARK = new Color(30, 30, 30);
    public static final Color BORDER_COLOR = new Color(100, 100, 100);
    public static final Color TABLE_HEADER_BG = new Color(46, 125, 50);
    public static final Color TABLE_ROW_ALT = new Color(240, 240, 240);
    public static final Color HP_RED = new Color(198, 40, 40);
    public static final Color HP_GREEN = new Color(46, 125, 50);
    public static final Color HP_BLUE = new Color(30, 100, 200);
    public static final Color ACCENT_BLUE = new Color(30, 100, 200);

    // ------------------------------------------------------------------ //
    //  字体（延迟加载）
    // ------------------------------------------------------------------ //

    private static PDFont chineseFont;
    private static PDFont monoFont;
    private static boolean fontSearchAttempted = false;

    // 系统字体搜索路径
    private static final String[] FONT_SEARCH_PATHS = {
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
    };

    /**
     * 初始化中文字体。
     * 按优先级搜索系统字体，如果都不可用则回退到 Helvetica（中文会显示为方块，但不会崩溃）。
     *
     * @param doc PDFBox 文档对象
     * @return 可用的中文字体
     * @throws IOException 如果字体加载失败
     */
    public static PDFont get_chinese_font(PDDocument doc) throws IOException {
        if (chineseFont != null) {
            return chineseFont;
        }

        if (!fontSearchAttempted) {
            fontSearchAttempted = true;
            for (String path : FONT_SEARCH_PATHS) {
                File fontFile = new File(path);
                if (fontFile.exists() && fontFile.canRead()) {
                    try {
                        chineseFont = PDType0Font.load(doc, fontFile);
                        LOGGER.info("成功加载中文字体: " + path);
                        return chineseFont;
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "尝试加载字体失败: " + path, e);
                    }
                }
            }
            // 回退到内置字体
            LOGGER.warning("未找到中文字体文件，回退到 Helvetica（中文可能无法正常显示）");
            chineseFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }

        return chineseFont;
    }

    /**
     * 获取等宽字体（用于数值显示）。
     *
     * @param doc PDFBox 文档对象
     * @return 等宽字体
     * @throws IOException 如果字体加载失败
     */
    public static PDFont get_mono_font(PDDocument doc) throws IOException {
        if (monoFont != null) {
            return monoFont;
        }
        monoFont = new PDType1Font(Standard14Fonts.FontName.COURIER);
        return monoFont;
    }

    // ------------------------------------------------------------------ //
    //  绘制工具方法
    // ------------------------------------------------------------------ //

    /**
     * 绘制单行文本。
     *
     * @param cs    页面内容流
     * @param font  字体
     * @param size  字号
     * @param text  文本内容
     * @param x     X 坐标
     * @param y     Y 坐标（从底部算起）
     * @param color 文字颜色
     * @throws IOException 如果绘制失败
     */
    public static void draw_text(PDPageContentStream cs, PDFont font, float size,
                                  String text, float x, float y, Color color) throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    /**
     * 绘制自动换行的文本。
     *
     * @param cs         页面内容流
     * @param font       字体
     * @param size       字号
     * @param text       文本内容
     * @param x          起始 X 坐标
     * @param y          起始 Y 坐标（第一行基线）
     * @param maxWidth   最大行宽
     * @param lineHeight 行高
     * @param color      文字颜色
     * @return 实际使用的行数
     * @throws IOException 如果绘制失败
     */
    public static int draw_wrapped_text(PDPageContentStream cs, PDFont font, float size,
                                         String text, float x, float y, float maxWidth,
                                         float lineHeight, Color color) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int lineCount = 0;
        float currentY = y;

        // 逐字符拆分并按 maxWidth 断行
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = start + 1;
            while (end <= length) {
                String candidate = text.substring(start, end);
                float textWidth = font.getStringWidth(candidate) / 1000f * size;
                if (textWidth > maxWidth && end > start + 1) {
                    end--;
                    break;
                }
                end++;
            }
            // 如果 end 超过长度，截断到末尾
            if (end > length) {
                end = length;
            }

            String line = text.substring(start, end);
            draw_text(cs, font, size, line, x, currentY, color);
            currentY -= lineHeight;
            lineCount++;
            start = end;
        }

        return lineCount;
    }

    /**
     * 绘制一条直线。
     *
     * @param cs    页面内容流
     * @param x1    起点 X
     * @param y1    起点 Y
     * @param x2    终点 X
     * @param y2    终点 Y
     * @param width 线宽
     * @param color 线条颜色
     * @throws IOException 如果绘制失败
     */
    public static void draw_line(PDPageContentStream cs, float x1, float y1,
                                  float x2, float y2, float width, Color color) throws IOException {
        cs.setLineWidth(width);
        cs.setStrokingColor(color);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    /**
     * 绘制一个矩形（可填充、可描边）。
     *
     * @param cs          页面内容流
     * @param x           左下角 X
     * @param y           左下角 Y
     * @param width       矩形宽度
     * @param height      矩形高度
     * @param fillColor   填充颜色（null 则不填充）
     * @param strokeColor 描边颜色（null 则不描边）
     * @throws IOException 如果绘制失败
     */
    public static void draw_rect(PDPageContentStream cs, float x, float y,
                                  float width, float height, Color fillColor, Color strokeColor) throws IOException {
        if (fillColor != null) {
            cs.setNonStrokingColor(fillColor);
            cs.addRect(x, y, width, height);
            cs.fill();
        }
        if (strokeColor != null) {
            cs.setStrokingColor(strokeColor);
            cs.setLineWidth(1f);
            cs.addRect(x, y, width, height);
            cs.stroke();
        }
    }

    /**
     * 绘制一个圆角矩形。
     * 使用贝塞尔曲线近似圆角。
     *
     * @param cs          页面内容流
     * @param x           左下角 X
     * @param y           左下角 Y
     * @param width       矩形宽度
     * @param height      矩形高度
     * @param radius      圆角半径
     * @param fillColor   填充颜色（null 则不填充）
     * @param strokeColor 描边颜色（null 则不描边）
     * @throws IOException 如果绘制失败
     */
    public static void draw_rounded_rect(PDPageContentStream cs, float x, float y,
                                          float width, float height, float radius,
                                          Color fillColor, Color strokeColor) throws IOException {
        float safeRadius = Math.min(radius, Math.min(width / 2f, height / 2f));
        if (safeRadius < 0.5f) {
            // 半径太小，退化为普通矩形
            draw_rect(cs, x, y, width, height, fillColor, strokeColor);
            return;
        }

        // 构建圆角矩形路径
        cs.moveTo(x + safeRadius, y);
        cs.lineTo(x + width - safeRadius, y);
        // 右下角
        float cx = x + width - safeRadius;
        float cy = y + safeRadius;
        float kappa = 0.5522847498f; // 贝塞尔曲线近似系数
        cs.curveTo(cx + safeRadius * kappa, y, x + width, cy - safeRadius * kappa, x + width, cy);
        cs.lineTo(x + width, y + height - safeRadius);
        // 右上角
        cy = y + height - safeRadius;
        cx = x + width - safeRadius;
        cs.curveTo(x + width, cy + safeRadius * kappa, cx + safeRadius * kappa, y + height, cx, y + height);
        cs.lineTo(x + safeRadius, y + height);
        // 左上角
        cy = y + height - safeRadius;
        cx = x + safeRadius;
        cs.curveTo(cx - safeRadius * kappa, y + height, x, cy + safeRadius * kappa, x, cy);
        cs.lineTo(x, y + safeRadius);
        // 左下角
        cy = y + safeRadius;
        cx = x + safeRadius;
        cs.curveTo(x, cy - safeRadius * kappa, cx - safeRadius * kappa, y, cx, y);
        cs.closePath();

        if (fillColor != null) {
            cs.setNonStrokingColor(fillColor);
            cs.fill();
        }
        if (strokeColor != null) {
            cs.setStrokingColor(strokeColor);
            cs.setLineWidth(1f);
            cs.stroke();
        }
    }

    /**
     * 计算文本的显示宽度。
     *
     * @param cs    页面内容流
     * @param font  字体
     * @param size  字号
     * @param text  文本
     * @return 文本宽度（pt）
     * @throws IOException 如果计算失败
     */
    public static float get_text_width(PDPageContentStream cs, PDFont font, float size, String text) throws IOException {
        if (text == null || text.isEmpty()) {
            return 0f;
        }
        return font.getStringWidth(text) / 1000f * size;
    }

    // ------------------------------------------------------------------ //
    //  文档操作
    // ------------------------------------------------------------------ //

    /**
     * 新建一个 A4 页面并添加到文档中。
     *
     * @param doc PDFBox 文档对象
     * @return 新创建的 PDPage
     */
    public static PDPage add_new_page(PDDocument doc) {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        return page;
    }

    /**
     * 保存 PDF 文档到指定路径。
     *
     * @param doc      PDFBox 文档对象
     * @param filePath 保存路径
     * @throws IOException 如果保存失败
     */
    public static void save_document(PDDocument doc, String filePath) throws IOException {
        doc.save(filePath);
    }

    /**
     * 创建一个新的空白 PDF 文档。
     *
     * @return 新的 PDDocument 实例
     */
    public static PDDocument create_document() {
        return new PDDocument();
    }

    /**
     * 关闭文档（释放资源）。
     *
     * @param doc PDFBox 文档对象
     */
    public static void close_document(PDDocument doc) {
        if (doc != null) {
            try {
                doc.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "关闭 PDF 文档时出错", e);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  辅助常量
    // ------------------------------------------------------------------ //

    /** A4 纸张宽度（pt） */
    public static final float A4_WIDTH = PDRectangle.A4.getWidth();

    /** A4 纸张高度（pt） */
    public static final float A4_HEIGHT = PDRectangle.A4.getHeight();

    /** 页面边距 */
    public static final float MARGIN = 40f;

    /** 内容区域宽度 */
    public static final float CONTENT_WIDTH = A4_WIDTH - 2 * MARGIN;

    /** 内容区域起始 X */
    public static final float CONTENT_X = MARGIN;

    /** 内容区域起始 Y（底部） */
    public static final float CONTENT_Y = MARGIN;

    /** 内容区域顶部 Y */
    public static final float CONTENT_TOP = A4_HEIGHT - MARGIN;

    /**
     * 重置字体缓存（用于切换文档时）。
     */
    public static void reset_font_cache() {
        chineseFont = null;
        monoFont = null;
        fontSearchAttempted = false;
    }

    // ------------------------------------------------------------------ //
    //  统计块绘制（用于 Encounter_Log_PDF）
    // ------------------------------------------------------------------ //

    /**
     * 绘制一个统计信息块（标签 + 数值）。
     *
     * @param cs       页面内容流
     * @param font     标签字体
     * @param monoFont 数值字体
     * @param x        左下角 X
     * @param y        顶部 Y（标签基线）
     * @param width    区域宽度
     * @param label    标签文本
     * @param value    数值文本
     * @param valueColor 数值颜色
     * @throws IOException 如果绘制失败
     */
    public static void draw_stat_block(PDPageContentStream cs, PDFont font, PDFont monoFont,
                                        float x, float y, float width,
                                        String label, String value, Color valueColor) throws IOException {
        // 标签
        float labelWidth = font.getStringWidth(label) / 1000f * 7f;
        PDF_Util.draw_text(cs, font, 7f, label, x + (width - labelWidth) / 2f, y - 8f, TEXT_LIGHT_GRAY);

        // 数值
        float valueWidth = monoFont.getStringWidth(value) / 1000f * 16f;
        PDF_Util.draw_text(cs, monoFont, 16f, value, x + (width - valueWidth) / 2f, y - 30f, valueColor);
    }
}
