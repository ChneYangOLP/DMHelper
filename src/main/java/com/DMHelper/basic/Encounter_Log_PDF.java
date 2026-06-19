package com.DMHelper.basic;

import com.DMHelper.basic.combat.Combat_Log_Entry;
import com.DMHelper.basic.combat.Combatant;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 遭遇日志 PDF 导出器。
 * 生成 D&D 5e 风格的遭遇战斗日志 PDF 文件，包含战斗摘要、时间线日志和参战者状态。
 */
public class Encounter_Log_PDF {

    // ------------------------------------------------------------------ //
    //  布局常量
    // ------------------------------------------------------------------ //

    private static final float HEADER_HEIGHT = 60f;
    private static final float SECTION_GAP = 10f;
    private static final float TITLE_SIZE = 20f;
    private static final float SUBTITLE_SIZE = 12f;
    private static final float BODY_SIZE = 9f;
    private static final float SMALL_SIZE = 8f;
    private static final float LABEL_SIZE = 7f;
    private static final float LINE_HEIGHT = 13f;
    private static final float SMALL_LINE_HEIGHT = 11f;
    private static final float TABLE_ROW_HEIGHT = 18f;
    private static final float TABLE_HEADER_HEIGHT = 20f;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 日志类型对应颜色
    private static final Color COLOR_ATTACK = new Color(198, 40, 40);
    private static final Color COLOR_HEALING = new Color(46, 125, 50);
    private static final Color COLOR_STATUS = new Color(30, 100, 200);
    private static final Color COLOR_SYSTEM = new Color(130, 130, 130);
    private static final Color COLOR_INITIATIVE = new Color(183, 149, 11);
    private static final Color COLOR_SAVE = new Color(100, 50, 150);
    private static final Color COLOR_DEATH = new Color(60, 60, 60);
    private static final Color COLOR_DEFAULT = new Color(50, 50, 50);

    // ------------------------------------------------------------------ //
    //  主入口
    // ------------------------------------------------------------------ //

