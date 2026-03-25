package com.DMDHelper.basic.menus;

import com.DMDHelper.basic.Character_Sheet;
import com.DMDHelper.basic.database.Character_DAO;
import com.DMDHelper.basic.database.Global_Data;
import com.DMDHelper.basic.equipment.Equipment_Item;
import com.DMDHelper.basic.equipment.Equipment_Slot;
import com.DMDHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMDHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMDHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMDHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMDHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMDHelper.basic.spell.Spell_Library;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Character_Manager_UI extends JFrame {

    private Character_Sheet current_char;
    private boolean is_reloading_selector;

    private JComboBox<String> char_selector;
    private JTextArea stats_area;
    private JTextArea level_info_area;
    private JTextArea inventory_area;
    private JTextArea spellcasting_area;

    private JComboBox<EquipmentChoice> armor_box;
    private JComboBox<EquipmentChoice> main_hand_box;
    private JComboBox<EquipmentChoice> off_hand_box;
    private JComboBox<EquipmentChoice> cloak_box;
    private JComboBox<EquipmentChoice> accessory_box;
    private JButton level_up_btn;
    private JButton add_xp_btn;
    private JButton long_rest_btn;
    private JButton manage_spellbook_btn;
    private JButton manage_spell_selection_btn;
    private JButton manage_prepared_spell_btn;

    public Character_Manager_UI() {
        setTitle("全功能角色管理控制台");
        setSize(760, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        current_char = Global_Data.character_pool.get(0);

        JPanel top_panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        top_panel.add(new JLabel("当前管理角色: "));
        char_selector = new JComboBox<>();
        reload_selector_items(current_char);
        top_panel.add(char_selector);

        char_selector.addActionListener(e -> {
            if (is_reloading_selector) {
                return;
            }
            int idx = char_selector.getSelectedIndex();
            if (idx >= 0) {
                current_char = Global_Data.character_pool.get(idx);
                refresh_ui();
            }
        });

        JTabbedPane tabbed_pane = new JTabbedPane();
        tabbed_pane.setFont(new Font("微软雅黑", Font.BOLD, 14));

        JPanel stats_panel = new JPanel(new BorderLayout());
        stats_area = build_text_area();
        stats_panel.add(new JScrollPane(stats_area), BorderLayout.CENTER);
        tabbed_pane.addTab("基础与属性", stats_panel);

        JPanel equip_panel = new JPanel(new BorderLayout());
        JPanel equipment_slot_panel = new JPanel(new GridLayout(5, 2, 10, 10));
        equipment_slot_panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        armor_box = new JComboBox<>();
        main_hand_box = new JComboBox<>();
        off_hand_box = new JComboBox<>();
        cloak_box = new JComboBox<>();
        accessory_box = new JComboBox<>();
        equipment_slot_panel.add(new JLabel("护甲槽位"));
        equipment_slot_panel.add(armor_box);
        equipment_slot_panel.add(new JLabel("主手武器"));
        equipment_slot_panel.add(main_hand_box);
        equipment_slot_panel.add(new JLabel("副手/盾牌"));
        equipment_slot_panel.add(off_hand_box);
        equipment_slot_panel.add(new JLabel("披风"));
        equipment_slot_panel.add(cloak_box);
        equipment_slot_panel.add(new JLabel("护符"));
        equipment_slot_panel.add(accessory_box);
        JButton equip_btn = new JButton("应用当前装备");
        inventory_area = build_text_area();
        equip_panel.add(equipment_slot_panel, BorderLayout.NORTH);
        equip_panel.add(new JScrollPane(inventory_area), BorderLayout.CENTER);
        equip_panel.add(equip_btn, BorderLayout.SOUTH);
        tabbed_pane.addTab("装备与物品", equip_panel);

        JPanel spell_panel = new JPanel(new BorderLayout());
        spellcasting_area = build_text_area();
        JPanel spell_btn_panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        manage_spellbook_btn = new JButton("管理戏法");
        manage_spell_selection_btn = new JButton("管理法术");
        manage_prepared_spell_btn = new JButton("管理准备法术");
        spell_btn_panel.add(manage_spellbook_btn);
        spell_btn_panel.add(manage_spell_selection_btn);
        spell_btn_panel.add(manage_prepared_spell_btn);
        spell_panel.add(new JScrollPane(spellcasting_area), BorderLayout.CENTER);
        spell_panel.add(spell_btn_panel, BorderLayout.SOUTH);
        tabbed_pane.addTab("施法与法术", spell_panel);

        JPanel progression_panel = new JPanel(new BorderLayout());
        level_info_area = build_text_area();
        JPanel level_btn_panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        add_xp_btn = new JButton("添加经验值");
        long_rest_btn = new JButton("进行长休");
        level_up_btn = new JButton("执行升级");
        level_up_btn.setFont(new Font("微软雅黑", Font.BOLD, 16));
        level_up_btn.setForeground(Color.RED);
        level_btn_panel.add(add_xp_btn);
        level_btn_panel.add(long_rest_btn);
        level_btn_panel.add(level_up_btn);
        progression_panel.add(new JScrollPane(level_info_area), BorderLayout.CENTER);
        progression_panel.add(level_btn_panel, BorderLayout.SOUTH);
        tabbed_pane.addTab("成长与升级", progression_panel);

        equip_btn.addActionListener(e -> handle_equip());
        add_xp_btn.addActionListener(e -> handle_add_experience());
        long_rest_btn.addActionListener(e -> handle_long_rest());
        level_up_btn.addActionListener(e -> handle_level_up());
        manage_spellbook_btn.addActionListener(e -> handle_manage_spellbook());
        manage_spell_selection_btn.addActionListener(e -> handle_manage_spell_selection());
        manage_prepared_spell_btn.addActionListener(e -> handle_manage_prepared_spells());

        add(top_panel, BorderLayout.NORTH);
        add(tabbed_pane, BorderLayout.CENTER);

        refresh_ui();
    }

    private JTextArea build_text_area() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("微软雅黑", Font.PLAIN, 15));
        area.setMargin(new Insets(15, 15, 15, 15));
        return area;
    }

    private void handle_equip() {
        current_char.equip_item(Equipment_Slot.ARMOR, get_selected_equipment_key(armor_box));
        current_char.equip_item(Equipment_Slot.MAIN_HAND, get_selected_equipment_key(main_hand_box));
        current_char.equip_item(Equipment_Slot.OFF_HAND, get_selected_equipment_key(off_hand_box));
        current_char.equip_item(Equipment_Slot.CLOAK, get_selected_equipment_key(cloak_box));
        current_char.equip_item(Equipment_Slot.ACCESSORY, get_selected_equipment_key(accessory_box));
        Character_DAO.update_character(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "装备已更新，当前 AC 为 " + current_char.ac + "。");
    }

    private String get_selected_equipment_key(JComboBox<EquipmentChoice> box) {
        EquipmentChoice choice = (EquipmentChoice) box.getSelectedItem();
        return choice == null ? "" : choice.item_key;
    }

    private void reload_equipment_boxes() {
        reload_slot_box(armor_box, Equipment_Slot.ARMOR, current_char.equipped_armor_key, false);
        reload_slot_box(main_hand_box, Equipment_Slot.MAIN_HAND, current_char.equipped_main_hand_key, false);
        reload_slot_box(off_hand_box, Equipment_Slot.OFF_HAND, current_char.equipped_off_hand_key, true);
        reload_slot_box(cloak_box, Equipment_Slot.CLOAK, current_char.equipped_cloak_key, true);
        reload_slot_box(accessory_box, Equipment_Slot.ACCESSORY, current_char.equipped_accessory_key, true);
    }

    private void reload_slot_box(JComboBox<EquipmentChoice> box, Equipment_Slot slot, String equippedKey, boolean allowEmpty) {
        box.removeAllItems();
        if (allowEmpty) {
            box.addItem(new EquipmentChoice("", "空置", "不装备此槽位物品。"));
        }
        for (Equipment_Item item : current_char.get_owned_items_for_slot(slot)) {
            box.addItem(new EquipmentChoice(item.key, item.display_name, item.description));
        }
        select_choice(box, equippedKey);
    }

    private void select_choice(JComboBox<EquipmentChoice> box, String equippedKey) {
        for (int i = 0; i < box.getItemCount(); i++) {
            EquipmentChoice choice = box.getItemAt(i);
            if (choice != null && choice.item_key.equals(equippedKey == null ? "" : equippedKey)) {
                box.setSelectedIndex(i);
                return;
            }
        }
        if (box.getItemCount() > 0) {
            box.setSelectedIndex(0);
        }
    }

    private void handle_add_experience() {
        String input = JOptionPane.showInputDialog(this, "请输入要增加的经验值：", "添加经验值", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;

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

    private void handle_long_rest() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "确认让 [" + current_char.name + "] 完成一次长休吗？\n这会把当前系统中的生命值、法术位与职业资源视为恢复到完整状态。",
                "长休确认",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        current_char.take_long_rest();
        Character_DAO.update_character(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "长休完成，角色已恢复到完整状态。");
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
        reload_selector_items(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "升级完成，新的职业能力与选择已保存。");
    }

    private void handle_manage_spellbook() {
        if (current_char.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理法师戏法",
                    "法师戏法已知数固定为 " + wizard.cantrips_known + "。",
                    Spell_Library.get_wizard_cantrip_keys(),
                    wizard.known_cantrip_keys,
                    wizard.cantrips_known,
                    wizard.cantrips_known
            );
            wizard.set_known_cantrips(selected);
            current_char.record_advancement("调整法师戏法，当前数量：" + wizard.known_cantrip_keys.size());
            current_char.recalculate_derived_stats();
            Character_DAO.update_character(current_char);
            refresh_ui();
        } else if (current_char.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理术士戏法",
                    "术士戏法已知上限：" + sorcerer.cantrips_known + "。",
                    Spell_Library.get_sorcerer_cantrip_keys(),
                    sorcerer.known_cantrip_keys,
                    sorcerer.cantrips_known,
                    sorcerer.cantrips_known
            );
            sorcerer.set_known_cantrips(selected);
            current_char.record_advancement("调整术士戏法，当前数量：" + sorcerer.known_cantrip_keys.size());
            current_char.recalculate_derived_stats();
            Character_DAO.update_character(current_char);
            refresh_ui();
        } else if (current_char.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理邪术士戏法",
                    "邪术士戏法已知上限：" + warlock.cantrips_known + "。",
                    Spell_Library.get_warlock_cantrip_keys(),
                    warlock.known_cantrip_keys,
                    warlock.cantrips_known,
                    warlock.cantrips_known
            );
            warlock.set_known_cantrips(selected);
            current_char.record_advancement("调整邪术士戏法，当前数量：" + warlock.known_cantrip_keys.size());
            current_char.recalculate_derived_stats();
            Character_DAO.update_character(current_char);
            refresh_ui();
        }
    }

    private void handle_manage_spell_selection() {
        if (current_char.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理法术书",
                    "法师法术书容量上限：" + wizard.spells_in_spellbook + "。当前最高可学习 " + wizard.get_max_spell_level() + " 环法术。",
                    Spell_Library.get_wizard_spell_keys_up_to_level(wizard.get_max_spell_level()),
                    wizard.spellbook_spell_keys,
                    wizard.spells_in_spellbook
            );
            wizard.spellbook_spell_keys.clear();
            wizard.spellbook_spell_keys.addAll(selected);
            current_char.record_advancement("整理法术书，当前记录法术数：" + wizard.spellbook_spell_keys.size());
        } else if (current_char.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理已知法术",
                    "术士已知法术上限：" + sorcerer.spells_known_count + "，当前最高可学 " + sorcerer.get_max_spell_level() + " 环法术。",
                    Spell_Library.get_sorcerer_spell_keys_up_to_level(sorcerer.get_max_spell_level()),
                    sorcerer.known_spell_keys,
                    sorcerer.spells_known_count,
                    sorcerer.spells_known_count
            );
            sorcerer.set_known_spells(selected);
            current_char.record_advancement("调整术士已知法术，当前数量：" + sorcerer.known_spell_keys.size());
        } else if (current_char.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理邪术士已知法术",
                    "邪术士已知法术上限：" + warlock.spells_known_count + "，当前契约法术最高按 " + warlock.pact_slot_level + " 环施放。",
                    Spell_Library.get_warlock_spell_keys_up_to_level(warlock.pact_slot_level),
                    warlock.known_spell_keys,
                    warlock.spells_known_count,
                    warlock.spells_known_count
            );
            warlock.set_known_spells(selected);
            current_char.record_advancement("调整邪术士已知法术，当前数量：" + warlock.known_spell_keys.size());
        }

        current_char.recalculate_derived_stats();
        Character_DAO.update_character(current_char);
        refresh_ui();
    }

    private void handle_manage_prepared_spells() {
        if (current_char.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current_char.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理准备法术",
                    "你可准备 " + wizard.get_prepared_spell_count(current_char.stats.get_mod(current_char.stats.intel)) + " 个法术。",
                    new ArrayList<>(wizard.spellbook_spell_keys),
                    wizard.prepared_spell_keys,
                    wizard.get_prepared_spell_count(current_char.stats.get_mod(current_char.stats.intel))
            );
            wizard.set_prepared_spells(selected, current_char.stats.get_mod(current_char.stats.intel));
            current_char.record_advancement("调整准备法术，当前准备数量：" + wizard.prepared_spell_keys.size());
        } else if (current_char.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) current_char.job;
            int preparedCount = paladin.get_prepared_spell_count(current_char.stats.get_mod(current_char.stats.cha));
            List<String> selected = Spell_Management_Helper.open_selection_dialog(
                    this,
                    "管理圣武士准备法术",
                    "你可准备 " + preparedCount + " 个法术。",
                    paladin.get_available_spell_options(),
                    paladin.prepared_spell_keys,
                    preparedCount
            );
            paladin.set_prepared_spells(selected, current_char.stats.get_mod(current_char.stats.cha));
            current_char.record_advancement("调整圣武士准备法术，当前数量：" + paladin.prepared_spell_keys.size());
        }

        current_char.recalculate_derived_stats();
        Character_DAO.update_character(current_char);
        refresh_ui();
    }

    private void reload_selector_items(Character_Sheet selectedCharacter) {
        if (char_selector == null) return;
        is_reloading_selector = true;
        char_selector.removeAllItems();
        int selectedIndex = 0;
        int idx = 0;
        for (Character_Sheet character : Global_Data.character_pool) {
            char_selector.addItem(character.name + " (" + character.job.class_name + " LV." + character.job.current_level + ")");
            if (character == selectedCharacter) selectedIndex = idx;
            idx++;
        }
        if (!Global_Data.character_pool.isEmpty()) {
            char_selector.setSelectedIndex(selectedIndex);
            current_char = Global_Data.character_pool.get(selectedIndex);
        }
        is_reloading_selector = false;
    }

    private void refresh_ui() {
        current_char.recalculate_derived_stats();

        StringBuilder sb_stats = new StringBuilder();
        sb_stats.append("姓名: ").append(current_char.name).append(" | 种族: ").append(current_char.race.race_name).append("\n");
        sb_stats.append("职业: ").append(current_char.job.class_name).append(" (LV.").append(current_char.job.current_level).append(")\n");
        sb_stats.append("年龄/性别: ").append(current_char.age).append(" / ").append(current_char.gender).append("\n");
        sb_stats.append("经验值: ").append(current_char.experience_points).append("\n");
        sb_stats.append("--------------------------------------------------\n");
        sb_stats.append("当前 HP: ").append(current_char.get_hp_summary()).append("\n");
        sb_stats.append("当前护甲 AC: ").append(current_char.ac).append(" (护甲: ").append(current_char.equipped_armor.armor_name).append(")\n");
        Equipment_Item weaponItem = current_char.get_equipped_item(Equipment_Slot.MAIN_HAND);
        sb_stats.append("当前主手: ").append(weaponItem == null ? "空置" : weaponItem.display_name).append("\n");
        sb_stats.append("熟练加值 PB: +").append(current_char.get_proficiency_bonus()).append("\n");
        sb_stats.append("子职业: ").append(current_char.job.get_subclass_name()).append("\n");
        if (has_profile_content()) {
            sb_stats.append("--------------------------------------------------\n");
            if (!current_char.background_story.trim().isEmpty()) sb_stats.append("背景故事: ").append(current_char.background_story).append("\n");
            if (!current_char.personality_traits.trim().isEmpty()) sb_stats.append("性格特点: ").append(current_char.personality_traits).append("\n");
            if (!current_char.ideals.trim().isEmpty()) sb_stats.append("理想信念: ").append(current_char.ideals).append("\n");
            if (!current_char.bonds.trim().isEmpty()) sb_stats.append("羁绊关系: ").append(current_char.bonds).append("\n");
            if (!current_char.flaws.trim().isEmpty()) sb_stats.append("缺陷弱点: ").append(current_char.flaws).append("\n");
        }
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
        stats_area.setText(sb_stats.toString());
        stats_area.setCaretPosition(0);

        reload_equipment_boxes();

        StringBuilder inventory = new StringBuilder();
        inventory.append("【").append(current_char.name).append(" 的装备面板】\n");
        inventory.append("护甲槽位: ").append(get_equipped_item_label(Equipment_Slot.ARMOR)).append("\n");
        inventory.append("主手槽位: ").append(get_equipped_item_label(Equipment_Slot.MAIN_HAND)).append("\n");
        inventory.append("副手槽位: ").append(get_equipped_item_label(Equipment_Slot.OFF_HAND)).append("\n");
        inventory.append("披风槽位: ").append(get_equipped_item_label(Equipment_Slot.CLOAK)).append("\n");
        inventory.append("护符槽位: ").append(get_equipped_item_label(Equipment_Slot.ACCESSORY)).append("\n\n");
        inventory.append("【职业熟练】\n").append(String.join("、", current_char.job.equipment_proficiencies)).append("\n\n");
        inventory.append("【背包物品】\n");
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.ARMOR)) {
            inventory.append("- ").append(item.to_inventory_line()).append("\n");
        }
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.MAIN_HAND)) {
            inventory.append("- ").append(item.to_inventory_line()).append("\n");
        }
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.OFF_HAND)) {
            inventory.append("- ").append(item.to_inventory_line()).append("\n");
        }
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.CLOAK)) {
            inventory.append("- ").append(item.to_inventory_line()).append("\n");
        }
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.ACCESSORY)) {
            inventory.append("- ").append(item.to_inventory_line()).append("\n");
        }
        inventory.append("【背包杂物】\n");
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.BACKPACK)) {
            inventory.append("- ").append(item.to_inventory_line()).append("\n");
        }
        inventory.append("\n");
        inventory.append("【技能熟练】\n");
        if (current_char.job.skill_proficiencies.isEmpty()) inventory.append("尚未选择\n");
        else inventory.append(String.join("、", current_char.job.skill_proficiencies)).append("\n");
        inventory_area.setText(inventory.toString());
        inventory_area.setCaretPosition(0);

        refresh_spellcasting_panel();
        refresh_progression_panel();

        boolean canLevelUp = current_char.can_level_up();
        level_up_btn.setEnabled(canLevelUp);
        level_up_btn.setText(canLevelUp ? "执行升级" : "经验不足，暂不可升级");
    }

    private void refresh_spellcasting_panel() {
        StringBuilder sb = new StringBuilder();
        manage_spellbook_btn.setVisible(false);
        manage_spell_selection_btn.setVisible(false);
        manage_prepared_spell_btn.setVisible(false);

        if (current_char.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current_char.job;
            manage_spellbook_btn.setVisible(true);
            manage_spell_selection_btn.setVisible(true);
            manage_prepared_spell_btn.setVisible(true);
            manage_spellbook_btn.setText("管理戏法");
            manage_spell_selection_btn.setText("管理法术书");
            manage_prepared_spell_btn.setText("管理准备法术");

            sb.append("【法师施法资源】\n");
            sb.append(wizard.get_spell_slot_summary()).append("\n");
            sb.append("戏法已知数: ").append(wizard.cantrips_known).append("\n");
            sb.append("法术书容量: ").append(wizard.spells_in_spellbook).append("\n");
            sb.append("可准备法术数: ").append(wizard.get_prepared_spell_count(current_char.stats.get_mod(current_char.stats.intel))).append("\n");
            sb.append("奥术回能额度: 最多恢复总环级 ").append(wizard.arcane_recovery_level).append("\n\n");
            sb.append("【已知戏法】\n");
            if (wizard.get_known_cantrip_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : wizard.get_known_cantrip_lines()) sb.append("- ").append(line).append("\n");
            sb.append("【法术书】\n");
            if (wizard.get_spellbook_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : wizard.get_spellbook_lines()) sb.append("- ").append(line).append("\n");
            sb.append("\n【准备法术】\n");
            if (wizard.get_prepared_spell_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : wizard.get_prepared_spell_lines()) sb.append("- ").append(line).append("\n");
        } else if (current_char.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current_char.job;
            manage_spellbook_btn.setVisible(true);
            manage_spell_selection_btn.setVisible(true);
            manage_spellbook_btn.setText("管理戏法");
            manage_spell_selection_btn.setText("管理已知法术");

            sb.append("【术士施法资源】\n");
            sb.append(sorcerer.get_spell_slot_summary()).append("\n");
            sb.append("术法点: ").append(sorcerer.get_sorcery_point_summary()).append("\n");
            sb.append("戏法已知数: ").append(sorcerer.cantrips_known).append("\n");
            sb.append("法术已知数: ").append(sorcerer.spells_known_count).append("\n\n");
            sb.append("【已知戏法】\n");
            if (sorcerer.get_known_cantrip_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : sorcerer.get_known_cantrip_lines()) sb.append("- ").append(line).append("\n");
            sb.append("【已知法术】\n");
            if (sorcerer.get_known_spell_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : sorcerer.get_known_spell_lines()) sb.append("- ").append(line).append("\n");
        } else if (current_char.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current_char.job;
            manage_spellbook_btn.setVisible(true);
            manage_spell_selection_btn.setVisible(true);
            manage_spellbook_btn.setText("管理戏法");
            manage_spell_selection_btn.setText("管理已知法术");

            sb.append("【邪术士施法资源】\n");
            sb.append("契约法术位: ").append(warlock.get_pact_slot_summary()).append("\n");
            if (warlock.mystic_arcanum_level > 0) {
                sb.append("神秘秘法: 1 个 ").append(warlock.mystic_arcanum_level).append(" 环秘法\n");
            }
            sb.append("戏法已知数: ").append(warlock.cantrips_known).append("\n");
            sb.append("法术已知数: ").append(warlock.spells_known_count).append("\n\n");
            sb.append("【已知戏法】\n");
            if (warlock.get_known_cantrip_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : warlock.get_known_cantrip_lines()) sb.append("- ").append(line).append("\n");
            sb.append("【已知法术】\n");
            if (warlock.get_known_spell_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : warlock.get_known_spell_lines()) sb.append("- ").append(line).append("\n");
        } else if (current_char.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) current_char.job;
            int charismaModifier = current_char.stats.get_mod(current_char.stats.cha);
            if (current_char.job.current_level >= 2) {
                manage_prepared_spell_btn.setVisible(true);
                manage_prepared_spell_btn.setText("管理准备法术");
            }
            sb.append("【圣武士施法资源】\n");
            sb.append(paladin.get_spell_slot_summary()).append("\n");
            sb.append("圣疗池: ").append(paladin.get_lay_on_hands_summary()).append("\n");
            sb.append("神圣感知次数: ").append(paladin.get_divine_sense_summary(charismaModifier)).append("\n");
            if (current_char.job.current_level >= 14) {
                sb.append("净化之触次数: ").append(paladin.get_cleansing_touch_summary(charismaModifier)).append("\n");
            }
            sb.append("可准备法术数: ").append(paladin.get_prepared_spell_count(charismaModifier)).append("\n\n");
            sb.append("【准备法术】\n");
            if (paladin.get_prepared_spell_lines().isEmpty()) sb.append("- 暂无\n");
            else for (String line : paladin.get_prepared_spell_lines()) sb.append("- ").append(line).append("\n");
        } else {
            sb.append("当前职业暂无专门的施法管理界面。");
        }

        spellcasting_area.setText(sb.toString());
        spellcasting_area.setCaretPosition(0);
    }

    private void refresh_progression_panel() {
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
        if (features.isEmpty()) progression.append("- 暂无\n");
        else for (String feature : features) progression.append("- ").append(feature).append("\n");

        progression.append("\n【待处理升级选择】\n");
        List<String> pendingChoices = current_char.job.get_pending_choices();
        if (pendingChoices.isEmpty()) progression.append("- 暂无\n");
        else for (String pendingChoice : pendingChoices) progression.append("- ").append(pendingChoice).append("\n");

        progression.append("\n【升级记录】\n");
        if (current_char.advancement_notes.isEmpty()) progression.append("- 暂无\n");
        else for (String note : current_char.advancement_notes) progression.append("- ").append(note).append("\n");

        if (current_char.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) current_char.job;
            progression.append("\n【战士资源】\n");
            progression.append("动作如潮次数: ").append(fighter.get_action_surge_summary()).append("\n");
            progression.append("不屈次数: ").append(fighter.get_indomitable_summary()).append("\n");
            progression.append("每次攻击动作攻击次数: ").append(fighter.attacks_per_action).append("\n");
            if (fighter.fighter_subclass == com.DMDHelper.basic.playerclass.Fighter.Fighter_Subclass.BATTLE_MASTER) {
                progression.append("卓越骰: ").append(fighter.get_superiority_dice_summary()).append("\n");
            }
        } else if (current_char.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current_char.job;
            progression.append("\n【术士资源】\n");
            progression.append("术法点: ").append(sorcerer.get_sorcery_point_summary()).append("\n");
            progression.append(sorcerer.get_spell_slot_summary()).append("\n");
        } else if (current_char.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current_char.job;
            progression.append("\n【邪术士资源】\n");
            progression.append("契约法术位: ").append(warlock.get_pact_slot_summary()).append("\n");
            if (warlock.mystic_arcanum_level > 0) {
                progression.append("神秘秘法: 1 个 ").append(warlock.mystic_arcanum_level).append(" 环秘法\n");
            }
        } else if (current_char.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) current_char.job;
            int charismaModifier = current_char.stats.get_mod(current_char.stats.cha);
            progression.append("\n【圣武士资源】\n");
            progression.append(paladin.get_spell_slot_summary()).append("\n");
            progression.append("圣疗池: ").append(paladin.get_lay_on_hands_summary()).append("\n");
            progression.append("神圣感知次数: ").append(paladin.get_divine_sense_summary(charismaModifier)).append("\n");
            if (current_char.job.current_level >= 14) {
                progression.append("净化之触次数: ").append(paladin.get_cleansing_touch_summary(charismaModifier)).append("\n");
            }
            progression.append("每次攻击动作攻击次数: ").append(paladin.attacks_per_action).append("\n");
        }

        level_info_area.setText(progression.toString());
        level_info_area.setCaretPosition(0);
    }

    private String get_equipped_item_label(Equipment_Slot slot) {
        Equipment_Item item = current_char.get_equipped_item(slot);
        if (item == null) {
            return "空置";
        }
        return item.display_name + " - " + item.description;
    }

    private boolean has_profile_content() {
        return !current_char.background_story.trim().isEmpty()
                || !current_char.personality_traits.trim().isEmpty()
                || !current_char.ideals.trim().isEmpty()
                || !current_char.bonds.trim().isEmpty()
                || !current_char.flaws.trim().isEmpty();
    }

    private static class EquipmentChoice {
        private final String item_key;
        private final String label;

        private EquipmentChoice(String item_key, String label, String description) {
            this.item_key = item_key;
            this.label = label + (description == null || description.trim().isEmpty() ? "" : " | " + description);
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
}
