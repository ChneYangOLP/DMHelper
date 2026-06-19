package com.DMHelper.basic;

import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 角色卡 PDF 导出器。
 * 生成标准 D&D 5e 风格的角色卡 PDF 文件。
 * 使用深色主题头部横幅、绿色强调色、交替行颜色和圆角矩形框。
 */
public class Character_Card_PDF {

    // ------------------------------------------------------------------ //
    //  布局常量
    // ------------------------------------------------------------------ //

    private static final float HEADER_HEIGHT = 70f;
    private static final float SECTION_GAP = 8f;
    private static final float SMALL_GAP = 4f;
    private static final float TITLE_SIZE = 18f;
    private static final float SUBTITLE_SIZE = 12f;
    private static final float BODY_SIZE = 9f;
    private static final float SMALL_SIZE = 8f;
    private static final float LABEL_SIZE = 7f;
    private static final float LINE_HEIGHT = 13f;
    private static final float SMALL_LINE_HEIGHT = 11f;

    // 六维属性名称
    private static final String[] ABILITY_NAMES = {"STR", "DEX", "CON", "INT", "WIS", "CHA"};
    private static final String[] ABILITY_FULL_NAMES = {"Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma"};

    // D&D 5e 技能列表（18 项）
    private static final String[] SKILL_NAMES = {
            "运动", "杂技", "巧手", "隐匿",
            "奥秘", "历史", "调查", "自然", "宗教",
            "驯兽", "洞悉", "医药", "察觉", "生存",
            "欺瞒", "威吓", "表演", "游说"
    };

    // 豁免检定名称
    private static final String[] SAVE_NAMES = {
            "Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Charisma"
    };

    // ------------------------------------------------------------------ //
    //  主入口
    // ------------------------------------------------------------------ //

    /**
     * 生成角色卡 PDF 并保存到指定路径。
     *
     * @param character 角色卡数据
     * @param filePath  输出文件路径
     * @return 保存的文件路径
     * @throws IOException 如果生成或保存失败
     */
    public static String generate(Character_Sheet character, String filePath) throws IOException {
        PDF_Util.reset_font_cache();
        PDDocument doc = PDF_Util.create_document();

        try {
            PDFont font = PDF_Util.get_chinese_font(doc);
            PDFont monoFont = PDF_Util.get_mono_font(doc);

            // 第一页：角色核心信息
            PDPage page1 = PDF_Util.add_new_page(doc);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                float y = PDF_Util.CONTENT_TOP;

                // 绘制头部横幅
                y = draw_header(cs, character, font, monoFont, y);
                y -= SECTION_GAP;

                // 左右分栏布局
                float leftX = PDF_Util.CONTENT_X;
                float rightX = PDF_Util.CONTENT_X + PDF_Util.CONTENT_WIDTH * 0.52f;
                float colWidth = PDF_Util.CONTENT_WIDTH * 0.46f;

                // 左上：六维属性
                float abilityHeight = 155f;
                draw_section_box(cs, leftX, y - abilityHeight, colWidth, abilityHeight, "属性 (Ability Scores)", font);
                draw_ability_scores(cs, character, font, monoFont, leftX, y - 12f, colWidth);

                // 右上：战斗数据
                float combatHeight = 155f;
                draw_section_box(cs, rightX, y - combatHeight, colWidth, combatHeight, "战斗数据 (Combat)", font);
                draw_combat_stats(cs, character, font, monoFont, rightX, y - 12f, colWidth);
                y -= (abilityHeight + SECTION_GAP);

                // 中部左：豁免检定
                float saveHeight = 105f;
                draw_section_box(cs, leftX, y - saveHeight, colWidth, saveHeight, "豁免检定 (Saving Throws)", font);
                draw_saving_throws(cs, character, font, monoFont, leftX, y - 12f, colWidth);

                // 中部右：技能列表
                float skillHeight = 105f;
                draw_section_box(cs, rightX, y - skillHeight, colWidth, skillHeight, "技能 (Skills)", font);
                draw_skills(cs, character, font, monoFont, rightX, y - 12f, colWidth);
                y -= (saveHeight + SECTION_GAP);

                // 下部左：装备信息
                float equipHeight = 100f;
                draw_section_box(cs, leftX, y - equipHeight, colWidth, equipHeight, "装备 (Equipment)", font);
                draw_equipment(cs, character, font, leftX, y - 12f, colWidth);

                // 下部右：施法信息或负重
                if (is_spellcaster(character)) {
                    float spellHeight = 100f;
                    draw_section_box(cs, rightX, y - spellHeight, colWidth, spellHeight, "施法信息 (Spellcasting)", font);
                    draw_spellcasting_info(cs, character, font, monoFont, rightX, y - 12f, colWidth);
                } else {
                    float encHeight = 100f;
                    draw_section_box(cs, rightX, y - encHeight, colWidth, encHeight, "负重信息 (Encumbrance)", font);
                    draw_encumbrance(cs, character, font, monoFont, rightX, y - 12f, colWidth);
                }

                // 页脚
                draw_footer(cs, font, page1);
            }

            // 第二页（如需要）：背包、特性、背景
            boolean needsSecondPage = needs_second_page(character);
            if (needsSecondPage) {
                PDPage page2 = PDF_Util.add_new_page(doc);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
                    float y = PDF_Util.CONTENT_TOP;

                    // 背包物品
                    y = draw_inventory(cs, character, font, monoFont, y, doc);
                    y -= SECTION_GAP;

                    // 特性与专长
                    float featureHeight = Math.min(200f, y - PDF_Util.CONTENT_Y - SECTION_GAP);
                    if (featureHeight > 50f) {
                        draw_section_box(cs, PDF_Util.CONTENT_X, y - featureHeight,
                                PDF_Util.CONTENT_WIDTH, featureHeight, "特性与专长 (Features & Traits)", font);
                        draw_features_and_traits(cs, character, font,
                                PDF_Util.CONTENT_X, y - 12f, PDF_Util.CONTENT_WIDTH, featureHeight - 20f);
                        y -= (featureHeight + SECTION_GAP);
                    }

                    // 背景故事
                    float backstoryHeight = Math.min(200f, y - PDF_Util.CONTENT_Y);
                    if (backstoryHeight > 50f) {
                        draw_section_box(cs, PDF_Util.CONTENT_X, y - backstoryHeight,
                                PDF_Util.CONTENT_WIDTH, backstoryHeight, "背景故事 (Backstory)", font);
                        draw_backstory(cs, character, font,
                                PDF_Util.CONTENT_X, y - 12f, PDF_Util.CONTENT_WIDTH, backstoryHeight - 20f);
                    }

                    draw_footer(cs, font, page2);
                }
            }

