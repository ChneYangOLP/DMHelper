package com.DMHelper.basic.menus;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Slot;

import javax.swing.*;
import java.awt.*;

public class Character_Summary_UI extends JFrame {

    public Character_Summary_UI(Character_Sheet character) {
        setTitle("角色数据总览 - " + character.name);
        setSize(720, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel rootPanel = new JPanel(new BorderLayout(12, 12));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        JLabel titleLabel = new JLabel(character.name + " 的角色总览");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(Ui_Theme.ACCENT_PRIMARY);
        JLabel subtitleLabel = new JLabel(character.race.race_name + " / " + character.job.class_name
                + " / 等级 " + character.job.current_level);
        subtitleLabel.setForeground(Ui_Theme.TEXT_MUTED);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        JTextArea info_area = new JTextArea();
        info_area.setEditable(false);
        info_area.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        info_area.setMargin(new Insets(20, 20, 20, 20));

        StringBuilder sb = new StringBuilder();
        sb.append("=== 基础信息 ===\n");
        sb.append("姓名: ").append(character.name).append("\n");
        sb.append("年龄/性别: ").append(character.age).append(" / ").append(character.gender).append("\n");
        sb.append("种族: ").append(character.race.race_name).append("\n");
        sb.append("职业: ").append(character.job.class_name).append(" (等级 ").append(character.job.current_level).append(")\n");
        sb.append("子职业: ").append(character.job.get_subclass_name()).append("\n");
        sb.append("经验值: ").append(character.experience_points).append("\n\n");

        sb.append("=== 战斗属性 ===\n");
        sb.append("生命值 (HP): ").append(character.get_hp_summary()).append("\n");
        sb.append("护甲等级 (AC): ").append(character.ac).append("\n");
        sb.append("熟练加值 (PB): +").append(character.get_proficiency_bonus()).append("\n\n");

        sb.append("=== 装备与熟练 ===\n");
        append_equipped_item(sb, "护甲", character.get_equipped_item(Equipment_Slot.ARMOR));
        append_equipped_item(sb, "主手武器", character.get_equipped_item(Equipment_Slot.MAIN_HAND));
        append_equipped_item(sb, "副手/盾牌", character.get_equipped_item(Equipment_Slot.OFF_HAND));
        append_equipped_item(sb, "披风", character.get_equipped_item(Equipment_Slot.CLOAK));
        append_equipped_item(sb, "护符", character.get_equipped_item(Equipment_Slot.ACCESSORY));
        sb.append("武器/护甲熟练: ").append(String.join(", ", character.job.equipment_proficiencies)).append("\n\n");

        sb.append("=== 豁免检定加值 ===\n");
        sb.append(String.format("力量: %+d | 敏捷: %+d | 体质: %+d\n",
                character.get_saving_throw_bonus("Strength"),
                character.get_saving_throw_bonus("Dexterity"),
                character.get_saving_throw_bonus("Constitution")));
        sb.append(String.format("智力: %+d | 感知: %+d | 魅力: %+d\n\n",
                character.get_saving_throw_bonus("Intelligence"),
                character.get_saving_throw_bonus("Wisdom"),
                character.get_saving_throw_bonus("Charisma")));

        sb.append("=== 已熟练技能检定 ===\n");
        if (character.job.skill_proficiencies.isEmpty()) {
            sb.append("尚未选择技能熟练项。\n");
        } else {
            for (String skill : character.job.skill_proficiencies) {
                sb.append(String.format("- %s: %+d\n", skill, character.get_skill_bonus(skill)));
            }
        }
        sb.append("\n");

        sb.append("=== 职业特性与专长 ===\n");
        for (String feature : character.job.get_feature_summaries()) {
            sb.append("- ").append(feature).append("\n");
        }
        sb.append("\n");

        sb.append("=== 背景与性格 ===\n");
        append_profile_line(sb, "背景故事", character.background_story);
        append_profile_line(sb, "性格特点", character.personality_traits);
        append_profile_line(sb, "理想信念", character.ideals);
        append_profile_line(sb, "羁绊关系", character.bonds);
        append_profile_line(sb, "缺陷弱点", character.flaws);
        sb.append("\n");

        sb.append("=== 升级记录 ===\n");
        if (character.advancement_notes.isEmpty()) {
            sb.append("- 暂无\n");
        } else {
            for (String note : character.advancement_notes) {
                sb.append("- ").append(note).append("\n");
            }
        }
        sb.append("\n");

        sb.append("=== 六维数据 ===\n");
        sb.append(String.format("力量: %d (调整值: %+d)\n", character.stats.str, character.stats.get_mod(character.stats.str)));
        sb.append(String.format("敏捷: %d (调整值: %+d)\n", character.stats.dex, character.stats.get_mod(character.stats.dex)));
        sb.append(String.format("体质: %d (调整值: %+d)\n", character.stats.con, character.stats.get_mod(character.stats.con)));
        sb.append(String.format("智力: %d (调整值: %+d)\n", character.stats.intel, character.stats.get_mod(character.stats.intel)));
        sb.append(String.format("感知: %d (调整值: %+d)\n", character.stats.wis, character.stats.get_mod(character.stats.wis)));
        sb.append(String.format("魅力: %d (调整值: %+d)\n", character.stats.cha, character.stats.get_mod(character.stats.cha)));

        info_area.setText(sb.toString());
        info_area.setCaretPosition(0);

        JScrollPane scroll_pane = Ui_Theme.wrap_scroll(info_area);
        scroll_pane.setBorder(Ui_Theme.create_section_border("角色详情"));

        JPanel btn_panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton continue_btn = new JButton("继续创建角色");
        JButton back_btn = new JButton("返回主界面");
        Ui_Theme.style_secondary_button(continue_btn);
        Ui_Theme.style_primary_button(back_btn);

        continue_btn.addActionListener(e -> {
            new Create_Character_UI().setVisible(true);
            this.dispose();
        });

        back_btn.addActionListener(e -> this.dispose());

        btn_panel.add(continue_btn);
        btn_panel.add(back_btn);
        rootPanel.add(headerPanel, BorderLayout.NORTH);
        rootPanel.add(scroll_pane, BorderLayout.CENTER);
        rootPanel.add(btn_panel, BorderLayout.SOUTH);
        add(rootPanel);
        Ui_Theme.apply_window(this);
    }

    private void append_equipped_item(StringBuilder sb, String title, Equipment_Item item) {
        sb.append(title).append(": ");
        if (item == null) {
            sb.append("空置\n");
        } else {
            sb.append(item.display_name).append(" | ").append(item.description).append("\n");
        }
    }

    private void append_profile_line(StringBuilder sb, String title, String value) {
        sb.append(title).append(": ").append(value == null || value.trim().isEmpty() ? "未填写" : value).append("\n");
    }
}