    /**
     * 生成遭遇日志 PDF 并保存到指定路径。
     *
     * @param logEntries    战斗日志条目列表
     * @param participants  参战者列表
     * @param totalRounds   总回合数
     * @param playerDamage  玩家造成的总伤害
     * @param enemyDamage   敌人造成的总伤害
     * @param totalHealing  总治疗量
     * @param playersWon    玩家是否获胜
     * @param xpAwarded     获得的经验值
     * @param filePath      输出文件路径
     * @return 保存的文件路径
     * @throws IOException 如果生成或保存失败
     */
    public static String generate(List<Combat_Log_Entry> logEntries,
                                   List<Combatant> participants,
                                   int totalRounds,
                                   int playerDamage,
                                   int enemyDamage,
                                   int totalHealing,
                                   boolean playersWon,
                                   int xpAwarded,
                                   String filePath) throws IOException {
        PDF_Util.reset_font_cache();
        PDDocument doc = PDF_Util.create_document();

        try {
            PDFont font = PDF_Util.get_chinese_font(doc);
            PDFont monoFont = PDF_Util.get_mono_font(doc);

            // 第一页：标题页 + 摘要
            PDPage page1 = PDF_Util.add_new_page(doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                float y = PDF_Util.CONTENT_TOP;

                // 标题横幅
                y = draw_title_banner(cs, font, monoFont, y);
                y -= SECTION_GAP;

                // 参战者列表
                float participantHeight = calculate_participant_height(participants);
                draw_section_box(cs, PDF_Util.CONTENT_X, y - participantHeight,
                        PDF_Util.CONTENT_WIDTH, participantHeight, "参战者 (Participants)", font);
                y = draw_participant_list(cs, participants, font, monoFont,
                        PDF_Util.CONTENT_X, y - 14f, PDF_Util.CONTENT_WIDTH);
                y -= SECTION_GAP;

                // 战斗统计摘要
                float statsHeight = 120f;
                draw_section_box(cs, PDF_Util.CONTENT_X, y - statsHeight,
                        PDF_Util.CONTENT_WIDTH, statsHeight, "战斗统计摘要 (Combat Summary)", font);
                draw_statistics(cs, font, monoFont,
                        PDF_Util.CONTENT_X, y - 14f, PDF_Util.CONTENT_WIDTH,
                        totalRounds, playerDamage, enemyDamage, totalHealing, playersWon, xpAwarded);

                draw_footer(cs, font, page1);
            }

            // 时间线日志页
            if (logEntries != null && !logEntries.isEmpty()) {
                List<List<Combat_Log_Entry>> pages = paginate_log_entries(logEntries, 22);
                for (List<Combat_Log_Entry> pageEntries : pages) {
                    PDPage logPage = PDF_Util.add_new_page(doc);
                    try (PDPageContentStream cs = new PDPageContentStream(doc, logPage)) {
                        float y = PDF_Util.CONTENT_TOP;

                        // 页面标题
                        PDF_Util.draw_text(cs, font, SUBTITLE_SIZE, "战斗时间线 (Combat Timeline)",
                                PDF_Util.CONTENT_X, y - 14f, PDF_Util.ACCENT_GREEN);
                        y -= 20f;

                        // 表头
                        draw_timeline_header(cs, font, y);
                        y -= TABLE_HEADER_HEIGHT;

                        // 日志条目
                        y = draw_timeline_rows(cs, font, monoFont, pageEntries, y);

                        draw_footer(cs, font, logPage);
                    }
                }
            }

            // 最后一页：参战者最终状态
            if (participants != null && !participants.isEmpty()) {
                PDPage summaryPage = PDF_Util.add_new_page(doc);
                try (PDPageContentStream cs = new PDPageContentStream(doc, summaryPage)) {
                    float y = PDF_Util.CONTENT_TOP;

                    PDF_Util.draw_text(cs, font, SUBTITLE_SIZE, "参战者最终状态 (Final Status)",
                            PDF_Util.CONTENT_X, y - 14f, PDF_Util.ACCENT_GREEN);
                    y -= 20f;

                    draw_participant_summary(cs, participants, font, monoFont, y);

                    // 经验值分配
                    if (xpAwarded > 0) {
                        y -= SECTION_GAP + 60f;
                        draw_section_box(cs, PDF_Util.CONTENT_X, y,
                                PDF_Util.CONTENT_WIDTH, 60f, "经验值与战利品 (XP & Loot)", font);
                        draw_xp_and_loot(cs, font, monoFont,
                                PDF_Util.CONTENT_X, y + 46f, PDF_Util.CONTENT_WIDTH,
                                xpAwarded, playersWon);
                    }

                    draw_footer(cs, font, summaryPage);
                }
            }

            PDF_Util.save_document(doc, filePath);
            return filePath;

        } finally {
            PDF_Util.close_document(doc);
        }
    }

    // ------------------------------------------------------------------ //
    //  标题横幅
    // ------------------------------------------------------------------ //

    private static float draw_title_banner(PDPageContentStream cs, PDFont font,
                                            PDFont monoFont, float topY) throws IOException {
        float bannerHeight = HEADER_HEIGHT;

        // 深色背景
        PDF_Util.draw_rounded_rect(cs, PDF_Util.CONTENT_X, topY - bannerHeight,
                PDF_Util.CONTENT_WIDTH, bannerHeight, 6f, PDF_Util.DARK_BG, null);

        // 标题
        PDF_Util.draw_text(cs, font, TITLE_SIZE, "遭遇战斗日志",
                PDF_Util.CONTENT_X + 15f, topY - 28f, PDF_Util.TEXT_WHITE);

        // 副标题
        PDF_Util.draw_text(cs, font, BODY_SIZE, "Encounter Combat Log",
                PDF_Util.CONTENT_X + 15f, topY - 44f, PDF_Util.ACCENT_GREEN);

        // 日期（右侧）
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        float dateWidth = monoFont.getStringWidth(dateStr) / 1000f * SMALL_SIZE;
        PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, dateStr,
                PDF_Util.CONTENT_X + PDF_Util.CONTENT_WIDTH - 15f - dateWidth, topY - 28f,
                PDF_Util.TEXT_LIGHT_GRAY);

