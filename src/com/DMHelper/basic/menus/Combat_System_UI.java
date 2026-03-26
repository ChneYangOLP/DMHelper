package com.DMHelper.basic.menus;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.combat.Attack_Option;
import com.DMHelper.basic.combat.Combat_Engine;
import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.combat.Monster_Definition;
import com.DMHelper.basic.combat.Monster_Library;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.database.Global_Data;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Combat_System_UI extends JFrame {
    private static Combat_System_UI active_instance;
    private final CardLayout card_layout;
    private final JPanel root_panel;

    private final DefaultListModel<String> character_list_model;
    private final JList<String> character_list;

    private final JTextField monster_search_field;
    private final DefaultListModel<String> monster_list_model;
    private final JList<String> monster_list;
    private final DefaultListModel<String> selected_enemy_model;
    private final JList<String> selected_enemy_list;
    private final JTextArea monster_detail_area;

    private final JTextArea initiative_area;
    private final JTextArea status_area;
    private final JTextArea log_area;
    private final JLabel current_turn_label;
    private final JComboBox<String> target_box;
    private final JComboBox<String> attack_box;
    private final JTextArea attack_detail_area;
    private final JButton perform_attack_button;
    private final JButton skip_turn_button;

    private final List<Monster_Definition> filtered_monsters;
    private final List<Monster_Definition> selected_monsters;
    private List<Combatant> current_targets;
    private List<Attack_Option> current_attacks;
    private Combat_Engine combat_engine;
    private boolean settlement_shown;

    public Combat_System_UI() {
        setTitle("战斗系统");
        setSize(1180, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.filtered_monsters = new ArrayList<>();
        this.selected_monsters = new ArrayList<>();
        this.current_targets = new ArrayList<>();
        this.current_attacks = new ArrayList<>();
        this.settlement_shown = false;

        this.card_layout = new CardLayout();
        this.root_panel = new JPanel(this.card_layout);

        this.character_list_model = new DefaultListModel<>();
        this.character_list = new JList<>(this.character_list_model);
        this.monster_search_field = new JTextField();
        this.monster_list_model = new DefaultListModel<>();
        this.monster_list = new JList<>(this.monster_list_model);
        this.selected_enemy_model = new DefaultListModel<>();
        this.selected_enemy_list = new JList<>(this.selected_enemy_model);
        this.monster_detail_area = build_text_area(13);

        this.initiative_area = build_text_area(15);
        this.status_area = build_text_area(14);
        this.log_area = build_text_area(14);
        this.current_turn_label = new JLabel("当前回合：未开始");
        this.target_box = new JComboBox<>();
        this.attack_box = new JComboBox<>();
        this.attack_detail_area = build_text_area(13);
        this.perform_attack_button = new JButton("执行攻击");
        this.skip_turn_button = new JButton("结束回合");
        active_instance = this;

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (active_instance == Combat_System_UI.this) {
                    active_instance = null;
                }
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (active_instance == Combat_System_UI.this) {
                    active_instance = null;
                }
            }
        });

        root_panel.add(build_setup_panel(), "setup");
        root_panel.add(build_battle_panel(), "battle");
        add(root_panel);

        reload_character_list();
        refresh_monster_search_results();
    }

    private JPanel build_setup_panel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel header = new JLabel("选择参战角色与敌人");
        header.setFont(new Font("微软雅黑", Font.BOLD, 22));
        panel.add(header, BorderLayout.NORTH);

        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        center.setResizeWeight(0.28);
        center.setDividerLocation(300);
        center.setBorder(null);

        JPanel characterPanel = new JPanel(new BorderLayout(8, 8));
        characterPanel.setBorder(BorderFactory.createTitledBorder("参战角色"));
        character_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        characterPanel.add(new JScrollPane(character_list), BorderLayout.CENTER);
        JTextArea characterHint = build_text_area(13);
        characterHint.setText("按住 Ctrl 或 Command 可以多选。\n所有被选中的角色会参与先攻、战斗和经验结算。");
        characterHint.setEditable(false);
        characterHint.setBackground(panel.getBackground());
        characterPanel.add(characterHint, BorderLayout.SOUTH);
        center.setLeftComponent(characterPanel);

        JPanel enemyPanel = new JPanel(new BorderLayout(8, 8));
        enemyPanel.setBorder(BorderFactory.createTitledBorder("敌人图鉴与遭遇配置"));

        JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
        searchPanel.add(new JLabel("搜索敌人（支持中英双语）："), BorderLayout.NORTH);
        searchPanel.add(monster_search_field, BorderLayout.CENTER);
        enemyPanel.add(searchPanel, BorderLayout.NORTH);

        monster_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selected_enemy_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        monster_search_field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh_monster_search_results();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh_monster_search_results();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh_monster_search_results();
            }
        });

        monster_list.addListSelectionListener(e -> refresh_selected_monster_hint());
        monster_list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    add_selected_monster();
                }
            }
        });

        JSplitPane resultSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        resultSplit.setResizeWeight(0.68);
        resultSplit.setDividerLocation(470);

        JPanel libraryPanel = new JPanel(new BorderLayout(6, 6));
        libraryPanel.add(new JLabel("图鉴检索结果"), BorderLayout.NORTH);
        JScrollPane monsterScroll = new JScrollPane(monster_list);
        monsterScroll.setPreferredSize(new Dimension(420, 420));
        libraryPanel.add(monsterScroll, BorderLayout.CENTER);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplit.setResizeWeight(0.38);
        rightSplit.setDividerLocation(190);
        rightSplit.setTopComponent(wrap_with_title("已选敌人", new JScrollPane(selected_enemy_list)));
        JScrollPane detailScroll = new JScrollPane(monster_detail_area);
        detailScroll.setPreferredSize(new Dimension(320, 260));
        rightSplit.setBottomComponent(wrap_with_title("敌人详情", detailScroll));

        resultSplit.setLeftComponent(libraryPanel);
        resultSplit.setRightComponent(rightSplit);
        enemyPanel.add(resultSplit, BorderLayout.CENTER);

        JPanel enemyButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addEnemyButton = new JButton("添加敌人");
        JButton removeEnemyButton = new JButton("移除已选敌人");
        JLabel helpLabel = new JLabel("同一个敌人可以重复添加，表示多只。");
        addEnemyButton.addActionListener(e -> add_selected_monster());
        removeEnemyButton.addActionListener(e -> remove_selected_monster());
        enemyButtonPanel.add(addEnemyButton);
        enemyButtonPanel.add(removeEnemyButton);
        enemyButtonPanel.add(new JLabel("当前图鉴数量：" + Monster_Library.get_all_monsters().size()));
        enemyButtonPanel.add(helpLabel);
        enemyPanel.add(enemyButtonPanel, BorderLayout.SOUTH);

        center.setRightComponent(enemyPanel);
        panel.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton startCombatButton = new JButton("开始战斗");
        startCombatButton.setFont(new Font("微软雅黑", Font.BOLD, 16));
        startCombatButton.addActionListener(e -> start_combat());
        bottom.add(startCombatButton);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel build_battle_panel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        JLabel orderTitle = new JLabel("先攻顺序");
        orderTitle.setFont(new Font("微软雅黑", Font.BOLD, 18));
        topPanel.add(orderTitle, BorderLayout.NORTH);
        initiative_area.setRows(3);
        initiative_area.setEditable(false);
        topPanel.add(new JScrollPane(initiative_area), BorderLayout.CENTER);
        panel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        status_area.setEditable(false);
        log_area.setEditable(false);
        centerPanel.add(wrap_with_title("战场状态", new JScrollPane(status_area)));
        centerPanel.add(wrap_with_title("战斗日志", new JScrollPane(log_area)));
        panel.add(centerPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout(8, 8));
        actionPanel.setBorder(BorderFactory.createTitledBorder("当前行动"));

        JPanel currentPanel = new JPanel(new GridLayout(3, 2, 6, 6));
        current_turn_label.setFont(new Font("微软雅黑", Font.BOLD, 16));
        currentPanel.add(new JLabel("行动者："));
        currentPanel.add(current_turn_label);
        currentPanel.add(new JLabel("攻击方式："));
        currentPanel.add(attack_box);
        currentPanel.add(new JLabel("目标："));
        currentPanel.add(target_box);
        actionPanel.add(currentPanel, BorderLayout.NORTH);

        attack_detail_area.setRows(4);
        attack_detail_area.setEditable(false);
        actionPanel.add(new JScrollPane(attack_detail_area), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton backSetupButton = new JButton("返回配置");
        perform_attack_button.addActionListener(e -> perform_attack());
        skip_turn_button.addActionListener(e -> skip_turn());
        backSetupButton.addActionListener(e -> return_to_setup());
        attack_box.addActionListener(e -> refresh_attack_detail());
        buttonPanel.add(backSetupButton);
        buttonPanel.add(skip_turn_button);
        buttonPanel.add(perform_attack_button);
        actionPanel.add(buttonPanel, BorderLayout.SOUTH);

        panel.add(actionPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel wrap_with_title(String title, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JTextArea build_text_area(int fontSize) {
        JTextArea area = new JTextArea();
        area.setFont(new Font("微软雅黑", Font.PLAIN, fontSize));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(10, 10, 10, 10));
        return area;
    }

    private void reload_character_list() {
        character_list_model.clear();
        for (Character_Sheet character : Global_Data.character_pool) {
            character_list_model.addElement(character.name + " | " + character.job.class_name + " | LV." + character.job.current_level);
        }
    }

    private void refresh_monster_search_results() {
        monster_list_model.clear();
        filtered_monsters.clear();
        List<Monster_Definition> monsters = Monster_Library.search(monster_search_field.getText());
        for (Monster_Definition monster : monsters) {
            filtered_monsters.add(monster);
            monster_list_model.addElement(monster.get_display_label());
        }
        refresh_selected_monster_hint();
    }

    private void refresh_selected_monster_hint() {
        int index = monster_list.getSelectedIndex();
        if (index < 0 || index >= filtered_monsters.size()) {
            monster_detail_area.setText("");
            return;
        }
        Monster_Definition monster = filtered_monsters.get(index);
        StringBuilder sb = new StringBuilder();
        sb.append(monster.get_full_name()).append("\n");
        sb.append("类型：").append(monster.monster_type).append("\n");
        sb.append("推荐等级：").append(monster.recommended_level).append(" | AC ").append(monster.armor_class)
                .append(" | HP ").append(monster.hit_dice_count).append("d").append(monster.hit_dice_size);
        if (monster.hit_point_bonus != 0) {
            sb.append(monster.hit_point_bonus > 0 ? "+" : "").append(monster.hit_point_bonus);
        }
        sb.append("\n");
        for (Attack_Option option : monster.attack_options) {
            sb.append("- ").append(option.to_display_label()).append("\n");
        }
        monster_detail_area.setText(sb.toString());
        monster_detail_area.setCaretPosition(0);
    }

    private void add_selected_monster() {
        int index = monster_list.getSelectedIndex();
        if (index < 0 || index >= filtered_monsters.size()) {
            JOptionPane.showMessageDialog(this, "请先选择一个敌人。");
            return;
        }
        Monster_Definition monster = filtered_monsters.get(index);
        selected_monsters.add(monster);
        selected_enemy_model.addElement(monster.get_display_label());
    }

    private void remove_selected_monster() {
        int index = selected_enemy_list.getSelectedIndex();
        if (index < 0 || index >= selected_monsters.size()) {
            return;
        }
        selected_monsters.remove(index);
        selected_enemy_model.remove(index);
    }

    private void start_combat() {
        int[] selectedIndices = character_list.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(this, "请至少选择一个参战角色。");
            return;
        }
        if (selected_monsters.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请至少添加一个敌人。");
            return;
        }

        List<Character_Sheet> selectedCharacters = new ArrayList<>();
        for (int index : selectedIndices) {
            if (index >= 0 && index < Global_Data.character_pool.size()) {
                selectedCharacters.add(Global_Data.character_pool.get(index));
            }
        }

        this.combat_engine = new Combat_Engine(selectedCharacters, selected_monsters);
        this.settlement_shown = false;
        this.log_area.setText("战斗开始。\n");
        this.card_layout.show(this.root_panel, "battle");
        refresh_battle_ui();
    }

    private void refresh_battle_ui() {
        if (this.combat_engine == null) {
            return;
        }

        Combatant active = this.combat_engine.get_active_combatant();
        List<Combatant> order = this.combat_engine.get_initiative_order();
        StringBuilder orderText = new StringBuilder();
        for (int i = 0; i < order.size(); i++) {
            Combatant combatant = order.get(i);
            if (i == 0) {
                orderText.append("[当前] ");
            }
            orderText.append(combatant.display_name)
                    .append(" (先攻 ").append(combatant.initiative_total)
                    .append(", HP ").append(combatant.current_hp).append("/").append(combatant.max_hp)
                    .append(", AC ").append(combatant.get_effective_armor_class())
                    .append(")");
            if (i < order.size() - 1) {
                orderText.append(" -> ");
            }
        }
        initiative_area.setText(orderText.toString());
        initiative_area.setCaretPosition(0);

        StringBuilder statusText = new StringBuilder();
        statusText.append("【角色】\n");
        for (Combatant combatant : order) {
            if (combatant.side == Combatant.Side.PLAYER) {
                statusText.append("- ").append(combatant.display_name)
                        .append(" | AC ").append(combatant.get_effective_armor_class())
                        .append(" | HP ").append(combatant.current_hp).append("/").append(combatant.max_hp)
                        .append(" | 状态 ").append(combatant.get_status_summary())
                        .append(" | ").append(get_resource_summary(combatant))
                        .append("\n");
            }
        }
        statusText.append("\n【敌人】\n");
        for (Combatant combatant : order) {
            if (combatant.side == Combatant.Side.ENEMY) {
                statusText.append("- ").append(combatant.display_name)
                        .append(" | AC ").append(combatant.get_effective_armor_class())
                        .append(" | HP ").append(combatant.current_hp).append("/").append(combatant.max_hp)
                        .append(" | 状态 ").append(combatant.get_status_summary())
                        .append("\n");
            }
        }
        status_area.setText(statusText.toString());
        status_area.setCaretPosition(0);

        target_box.removeAllItems();
        attack_box.removeAllItems();
        current_targets = new ArrayList<>();
        current_attacks = new ArrayList<>();

        if (active == null) {
            current_turn_label.setText("当前回合：已结束");
            perform_attack_button.setEnabled(false);
            skip_turn_button.setEnabled(false);
            attack_detail_area.setText("战斗已经结束。");
            maybe_show_settlement_dialog();
            return;
        }

        if (this.combat_engine.is_combat_finished()) {
            current_turn_label.setText("当前回合：战斗结束");
            perform_attack_button.setEnabled(false);
            skip_turn_button.setEnabled(false);
            attack_detail_area.setText(this.combat_engine.did_players_win()
                    ? "战斗胜利，正在等待战后结算。"
                    : "战斗失败，当前遭遇已结束。");
            maybe_show_settlement_dialog();
            return;
        }

        current_turn_label.setText(active.display_name);
        current_targets = this.combat_engine.get_valid_targets();
        current_attacks = new ArrayList<>(active.attack_options);

        for (Combatant target : current_targets) {
            target_box.addItem(target.display_name + " | AC " + target.get_effective_armor_class() + " | HP " + target.current_hp + "/" + target.max_hp);
        }
        for (Attack_Option attack : current_attacks) {
            attack_box.addItem(attack.to_display_label());
        }

        boolean hasAction = !this.combat_engine.is_combat_finished() && !current_targets.isEmpty() && !current_attacks.isEmpty();
        perform_attack_button.setEnabled(hasAction && !active.is_turn_blocked());
        skip_turn_button.setEnabled(!this.combat_engine.is_combat_finished());
        if (active.is_turn_blocked()) {
            attack_detail_area.setText("当前角色处于无法行动状态，只能结束回合。\n资源：" + get_resource_summary(active));
            return;
        }
        refresh_attack_detail();
    }

    private void refresh_attack_detail() {
        int index = attack_box.getSelectedIndex();
        if (index < 0 || index >= current_attacks.size()) {
            attack_detail_area.setText("请选择一个攻击方式。");
            return;
        }
        Attack_Option attack = current_attacks.get(index);
        attack_detail_area.setText(attack.to_display_label() + "\n" + attack.description);
        Combatant active = this.combat_engine == null ? null : this.combat_engine.get_active_combatant();
        if (active != null) {
            attack_detail_area.append("\n当前资源：" + get_resource_summary(active));
        }
        attack_detail_area.setCaretPosition(0);
    }

    private void perform_attack() {
        if (this.combat_engine == null || this.combat_engine.is_combat_finished()) {
            return;
        }
        int attackIndex = attack_box.getSelectedIndex();
        int targetIndex = target_box.getSelectedIndex();
        if (attackIndex < 0 || targetIndex < 0 || attackIndex >= current_attacks.size() || targetIndex >= current_targets.size()) {
            JOptionPane.showMessageDialog(this, "请选择攻击方式和目标。");
            return;
        }
        String result = this.combat_engine.execute_attack(current_attacks.get(attackIndex), current_targets.get(targetIndex));
        append_log(result);
        refresh_battle_ui();
        append_pending_system_log();
    }

    private void skip_turn() {
        if (this.combat_engine == null || this.combat_engine.is_combat_finished()) {
            return;
        }
        append_log(this.combat_engine.skip_turn());
        refresh_battle_ui();
        append_pending_system_log();
    }

    private void append_log(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        if (!log_area.getText().trim().isEmpty()) {
            log_area.append("\n\n");
        }
        log_area.append(text);
        log_area.setCaretPosition(log_area.getDocument().getLength());
    }

    private void return_to_setup() {
        this.combat_engine = null;
        this.current_attacks = new ArrayList<>();
        this.current_targets = new ArrayList<>();
        this.settlement_shown = false;
        this.attack_detail_area.setText("");
        this.card_layout.show(this.root_panel, "setup");
    }

    public static Combat_System_UI get_active_instance() {
        return active_instance;
    }

    public Combat_Engine get_current_combat_engine() {
        return this.combat_engine;
    }

    public void refresh_after_external_effect(String logText) {
        append_log(logText);
        refresh_battle_ui();
        append_pending_system_log();
    }

    private void append_pending_system_log() {
        if (this.combat_engine == null) {
            return;
        }
        append_log(this.combat_engine.get_and_clear_pending_log());
    }

    private String get_resource_summary(Combatant combatant) {
        List<String> parts = new ArrayList<>();
        for (int level = 1; level < combatant.spell_slots_remaining.length; level++) {
            if (combatant.spell_slots_remaining[level] > 0) {
                parts.add(level + "环位 " + combatant.spell_slots_remaining[level]);
            }
        }
        if (combatant.pact_slots_remaining > 0) {
            parts.add("契约位 " + combatant.pact_slots_remaining + "@" + combatant.pact_slot_level + "环");
        }
        if (combatant.sorcery_points_remaining > 0) {
            parts.add("术法点 " + combatant.sorcery_points_remaining);
        }
        if (combatant.superiority_dice_remaining > 0) {
            parts.add("卓越骰 " + combatant.superiority_dice_remaining + "d" + combatant.superiority_dice_size);
        }
        if (combatant.lay_on_hands_remaining > 0) {
            parts.add("圣疗池 " + combatant.lay_on_hands_remaining);
        }
        return parts.isEmpty() ? "无特殊消耗资源" : String.join(" | ", parts);
    }

    private void maybe_show_settlement_dialog() {
        if (this.combat_engine == null || !this.combat_engine.is_combat_finished() || !this.combat_engine.did_players_win() || this.settlement_shown) {
            return;
        }
        this.settlement_shown = true;
        SettlementDialog dialog = new SettlementDialog(this, this.combat_engine);
        dialog.setVisible(true);
        append_log("战后结算完成。剩余未分配掉落：" + this.combat_engine.get_pending_loot_keys().size() + " 件。");
        refresh_battle_ui();
    }

    private static class SettlementDialog extends JDialog {
        private final Combat_Engine combatEngine;
        private final JTextArea xpArea;
        private final DefaultListModel<String> lootModel;
        private final JList<String> lootList;

        SettlementDialog(Frame owner, Combat_Engine combatEngine) {
            super(owner, "战斗结算", true);
            this.combatEngine = combatEngine;
            this.xpArea = new JTextArea();
            this.lootModel = new DefaultListModel<>();
            this.lootList = new JList<>(this.lootModel);

            setSize(820, 620);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
            splitPane.setResizeWeight(0.22);
            splitPane.setDividerLocation(140);

            xpArea.setEditable(false);
            xpArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            xpArea.setMargin(new Insets(10, 10, 10, 10));
            splitPane.setTopComponent(wrapPanel("经验结算", new JScrollPane(xpArea)));

            lootList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            lootList.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    maybeShowMenu(e);
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    maybeShowMenu(e);
                }
            });

            JTextArea hintArea = new JTextArea();
            hintArea.setEditable(false);
            hintArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
            hintArea.setBackground(getBackground());
            hintArea.setText("上方显示参战角色经验进度 a/b。\n下方掉落物支持单选或多选，左键选择，右键后把选中物品分配给指定角色。");

            JPanel lootPanel = new JPanel(new BorderLayout(8, 8));
            lootPanel.add(hintArea, BorderLayout.NORTH);
            lootPanel.add(new JScrollPane(lootList), BorderLayout.CENTER);
            splitPane.setBottomComponent(wrapPanel("怪物掉落", lootPanel));

            add(splitPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton closeButton = new JButton("完成结算");
            closeButton.addActionListener(e -> dispose());
            buttonPanel.add(closeButton);
            add(buttonPanel, BorderLayout.SOUTH);

            refreshXpArea();
            refreshLootModel();
        }

        private JPanel wrapPanel(String title, JComponent component) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createTitledBorder(title));
            panel.add(component, BorderLayout.CENTER);
            return panel;
        }

        private void refreshXpArea() {
            StringBuilder sb = new StringBuilder();
            for (Character_Sheet character : this.combatEngine.get_participating_characters()) {
                int nextXp = character.get_next_level_xp();
                String progress = nextXp < 0 ? character.experience_points + "/已满级" : character.experience_points + "/" + nextXp;
                sb.append(character.name)
                        .append(" | XP ")
                        .append(progress)
                        .append("\n");
            }
            xpArea.setText(sb.toString());
            xpArea.setCaretPosition(0);
        }

        private void refreshLootModel() {
            this.lootModel.clear();
            List<String> lootKeys = this.combatEngine.get_pending_loot_keys();
            if (lootKeys.isEmpty()) {
                this.lootModel.addElement("本次无掉落物，或已全部分配完成。");
                this.lootList.setEnabled(false);
                return;
            }
            this.lootList.setEnabled(true);
            for (String lootKey : lootKeys) {
                Equipment_Item item = Equipment_Library.get_item(lootKey);
                this.lootModel.addElement(item == null ? lootKey : item.to_inventory_line());
            }
        }

        private void maybeShowMenu(java.awt.event.MouseEvent e) {
            if (!e.isPopupTrigger() || !lootList.isEnabled()) {
                return;
            }
            int index = lootList.locationToIndex(e.getPoint());
            if (index >= 0 && !lootList.isSelectedIndex(index)) {
                lootList.setSelectedIndex(index);
            }
            int[] selectedIndices = lootList.getSelectedIndices();
            if (selectedIndices.length == 0) {
                return;
            }

            JPopupMenu menu = new JPopupMenu();
            for (Character_Sheet character : this.combatEngine.get_participating_characters()) {
                JMenuItem item = new JMenuItem("给予 " + character.name);
                item.addActionListener(ae -> assignSelectedLoot(character));
                menu.add(item);
            }
            menu.show(lootList, e.getX(), e.getY());
        }

        private void assignSelectedLoot(Character_Sheet receiver) {
            int[] selectedIndices = lootList.getSelectedIndices();
            List<String> pendingLoot = this.combatEngine.get_pending_loot_keys();
            List<String> toAssign = new ArrayList<>();
            for (int selectedIndex : selectedIndices) {
                if (selectedIndex >= 0 && selectedIndex < pendingLoot.size()) {
                    toAssign.add(pendingLoot.get(selectedIndex));
                }
            }
            this.combatEngine.assign_loot(toAssign, receiver);
            refreshLootModel();
        }
    }
}