            PDF_Util.save_document(doc, filePath);
            return filePath;

        } finally {
            PDF_Util.close_document(doc);
        }
    }

    // ------------------------------------------------------------------ //
    //  头部横幅
    // ------------------------------------------------------------------ //

    private static float draw_header(PDPageContentStream cs, Character_Sheet character,
                                      PDFont font, PDFont monoFont, float topY) throws IOException {
        float bannerY = topY;
        float bannerHeight = HEADER_HEIGHT;

        // 深色背景横幅
        PDF_Util.draw_rounded_rect(cs, PDF_Util.CONTENT_X, bannerY - bannerHeight,
                PDF_Util.CONTENT_WIDTH, bannerHeight, 6f, PDF_Util.DARK_BG, null);

        // 角色名（大号白色）
        float nameY = bannerY - 28f;
        PDF_Util.draw_text(cs, font, TITLE_SIZE, character.name,
                PDF_Util.CONTENT_X + 15f, nameY, PDF_Util.TEXT_WHITE);

        // 种族/职业/等级
        String classInfo = character.race.race_name;
        if (character.race.subrace_name != null && !character.race.subrace_name.isEmpty()) {
            classInfo += " (" + character.race.subrace_name + ")";
        }
        classInfo += "  |  " + character.job.class_name;
        if (!character.job.get_subclass_name().isEmpty()) {
            classInfo += " (" + character.job.get_subclass_name() + ")";
        }
        classInfo += "  Lv." + character.job.current_level;
        PDF_Util.draw_text(cs, font, SUBTITLE_SIZE, classInfo,
                PDF_Util.CONTENT_X + 15f, nameY - 18f, PDF_Util.ACCENT_GREEN);

        // 经验值（右侧）
        String xpText = "XP: " + character.experience_points;
        if (character.get_next_level_xp() > 0) {
            xpText += " / " + character.get_next_level_xp();
        }
        float xpWidth = monoFont.getStringWidth(xpText) / 1000f * SUBTITLE_SIZE;
        PDF_Util.draw_text(cs, monoFont, SUBTITLE_SIZE, xpText,
                PDF_Util.CONTENT_X + PDF_Util.CONTENT_WIDTH - 15f - xpWidth, nameY,
                PDF_Util.ACCENT_GOLD);

        // 金币（右侧第二行）
        String goldText = character.get_currency_summary();
        float goldWidth = monoFont.getStringWidth(goldText) / 1000f * SMALL_SIZE;
        PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, goldText,
                PDF_Util.CONTENT_X + PDF_Util.CONTENT_WIDTH - 15f - goldWidth, nameY - 18f,
                PDF_Util.TEXT_LIGHT_GRAY);

        return bannerY - bannerHeight;
    }

    // ------------------------------------------------------------------ //
    //  六维属性
    // ------------------------------------------------------------------ //

    private static void draw_ability_scores(PDPageContentStream cs, Character_Sheet character,
                                             PDFont font, PDFont monoFont,
                                             float startX, float startY, float width) throws IOException {
        int[] values = {
                character.stats.str, character.stats.dex, character.stats.con,
                character.stats.intel, character.stats.wis, character.stats.cha
        };

        float boxSize = 42f;
        float gapX = (width - boxSize * 3) / 4f;
        float gapY = 8f;

        for (int i = 0; i < 6; i++) {
            int col = i % 3;
            int row = i / 3;
            float cx = startX + gapX + col * (boxSize + gapX);
            float cy = startY - 10f - row * (boxSize + gapY);

            // 圆角矩形框
            PDF_Util.draw_rounded_rect(cs, cx, cy - boxSize, boxSize, boxSize, 4f,
                    null, PDF_Util.BORDER_COLOR);

            // 属性缩写（顶部小字）
            String abbr = ABILITY_NAMES[i];
            float abbrWidth = font.getStringWidth(abbr) / 1000f * LABEL_SIZE;
            PDF_Util.draw_text(cs, font, LABEL_SIZE, abbr,
                    cx + (boxSize - abbrWidth) / 2f, cy - 12f, PDF_Util.ACCENT_GREEN);

            // 属性值（中间大号数字）
            String valStr = String.valueOf(values[i]);
            float valWidth = monoFont.getStringWidth(valStr) / 1000f * 16f;
            PDF_Util.draw_text(cs, monoFont, 16f, valStr,
                    cx + (boxSize - valWidth) / 2f, cy - 28f, PDF_Util.TEXT_DARK);

            // 调整值（底部）
            int mod = character.stats.get_mod(values[i]);
            String modStr = format_modifier(mod);
            float modWidth = monoFont.getStringWidth(modStr) / 1000f * BODY_SIZE;
            PDF_Util.draw_text(cs, monoFont, BODY_SIZE, modStr,
                    cx + (boxSize - modWidth) / 2f, cy - boxSize + 5f,
                    mod >= 0 ? PDF_Util.ACCENT_GREEN : PDF_Util.HP_RED);
        }
    }

    // ------------------------------------------------------------------ //
    //  战斗数据
    // ------------------------------------------------------------------ //

    private static void draw_combat_stats(PDPageContentStream cs, Character_Sheet character,
                                           PDFont font, PDFont monoFont,
                                           float startX, float startY, float width) throws IOException {
        float y = startY;
        float labelX = startX + 10f;
        float valueX = startX + width - 10f;
        float rowHeight = 22f;

        // HP
        draw_combat_row(cs, font, monoFont, labelX, valueX, y, rowHeight,
                "HP", character.get_hp_summary(), PDF_Util.HP_RED);
        y -= rowHeight;

        // AC
        draw_combat_row(cs, font, monoFont, labelX, valueX, y, rowHeight,
                "AC", String.valueOf(character.ac), PDF_Util.ACCENT_BLUE);
        y -= rowHeight;

        // 先攻修正
        draw_combat_row(cs, font, monoFont, labelX, valueX, y, rowHeight,
                "Initiative", format_modifier(character.get_initiative_modifier()), PDF_Util.ACCENT_GOLD);
        y -= rowHeight;

        // 速度
        int speed = Encumbrance_System.get_effective_speed(character);
        draw_combat_row(cs, font, monoFont, labelX, valueX, y, rowHeight,
                "Speed", speed + " ft.", PDF_Util.TEXT_DARK);
        y -= rowHeight;

        // 熟练加值
        draw_combat_row(cs, font, monoFont, labelX, valueX, y, rowHeight,
                "Proficiency", "+" + character.get_proficiency_bonus(), PDF_Util.ACCENT_GREEN);
        y -= rowHeight;

        // 生命骰
        draw_combat_row(cs, font, monoFont, labelX, valueX, y, rowHeight,
                "Hit Dice", character.get_hit_dice_summary(), PDF_Util.TEXT_DARK);
    }

    private static void draw_combat_row(PDPageContentStream cs, PDFont font, PDFont monoFont,
                                         float labelX, float valueX, float y, float rowHeight,
                                         String label, String value, Color valueColor) throws IOException {
        PDF_Util.draw_text(cs, font, BODY_SIZE, label, labelX, y - 12f, PDF_Util.TEXT_DARK);
        float valWidth = monoFont.getStringWidth(value) / 1000f * SUBTITLE_SIZE;
        PDF_Util.draw_text(cs, monoFont, SUBTITLE_SIZE, value,
                valueX - valWidth, y - 13f, valueColor);
        PDF_Util.draw_line(cs, labelX, y - rowHeight + 2f, valueX, y - rowHeight + 2f,
                0.5f, PDF_Util.TABLE_ROW_ALT);
    }

    // ------------------------------------------------------------------ //
    //  豁免检定
    // ------------------------------------------------------------------ //

    private static void draw_saving_throws(PDPageContentStream cs, Character_Sheet character,
                                            PDFont font, PDFont monoFont,
                                            float startX, float startY, float width) throws IOException {
        float y = startY;
        float x = startX + 10f;
        float rowHeight = 15f;

        for (int i = 0; i < SAVE_NAMES.length; i++) {
            String saveName = SAVE_NAMES[i];
            int bonus = character.get_saving_throw_bonus(saveName);
            boolean proficient = character.job.saving_throws.contains(saveName);

            // 熟练标记圆圈
            float circleX = x + 2f;
            float circleY = y - 9f;
            float circleR = 4f;
            cs.setStrokingColor(proficient ? PDF_Util.ACCENT_GREEN : PDF_Util.BORDER_COLOR);
            cs.setLineWidth(1f);
            // 简单圆形
            cs.moveTo(circleX + circleR, circleY);
            cs.lineTo(circleX + 2 * circleR, circleY + circleR);
            cs.lineTo(circleX + circleR, circleY + 2 * circleR);
            cs.lineTo(circleX, circleY + circleR);
            cs.closePath();
            if (proficient) {
                cs.setNonStrokingColor(PDF_Util.ACCENT_GREEN);
                cs.fill();
            }
            cs.stroke();

            // 加值
            String modStr = format_modifier(bonus);
            PDF_Util.draw_text(cs, monoFont, BODY_SIZE, modStr, x + 12f, y - 11f, PDF_Util.TEXT_DARK);

            // 属性名
            PDF_Util.draw_text(cs, font, SMALL_SIZE, ABILITY_NAMES[i], x + 35f, y - 11f, PDF_Util.TEXT_DARK);

            y -= rowHeight;
        }
    }

    // ------------------------------------------------------------------ //
    //  技能列表
    // ------------------------------------------------------------------ //

    private static void draw_skills(PDPageContentStream cs, Character_Sheet character,
                                     PDFont font, PDFont monoFont,
                                     float startX, float startY, float width) throws IOException {
        float y = startY;
        float x = startX + 8f;
        float rowHeight = 11f;
        float colWidth = width / 3f;

        for (int i = 0; i < SKILL_NAMES.length; i++) {
            int col = i % 3;
            int row = i / 3;
            float cx = x + col * colWidth;
            float cy = y - row * rowHeight;

            String skillName = SKILL_NAMES[i];
            int bonus = character.get_skill_bonus(skillName);
            boolean proficient = character.job.skill_proficiencies.contains(skillName);
            boolean expert = character.job.has_skill_expertise(skillName);

            // 熟练标记
            float markX = cx + 1f;
            float markY = cy - 7f;
            float markR = 3f;
            cs.setLineWidth(0.8f);
            if (expert) {
                // 双圆圈（专精）
                cs.setStrokingColor(PDF_Util.ACCENT_GREEN);
                cs.moveTo(markX + markR, markY);
                cs.lineTo(markX + 2 * markR, markY + markR);
                cs.lineTo(markX + markR, markY + 2 * markR);
                cs.lineTo(markX, markY + markR);
                cs.closePath();
                cs.setNonStrokingColor(PDF_Util.ACCENT_GREEN);
                cs.fill();
                // 外圈
                cs.moveTo(markX + markR - 1, markY - 1);
                cs.lineTo(markX + 3 * markR + 1, markY + markR - 1);
                cs.lineTo(markX + markR - 1, markY + 3 * markR + 1);
                cs.lineTo(markX - 1, markY + markR - 1);
                cs.closePath();
                cs.stroke();
            } else if (proficient) {
                // 单圆圈（熟练）
                cs.setStrokingColor(PDF_Util.ACCENT_GREEN);
                cs.moveTo(markX + markR, markY);
                cs.lineTo(markX + 2 * markR, markY + markR);
                cs.lineTo(markX + markR, markY + 2 * markR);
                cs.lineTo(markX, markY + markR);
                cs.closePath();
                cs.setNonStrokingColor(PDF_Util.ACCENT_GREEN);
                cs.fill();
            } else {
                // 空圆圈
                cs.setStrokingColor(PDF_Util.BORDER_COLOR);
                cs.moveTo(markX + markR, markY);
                cs.lineTo(markX + 2 * markR, markY + markR);
                cs.lineTo(markX + markR, markY + 2 * markR);
                cs.lineTo(markX, markY + markR);
                cs.closePath();
                cs.stroke();
            }

            // 加值
            String modStr = format_modifier(bonus);
            PDF_Util.draw_text(cs, monoFont, SMALL_SIZE, modStr, cx + 10f, cy - 9f, PDF_Util.TEXT_DARK);

            // 技能名
            PDF_Util.draw_text(cs, font, SMALL_SIZE, skillName, cx + 28f, cy - 9f, PDF_Util.TEXT_DARK);
        }
    }

    // ------------------------------------------------------------------ //
    //  装备信息
    // ------------------------------------------------------------------ //

    private static void draw_equipment(PDPageContentStream cs, Character_Sheet character,
                                        PDFont font, float startX, float startY, float width) throws IOException {
        float y = startY;
        float x = startX + 10f;
        float rowHeight = 16f;

        Equipment_Slot[] slots = {
                Equipment_Slot.ARMOR, Equipment_Slot.MAIN_HAND, Equipment_Slot.OFF_HAND,
                Equipment_Slot.CLOAK, Equipment_Slot.ACCESSORY
        };
        String[] slotLabels = {"护甲", "主手", "副手", "披风", "护符"};

        for (int i = 0; i < slots.length; i++) {
            Equipment_Item item = character.get_equipped_item(slots[i]);
            String label = slotLabels[i] + ": ";
            String value = (item != null) ? item.display_name : "(无)";

            PDF_Util.draw_text(cs, font, LABEL_SIZE, label, x, y - 10f, PDF_Util.ACCENT_GREEN);
            float labelWidth = font.getStringWidth(label) / 1000f * LABEL_SIZE;
            PDF_Util.draw_text(cs, font, SMALL_SIZE, value, x + labelWidth, y - 10f, PDF_Util.TEXT_DARK);

            y -= rowHeight;
        }
    }

    // ------------------------------------------------------------------ //
    //  背包物品
    // ------------------------------------------------------------------ //

    private static float draw_inventory(PDPageContentStream cs, Character_Sheet character,
                                         PDFont font, PDFont monoFont, float topY,
                                         PDDocument doc) throws IOException {
        float boxHeight = 180f;
        draw_section_box(cs, PDF_Util.CONTENT_X, topY - boxHeight,
                PDF_Util.CONTENT_WIDTH, boxHeight, "背包物品 (Inventory)", font);

        float y = topY - 14f;
        float x = PDF_Util.CONTENT_X + 10f;
        float maxWidth = PDF_Util.CONTENT_WIDTH - 20f;
        int count = 0;

        if (character.owned_equipment_keys != null) {
            for (String itemKey : character.owned_equipment_keys) {
                Equipment_Item item = Equipment_Library.get_item(itemKey);
                if (item == null) continue;

                // 只显示背包物品和已装备物品
                boolean isEquipped = itemKey.equals(character.equipped_armor_key)
                        || itemKey.equals(character.equipped_main_hand_key)
                        || itemKey.equals(character.equipped_off_hand_key)
                        || itemKey.equals(character.equipped_cloak_key)
                        || itemKey.equals(character.equipped_accessory_key);

                StringBuilder line = new StringBuilder();
                line.append(item.display_name);
                if (isEquipped) {
                    line.append(" [已装备]");
                }
                int itemCount = character.get_item_count(itemKey);
                if (itemCount > 1) {
                    line.append(" x").append(itemCount);
                }
                if (item.value_in_cp > 0) {
                    line.append(" (").append(item.get_value_summary()).append(")");
                }

                if (y - 10f < PDF_Util.CONTENT_Y + 10f) {
                    // 需要换页
                    break;
                }

                PDF_Util.draw_text(cs, font, SMALL_SIZE, line.toString(), x, y - 10f, PDF_Util.TEXT_DARK);
                y -= SMALL_LINE_HEIGHT;
                count++;
            }
        }

        if (count == 0) {
            PDF_Util.draw_text(cs, font, SMALL_SIZE, "(背包为空)", x, y - 10f, PDF_Util.TEXT_LIGHT_GRAY);
        }

        return topY - boxHeight;
    }

    // ------------------------------------------------------------------ //
    //  特性与专长
    // ------------------------------------------------------------------ //

    private static void draw_features_and_traits(PDPageContentStream cs, Character_Sheet character,
                                                  PDFont font, float startX, float startY,
                                                  float width, float availableHeight) throws IOException {
        float y = startY;
        float x = startX + 10f;
        float maxWidth = width - 20f;

        // 种族特性
        PDF_Util.draw_text(cs, font, LABEL_SIZE, "[种族特性]", x, y - 8f, PDF_Util.ACCENT_GREEN);
        y -= 14f;
        List<String> raceFeatures = character.race.get_feature_summaries();
        for (String feature : raceFeatures) {
            if (y < startY - availableHeight + 5f) break;
            int lines = PDF_Util.draw_wrapped_text(cs, font, SMALL_SIZE, feature,
                    x, y - 8f, maxWidth, SMALL_LINE_HEIGHT, PDF_Util.TEXT_DARK);
            y -= lines * SMALL_LINE_HEIGHT + 2f;
        }

        y -= 4f;
        // 职业特性
        PDF_Util.draw_text(cs, font, LABEL_SIZE, "[职业特性]", x, y - 8f, PDF_Util.ACCENT_GREEN);
        y -= 14f;
        List<String> classFeatures = character.job.get_feature_summaries();
        for (String feature : classFeatures) {
            if (y < startY - availableHeight + 5f) break;
            int lines = PDF_Util.draw_wrapped_text(cs, font, SMALL_SIZE, feature,
                    x, y - 8f, maxWidth, SMALL_LINE_HEIGHT, PDF_Util.TEXT_DARK);
            y -= lines * SMALL_LINE_HEIGHT + 2f;
        }
    }

    // ------------------------------------------------------------------ //
    //  背景故事
    // ------------------------------------------------------------------ //

    private static void draw_backstory(PDPageContentStream cs, Character_Sheet character,
                                        PDFont font, float startX, float startY,
                                        float width, float availableHeight) throws IOException {
        float y = startY;
        float x = startX + 10f;
        float maxWidth = width - 20f;

        String[][] fields = {
                {"性格特点 (Personality Traits)", character.personality_traits},
                {"理想 (Ideals)", character.ideals},
                {"羁绊 (Bonds)", character.bonds},
                {"缺陷 (Flaws)", character.flaws},
                {"背景故事 (Background Story)", character.background_story}
        };

        for (String[] field : fields) {
            if (field[1] == null || field[1].trim().isEmpty()) continue;
            if (y < startY - availableHeight + 5f) break;

            PDF_Util.draw_text(cs, font, LABEL_SIZE, field[0], x, y - 8f, PDF_Util.ACCENT_GREEN);
            y -= 14f;
            int lines = PDF_Util.draw_wrapped_text(cs, font, SMALL_SIZE, field[1],
                    x, y - 8f, maxWidth, SMALL_LINE_HEIGHT, PDF_Util.TEXT_DARK);
            y -= lines * SMALL_LINE_HEIGHT + 6f;
        }
    }

    // ------------------------------------------------------------------ //
    //  施法信息
    // ------------------------------------------------------------------ //

    private static void draw_spellcasting_info(PDPageContentStream cs, Character_Sheet character,
                                                PDFont font, PDFont monoFont,
                                                float startX, float startY, float width) throws IOException {
        float y = startY;
        float x = startX + 10f;

        // 施法关键属性
        String castingStat = get_casting_ability(character);
        PDF_Util.draw_text(cs, font, SMALL_SIZE, "施法属性: " + castingStat, x, y - 10f, PDF_Util.TEXT_DARK);
        y -= 14f;

        // 法术位信息
        int[] slots = get_spell_slots(character);
        if (slots != null) {
            StringBuilder slotInfo = new StringBuilder("法术位: ");
            boolean first = true;
            for (int level = 1; level < slots.length; level++) {
                if (slots[level] > 0) {
                    if (!first) slotInfo.append(" / ");
                    slotInfo.append(level).append("环:").append(slots[level]);
                    first = false;
                }
            }
            PDF_Util.draw_text(cs, font, SMALL_SIZE, slotInfo.toString(), x, y - 10f, PDF_Util.TEXT_DARK);
            y -= 14f;
        }

        // 已知法术数量
        int knownCount = get_known_spell_count(character);
        if (knownCount > 0) {
            PDF_Util.draw_text(cs, font, SMALL_SIZE, "已知法术: " + knownCount + " 个", x, y - 10f, PDF_Util.TEXT_DARK);
            y -= 14f;
        }

        // 准备法术列表（仅显示前几个）
        List<String> preparedKeys = get_prepared_spell_keys(character);
        if (preparedKeys != null && !preparedKeys.isEmpty()) {
            PDF_Util.draw_text(cs, font, LABEL_SIZE, "准备法术:", x, y - 8f, PDF_Util.ACCENT_GREEN);
            y -= 12f;
            int maxShow = Math.min(preparedKeys.size(), 4);
            for (int i = 0; i < maxShow; i++) {
                Spell_Definition spell = Spell_Library.get_spell(preparedKeys.get(i));
                if (spell != null) {
                    PDF_Util.draw_text(cs, font, SMALL_SIZE,
                            spell.display_name + " (" + spell.level + "环)", x + 5f, y - 8f, PDF_Util.TEXT_DARK);
                    y -= 11f;
                }
            }
            if (preparedKeys.size() > maxShow) {
                PDF_Util.draw_text(cs, font, SMALL_SIZE,
                        "...及其他 " + (preparedKeys.size() - maxShow) + " 个法术",
                        x + 5f, y - 8f, PDF_Util.TEXT_LIGHT_GRAY);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  负重信息
    // ------------------------------------------------------------------ //

    private static void draw_encumbrance(PDPageContentStream cs, Character_Sheet character,
                                          PDFont font, PDFont monoFont,
                                          float startX, float startY, float width) throws IOException {
        float y = startY;
        float x = startX + 10f;

        Encumbrance_System.Encumbrance_Result result = Encumbrance_System.calculate(character);

        PDF_Util.draw_text(cs, font, SMALL_SIZE, "当前负重: " + result.total_weight_lb + " 磅",
                x, y - 10f, PDF_Util.TEXT_DARK);
        y -= 14f;

        PDF_Util.draw_text(cs, font, SMALL_SIZE, "携带能力: " + result.carry_capacity_lb + " 磅",
                x, y - 10f, PDF_Util.TEXT_DARK);
        y -= 14f;

        PDF_Util.draw_text(cs, font, SMALL_SIZE, "推/拉/举: " + result.push_drag_lift_lb + " 磅",
                x, y - 10f, PDF_Util.TEXT_DARK);
        y -= 14f;

        // 负重等级（带颜色）
        Color levelColor = switch (result.level) {
            case UNENCUMBERED -> PDF_Util.ACCENT_GREEN;
            case LIGHTLY_ENCUMBERED -> PDF_Util.ACCENT_GOLD;
            case HEAVILY_ENCUMBERED -> new Color(200, 120, 0);
            case OVERENCUMBERED -> PDF_Util.HP_RED;
        };
        PDF_Util.draw_text(cs, font, SMALL_SIZE, "负重等级: " + result.level.name,
                x, y - 10f, levelColor);
        y -= 14f;

        int speed = Encumbrance_System.get_effective_speed(character);
        PDF_Util.draw_text(cs, font, SMALL_SIZE, "有效速度: " + speed + " 尺",
                x, y - 10f, PDF_Util.TEXT_DARK);

        // 负重条
        y -= 18f;
        float barWidth = width - 20f;
        float barHeight = 8f;
        float ratio = result.carry_capacity_lb > 0
                ? Math.min(1.0f, (float) result.total_weight_lb / result.carry_capacity_lb) : 1.0f;

        // 背景条
        PDF_Util.draw_rounded_rect(cs, x, y - barHeight, barWidth, barHeight, 3f,
                PDF_Util.TABLE_ROW_ALT, PDF_Util.BORDER_COLOR);
        // 填充条
        if (ratio > 0.01f) {
            PDF_Util.draw_rounded_rect(cs, x, y - barHeight, barWidth * ratio, barHeight, 3f,
                    levelColor, null);
        }
    }

    // ------------------------------------------------------------------ //
    //  辅助绘制方法
    // ------------------------------------------------------------------ //

    /**
     * 绘制一个带标题的区域框。
     */
    private static void draw_section_box(PDPageContentStream cs, float x, float y,
                                          float width, float height, String title,
                                          PDFont font) throws IOException {
        // 浅灰色背景
        PDF_Util.draw_rounded_rect(cs, x, y, width, height, 4f,
                new Color(250, 250, 252), PDF_Util.BORDER_COLOR);

        // 标题背景条
        PDF_Util.draw_rounded_rect(cs, x + 1f, y + height - 18f, width - 2f, 17f, 3f,
                PDF_Util.ACCENT_GREEN, null);

        // 标题文字
        PDF_Util.draw_text(cs, font, LABEL_SIZE, title, x + 8f, y + height - 14f, PDF_Util.TEXT_WHITE);
    }

    /**
     * 绘制页脚。
     */
    private static void draw_footer(PDPageContentStream cs, PDFont font, PDPage page) throws IOException {
        float y = PDF_Util.CONTENT_Y - 5f;
        PDF_Util.draw_line(cs, PDF_Util.CONTENT_X, y + 10f,
                PDF_Util.CONTENT_X + PDF_Util.CONTENT_WIDTH, y + 10f, 0.5f, PDF_Util.BORDER_COLOR);

        String footerText = "DMHelper - D&D 5e Character Sheet";
        float footerWidth = font.getStringWidth(footerText) / 1000f * LABEL_SIZE;
        PDF_Util.draw_text(cs, font, LABEL_SIZE, footerText,
                PDF_Util.CONTENT_X + (PDF_Util.CONTENT_WIDTH - footerWidth) / 2f, y, PDF_Util.TEXT_LIGHT_GRAY);
    }

    // ------------------------------------------------------------------ //
    //  施法相关辅助方法
    // ------------------------------------------------------------------ //

    private static boolean is_spellcaster(Character_Sheet character) {
        return character.job instanceof Wizard_Class
                || character.job instanceof Sorcerer_Class
                || character.job instanceof Warlock_Class
                || character.job instanceof Bard_Class
                || character.job instanceof Paladin_Class;
    }

    private static String get_casting_ability(Character_Sheet character) {
        if (character.job instanceof Wizard_Class) return "Intelligence (智力)";
        if (character.job instanceof Sorcerer_Class) return "Charisma (魅力)";
        if (character.job instanceof Warlock_Class) return "Charisma (魅力)";
        if (character.job instanceof Bard_Class) return "Charisma (魅力)";
        if (character.job instanceof Paladin_Class) return "Charisma (魅力)";
        return "N/A";
    }

    private static int[] get_spell_slots(Character_Sheet character) {
        if (character.job instanceof Wizard_Class wizard) {
            return wizard.spell_slots;
        }
        if (character.job instanceof Sorcerer_Class sorcerer) {
            return sorcerer.spell_slots;
        }
        if (character.job instanceof Bard_Class bard) {
            return bard.spell_slots;
        }
        if (character.job instanceof Paladin_Class paladin) {
            return paladin.spell_slots;
        }
        if (character.job instanceof Warlock_Class warlock) {
            int[] slots = new int[10];
            if (warlock.pact_slot_level > 0 && warlock.pact_slot_level < slots.length) {
                slots[warlock.pact_slot_level] = warlock.current_pact_slot_count;
            }
            return slots;
        }
        return null;
    }

    private static int get_known_spell_count(Character_Sheet character) {
        if (character.job instanceof Wizard_Class wizard) {
            return wizard.spellbook_spell_keys.size();
        }
        if (character.job instanceof Sorcerer_Class sorcerer) {
            return sorcerer.known_spell_keys.size();
        }
        if (character.job instanceof Bard_Class bard) {
            return bard.known_spell_keys.size();
        }
        if (character.job instanceof Warlock_Class warlock) {
            return warlock.known_spell_keys.size();
        }
        if (character.job instanceof Paladin_Class paladin) {
            return paladin.prepared_spell_keys.size();
        }
        return 0;
    }

    private static List<String> get_prepared_spell_keys(Character_Sheet character) {
        if (character.job instanceof Wizard_Class wizard) {
            return wizard.prepared_spell_keys;
        }
        if (character.job instanceof Paladin_Class paladin) {
            return paladin.prepared_spell_keys;
        }
        return null;
    }

    // ------------------------------------------------------------------ //
    //  通用辅助方法
    // ------------------------------------------------------------------ //

    private static boolean needs_second_page(Character_Sheet character) {
        if (character.owned_equipment_keys != null && !character.owned_equipment_keys.isEmpty()) {
            return true;
        }
        if (character.background_story != null && !character.background_story.trim().isEmpty()) {
            return true;
        }
        if (character.personality_traits != null && !character.personality_traits.trim().isEmpty()) {
            return true;
        }
        if (!character.job.get_feature_summaries().isEmpty()) {
            return true;
        }
        if (!character.race.get_feature_summaries().isEmpty()) {
            return true;
        }
        return false;
    }

    private static String format_modifier(int mod) {
        if (mod >= 0) {
            return "+" + mod;
        }
        return String.valueOf(mod);
    }
}