        return topY - bannerHeight;
    }

    // ------------------------------------------------------------------ //
    //  参战者列表
    // ------------------------------------------------------------------ //

    private static float draw_participant_list(PDPageContentStream cs, List<Combatant> participants,
                                               PDFont font, PDFont monoFont,
                                               float startX, float startY, float width) throws IOException {
        float y = startY;
        float x = startX + 10f;

        // 分为玩家和敌方两组
        List<Combatant> players = new ArrayList<>();
        List<Combatant> enemies = new ArrayList<>();
        if (participants != null) {
            for (Combatant c : participants) {
                if (c.side == Combatant.Side.PLAYER) {
                    players.add(c);
                } else {
                    enemies.add(c);
                }
            }
        }

        // 玩家组
        if (!players.isEmpty()) {
            PDF_Util.draw_text(cs, font, LABEL_SIZE, "[玩家方] (" + players.size() + ")",
                    x, y - 8f, PDF_Util.ACCENT_GREEN);
            y -= 14f;
            for (Combatant c : players) {
                String hpText = c.current_hp + "/" + c.max_hp + " HP";
                PDF_Util.draw_text(cs, font, SMALL_SIZE, c.display_name, x + 5f, y - 8f, PDF_Util.TEXT_DARK);
                float nameWidth = font.getStringWidth(c.display_name) / 1000f * SMALL_SIZE;
                PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, hpText,
                        x + 5f + nameWidth + 10f, y - 8f,
                        c.is_alive() ? PDF_Util.HP_GREEN : PDF_Util.HP_RED);
                y -= 12f;
            }
        }

        // 敌方组
        if (!enemies.isEmpty()) {
            y -= 4f;
            PDF_Util.draw_text(cs, font, LABEL_SIZE, "[敌方] (" + enemies.size() + ")",
                    x, y - 8f, PDF_Util.HP_RED);
            y -= 14f;
            for (Combatant c : enemies) {
                String hpText = c.current_hp + "/" + c.max_hp + " HP";
                PDF_Util.draw_text(cs, font, SMALL_SIZE, c.display_name, x + 5f, y - 8f, PDF_Util.TEXT_DARK);
                float nameWidth = font.getStringWidth(c.display_name) / 1000f * SMALL_SIZE;
                PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, hpText,
                        x + 5f + nameWidth + 10f, y - 8f,
                        c.is_alive() ? PDF_Util.HP_GREEN : PDF_Util.HP_RED);
                y -= 12f;
            }
        }

        return y;
    }

    // ------------------------------------------------------------------ //
    //  战斗统计摘要
    // ------------------------------------------------------------------ //

    private static void draw_statistics(PDPageContentStream cs, PDFont font, PDFont monoFont,
                                         float startX, float startY, float width,
                                         int totalRounds, int playerDamage, int enemyDamage,
                                         int totalHealing, boolean playersWon, int xpAwarded) throws IOException {
        float y = startY;
        float x = startX + 15f;
        float colWidth = width / 3f;

        // 第一列
        PDF_Util.draw_stat_block(cs, font, monoFont, x, y, colWidth,
                "总回合", String.valueOf(totalRounds), PDF_Util.ACCENT_BLUE);

        // 第二列
        PDF_Util.draw_stat_block(cs, font, monoFont, x + colWidth, y, colWidth,
                "战斗结果", playersWon ? "胜利" : "失败",
                playersWon ? PDF_Util.ACCENT_GREEN : PDF_Util.HP_RED);

        // 第三列
        PDF_Util.draw_stat_block(cs, font, monoFont, x + colWidth * 2, y, colWidth,
                "经验值", "+" + xpAwarded, PDF_Util.ACCENT_GOLD);

        y -= 50f;

        // 伤害统计
        PDF_Util.draw_stat_block(cs, font, monoFont, x, y, colWidth,
                "玩家总伤害", String.valueOf(playerDamage), PDF_Util.HP_RED);

        PDF_Util.draw_stat_block(cs, font, monoFont, x + colWidth, y, colWidth,
                "敌方总伤害", String.valueOf(enemyDamage), PDF_Util.HP_RED);

        PDF_Util.draw_stat_block(cs, font, monoFont, x + colWidth * 2, y, colWidth,
                "总治疗量", String.valueOf(totalHealing), PDF_Util.HP_GREEN);
    }

    // ------------------------------------------------------------------ //
    //  时间线日志
    // ------------------------------------------------------------------ //

    private static void draw_timeline_header(PDPageContentStream cs, PDFont font, float y) throws IOException {
        float x = PDF_Util.CONTENT_X;
        float width = PDF_Util.CONTENT_WIDTH;

        // 表头背景
        PDF_Util.draw_rect(cs, x, y - TABLE_HEADER_HEIGHT, width, TABLE_HEADER_HEIGHT,
                PDF_Util.TABLE_HEADER_BG, null);

        float colX = x + 5f;
        float[] colWidths = {30f, 70f, 80f, 80f, width - 265f, 40f}; // 回合、类型、行动者、目标、描述、数值

        String[] headers = {"回合", "类型", "行动者", "目标", "描述", "数值"};
        for (int i = 0; i < headers.length; i++) {
            PDF_Util.draw_text(cs, font, LABEL_SIZE, headers[i], colX, y - 14f, PDF_Util.TEXT_WHITE);
            colX += colWidths[i];
        }
    }

    private static float draw_timeline_rows(PDPageContentStream cs, PDFont font, PDFont monoFont,
                                             List<Combat_Log_Entry> entries, float startY) throws IOException {
        float y = startY;
        float x = PDF_Util.CONTENT_X;
        float width = PDF_Util.CONTENT_WIDTH;
        float[] colWidths = {30f, 70f, 80f, 80f, width - 265f, 40f};

        for (int i = 0; i < entries.size(); i++) {
            Combat_Log_Entry entry = entries.get(i);

            // 交替行背景
            if (i % 2 == 1) {
                PDF_Util.draw_rect(cs, x, y - TABLE_ROW_HEIGHT, width, TABLE_ROW_HEIGHT,
                        PDF_Util.TABLE_ROW_ALT, null);
            }

            // 左侧类型颜色条
            Color typeColor = get_log_type_color(entry.type);
            PDF_Util.draw_rect(cs, x, y - TABLE_ROW_HEIGHT, 3f, TABLE_ROW_HEIGHT, typeColor, null);

            float colX = x + 5f;

            // 回合
            PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, String.valueOf(entry.round_number),
                    colX, y - 12f, PDF_Util.TEXT_DARK);
            colX += colWidths[0];

            // 类型
            PDF_Util.draw_text(cs, font, SMALL_SIZE, entry.type.label,
                    colX, y - 12f, typeColor);
            colX += colWidths[1];

            // 行动者
            String actor = (entry.actor_name != null && !entry.actor_name.isEmpty())
                    ? truncate_text(entry.actor_name, font, SMALL_SIZE, colWidths[2] - 5f) : "-";
            PDF_Util.draw_text(cs, font, SMALL_SIZE, actor, colX, y - 12f, PDF_Util.TEXT_DARK);
            colX += colWidths[2];

            // 目标
            String target = (entry.target_name != null && !entry.target_name.isEmpty())
                    ? truncate_text(entry.target_name, font, SMALL_SIZE, colWidths[3] - 5f) : "-";
            PDF_Util.draw_text(cs, font, SMALL_SIZE, target, colX, y - 12f, PDF_Util.TEXT_DARK);
            colX += colWidths[3];

            // 描述
            String desc = (entry.description != null && !entry.description.isEmpty())
                    ? truncate_text(entry.description, font, SMALL_SIZE, colWidths[4] - 5f) : "-";
            PDF_Util.draw_text(cs, font, SMALL_SIZE, desc, colX, y - 12f, PDF_Util.TEXT_DARK);
            colX += colWidths[4];

            // 数值
            String numVal = entry.numeric_value != 0 ? String.valueOf(entry.numeric_value) : "-";
            PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, numVal, colX, y - 12f, typeColor);

            y -= TABLE_ROW_HEIGHT;

            // 检查是否需要换页（留出页脚空间）
            if (y < PDF_Util.CONTENT_Y + 20f) {
                break;
            }
        }

        return y;
    }

    // ------------------------------------------------------------------ //
    //  参战者最终状态
    // ------------------------------------------------------------------ //

    private static void draw_participant_summary(PDPageContentStream cs, List<Combatant> participants,
                                                  PDFont font, PDFont monoFont, float startY) throws IOException {
        float y = startY;
        float x = PDF_Util.CONTENT_X;
        float width = PDF_Util.CONTENT_WIDTH;

        // 表头
        PDF_Util.draw_rect(cs, x, y - TABLE_HEADER_HEIGHT, width, TABLE_HEADER_HEIGHT,
                PDF_Util.TABLE_HEADER_BG, null);

        float colX = x + 5f;
        float[] colWidths = {width * 0.25f, width * 0.15f, width * 0.15f, width * 0.15f, width * 0.30f};

        String[] headers = {"名称", "阵营", "HP", "AC", "状态"};
        for (int i = 0; i < headers.length; i++) {
            PDF_Util.draw_text(cs, font, LABEL_SIZE, headers[i], colX, y - 14f, PDF_Util.TEXT_WHITE);
            colX += colWidths[i];
        }
        y -= TABLE_HEADER_HEIGHT;

        if (participants == null) return;

        for (int i = 0; i < participants.size(); i++) {
            Combatant c = participants.get(i);

            // 交替行背景
            if (i % 2 == 1) {
                PDF_Util.draw_rect(cs, x, y - TABLE_ROW_HEIGHT, width, TABLE_ROW_HEIGHT,
                        PDF_Util.TABLE_ROW_ALT, null);
            }

            // 阵营颜色条
            Color sideColor = (c.side == Combatant.Side.PLAYER) ? PDF_Util.ACCENT_GREEN : PDF_Util.HP_RED;
            PDF_Util.draw_rect(cs, x, y - TABLE_ROW_HEIGHT, 3f, TABLE_ROW_HEIGHT, sideColor, null);

            colX = x + 5f;

            // 名称
            PDF_Util.draw_text(cs, font, SMALL_SIZE, c.display_name, colX, y - 12f, PDF_Util.TEXT_DARK);
            colX += colWidths[0];

            // 阵营
            String sideLabel = (c.side == Combatant.Side.PLAYER) ? "玩家" : "敌方";
            PDF_Util.draw_text(cs, font, SMALL_SIZE, sideLabel, colX, y - 12f, sideColor);
            colX += colWidths[1];

            // HP
            String hpStr = c.current_hp + "/" + c.max_hp;
            Color hpColor = c.is_alive() ? PDF_Util.HP_GREEN : PDF_Util.HP_RED;
            PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, hpStr, colX, y - 12f, hpColor);
            colX += colWidths[2];

            // AC
            PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, String.valueOf(c.armor_class),
                    colX, y - 12f, PDF_Util.TEXT_DARK);
            colX += colWidths[3];

            // 状态
            String status = c.is_alive() ? c.get_status_summary() : "死亡";
            PDF_Util.draw_text(cs, font, SMALL_SIZE, status, colX, y - 12f,
                    c.is_alive() ? PDF_Util.TEXT_DARK : PDF_Util.HP_RED);

            y -= TABLE_ROW_HEIGHT;
        }
    }

    // ------------------------------------------------------------------ //
    //  经验值与战利品
    // ------------------------------------------------------------------ //

    private static void draw_xp_and_loot(PDPageContentStream cs, PDFont font, PDFont monoFont,
                                          float startX, float startY, float width,
                                          int xpAwarded, boolean playersWon) throws IOException {
        float y = startY;
        float x = startX + 15f;

        // 经验值
        PDF_Util.draw_text(cs, font, BODY_SIZE, "获得经验值:",
                x, y - 10f, PDF_Util.TEXT_DARK);
        float labelWidth = font.getStringWidth("获得经验值:") / 1000f * BODY_SIZE;
        PDF_Util.draw_text(cs, monoFont, SUBTITLE_SIZE, "+" + xpAwarded + " XP",
                x + labelWidth + 10f, y - 11f, PDF_Util.ACCENT_GOLD);

        y -= 20f;

        // 战斗结果
        String resultText = playersWon ? "战斗胜利 - 所有敌人已被击败" : "战斗失败 - 玩家方撤退或被击败";
        PDF_Util.draw_text(cs, font, SMALL_SIZE, resultText, x, y - 8f,
                playersWon ? PDF_Util.ACCENT_GREEN : PDF_Util.HP_RED);
    }

    // ------------------------------------------------------------------ //
    //  辅助方法
    // ------------------------------------------------------------------ //

    /**
     * 根据日志类型获取对应颜色。
     */
    private static Color get_log_type_color(Combat_Log_Entry.Log_Type type) {
        if (type == null) return COLOR_DEFAULT;

        return switch (type) {
            case ATTACK_ROLL, DAMAGE_DEALT, OPPORTUNITY_ATTACK -> COLOR_ATTACK;
            case HEALING_DONE -> COLOR_HEALING;
            case STATUS_APPLIED, STATUS_EXPIRED, STATUS_DAMAGE, CONCENTRATION_CHECK, CONCENTRATION_BROKEN -> COLOR_STATUS;
            case COMBAT_START, COMBAT_END, ROUND_START, TURN_END, TURN_SKIPPED, SYSTEM, SHORT_REST, LONG_REST -> COLOR_SYSTEM;
            case INITIATIVE_ROLL, ADVANTAGE_ROLL, DISADVANTAGE_ROLL -> COLOR_INITIATIVE;
            case SAVE_THROW -> COLOR_SAVE;
            case DEATH -> COLOR_DEATH;
            default -> COLOR_DEFAULT;
        };
    }

    /**
     * 将日志条目分页。
     */
    private static List<List<Combat_Log_Entry>> paginate_log_entries(List<Combat_Log_Entry> entries, int perPage) {
        List<List<Combat_Log_Entry>> pages = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return pages;
        }

        for (int i = 0; i < entries.size(); i += perPage) {
            int end = Math.min(i + perPage, entries.size());
            pages.add(entries.subList(i, end));
        }
        return pages;
    }

    /**
     * 计算参战者列表区域的高度。
     */
    private static float calculate_participant_height(List<Combatant> participants) {
        if (participants == null || participants.isEmpty()) {
            return 40f;
        }
        // 每个参战者约 12pt，加上标题和分组标签
        int count = participants.size();
        return Math.max(60f, 30f + count * 12f + 20f);
    }

    /**
     * 截断文本以适应指定宽度。
     */
    private static String truncate_text(String text, PDFont font, float size, float maxWidth) {
        if (text == null) return "-";
        if (text.isEmpty()) return "-";

        try {
            float textWidth = font.getStringWidth(text) / 1000f * size;
            if (textWidth <= maxWidth) {
                return text;
            }

            // 逐字符截断
            int end = text.length();
            while (end > 1) {
                String candidate = text.substring(0, end) + "..";
                float candidateWidth = font.getStringWidth(candidate) / 1000f * size;
                if (candidateWidth <= maxWidth) {
                    return candidate;
                }
                end--;
            }
            return text.substring(0, 1) + "..";
        } catch (IOException e) {
            return text.length() > 10 ? text.substring(0, 10) + ".." : text;
        }
    }

    /**
     * 绘制一个带标题的区域框。
     */
    private static void draw_section_box(PDPageContentStream cs, float x, float y,
                                          float width, float height, String title,
                                          PDFont font) throws IOException {
        PDF_Util.draw_rounded_rect(cs, x, y, width, height, 4f,
                new Color(250, 250, 252), PDF_Util.BORDER_COLOR);
        PDF_Util.draw_rounded_rect(cs, x + 1f, y + height - 18f, width - 2f, 17f, 3f,
                PDF_Util.ACCENT_GREEN, null);
        PDF_Util.draw_text(cs, font, LABEL_SIZE, title, x + 8f, y + height - 14f, PDF_Util.TEXT_WHITE);
    }

    /**
     * 绘制页脚。
     */
    private static void draw_footer(PDPageContentStream cs, PDFont font, PDPage page) throws IOException {
        float y = PDF_Util.CONTENT_Y - 5f;
        PDF_Util.draw_line(cs, PDF_Util.CONTENT_X, y + 10f,
                PDF_Util.CONTENT_X + PDF_Util.CONTENT_WIDTH, y + 10f, 0.5f, PDF_Util.BORDER_COLOR);

        String footerText = "DMHelper - D&D 5e Encounter Combat Log";
        float footerWidth = font.getStringWidth(footerText) / 1000f * LABEL_SIZE;
        PDF_Util.draw_text(cs, font, LABEL_SIZE, footerText,
                PDF_Util.CONTENT_X + (PDF_Util.CONTENT_WIDTH - footerWidth) / 2f, y, PDF_Util.TEXT_LIGHT_GRAY);
    }
}
