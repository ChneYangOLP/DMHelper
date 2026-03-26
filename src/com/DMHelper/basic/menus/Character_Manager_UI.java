package com.DMHelper.basic.menus;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.combat.Combat_Engine;
import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.combat.Dice_Util;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Custom_Equipment_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Character_Manager_UI extends JFrame {

    private Character_Sheet current_char;
    private boolean is_reloading_selector;

    private JComboBox<String> char_selector;
    private JTextArea stats_area;
    private JTextArea level_info_area;
    private JTextArea spellcasting_area;
    private JTextArea inventory_detail_area;
    private DefaultListModel<Equipment_Item> backpack_list_model;
    private JList<Equipment_Item> backpack_list;
    private JComboBox<String> backpack_filter_box;

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
    private JButton add_item_btn;
    private JButton use_item_btn;

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

        JPanel equip_panel = new JPanel(new BorderLayout(10, 10));
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
        backpack_list_model = new DefaultListModel<>();
        backpack_list = new JList<>(backpack_list_model);
        backpack_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        backpack_list.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : build_backpack_row_label(value));
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        });
        backpack_list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refresh_inventory_detail_area();
            }
        });
        backpack_list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int index = backpack_list.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        backpack_list.setSelectedIndex(index);
                        handle_use_selected_item();
                    }
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                maybe_show_backpack_menu(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                maybe_show_backpack_menu(e);
            }
        });
        inventory_detail_area = build_text_area();
        inventory_detail_area.setRows(8);
        backpack_filter_box = new JComboBox<>(new String[]{"全部", "消耗品", "材料/战利品", "工具/任务", "自定义"});
        backpack_filter_box.addActionListener(e -> reload_backpack_list());
        JPanel inventory_center_panel = new JPanel(new BorderLayout(8, 8));
        inventory_center_panel.setBorder(BorderFactory.createTitledBorder("背包物品"));
        inventory_center_panel.add(backpack_filter_box, BorderLayout.NORTH);
        inventory_center_panel.add(new JScrollPane(backpack_list), BorderLayout.CENTER);
        inventory_center_panel.add(new JScrollPane(inventory_detail_area), BorderLayout.SOUTH);
        JButton equip_btn = new JButton("应用当前装备");
        add_item_btn = new JButton("新增物品");
        use_item_btn = new JButton("使用选中物品");
        JPanel equip_button_panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        equip_button_panel.add(use_item_btn);
        equip_button_panel.add(add_item_btn);
        equip_button_panel.add(equip_btn);
        equip_panel.add(equipment_slot_panel, BorderLayout.NORTH);
        equip_panel.add(inventory_center_panel, BorderLayout.CENTER);
        equip_panel.add(equip_button_panel, BorderLayout.SOUTH);
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
        add_item_btn.addActionListener(e -> handle_add_item());
        use_item_btn.addActionListener(e -> handle_use_selected_item());
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

    private void handle_add_item() {
        String[] options = {"从物品库搜索", "自定义普通物品", "自定义武器/护甲"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "请选择新增物品的方式：",
                "新增物品",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            open_library_item_picker();
        } else if (choice == 1) {
            open_custom_misc_item_dialog();
        } else if (choice == 2) {
            open_custom_equipment_dialog();
        }
    }

    private void open_library_item_picker() {
        JTextField searchField = new JTextField();
        DefaultListModel<Equipment_Item> listModel = new DefaultListModel<>();
        JList<Equipment_Item> resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setVisibleRowCount(10);
        resultList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value == null ? "" : value.to_inventory_line());
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }
            return label;
        });

        Runnable refreshResults = () -> {
            listModel.clear();
            for (Equipment_Item item : Equipment_Library.search_items(searchField.getText(), null, true)) {
                if (!current_char.owned_equipment_keys.contains(item.key) || item.is_stackable()) {
                    listModel.addElement(item);
                }
            }
            if (!listModel.isEmpty()) {
                resultList.setSelectedIndex(0);
            }
        };

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshResults.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshResults.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshResults.run();
            }
        });

        JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
        searchPanel.add(new JLabel("搜索物品库（支持名称、描述、槽位关键词）："), BorderLayout.NORTH);
        searchField.setPreferredSize(new Dimension(640, 34));
        searchPanel.add(searchField, BorderLayout.SOUTH);

        JTextArea hintArea = new JTextArea();
        hintArea.setEditable(false);
        hintArea.setLineWrap(true);
        hintArea.setWrapStyleWord(true);
        hintArea.setBackground(searchPanel.getBackground());
        hintArea.setText("只显示当前角色尚未拥有的内置物品。选中后点击确定即可加入背包/装备池。");

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setPreferredSize(new Dimension(760, 520));
        content.add(searchPanel, BorderLayout.NORTH);
        content.add(new JScrollPane(resultList), BorderLayout.CENTER);
        content.add(hintArea, BorderLayout.SOUTH);

        refreshResults.run();
        int result = JOptionPane.showConfirmDialog(this, content, "从物品库添加", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        Equipment_Item selected = resultList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个物品。");
            return;
        }

        current_char.add_item_to_inventory(selected.key);
        current_char.record_advancement("从物品库获得物品：" + selected.display_name);
        Character_DAO.update_character(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "已将 [" + selected.display_name + "] 加入 " + current_char.name + " 的物品列表。");
    }

    private void open_custom_misc_item_dialog() {
        JTextField nameField = new JTextField();
        JComboBox<SlotChoice> slotBox = new JComboBox<>(new SlotChoice[]{
                new SlotChoice(Equipment_Slot.BACKPACK, "背包杂物"),
                new SlotChoice(Equipment_Slot.OFF_HAND, "副手/法器"),
                new SlotChoice(Equipment_Slot.CLOAK, "披风"),
                new SlotChoice(Equipment_Slot.ACCESSORY, "护符")
        });
        JTextArea descriptionArea = new JTextArea(4, 28);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("物品名称"));
        panel.add(nameField);
        panel.add(new JLabel("物品槽位"));
        panel.add(slotBox);
        panel.add(new JLabel("功能/描述"));
        panel.add(new JScrollPane(descriptionArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "创建自定义普通物品", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String displayName = nameField.getText().trim();
        if (displayName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "物品名称不能为空。");
            return;
        }

        SlotChoice slotChoice = (SlotChoice) slotBox.getSelectedItem();
        if (slotChoice == null) {
            JOptionPane.showMessageDialog(this, "请选择一个物品槽位。");
            return;
        }

        Equipment_Item item = build_custom_item(
                slotChoice.slot,
                displayName,
                descriptionArea.getText().trim(),
                "",
                0,
                0,
                0,
                0,
                0,
                "",
                false,
                false
        );
        persist_custom_item(item, "创建自定义物品");
    }

    private void open_custom_equipment_dialog() {
        JComboBox<ItemTemplate> templateBox = new JComboBox<>(build_item_templates());
        JTextField nameField = new JTextField();
        JTextArea descriptionArea = new JTextArea(4, 28);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JSpinner armorAcSpinner = new JSpinner(new SpinnerNumberModel(10, 0, 30, 1));
        JSpinner shieldBonusSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 10, 1));
        JSpinner diceCountSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 10, 1));
        JSpinner dieSizeSpinner = new JSpinner(new SpinnerNumberModel(6, 0, 20, 1));
        JSpinner attackBonusSpinner = new JSpinner(new SpinnerNumberModel(0, -10, 20, 1));
        JTextField damageTypeField = new JTextField();
        JCheckBox finesseBox = new JCheckBox("灵巧");
        JCheckBox rangedBox = new JCheckBox("远程");
        JLabel slotHintLabel = new JLabel();
        JLabel armorLabel = new JLabel("护甲 AC");
        JLabel shieldLabel = new JLabel("盾牌 AC 加值");
        JLabel damageLabel = new JLabel("伤害类型");

        Runnable applyTemplate = () -> {
            ItemTemplate template = (ItemTemplate) templateBox.getSelectedItem();
            if (template == null) {
                return;
            }
            slotHintLabel.setText("当前模板槽位：" + get_slot_label(template.slot));
            if (nameField.getText().trim().isEmpty()) {
                nameField.setText(template.suggestedName);
            }
            if (descriptionArea.getText().trim().isEmpty()) {
                descriptionArea.setText(template.description);
            }
            armorAcSpinner.setValue(template.baseAc);
            shieldBonusSpinner.setValue(template.shieldBonus);
            diceCountSpinner.setValue(template.attackDiceCount);
            dieSizeSpinner.setValue(template.attackDieSize);
            attackBonusSpinner.setValue(template.attackBonus);
            damageTypeField.setText(template.damageType);
            finesseBox.setSelected(template.finesse);
            rangedBox.setSelected(template.ranged);

            boolean armorTemplate = template.slot == Equipment_Slot.ARMOR;
            boolean weaponTemplate = template.slot == Equipment_Slot.MAIN_HAND;
            boolean shieldTemplate = template.slot == Equipment_Slot.OFF_HAND && template.shieldBonus > 0;

            armorAcSpinner.setEnabled(armorTemplate);
            shieldBonusSpinner.setEnabled(shieldTemplate);
            diceCountSpinner.setEnabled(weaponTemplate);
            dieSizeSpinner.setEnabled(weaponTemplate);
            attackBonusSpinner.setEnabled(weaponTemplate);
            damageTypeField.setEnabled(weaponTemplate);
            finesseBox.setEnabled(weaponTemplate);
            rangedBox.setEnabled(weaponTemplate);

            armorLabel.setEnabled(armorTemplate);
            shieldLabel.setEnabled(shieldTemplate);
            damageLabel.setEnabled(weaponTemplate);
        };

        templateBox.addActionListener(e -> applyTemplate.run());

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("模板类型"));
        panel.add(templateBox);
        panel.add(slotHintLabel);
        panel.add(new JLabel("装备名称"));
        panel.add(nameField);
        panel.add(new JLabel("功能/描述"));
        panel.add(new JScrollPane(descriptionArea));
        panel.add(armorLabel);
        panel.add(armorAcSpinner);
        panel.add(shieldLabel);
        panel.add(shieldBonusSpinner);
        panel.add(new JLabel("伤害骰个数"));
        panel.add(diceCountSpinner);
        panel.add(new JLabel("伤害骰面数"));
        panel.add(dieSizeSpinner);
        panel.add(new JLabel("攻击加值"));
        panel.add(attackBonusSpinner);
        panel.add(damageLabel);
        panel.add(damageTypeField);
        panel.add(finesseBox);
        panel.add(rangedBox);

        applyTemplate.run();

        int result = JOptionPane.showConfirmDialog(this, panel, "创建自定义武器/护甲", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        ItemTemplate template = (ItemTemplate) templateBox.getSelectedItem();
        if (template == null) {
            JOptionPane.showMessageDialog(this, "请选择一个装备模板。");
            return;
        }

        String displayName = nameField.getText().trim();
        if (displayName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "装备名称不能为空。");
            return;
        }

        int armorAc = (Integer) armorAcSpinner.getValue();
        int shieldBonus = (Integer) shieldBonusSpinner.getValue();
        int diceCount = (Integer) diceCountSpinner.getValue();
        int dieSize = (Integer) dieSizeSpinner.getValue();
        int attackBonus = (Integer) attackBonusSpinner.getValue();
        String damageType = damageTypeField.getText().trim();

        if (template.slot == Equipment_Slot.ARMOR && armorAc <= 0) {
            JOptionPane.showMessageDialog(this, "护甲 AC 至少需要大于 0。");
            return;
        }
        if (template.slot == Equipment_Slot.MAIN_HAND && (diceCount <= 0 || dieSize <= 0)) {
            JOptionPane.showMessageDialog(this, "武器伤害骰必须大于 0。");
            return;
        }
        if (template.slot == Equipment_Slot.OFF_HAND && shieldBonus <= 0) {
            JOptionPane.showMessageDialog(this, "盾牌模板的 AC 加值必须大于 0。");
            return;
        }

        Equipment_Item item = build_custom_item(
                template.slot,
                displayName,
                descriptionArea.getText().trim(),
                template.armorType,
                armorAc,
                shieldBonus,
                diceCount,
                dieSize,
                attackBonus,
                damageType,
                finesseBox.isSelected(),
                rangedBox.isSelected()
        );
        persist_custom_item(item, "创建自定义装备");
    }

    private Equipment_Item build_custom_item(Equipment_Slot slot,
                                             String displayName,
                                             String description,
                                             String armorType,
                                             int baseAc,
                                             int shieldBonus,
                                             int attackDiceCount,
                                             int attackDieSize,
                                             int attackBonus,
                                             String damageType,
                                             boolean finesse,
                                             boolean ranged) {
        ensure_character_has_database_id();
        String itemKey = Equipment_Library.build_custom_key(current_char.database_id, displayName);
        return new Equipment_Item(
                itemKey,
                displayName,
                slot,
                description == null || description.trim().isEmpty() ? "玩家自定义物品。" : description.trim(),
                armorType == null ? "" : armorType,
                baseAc,
                shieldBonus,
                attackDiceCount,
                attackDieSize,
                attackBonus,
                damageType == null ? "" : damageType.trim(),
                finesse,
                ranged
        );
    }

    private void persist_custom_item(Equipment_Item item, String actionLabel) {
        if (item == null) {
            return;
        }
        ensure_character_has_database_id();
        if (!Custom_Equipment_DAO.save_custom_item(current_char.database_id, item)) {
            JOptionPane.showMessageDialog(this, "自定义物品保存失败，请稍后再试。");
            return;
        }

        current_char.add_item_to_inventory(item.key);
        current_char.record_advancement(actionLabel + "：" + item.display_name);
        Character_DAO.update_character(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "已创建并加入物品列表：[" + item.display_name + "]。");
    }

    private void ensure_character_has_database_id() {
        if (current_char.database_id > 0) {
            return;
        }
        Character_DAO.update_character(current_char);
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
        reload_backpack_list();
        refresh_inventory_detail_area();

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
            if (fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.BATTLE_MASTER) {
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

    private void reload_backpack_list() {
        Equipment_Item selected = backpack_list.getSelectedValue();
        String selectedKey = selected == null ? "" : selected.key;
        String selectedCategory = backpack_filter_box == null ? "全部" : (String) backpack_filter_box.getSelectedItem();
        backpack_list_model.clear();
        for (Equipment_Item item : current_char.get_owned_items_for_slot(Equipment_Slot.BACKPACK)) {
            if ("全部".equals(selectedCategory) || item.get_inventory_category().equals(selectedCategory)) {
                backpack_list_model.addElement(item);
            }
        }
        if (backpack_list_model.isEmpty()) {
            backpack_list.clearSelection();
            use_item_btn.setEnabled(false);
            return;
        }
        int selectedIndex = 0;
        for (int i = 0; i < backpack_list_model.size(); i++) {
            if (backpack_list_model.get(i).key.equals(selectedKey)) {
                selectedIndex = i;
                break;
            }
        }
        backpack_list.setSelectedIndex(selectedIndex);
        use_item_btn.setEnabled(true);
    }

    private void refresh_inventory_detail_area() {
        StringBuilder detail = new StringBuilder();
        Equipment_Item item = backpack_list.getSelectedValue();
        if (item == null) {
            detail.append("【背包说明】\n");
            detail.append("请选择一个背包物品，可查看详情并执行“使用选中物品”。\n");
        } else {
            detail.append("【所选物品】\n");
            detail.append(build_backpack_row_label(item)).append("\n");
            detail.append("分类: ").append(item.get_inventory_category()).append("\n");
            detail.append("用途: ").append(item.get_use_hint()).append("\n");
        }
        detail.append("\n【武器/护甲熟练】\n");
        if (current_char.job.equipment_proficiencies.isEmpty()) {
            detail.append("暂无\n");
        } else {
            detail.append(String.join("、", current_char.job.equipment_proficiencies)).append("\n");
        }
        detail.append("\n【技能熟练】\n");
        if (current_char.job.skill_proficiencies.isEmpty()) {
            detail.append("尚未选择\n");
        } else {
            detail.append(String.join("、", current_char.job.skill_proficiencies)).append("\n");
        }
        inventory_detail_area.setText(detail.toString());
        inventory_detail_area.setCaretPosition(0);
        use_item_btn.setEnabled(item != null);
    }

    private void handle_use_selected_item() {
        Equipment_Item item = backpack_list.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "请先从背包列表中选择一个物品。");
            return;
        }

        if (item.is_healing_item()) {
            int healAmount = item.get_flat_healing_amount();
            if (healAmount <= 0 && item.get_healing_dice_count() > 0) {
                healAmount = Dice_Util.roll_dice(item.get_healing_dice_count(), item.get_healing_die_size()) + item.get_healing_bonus();
            }
            int beforeHp = current_char.current_hp;
            current_char.set_current_hp(current_char.current_hp + healAmount);
            current_char.remove_item_from_inventory(item.key);
            int remainingCount = current_char.get_item_count(item.key);
            current_char.record_advancement("使用物品：" + item.display_name + "，恢复生命值 " + (current_char.current_hp - beforeHp)
                    + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
            Character_DAO.update_character(current_char);
            refresh_ui();
            JOptionPane.showMessageDialog(this,
                    current_char.name + " 使用了 [" + item.display_name + "]，生命值从 "
                            + beforeHp + " 提升到 " + current_char.current_hp + "/" + current_char.hp + "。"
                            + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
            return;
        }

        if (item.is_scroll_item()) {
            if (handle_scroll_item_use(item)) {
                refresh_ui();
            }
            return;
        }

        if (item.is_bomb_item()) {
            if (handle_bomb_item_use(item)) {
                refresh_ui();
            }
            return;
        }

        if (item.is_key_item()) {
            current_char.record_advancement("检查钥匙：" + item.display_name);
            Character_DAO.update_character(current_char);
            JOptionPane.showMessageDialog(this,
                    "钥匙已经准备好使用，但不会自动消耗。\n你可以在剧情里把它保留给门锁、宝箱或机关。");
            return;
        }

        if (item.is_quest_item()) {
            current_char.record_advancement("查看任务物品：" + item.display_name);
            Character_DAO.update_character(current_char);
            JOptionPane.showMessageDialog(this,
                    "已查看任务物品。\n这类物品默认不会自动消耗，适合在剧情推进、交付 NPC 或探索节点时手动处理。");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "当前物品还没有自动结算效果。\n是否将 [" + item.display_name + "] 标记为已使用并从背包移除？",
                "使用物品",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        current_char.remove_item_from_inventory(item.key);
        int remainingCount = current_char.get_item_count(item.key);
        current_char.record_advancement("使用物品：" + item.display_name + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        Character_DAO.update_character(current_char);
        refresh_ui();
        JOptionPane.showMessageDialog(this, "已将 [" + item.display_name + "] 标记为已使用。"
                + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
    }

    private String build_backpack_row_label(Equipment_Item item) {
        if (item == null) {
            return "";
        }
        int count = Math.max(1, current_char.get_item_count(item.key));
        return item.display_name + " x" + count + " | " + item.get_inventory_category() + " | " + item.description;
    }

    private void maybe_show_backpack_menu(java.awt.event.MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int index = backpack_list.locationToIndex(e.getPoint());
        if (index >= 0 && !backpack_list.isSelectedIndex(index)) {
            backpack_list.setSelectedIndex(index);
        }
        Equipment_Item item = backpack_list.getSelectedValue();
        if (item == null) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        JMenuItem useMenu = new JMenuItem("使用");
        useMenu.addActionListener(ae -> handle_use_selected_item());
        menu.add(useMenu);

        JMenuItem discardOneMenu = new JMenuItem("丢弃一件");
        discardOneMenu.addActionListener(ae -> discard_selected_item(false));
        menu.add(discardOneMenu);

        JMenuItem discardAllMenu = new JMenuItem("丢弃全部");
        discardAllMenu.addActionListener(ae -> discard_selected_item(true));
        menu.add(discardAllMenu);

        menu.show(backpack_list, e.getX(), e.getY());
    }

    private void discard_selected_item(boolean discardAll) {
        Equipment_Item item = backpack_list.getSelectedValue();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个背包物品。");
            return;
        }

        int currentCount = Math.max(1, current_char.get_item_count(item.key));
        String message = discardAll || currentCount <= 1
                ? "确认丢弃 [" + item.display_name + "] 的全部数量吗？"
                : "确认丢弃 [" + item.display_name + "] 的 1 件吗？";
        int confirm = JOptionPane.showConfirmDialog(this, message, "丢弃物品", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (discardAll || currentCount <= 1) {
            while (current_char.get_item_count(item.key) > 0) {
                if (!current_char.remove_item_from_inventory(item.key)) {
                    break;
                }
            }
            current_char.record_advancement("丢弃物品：" + item.display_name + "（全部）");
        } else {
            current_char.remove_item_from_inventory(item.key);
            current_char.record_advancement("丢弃物品：" + item.display_name + "（1 件）");
        }

        Character_DAO.update_character(current_char);
        refresh_ui();
    }

    private boolean handle_scroll_item_use(Equipment_Item item) {
        if ("scroll_of_healing_touch".equals(item.key)) {
            return handle_healing_scroll_use(item);
        }
        if ("scroll_of_fireball".equals(item.key)) {
            return handle_fireball_scroll_use(item);
        }
        if ("scroll_of_identify".equals(item.key)) {
            return handle_identify_scroll_use(item);
        }

        return handle_lore_scroll_use(item);
    }

    private boolean handle_lore_scroll_use(Equipment_Item item) {
        JTextField subjectField = new JTextField();
        JTextArea resultArea = new JTextArea(4, 24);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        if ("mystic_scroll_fragment".equals(item.key)) {
            panel.add(new JLabel("解读出的符文/法阵关键词"));
            panel.add(subjectField);
            panel.add(new JLabel("本次得到的奥术线索"));
        } else {
            panel.add(new JLabel("研究主题/目标地点"));
            panel.add(subjectField);
            panel.add(new JLabel("从卷轴中获得的具体情报"));
        }
        panel.add(new JScrollPane(resultArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "阅读卷轴 - " + item.display_name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        String subject = subjectField.getText().trim();
        String insight = resultArea.getText().trim();
        if (subject.isEmpty()) {
            subject = "未指定主题";
        }
        if (insight.isEmpty()) {
            insight = "尚未记录具体结果";
        }

        current_char.remove_item_from_inventory(item.key);
        int remainingCount = current_char.get_item_count(item.key);
        if ("mystic_scroll_fragment".equals(item.key)) {
            current_char.record_advancement("阅读卷轴：" + item.display_name + "，解读出 [" + subject + "]，线索：" + insight
                    + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        } else {
            current_char.record_advancement("阅读卷轴：" + item.display_name + "，主题 [" + subject + "]，情报：" + insight
                    + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        }
        Character_DAO.update_character(current_char);
        JOptionPane.showMessageDialog(this,
                "卷轴内容已记录到角色成长记录中。\n主题: " + subject + "\n结果: " + insight
                        + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件同类卷轴。" : ""));
        return true;
    }

    private boolean handle_healing_scroll_use(Equipment_Item item) {
        Spell_Definition spell = Spell_Library.get_spell("cure_wounds");
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = get_active_combat_engine();

        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = get_living_combatant_choices(combatEngine);
            if (targetChoices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前战斗中没有可治疗的有效目标。");
                return false;
            }

            JComboBox<CombatantChoice> targetBox = new JComboBox<>(targetChoices.toArray(new CombatantChoice[0]));
            JTextField noteField = new JTextField();
            JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
            panel.add(new JLabel(spell == null ? "治疗卷轴" : spell.display_name + " - " + spell.short_description));
            panel.add(new JLabel("选择治疗目标"));
            panel.add(targetBox);
            panel.add(new JLabel("备注（例如接触治疗、战术协助）"));
            panel.add(noteField);

            int result = JOptionPane.showConfirmDialog(this, panel, "施放卷轴 - " + item.display_name,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }

            CombatantChoice targetChoice = (CombatantChoice) targetBox.getSelectedItem();
            if (targetChoice == null || targetChoice.combatant == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个治疗目标。");
                return false;
            }

            int healAmount = Dice_Util.roll_dice(1, 8) + 3;
            String note = noteField.getText().trim();
            String sourceLabel = spell == null ? item.display_name : item.display_name + " / " + spell.display_name;
            String log = combatEngine.apply_external_healing(sourceLabel, targetChoice.combatant, healAmount, note);
            current_char.remove_item_from_inventory(item.key);
            int remainingCount = current_char.get_item_count(item.key);
            current_char.record_advancement("使用卷轴：" + item.display_name + "，为 [" + targetChoice.combatant.display_name + "] 恢复 "
                    + healAmount + " 点生命值" + (note.isEmpty() ? "" : "，备注：" + note)
                    + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
            Character_DAO.update_character(current_char);
            combatUI.refresh_after_external_effect(log);
            JOptionPane.showMessageDialog(this,
                    targetChoice.combatant.display_name + " 受到疗伤术卷轴影响，恢复 " + healAmount + " 点生命值。"
                            + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件同类卷轴。" : ""));
            return true;
        }

        JTextField noteField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel(spell == null ? "治疗卷轴将默认对当前角色生效。" : spell.display_name + " 将默认对当前角色生效。"));
        panel.add(new JLabel("备注（可留空）"));
        panel.add(noteField);

        int result = JOptionPane.showConfirmDialog(this, panel, "施放卷轴 - " + item.display_name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        int healAmount = Dice_Util.roll_dice(1, 8) + 3;
        int beforeHp = current_char.current_hp;
        current_char.set_current_hp(current_char.current_hp + healAmount);
        current_char.remove_item_from_inventory(item.key);
        int remainingCount = current_char.get_item_count(item.key);
        String note = noteField.getText().trim();
        current_char.record_advancement("使用卷轴：" + item.display_name + "，恢复生命值 " + (current_char.current_hp - beforeHp)
                + (note.isEmpty() ? "" : "，备注：" + note)
                + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        Character_DAO.update_character(current_char);
        JOptionPane.showMessageDialog(this,
                current_char.name + " 使用了 [" + item.display_name + "]，生命值从 "
                        + beforeHp + " 提升到 " + current_char.current_hp + "/" + current_char.hp + "。"
                        + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
        return true;
    }

    private boolean handle_fireball_scroll_use(Equipment_Item item) {
        Spell_Definition spell = Spell_Library.get_spell("fireball");
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = get_active_combat_engine();

        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = get_living_combatant_choices(combatEngine);
            if (targetChoices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前战斗中没有可选中的目标。");
                return false;
            }

            DefaultListModel<CombatantChoice> targetModel = new DefaultListModel<>();
            for (CombatantChoice choice : targetChoices) {
                targetModel.addElement(choice);
            }
            JList<CombatantChoice> targetList = new JList<>(targetModel);
            targetList.setVisibleRowCount(Math.min(8, targetChoices.size()));
            targetList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            JTextField areaField = new JTextField();
            JTextArea noteArea = new JTextArea(3, 24);
            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);

            JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
            panel.add(new JLabel(spell == null ? "火球术卷轴" : spell.display_name + " - " + spell.short_description));
            panel.add(new JLabel("选择本次被火球波及的目标（可多选）"));
            panel.add(new JScrollPane(targetList));
            panel.add(new JLabel("爆心区域 / 命中位置"));
            panel.add(areaField);
            panel.add(new JLabel("备注（例如掩体、豁免判定、环境着火）"));
            panel.add(new JScrollPane(noteArea));

            int result = JOptionPane.showConfirmDialog(this, panel, "施放卷轴 - " + item.display_name,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }

            List<CombatantChoice> selectedChoices = targetList.getSelectedValuesList();
            if (selectedChoices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请至少选择一个被火球波及的目标。");
                return false;
            }

            String area = areaField.getText().trim();
            String note = noteArea.getText().trim();
            String combinedNote = (area.isEmpty() ? "" : "爆心区域：" + area)
                    + (note.isEmpty() ? "" : ((area.isEmpty() ? "" : "；") + note));
            int damage = Dice_Util.roll_dice(8, 6);
            String sourceLabel = spell == null ? item.display_name : item.display_name + " / " + spell.display_name;
            StringBuilder battleLog = new StringBuilder();
            List<String> targetNames = new ArrayList<>();
            for (CombatantChoice choice : selectedChoices) {
                if (battleLog.length() > 0) {
                    battleLog.append("\n\n");
                }
                battleLog.append(combatEngine.apply_external_damage(sourceLabel, choice.combatant, damage, "火焰", combinedNote));
                targetNames.add(choice.combatant.display_name);
            }

            current_char.remove_item_from_inventory(item.key);
            int remainingCount = current_char.get_item_count(item.key);
            current_char.record_advancement("使用卷轴：" + item.display_name + "，对 [" + String.join("、", targetNames) + "] 造成 "
                    + damage + " 点火焰伤害" + (combinedNote.isEmpty() ? "" : "，备注：" + combinedNote)
                    + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
            Character_DAO.update_character(current_char);
            combatUI.refresh_after_external_effect(battleLog.toString());
            JOptionPane.showMessageDialog(this,
                    item.display_name + " 已施放。\n受影响目标：" + String.join("、", targetNames) + "\n统一伤害：" + damage + " 点火焰伤害"
                            + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件同类卷轴。" : ""));
            return true;
        }

        JTextField areaField = new JTextField();
        JTextField targetField = new JTextField();
        JTextArea noteArea = new JTextArea(3, 24);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel(spell == null ? "火球术卷轴" : spell.display_name + " - " + spell.short_description));
        panel.add(new JLabel("爆心区域 / 命中位置"));
        panel.add(areaField);
        panel.add(new JLabel("受影响目标（文本记录）"));
        panel.add(targetField);
        panel.add(new JLabel("备注（例如掩体、地形、着火效果）"));
        panel.add(new JScrollPane(noteArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "施放卷轴 - " + item.display_name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        String area = areaField.getText().trim();
        String targets = targetField.getText().trim();
        String note = noteArea.getText().trim();
        if (area.isEmpty()) {
            area = "未注明区域";
        }
        if (targets.isEmpty()) {
            targets = "未注明目标";
        }

        int damage = Dice_Util.roll_dice(8, 6);
        current_char.remove_item_from_inventory(item.key);
        int remainingCount = current_char.get_item_count(item.key);
        current_char.record_advancement("使用卷轴：" + item.display_name + "，在 [" + area + "] 释放火球术，对 ["
                + targets + "] 造成约 " + damage + " 点火焰伤害"
                + (note.isEmpty() ? "" : "，备注：" + note)
                + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        Character_DAO.update_character(current_char);
        JOptionPane.showMessageDialog(this,
                item.display_name + " 已记录为施放。\n区域: " + area + "\n目标: " + targets + "\n伤害: " + damage + " 点火焰伤害"
                        + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件同类卷轴。" : ""));
        return true;
    }

    private boolean handle_identify_scroll_use(Equipment_Item item) {
        Spell_Definition spell = Spell_Library.get_spell("identify");
        JTextField targetField = new JTextField();
        JTextArea propertyArea = new JTextArea(4, 24);
        propertyArea.setLineWrap(true);
        propertyArea.setWrapStyleWord(true);
        JTextArea methodArea = new JTextArea(3, 24);
        methodArea.setLineWrap(true);
        methodArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel(spell == null ? "鉴定卷轴" : spell.display_name + " - " + spell.short_description));
        panel.add(new JLabel("被鉴定的物品 / 法阵 / 现象"));
        panel.add(targetField);
        panel.add(new JLabel("识别出的魔法性质 / 特殊效果"));
        panel.add(new JScrollPane(propertyArea));
        panel.add(new JLabel("附加备注（例如使用方式、来源、风险）"));
        panel.add(new JScrollPane(methodArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "施放卷轴 - " + item.display_name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        String target = targetField.getText().trim();
        String property = propertyArea.getText().trim();
        String method = methodArea.getText().trim();
        if (target.isEmpty()) {
            target = "未指定对象";
        }
        if (property.isEmpty()) {
            property = "尚未记录具体鉴定结果";
        }

        current_char.remove_item_from_inventory(item.key);
        int remainingCount = current_char.get_item_count(item.key);
        current_char.record_advancement("使用卷轴：" + item.display_name + "，鉴定对象 [" + target + "]，结果："
                + property + (method.isEmpty() ? "" : "，备注：" + method)
                + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        Character_DAO.update_character(current_char);
        JOptionPane.showMessageDialog(this,
                "鉴定结果已记录。\n对象: " + target + "\n结果: " + property
                        + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件同类卷轴。" : ""));
        return true;
    }

    private boolean handle_bomb_item_use(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = get_active_combat_engine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = get_living_combatant_choices(combatEngine);
            if (targetChoices.isEmpty()) {
                JOptionPane.showMessageDialog(this, "当前战斗中没有可投掷的有效目标。");
                return false;
            }

            JComboBox<CombatantChoice> targetBox = new JComboBox<>(targetChoices.toArray(new CombatantChoice[0]));
            JTextField areaField = new JTextField();
            JTextArea noteArea = new JTextArea(3, 24);
            noteArea.setLineWrap(true);
            noteArea.setWrapStyleWord(true);

            JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
            panel.add(new JLabel("选择炸弹命中的主目标"));
            panel.add(targetBox);
            panel.add(new JLabel("爆炸区域 / 落点"));
            panel.add(areaField);
            panel.add(new JLabel("备注（例如掩体、溅射、附带效果）"));
            panel.add(new JScrollPane(noteArea));

            int result = JOptionPane.showConfirmDialog(this, panel, "使用爆炸物 - " + item.display_name,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }

            CombatantChoice targetChoice = (CombatantChoice) targetBox.getSelectedItem();
            if (targetChoice == null || targetChoice.combatant == null) {
                JOptionPane.showMessageDialog(this, "请先选择一个目标。");
                return false;
            }

            String area = areaField.getText().trim();
            String note = noteArea.getText().trim();
            String combinedNote = (area.isEmpty() ? "" : "爆炸区域：" + area)
                    + (note.isEmpty() ? "" : ((area.isEmpty() ? "" : "；") + note));
            int damage = Dice_Util.roll_dice(item.get_bomb_dice_count(), item.get_bomb_die_size()) + item.get_bomb_bonus();
            String log = combatEngine.apply_external_damage(item.display_name, targetChoice.combatant, damage, item.get_bomb_damage_type(), combinedNote);
            current_char.remove_item_from_inventory(item.key);
            int remainingCount = current_char.get_item_count(item.key);
            current_char.record_advancement("使用爆炸物：" + item.display_name + "，命中 [" + targetChoice.combatant.display_name + "]，造成 "
                    + damage + " 点" + item.get_bomb_damage_type() + "伤害"
                    + (combinedNote.isEmpty() ? "" : "，备注：" + combinedNote)
                    + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
            Character_DAO.update_character(current_char);
            combatUI.refresh_after_external_effect(log);
            JOptionPane.showMessageDialog(this,
                    item.display_name + " 已投掷到 [" + targetChoice.combatant.display_name + "]。\n伤害: "
                            + damage + " 点" + item.get_bomb_damage_type()
                            + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
            return true;
        }

        JTextField targetField = new JTextField();
        JTextArea noteArea = new JTextArea(3, 24);
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("命中目标 / 区域"));
        panel.add(targetField);
        panel.add(new JLabel("备注（例如是否掩体、范围边缘、附带效果）"));
        panel.add(new JScrollPane(noteArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "使用爆炸物 - " + item.display_name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return false;
        }

        String target = targetField.getText().trim();
        String note = noteArea.getText().trim();
        if (target.isEmpty()) {
            target = "未指定目标";
        }
        if (note.isEmpty()) {
            note = "无额外备注";
        }

        int damage = Dice_Util.roll_dice(item.get_bomb_dice_count(), item.get_bomb_die_size()) + item.get_bomb_bonus();
        current_char.remove_item_from_inventory(item.key);
        int remainingCount = current_char.get_item_count(item.key);
        current_char.record_advancement("使用爆炸物：" + item.display_name + "，目标 [" + target + "]，造成约 "
                + damage + " 点" + item.get_bomb_damage_type() + "伤害，备注：" + note
                + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        Character_DAO.update_character(current_char);
        JOptionPane.showMessageDialog(this,
                item.display_name + " 已投掷。\n目标: " + target + "\n伤害: " + damage + " 点" + item.get_bomb_damage_type()
                        + "\n备注: " + note
                        + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
        return true;
    }

    private Combat_Engine get_active_combat_engine() {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        if (combatUI == null) {
            return null;
        }
        Combat_Engine combatEngine = combatUI.get_current_combat_engine();
        if (combatEngine == null || combatEngine.is_combat_finished()) {
            return null;
        }
        return combatEngine;
    }

    private List<CombatantChoice> get_living_combatant_choices(Combat_Engine combatEngine) {
        List<CombatantChoice> choices = new ArrayList<>();
        if (combatEngine == null) {
            return choices;
        }
        for (Combatant combatant : combatEngine.get_initiative_order()) {
            if (combatant != null && combatant.is_alive()) {
                choices.add(new CombatantChoice(combatant));
            }
        }
        return choices;
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

    private String get_slot_label(Equipment_Slot slot) {
        if (slot == Equipment_Slot.ARMOR) return "护甲";
        if (slot == Equipment_Slot.MAIN_HAND) return "主手武器";
        if (slot == Equipment_Slot.OFF_HAND) return "副手/盾牌";
        if (slot == Equipment_Slot.CLOAK) return "披风";
        if (slot == Equipment_Slot.ACCESSORY) return "护符";
        return "背包杂物";
    }

    private ItemTemplate[] build_item_templates() {
        List<ItemTemplate> templates = Arrays.asList(
                new ItemTemplate("轻甲模板", "自定义轻甲", Equipment_Slot.ARMOR, "贴身灵活的轻甲，适合偏敏捷流派。", "Light", 11, 0, 0, 0, 0, "", false, false),
                new ItemTemplate("中甲模板", "自定义中甲", Equipment_Slot.ARMOR, "提供均衡保护的中甲，适合前线角色。", "Medium", 14, 0, 0, 0, 0, "", false, false),
                new ItemTemplate("重甲模板", "自定义重甲", Equipment_Slot.ARMOR, "厚重结实的重甲，强调正面防护。", "Heavy", 16, 0, 0, 0, 0, "", false, false),
                new ItemTemplate("单手武器模板", "自定义单手武器", Equipment_Slot.MAIN_HAND, "标准单手近战武器。", "", 0, 0, 1, 8, 0, "挥砍", false, false),
                new ItemTemplate("双手武器模板", "自定义双手武器", Equipment_Slot.MAIN_HAND, "偏向高伤害的双手重武器。", "", 0, 0, 2, 6, 0, "挥砍", false, false),
                new ItemTemplate("灵巧武器模板", "自定义灵巧武器", Equipment_Slot.MAIN_HAND, "轻便精准，适合靠敏捷作战。", "", 0, 0, 1, 6, 0, "穿刺", true, false),
                new ItemTemplate("远程武器模板", "自定义远程武器", Equipment_Slot.MAIN_HAND, "用于安全距离输出的远程武器。", "", 0, 0, 1, 8, 0, "穿刺", false, true),
                new ItemTemplate("盾牌模板", "自定义盾牌", Equipment_Slot.OFF_HAND, "用于格挡和强化防御的盾牌。", "", 0, 2, 0, 0, 0, "", false, false)
        );
        return templates.toArray(new ItemTemplate[0]);
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

    private static class SlotChoice {
        private final Equipment_Slot slot;
        private final String label;

        private SlotChoice(Equipment_Slot slot, String label) {
            this.slot = slot;
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private static class CombatantChoice {
        private final Combatant combatant;
        private final String label;

        private CombatantChoice(Combatant combatant) {
            this.combatant = combatant;
            String sideLabel = combatant.side == Combatant.Side.PLAYER ? "玩家" : "敌人";
            this.label = combatant.display_name + " [" + sideLabel + "] HP "
                    + combatant.current_hp + "/" + combatant.max_hp
                    + " | AC " + combatant.get_effective_armor_class();
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private static class ItemTemplate {
        private final String label;
        private final String suggestedName;
        private final Equipment_Slot slot;
        private final String description;
        private final String armorType;
        private final int baseAc;
        private final int shieldBonus;
        private final int attackDiceCount;
        private final int attackDieSize;
        private final int attackBonus;
        private final String damageType;
        private final boolean finesse;
        private final boolean ranged;

        private ItemTemplate(String label,
                             String suggestedName,
                             Equipment_Slot slot,
                             String description,
                             String armorType,
                             int baseAc,
                             int shieldBonus,
                             int attackDiceCount,
                             int attackDieSize,
                             int attackBonus,
                             String damageType,
                             boolean finesse,
                             boolean ranged) {
            this.label = label;
            this.suggestedName = suggestedName;
            this.slot = slot;
            this.description = description;
            this.armorType = armorType;
            this.baseAc = baseAc;
            this.shieldBonus = shieldBonus;
            this.attackDiceCount = attackDiceCount;
            this.attackDieSize = attackDieSize;
            this.attackBonus = attackBonus;
            this.damageType = damageType;
            this.finesse = finesse;
            this.ranged = ranged;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
}
