package com.DMDHelper.basic.menus;

import com.DMDHelper.basic.Character_Sheet;
import com.DMDHelper.basic.Class.Fighter.Fighter_Class;
import com.DMDHelper.basic.Class.wizard.Wizard_Class;
import com.DMDHelper.basic.armor.Armor;
import com.DMDHelper.basic.database.Character_DAO;
import com.DMDHelper.basic.database.Global_Data;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Character_Manager_UI extends JFrame {

    private Character_Sheet current_char;

    private JComboBox<String> char_selector;
    private JTextArea stats_area;
    private JTextArea level_info_area;
    private JTextArea inventory_area;

    private JComboBox<String> armor_box;
    private JCheckBox shield_check;
    private JButton level_up_btn;
    private JButton add_xp_btn;

    public Character_Manager_UI() {
        setTitle("全功能角色管理控制台");
        setSize(640, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        current_char = Global_Data.character_pool.get(0);

        JPanel top_panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top_panel.add(new JLabel("当前管理角色: "));
        char_selector = new JComboBox<>();
        reload_selector_items();
        top_panel.add(char_selector);

        char_selector.addActionListener(e -> {
            int idx = char_selector.getSelectedIndex();
            if (idx >= 0) {
                current_char = Global_Data.character_pool.get(idx);
                refresh_ui();
            }
        });

        JTabbedPane tabbed_pane = new JTabbedPane();
        tabbed_pane.setFont(new Font("微软雅黑", Font.BOLD, 14));

        JPanel stats_panel = new JPanel(new BorderLayout());
        stats_area = new JTextArea();
        stats_area.setEditable(false);
        stats_area.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        stats_area.setMargin(new Insets(15, 15, 15, 15));
        stats_panel.add(new JScrollPane(stats_area), BorderLayout.CENTER);
        tabbed_pane.addTab("基础与属性", stats_panel);

        JPanel equip_panel = new JPanel(new BorderLayout());
        JPanel armor_select_panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        armor_select_panel.add(new JLabel("更换护甲:"));
        String[] armors = {"普通衣服 (None, AC:10)", "皮甲 (Light, AC:11)", "半身甲 (Medium, AC:15)", "锁子甲 (Heavy, AC:16)"};
        armor_box = new JComboBox<>(armors);
        shield_check = new JCheckBox("装备盾牌 (+2 AC)");
        JButton equip_btn = new JButton("确认换装");

        armor_select_panel.add(armor_box);
        armor_select_panel.add(shield_check);
        armor_select_panel.add(equip_btn);

        inventory_area = new JTextArea();
        inventory_area.setEditable(false);
        inventory_area.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        inventory_area.setMargin(new Insets(15, 15, 15, 15));

        equip_panel.add(armor_select_panel, BorderLayout.NORTH);
        equip_panel.add(new JScrollPane(inventory_area), BorderLayout.CENTER);
        tabbed_pane.addTab("装备与物品", equip_panel);

        JPanel progression_panel = new JPanel(new BorderLayout());
        level_info_area = new JTextArea();
        level_info_area.setEditable(false);
        level_info_area.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        level_info_area.setMargin(new Insets(15, 15, 15, 15));

        JPanel level_btn_panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        add_xp_btn = new JButton("添加经验值");
        level_up_btn = new JButton("执行升级");
        level_up_btn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        level_up_btn.setForeground(Color.RED);
        level_btn_panel.add(add_xp_btn);
        level_btn_panel.add(level_up_btn);

        progression_panel.add(new JScrollPane(level_info_area), BorderLayout.CENTER);
        progression_panel.add(level_btn_panel, BorderLayout.SOUTH);
        tabbed_pane.addTab("成长与升级", progression_panel);

        equip_btn.addActionListener(e -> handle_equip());
        add_xp_btn.addActionListener(e -> handle_add_experience());
        level_up_btn.addActionListener(e -> handle_level_up());

        add(top_panel, BorderLayout.NORTH);
        add(tabbed_pane, BorderLayout.CENTER);

        refresh_ui();
    }

    private void handle_equip() {
        String selected = (String) armor_box.getSelectedItem();
        Armor newArmor = new Armor("普通衣服", "None", 10);
        if (selected != null && selected.contains("Light")) {
            newArmor = new Armor("皮甲", "Light", 11);
        } else if (selected != null && selected.contains("Medium")) {
            newArmor = new Armor("半身甲", "Medium", 15);
        } else if (selected != null && selected.contains("Heavy")) {
            newArmor = new Armor("锁子甲", "Heavy", 16);
        }

        current_char.set_equipment(newArmor, shield_check.isSelected());
        Character_DAO.update_character(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "换装成功，当前 AC 为 " + current_char.ac + "。");
    }

    private void handle_add_experience() {
        String input = JOptionPane.showInputDialog(this, "请输入要增加的经验值：", "添加经验值", JOptionPane.PLAIN_MESSAGE);
        if (input == null) {
            return;
        }

        try {
            int xp = Integer.parseInt(input.trim());
            if (xp <= 0) {
                JOptionPane.showMessageDialog(this, "经验值必须是正整数。");
                return;
            }

            current_char.add_experience(xp);
            Character_DAO.update_character(current_char);
            refresh_ui();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "请输入合法的数字经验值。");
        }
    }

    private void handle_level_up() {
        if (!current_char.can_level_up()) {
            JOptionPane.showMessageDialog(this, "当前经验值不足，暂时还不能升级。");
            return;
        }
        if (current_char.job.current_level >= 20) {
            JOptionPane.showMessageDialog(this, "角色已达到最高等级。");
            return;
        }

        int nextLevel = current_char.job.current_level + 1;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确认将 [" + current_char.name + "] 提升至 " + nextLevel + " 级吗？",
                "升级确认",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        current_char.job.level_up(nextLevel);
        current_char.record_advancement("角色升级至 " + nextLevel + " 级");
        Character_Advancement_Helper.process_pending_choices(this, current_char);
        current_char.recalculate_derived_stats();
        Character_DAO.update_character(current_char);
        reload_selector_items();
        char_selector.setSelectedIndex(Global_Data.character_pool.indexOf(current_char));
        refresh_ui();
        JOptionPane.showMessageDialog(this, "升级完成，新的职业能力与选择已保存。");
    }

    private void reload_selector_items() {
        if (char_selector == null) {
            return;
        }

        char_selector.removeAllItems();
        for (Character_Sheet character : Global_Data.character_pool) {
            char_selector.addItem(character.name + " (" + character.job.class_name + " LV." + character.job.current_level + ")");
        }
    }

    private void refresh_ui() {
        current_char.recalculate_derived_stats();

        StringBuilder sb_stats = new StringBuilder();
        sb_stats.append("姓名: ").append(current_char.name).append(" | 种族: ").append(current_char.race.race_name).append("\n");
        sb_stats.append("职业: ").append(current_char.job.class_name).append(" (LV.").append(current_char.job.current_level).append(")\n");
        sb_stats.append("年龄/性别: ").append(current_char.age).append(" / ").append(current_char.gender).append("\n");
        sb_stats.append("经验值: ").append(current_char.experience_points).append("\n");
        sb_stats.append("--------------------------------------------------\n");
        sb_stats.append("当前最大 HP: ").append(current_char.hp).append("\n");
        sb_stats.append("当前护甲 AC: ").append(current_char.ac).append(" (防具: ").append(current_char.equipped_armor.armor_name).append(")\n");
        sb_stats.append("熟练加值 PB: +").append(current_char.get_proficiency_bonus()).append("\n");
        sb_stats.append("子职业: ").append(current_char.job.get_subclass_name()).append("\n");
        sb_stats.append("--------------------------------------------------\n");
        sb_stats.append("[豁免检定加值]\n");
        sb_stats.append(String.format("力量: %+d | 敏捷: %+d | 体质: %+d\n",
                current_char.get_saving_throw_bonus("Strength"),
                current_char.get_saving_throw_bonus("Dexterity"),
                current_char.get_saving_throw_bonus("Constitution")));
        sb_stats.append(String.format("智力: %+d | 感知: %+d | 魅力: %+d\n",
                current_char.get_saving_throw_bonus("Intelligence"),
                current_char.get_saving_throw_bonus("Wisdom"),
                current_char.get_saving_throw_bonus("Charisma")));
        sb_stats.append("--------------------------------------------------\n");
        sb_stats.append(String.format("力量 %d (%+d) | 敏捷 %d (%+d) | 体质 %d (%+d)\n",
                current_char.stats.str, current_char.stats.get_mod(current_char.stats.str),
                current_char.stats.dex, current_char.stats.get_mod(current_char.stats.dex),
                current_char.stats.con, current_char.stats.get_mod(current_char.stats.con)));
        sb_stats.append(String.format("智力 %d (%+d) | 感知 %d (%+d) | 魅力 %d (%+d)\n",
                current_char.stats.intel, current_char.stats.get_mod(current_char.stats.intel),
                current_char.stats.wis, current_char.stats.get_mod(current_char.stats.wis),
                current_char.stats.cha, current_char.stats.get_mod(current_char.stats.cha)));
        stats_area.setText(sb_stats.toString());
        stats_area.setCaretPosition(0);

        shield_check.setSelected(current_char.has_shield);
        String armorName = current_char.equipped_armor.armor_name;
        if (armorName.contains("衣服") || armorName.contains("长袍")) {
            armor_box.setSelectedIndex(0);
        } else if (armorName.contains("皮甲")) {
            armor_box.setSelectedIndex(1);
        } else if (armorName.contains("半身甲")) {
            armor_box.setSelectedIndex(2);
        } else if (armorName.contains("锁子甲")) {
            armor_box.setSelectedIndex(3);
        }

        StringBuilder inventory = new StringBuilder();
        inventory.append("【").append(current_char.name).append(" 的装备状态】\n");
        inventory.append("当前护甲: ").append(current_char.equipped_armor.armor_name).append("\n");
        inventory.append("盾牌状态: ").append(current_char.has_shield ? "已装备" : "未装备").append("\n\n");
        inventory.append("【职业熟练】\n");
        inventory.append(String.join("、", current_char.job.equipment_proficiencies)).append("\n\n");
        inventory.append("【技能熟练】\n");
        if (current_char.job.skill_proficiencies.isEmpty()) {
            inventory.append("尚未选择\n");
        } else {
            inventory.append(String.join("、", current_char.job.skill_proficiencies)).append("\n");
        }
        inventory_area.setText(inventory.toString());
        inventory_area.setCaretPosition(0);

        StringBuilder progression = new StringBuilder();
        progression.append("当前等级: ").append(current_char.job.current_level).append("\n");
        progression.append("当前经验值: ").append(current_char.experience_points).append("\n");
        if (current_char.job.current_level < 20) {
            progression.append("下一级所需经验值: ").append(current_char.get_next_level_xp()).append("\n");
            progression.append("距离升级还差: ").append(current_char.get_xp_to_next_level()).append("\n");
        } else {
            progression.append("已达到最高等级。\n");
        }

        progression.append("\n【当前职业特性】\n");
        List<String> features = current_char.job.get_feature_summaries();
        if (features.isEmpty()) {
            progression.append("- 暂无\n");
        } else {
            for (String feature : features) {
                progression.append("- ").append(feature).append("\n");
            }
        }

        progression.append("\n【待处理升级选择】\n");
        List<String> pendingChoices = current_char.job.get_pending_choices();
        if (pendingChoices.isEmpty()) {
            progression.append("- 暂无\n");
        } else {
            for (String pendingChoice : pendingChoices) {
                progression.append("- ").append(pendingChoice).append("\n");
            }
        }

        progression.append("\n【升级记录】\n");
        if (current_char.advancement_notes.isEmpty()) {
            progression.append("- 暂无\n");
        } else {
            for (String note : current_char.advancement_notes) {
                progression.append("- ").append(note).append("\n");
            }
        }

        if (current_char.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) current_char.job;
            progression.append("\n【战士资源】\n");
            progression.append("动作如潮次数: ").append(fighter.action_surge_uses).append("\n");
            progression.append("不屈次数: ").append(fighter.indomitable_uses).append("\n");
            progression.append("每次攻击动作攻击次数: ").append(fighter.attacks_per_action).append("\n");
            if (fighter.fighter_subclass == com.DMDHelper.basic.Class.Fighter.Fighter_Subclass.BATTLE_MASTER) {
                progression.append("卓越骰: ").append(fighter.superiority_dice).append(" 颗 d").append(fighter.superiority_dice_type).append("\n");
            }
        } else if (current_char.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current_char.job;
            progression.append("\n【法师资源】\n");
            progression.append(wizard.get_spell_slot_summary()).append("\n");
        }

        level_info_area.setText(progression.toString());
        level_info_area.setCaretPosition(0);

        boolean canLevelUp = current_char.can_level_up();
        level_up_btn.setEnabled(canLevelUp);
        level_up_btn.setText(canLevelUp ? "执行升级" : "经验不足，暂不可升级");
    }
}
