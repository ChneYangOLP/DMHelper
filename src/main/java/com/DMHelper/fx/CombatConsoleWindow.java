package com.DMHelper.fx;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Encounter_Log_PDF;
import com.DMHelper.basic.combat.Attack_Option;
import com.DMHelper.basic.combat.Combat_Engine;
import com.DMHelper.basic.combat.Combat_Status_Effect;
import com.DMHelper.basic.combat.Combat_Status_Type;
import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.combat.Dice_Util;
import com.DMHelper.basic.combat.Monster_Definition;
import com.DMHelper.basic.combat.Monster_Library;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.DMHelper.basic.database.Global_Data;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Optional;

/**
 * D&D 5e 战斗系统窗口 - JavaFX 完整版
 * <p>
 * 功能与 Swing 版 Combat_System_UI 完全一致：
 * - 配置面板（Setup）：选择参战角色、搜索/添加敌人、开始战斗
 * - 战斗面板（Battle）：先攻顺序、战场状态、战斗日志、攻击/道具/回合管理
 * - 道具使用系统：治疗物品、各类卷轴、炸弹、药水、钱币、任务物品等
 * - 战后结算：经验进度、掉落物分配、PDF 导出
 */
public class CombatConsoleWindow extends Stage {

    // ==================== 面板切换 ====================
    private final StackPane rootStack;
    private Node setupPanel;
    private Node battlePanel;

    // ==================== 配置面板控件 ====================
    private final ObservableList<String> characterListModel = FXCollections.observableArrayList();
    private final ListView<String> characterList;
    private final Set<Integer> selectedCharacterIndices = new LinkedHashSet<>();

    private final TextField monsterSearchField;
    private final ObservableList<String> monsterListModel = FXCollections.observableArrayList();
    private final ListView<String> monsterList;
    private final ObservableList<String> selectedEnemyModel = FXCollections.observableArrayList();
    private final ListView<String> selectedEnemyList;
    private final TextArea monsterDetailArea;

    // ==================== 战斗面板控件 ====================
    private final TextArea initiativeArea;
    private final TextArea statusArea;
    private final TextArea logArea;
    private final Label currentTurnLabel;
    private final ComboBox<String> targetBox;
    private final ComboBox<String> attackBox;
    private final TextArea attackDetailArea;
    private final Button performAttackButton;
    private final Button skipTurnButton;
    private final Button useItemButton;

    // ==================== 内部状态 ====================
    private final List<Monster_Definition> filteredMonsters = new ArrayList<>();
    private final List<Monster_Definition> selectedMonsters = new ArrayList<>();
    private List<Combatant> currentTargets = new ArrayList<>();
    private List<Attack_Option> currentAttacks = new ArrayList<>();
    private Combat_Engine combatEngine;
    private boolean settlementShown;

    // ==================== 构造函数 ====================
    public CombatConsoleWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("战斗系统");
        setMinWidth(1200);
        setMinHeight(850);
        setWidth(1280);
        setHeight(880);

        this.rootStack = new StackPane();
        this.characterList = new ListView<>(characterListModel);
        this.monsterSearchField = new TextField();
        this.monsterList = new ListView<>(monsterListModel);
        this.selectedEnemyList = new ListView<>(selectedEnemyModel);
        this.monsterDetailArea = new TextArea();
        this.initiativeArea = new TextArea();
        this.statusArea = new TextArea();
        this.logArea = new TextArea();
        this.currentTurnLabel = new Label("当前回合：未开始");
        this.targetBox = new ComboBox<>();
        this.attackBox = new ComboBox<>();
        this.attackDetailArea = new TextArea();
        this.performAttackButton = new Button("执行攻击");
        this.skipTurnButton = new Button("结束回合");
        this.useItemButton = new Button("使用道具");
        this.settlementShown = false;

        // 构建两个面板
        this.setupPanel = buildSetupPanel();
        this.battlePanel = buildBattlePanel();
        rootStack.getChildren().addAll(setupPanel, battlePanel);
        showSetupPanel();

        Scene scene = new Scene(rootStack, 1280, 880);
        FxThemes.apply(scene);
        setScene(scene);

        // 初始化数据
        reloadCharacterList();
        refreshMonsterSearchResults();

        // 窗口关闭时清理
        setOnCloseRequest(e -> {
            combatEngine = null;
        });
    }

    // ==================== 面板切换 ====================
    private void showSetupPanel() {
        setupPanel.setVisible(true);
        setupPanel.setManaged(true);
        battlePanel.setVisible(false);
        battlePanel.setManaged(false);
    }

    private void showBattlePanel() {
        setupPanel.setVisible(false);
        setupPanel.setManaged(false);
        battlePanel.setVisible(true);
        battlePanel.setManaged(true);
    }

    // ==================== 配置面板构建 ====================
    private Node buildSetupPanel() {
        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(16, 16, 16, 16));

        // 顶部标题
        Label header = new Label("选择参战角色与敌人");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        panel.setTop(header);

        // 中间区域：左侧角色列表 + 右侧敌人配置
        SplitPane centerSplit = new SplitPane();
        centerSplit.setDividerPositions(0.28);

        // 左侧：参战角色
        VBox characterPanel = buildSection("参战角色");
        characterList.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                    return;
                }
                int index = getIndex();
                Character_Sheet character = index >= 0 && index < Global_Data.character_pool.size()
                        ? Global_Data.character_pool.get(index) : null;
                boolean dead = character != null && !character.is_alive();
                boolean selected = selectedCharacterIndices.contains(index) && !dead;

                if (selected) {
                    setText("[已选] " + item);
                    setStyle("-fx-font-weight: bold;");
                } else if (dead) {
                    setText("[已死亡] " + item);
                    setStyle("-fx-text-fill: #825a5a;");
                } else {
                    setText(item);
                    setStyle("");
                }
            }
        });
        characterList.setOnMouseClicked(e -> {
            int index = characterList.getSelectionModel().getSelectedIndex();
            if (index < 0) return;
            if (!isCharacterSelectable(index)) {
                Character_Sheet character = Global_Data.character_pool.get(index);
                showAlert(Alert.AlertType.WARNING,
                        "[" + character.name + "] 当前生命值为 0，已视为死亡，不能加入本次战斗。");
                return;
            }
            toggleCharacterSelection(index);
        });
        VBox.setVgrow(characterList, Priority.ALWAYS);
        characterPanel.getChildren().add(characterList);

        TextArea characterHint = new TextArea("单击角色即可切换是否参战。\n生命值为 0 的角色会显示为 [已死亡]，不能加入本次战斗。");
        characterHint.setEditable(false);
        characterHint.setWrapText(true);
        characterHint.setPrefRowCount(2);
        characterHint.setStyle("-fx-text-fill: #695d4a; -fx-background-color: transparent;");
        characterPanel.getChildren().add(characterHint);
        centerSplit.getItems().add(characterPanel);

        // 右侧：敌人图鉴与遭遇配置
        VBox enemyPanel = buildSection("敌人图鉴与遭遇配置");

        // 搜索栏
        VBox searchBox = new VBox(4);
        Label searchLabel = new Label("搜索敌人（支持中英双语）：");
        searchLabel.setStyle("-fx-text-fill: #695d4a;");
        monsterSearchField.setPromptText("输入名称搜索...");
        monsterSearchField.textProperty().addListener((obs, oldVal, newVal) -> refreshMonsterSearchResults());
        searchBox.getChildren().addAll(searchLabel, monsterSearchField);
        enemyPanel.getChildren().add(searchBox);

        // 图鉴检索结果 + 已选敌人 + 敌人详情
        SplitPane resultSplit = new SplitPane();
        resultSplit.setDividerPositions(0.68);
        resultSplit.setOrientation(Orientation.HORIZONTAL);

        // 左侧：图鉴检索结果
        VBox libraryPanel = buildSection("图鉴检索结果");
        monsterList.setOnMouseClicked(e -> {
            refreshSelectedMonsterHint();
            if (e.getClickCount() == 2) {
                addSelectedMonster();
            }
        });
        VBox.setVgrow(monsterList, Priority.ALWAYS);
        libraryPanel.getChildren().add(monsterList);
        resultSplit.getItems().add(libraryPanel);

        // 右侧：已选敌人 + 敌人详情
        SplitPane rightSplit = new SplitPane();
        rightSplit.setDividerPositions(0.38);
        rightSplit.setOrientation(Orientation.VERTICAL);

        VBox selectedEnemyPanel = buildSection("已选敌人");
        selectedEnemyList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                removeSelectedMonster();
            }
        });
        VBox.setVgrow(selectedEnemyList, Priority.ALWAYS);
        selectedEnemyPanel.getChildren().add(selectedEnemyList);
        rightSplit.getItems().add(selectedEnemyPanel);

        VBox detailPanel = buildSection("敌人详情");
        monsterDetailArea.setEditable(false);
        monsterDetailArea.setWrapText(true);
        monsterDetailArea.setPrefRowCount(10);
        VBox.setVgrow(monsterDetailArea, Priority.ALWAYS);
        detailPanel.getChildren().add(monsterDetailArea);
        rightSplit.getItems().add(detailPanel);

        resultSplit.getItems().add(rightSplit);
        VBox.setVgrow(resultSplit, Priority.ALWAYS);
        enemyPanel.getChildren().add(resultSplit);

        // 底部按钮
        HBox enemyButtonBar = new HBox(10);
        enemyButtonBar.setAlignment(Pos.CENTER_LEFT);
        Button addEnemyButton = new Button("添加敌人");
        addEnemyButton.getStyleClass().add("primary-button");
        addEnemyButton.setOnAction(e -> addSelectedMonster());
        Button removeEnemyButton = new Button("移除已选敌人");
        removeEnemyButton.setOnAction(e -> removeSelectedMonster());
        Label helpLabel = new Label("同一个敌人可以重复添加，表示多只；已选敌人支持双击移除。");
        helpLabel.setStyle("-fx-text-fill: #695d4a;");
        Label countLabel = new Label("当前图鉴数量：" + Monster_Library.get_all_monsters().size());
        countLabel.setStyle("-fx-text-fill: #695d4a;");
        enemyButtonBar.getChildren().addAll(addEnemyButton, removeEnemyButton, countLabel, helpLabel);
        enemyPanel.getChildren().add(enemyButtonBar);

        centerSplit.getItems().add(enemyPanel);
        panel.setCenter(centerSplit);

        // 底部：开始战斗按钮
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        Button startCombatButton = new Button("开始战斗");
        startCombatButton.getStyleClass().add("primary-button");
        startCombatButton.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        startCombatButton.setOnAction(e -> startCombat());
        bottomBar.getChildren().add(startCombatButton);
        panel.setBottom(bottomBar);

        return panel;
    }

    // ==================== 战斗面板构建 ====================
    private Node buildBattlePanel() {
        BorderPane panel = new BorderPane();
        panel.setPadding(new Insets(16, 16, 16, 16));

        // 顶部：先攻顺序
        VBox topPanel = buildSection("先攻顺序");
        Label orderTitle = new Label("先攻顺序");
        orderTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        initiativeArea.setEditable(false);
        initiativeArea.setWrapText(true);
        initiativeArea.setPrefRowCount(3);
        topPanel.getChildren().addAll(orderTitle, initiativeArea);
        panel.setTop(topPanel);

        // 中部：战场状态 + 战斗日志
        HBox centerPanel = new HBox(12);
        VBox statusPanel = buildSection("战场状态");
        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        VBox.setVgrow(statusArea, Priority.ALWAYS);
        statusPanel.getChildren().add(statusArea);
        HBox.setHgrow(statusPanel, Priority.ALWAYS);

        VBox logPanel = buildSection("战斗日志");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logPanel.getChildren().add(logArea);
        HBox.setHgrow(logPanel, Priority.ALWAYS);

        centerPanel.getChildren().addAll(statusPanel, logPanel);
        panel.setCenter(centerPanel);

        // 底部：当前行动区域
        VBox actionPanel = buildSection("当前行动");

        // 行动者信息
        GridPane currentInfo = new GridPane();
        currentInfo.setHgap(8);
        currentInfo.setVgap(6);
        currentTurnLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #977039;");
        currentInfo.add(new Label("行动者："), 0, 0);
        currentInfo.add(currentTurnLabel, 1, 0);
        currentInfo.add(new Label("攻击方式："), 0, 1);
        currentInfo.add(attackBox, 1, 1);
        currentInfo.add(new Label("目标："), 0, 2);
        currentInfo.add(targetBox, 1, 2);
        attackBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> refreshTargetOptionsForSelectedAttack());
        actionPanel.getChildren().add(currentInfo);

        // 攻击详情
        attackDetailArea.setEditable(false);
        attackDetailArea.setWrapText(true);
        attackDetailArea.setPrefRowCount(4);
        VBox.setVgrow(attackDetailArea, Priority.SOMETIMES);
        actionPanel.getChildren().add(attackDetailArea);

        // 按钮栏
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        Button backSetupButton = new Button("返回配置");
        backSetupButton.setOnAction(e -> returnToSetup());
        performAttackButton.getStyleClass().add("primary-button");
        performAttackButton.setOnAction(e -> performAttack());
        skipTurnButton.setOnAction(e -> skipTurn());
        useItemButton.setOnAction(e -> useItemInCombat());
        buttonBar.getChildren().addAll(backSetupButton, useItemButton, skipTurnButton, performAttackButton);
        actionPanel.getChildren().add(buttonBar);

        panel.setBottom(actionPanel);

        return panel;
    }

    // ==================== 辅助 UI 方法 ====================
    private VBox buildSection(String title) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8, 8, 8, 8));
        box.setStyle("-fx-border-color: #b7a685; -fx-border-width: 1; -fx-border-radius: 4;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        box.getChildren().add(titleLabel);
        return box;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(this);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== 配置面板逻辑 ====================
    private void reloadCharacterList() {
        // 保留有效选中
        Set<Integer> validSelection = new LinkedHashSet<>();
        for (Integer index : this.selectedCharacterIndices) {
            if (index != null && index >= 0 && index < Global_Data.character_pool.size()
                    && isCharacterSelectable(index)) {
                validSelection.add(index);
            }
        }
        this.selectedCharacterIndices.clear();
        this.selectedCharacterIndices.addAll(validSelection);
        characterListModel.clear();
        for (Character_Sheet character : Global_Data.character_pool) {
            characterListModel.add(buildCharacterSetupLabel(character));
        }
    }

    private void toggleCharacterSelection(int index) {
        if (!isCharacterSelectable(index)) return;
        if (this.selectedCharacterIndices.contains(index)) {
            this.selectedCharacterIndices.remove(index);
        } else {
            this.selectedCharacterIndices.add(index);
        }
        characterList.refresh();
    }

    private boolean isCharacterSelectable(int index) {
        return index >= 0
                && index < Global_Data.character_pool.size()
                && Global_Data.character_pool.get(index) != null
                && Global_Data.character_pool.get(index).is_alive();
    }

    private String buildCharacterSetupLabel(Character_Sheet character) {
        if (character == null) return "";
        return character.name + " | " + character.job.class_name
                + " | LV." + character.job.current_level
                + " | HP " + character.get_hp_summary();
    }

    private void refreshMonsterSearchResults() {
        monsterListModel.clear();
        filteredMonsters.clear();
        List<Monster_Definition> monsters = Monster_Library.search(monsterSearchField.getText());
        for (Monster_Definition monster : monsters) {
            filteredMonsters.add(monster);
            monsterListModel.add(monster.get_display_label());
        }
        refreshSelectedMonsterHint();
    }

    private void refreshSelectedMonsterHint() {
        int index = monsterList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= filteredMonsters.size()) {
            monsterDetailArea.setText("");
            return;
        }
        Monster_Definition monster = filteredMonsters.get(index);
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
        monsterDetailArea.setText(sb.toString());
    }

    private void addSelectedMonster() {
        int index = monsterList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= filteredMonsters.size()) {
            showAlert(Alert.AlertType.WARNING, "请先选择一个敌人。");
            return;
        }
        Monster_Definition monster = filteredMonsters.get(index);
        selectedMonsters.add(monster);
        selectedEnemyModel.add(monster.get_display_label());
    }

    private void removeSelectedMonster() {
        int index = selectedEnemyList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= selectedMonsters.size()) return;
        selectedMonsters.remove(index);
        selectedEnemyModel.remove(index);
    }

    // ==================== 战斗开始 ====================
    private void startCombat() {
        if (this.selectedCharacterIndices.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "请至少选择一个参战角色。");
            return;
        }
        if (selectedMonsters.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "请至少添加一个敌人。");
            return;
        }

        List<Character_Sheet> selectedCharacters = new ArrayList<>();
        List<String> removedDeadNames = new ArrayList<>();
        for (int index : this.selectedCharacterIndices) {
            if (index >= 0 && index < Global_Data.character_pool.size()) {
                Character_Sheet character = Global_Data.character_pool.get(index);
                if (character.is_alive()) {
                    selectedCharacters.add(character);
                } else {
                    removedDeadNames.add(character.name);
                }
            }
        }
        if (!removedDeadNames.isEmpty()) {
            this.selectedCharacterIndices.removeIf(index ->
                    index != null
                            && index >= 0
                            && index < Global_Data.character_pool.size()
                            && !Global_Data.character_pool.get(index).is_alive());
            characterList.refresh();
            showAlert(Alert.AlertType.INFORMATION,
                    "以下角色已死亡，已自动从参战列表中移除：\n" + String.join("、", removedDeadNames));
        }
        if (selectedCharacters.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可参战的存活角色。");
            return;
        }

        this.combatEngine = new Combat_Engine(selectedCharacters, selectedMonsters);
        this.settlementShown = false;
        this.logArea.setText("战斗开始。\n");
        showBattlePanel();
        refreshBattleUI();
    }

    // ==================== 战斗 UI 刷新 ====================
    private void refreshBattleUI() {
        if (this.combatEngine == null) return;

        Combatant active = this.combatEngine.get_active_combatant();
        List<Combatant> order = this.combatEngine.get_initiative_order();

        // 先攻顺序
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
        initiativeArea.setText(orderText.toString());

        // 战场状态
        StringBuilder statusText = new StringBuilder();
        statusText.append("【状态倒计时】\n");
        boolean hasCountdown = false;
        for (Combatant combatant : order) {
            if (combatant.get_status_summary().equals("无")) continue;
            hasCountdown = true;
            statusText.append("- ").append(combatant.display_name)
                    .append(" -> ").append(combatant.get_status_summary()).append("\n");
        }
        if (!hasCountdown) {
            statusText.append("- 当前没有需要倒计时的状态效果。\n");
        }
        statusText.append("\n【角色】\n");
        for (Combatant combatant : order) {
            if (combatant.side == Combatant.Side.PLAYER) {
                statusText.append("- ").append(combatant.display_name)
                        .append(" | AC ").append(combatant.get_effective_armor_class())
                        .append(" | HP ").append(combatant.current_hp).append("/").append(combatant.max_hp)
                        .append(" | 状态 ").append(combatant.get_status_summary())
                        .append(" | ").append(getResourceSummary(combatant))
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
        statusArea.setText(statusText.toString());

        // 清空攻击/目标选择
        targetBox.getItems().clear();
        attackBox.getItems().clear();
        currentTargets = new ArrayList<>();
        currentAttacks = new ArrayList<>();

        if (active == null) {
            currentTurnLabel.setText("当前回合：已结束");
            performAttackButton.setDisable(true);
            skipTurnButton.setDisable(true);
            useItemButton.setDisable(true);
            attackDetailArea.setText("战斗已经结束。");
            maybeShowSettlementDialog();
            return;
        }

        if (this.combatEngine.is_combat_finished()) {
            currentTurnLabel.setText("当前回合：战斗结束");
            performAttackButton.setDisable(true);
            skipTurnButton.setDisable(true);
            useItemButton.setDisable(true);
            attackDetailArea.setText(this.combatEngine.did_players_win()
                    ? "战斗胜利，正在等待战后结算。"
                    : "战斗失败，当前遭遇已结束。");
            maybeShowSettlementDialog();
            return;
        }

        currentTurnLabel.setText(active.display_name);
        currentAttacks = new ArrayList<>(active.attack_options);
        for (Attack_Option attack : currentAttacks) {
            attackBox.getItems().add(attack.to_display_label());
        }

        skipTurnButton.setDisable(false);
        useItemButton.setDisable(!canActiveCombatantUseItem(active));

        if (active.is_turn_blocked()) {
            attackDetailArea.setText("当前角色处于无法行动状态，只能结束回合。\n资源：" + getResourceSummary(active));
            return;
        }

        if (!currentAttacks.isEmpty()) {
            attackBox.getSelectionModel().selectFirst();
        }
        refreshTargetOptionsForSelectedAttack();
    }

    private void refreshAttackDetail() {
        int index = attackBox.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= currentAttacks.size()) {
            attackDetailArea.setText("请选择一个攻击方式。");
            return;
        }
        Attack_Option attack = currentAttacks.get(index);
        attackDetailArea.setText(attack.to_display_label() + "\n" + attack.description);
        Combatant active = this.combatEngine == null ? null : this.combatEngine.get_active_combatant();
        if (active != null) {
            attackDetailArea.appendText("\n当前资源：" + getResourceSummary(active));
            attackDetailArea.appendText("\n当前状态倒计时：" + active.get_status_summary());
            int targetIndex = targetBox.getSelectionModel().getSelectedIndex();
            if (targetIndex >= 0 && targetIndex < currentTargets.size()) {
                attackDetailArea.appendText("\n目标状态倒计时：" + currentTargets.get(targetIndex).get_status_summary());
            }
        }
    }

    private void refreshTargetOptionsForSelectedAttack() {
        targetBox.getItems().clear();
        currentTargets = new ArrayList<>();
        int attackIndex = attackBox.getSelectionModel().getSelectedIndex();
        if (attackIndex >= 0 && attackIndex < currentAttacks.size() && this.combatEngine != null) {
            currentTargets = this.combatEngine.get_valid_targets(currentAttacks.get(attackIndex));
            for (Combatant target : currentTargets) {
                targetBox.getItems().add(target.display_name + " | AC " + target.get_effective_armor_class()
                        + " | HP " + target.current_hp + "/" + target.max_hp);
            }
        }
        Combatant active = this.combatEngine == null ? null : this.combatEngine.get_active_combatant();
        boolean hasAction = !currentAttacks.isEmpty() && !currentTargets.isEmpty()
                && active != null && !this.combatEngine.is_combat_finished();
        performAttackButton.setDisable(!(hasAction && !active.is_turn_blocked()));
        refreshAttackDetail();
    }

    // ==================== 战斗操作 ====================
    private void performAttack() {
        if (this.combatEngine == null || this.combatEngine.is_combat_finished()) return;
        int attackIndex = attackBox.getSelectionModel().getSelectedIndex();
        int targetIndex = targetBox.getSelectionModel().getSelectedIndex();
        if (attackIndex < 0 || targetIndex < 0
                || attackIndex >= currentAttacks.size() || targetIndex >= currentTargets.size()) {
            showAlert(Alert.AlertType.WARNING, "请选择攻击方式和目标。");
            return;
        }
        String result = this.combatEngine.execute_attack(currentAttacks.get(attackIndex), currentTargets.get(targetIndex));
        appendLog(result);
        refreshBattleUI();
        appendPendingSystemLog();
    }

    private void skipTurn() {
        if (this.combatEngine == null || this.combatEngine.is_combat_finished()) return;
        appendLog(this.combatEngine.skip_turn());
        refreshBattleUI();
        appendPendingSystemLog();
    }

    private void returnToSetup() {
        this.combatEngine = null;
        this.currentAttacks = new ArrayList<>();
        this.currentTargets = new ArrayList<>();
        this.settlementShown = false;
        this.attackDetailArea.setText("");
        showSetupPanel();
    }

    // ==================== 道具使用系统 ====================
    private boolean canActiveCombatantUseItem(Combatant active) {
        return active != null
                && active.linked_character != null
                && !active.is_turn_blocked()
                && !getUsableBackpackItems(active.linked_character).isEmpty();
    }

    private List<Equipment_Item> getUsableBackpackItems(Character_Sheet character) {
        List<Equipment_Item> usable = new ArrayList<>();
        if (character == null) return usable;
        for (Equipment_Item item : character.get_owned_items_for_slot(Equipment_Slot.BACKPACK)) {
            if (item != null && item.is_usable_inventory_item() && character.get_item_count(item.key) > 0) {
                usable.add(item);
            }
        }
        return usable;
    }

    private void useItemInCombat() {
        if (this.combatEngine == null || this.combatEngine.is_combat_finished()) return;
        Combatant active = this.combatEngine.get_active_combatant();
        if (active == null || active.linked_character == null) {
            showAlert(Alert.AlertType.WARNING, "当前行动者不是可使用背包物品的玩家角色。");
            return;
        }
        if (active.is_turn_blocked()) {
            showAlert(Alert.AlertType.WARNING, "当前角色处于无法行动状态，不能使用道具。");
            return;
        }

        List<Equipment_Item> usableItems = getUsableBackpackItems(active.linked_character);
        if (usableItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前角色背包里没有可在战斗中使用的道具。");
            return;
        }

        // 构建道具选择对话框
        ComboBox<InventoryChoice> itemBox = new ComboBox<>();
        for (Equipment_Item item : usableItems) {
            itemBox.getItems().add(new InventoryChoice(active.linked_character, item));
        }
        itemBox.setConverter(new StringConverter<InventoryChoice>() {
            @Override
            public String toString(InventoryChoice choice) {
                return choice == null ? "" : choice.label;
            }
            @Override
            public InventoryChoice fromString(String string) {
                return null;
            }
        });

        TextArea hintArea = new TextArea("选择一个道具后，会在下一步弹出对应的目标或备注输入。");
        hintArea.setEditable(false);
        hintArea.setWrapText(true);
        hintArea.setPrefRowCount(4);

        itemBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                hintArea.setText(newVal.item.display_name + "\n" + newVal.item.get_use_hint());
            }
        });
        if (!itemBox.getItems().isEmpty()) {
            itemBox.getSelectionModel().selectFirst();
        }

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择要在本回合使用的背包道具"),
                itemBox,
                hintArea
        );

        Optional<ButtonType> result = showCustomDialog("战斗中使用道具", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return;

        InventoryChoice choice = itemBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.item == null) return;

        String logText = resolveItemUseInCombat(active, choice.item);
        if (logText == null || logText.trim().isEmpty()) return;

        appendLog(logText);
        if (this.combatEngine != null && !this.combatEngine.is_combat_finished()) {
            appendLog(this.combatEngine.skip_turn());
        }
        refreshBattleUI();
        appendPendingSystemLog();
    }

    private Optional<ButtonType> showCustomDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(this);
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog.showAndWait();
    }

    private Optional<ButtonType> showCustomDialogWithScrollPane(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(this);
        dialog.setTitle(title);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(400);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog.showAndWait();
    }

    // ==================== 道具结算（大量分支） ====================
    private String resolveItemUseInCombat(Combatant active, Equipment_Item item) {
        if (item.is_healing_item()) {
            return useHealingItemInCombat(active, item);
        }
        if ("scroll_of_magic_missile".equals(item.key)) {
            return useMagicMissileScrollInCombat(active, item);
        }
        if ("scroll_of_arcane_insight".equals(item.key)) {
            return useArcaneInsightScrollInCombat(active, item);
        }
        if ("scroll_of_fireball".equals(item.key)) {
            return useFireballScrollInCombat(active, item);
        }
        if ("scroll_of_shield".equals(item.key)) {
            return useStatusItemInCombat(active, item, Combat_Status_Type.SHIELDED, 1, Combatant.Side.PLAYER,
                    "获得护盾状态，当前系统中提供 +5 AC，持续 1 轮。");
        }
        if ("scroll_of_mage_armor".equals(item.key)) {
            return useStatusItemInCombat(active, item, Combat_Status_Type.SHIELDED, 3, Combatant.Side.PLAYER,
                    "获得法师护甲保护，当前系统中以护盾状态近似，提供额外 AC，持续 3 轮。");
        }
        if ("scroll_of_misty_step".equals(item.key)) {
            return useMistyStepScrollInCombat(active, item);
        }
        if ("scroll_of_detect_magic".equals(item.key)) {
            return useDetectMagicScrollInCombat(active, item);
        }
        if ("scroll_of_web".equals(item.key)) {
            return useControlScrollInCombat(active, item, Combat_Status_Type.RESTRAINED, 2, "Dexterity", 13,
                    "蛛网缠住了目标，进入束缚状态。");
        }
        if ("scroll_of_hold_person".equals(item.key)) {
            return useControlScrollInCombat(active, item, Combat_Status_Type.PARALYZED, 2, "Wisdom", 14,
                    "人类定身术生效，目标陷入麻痹。");
        }
        if ("scroll_of_sleep".equals(item.key)) {
            return useSleepScrollInCombat(active, item);
        }
        if ("scroll_of_scorching_ray".equals(item.key)) {
            return useScorchingRayScrollInCombat(active, item);
        }
        if ("scroll_of_ray_of_frost".equals(item.key)) {
            return useRayOfFrostScrollInCombat(active, item);
        }
        if ("scroll_of_dispel_magic".equals(item.key)) {
            return useDispelMagicScrollInCombat(active, item);
        }
        if (item.is_bomb_item()) {
            return useBombInCombat(active, item);
        }
        if ("scroll_of_identify".equals(item.key)) {
            return useIdentifyScrollInCombat(active, item);
        }
        if ("potion_of_fire_breath".equals(item.key)) {
            return useFireBreathPotionInCombat(active, item);
        }
        if ("potion_of_invisibility".equals(item.key)) {
            return useStatusItemInCombat(active, item, Combat_Status_Type.INVISIBLE, 2, Combatant.Side.PLAYER,
                    "目标进入隐形状态（2 轮），在当前系统中提供攻击与防护优势。");
        }
        if ("antitoxin".equals(item.key)) {
            return useAntitoxinInCombat(active, item);
        }
        if ("holy_water".equals(item.key)) {
            return useHolyWaterInCombat(active, item);
        }
        if ("potion_of_climbing".equals(item.key)) {
            return useClimbingPotionInCombat(active, item);
        }
        if (item.is_scroll_item()) {
            return useLoreScrollInCombat(active, item);
        }
        if (item.is_coin_item()) {
            return useCoinItemInCombat(active, item);
        }
        if (item.is_key_item() || item.is_quest_item()) {
            return useNonConsumingItemInCombat(active, item);
        }

        // 没有专门逻辑的道具
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(this);
        confirm.setTitle("使用道具");
        confirm.setHeaderText(null);
        confirm.setContentText("当前道具没有专门的战斗结算逻辑。\n是否记录为本回合已使用？");
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (!confirmResult.isPresent() || confirmResult.get() != ButtonType.YES) {
            return null;
        }

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 使用了 [" + item.display_name + "]。"
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 治疗物品 ====================
    private String useHealingItemInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices();
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可治疗的目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(new StringConverter<CombatantChoice>() {
            @Override
            public String toString(CombatantChoice c) { return c == null ? "" : c.label; }
            @Override
            public CombatantChoice fromString(String s) { return null; }
        });
        TextField noteField = new TextField();
        noteField.setPromptText("备注（可留空）");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择治疗目标"),
                targetBox,
                new Label("备注（可留空）"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用治疗物品 - " + item.display_name, panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int healAmount = item.get_flat_healing_amount();
        if (healAmount <= 0 && item.get_healing_dice_count() > 0) {
            healAmount = Dice_Util.roll_dice(item.get_healing_dice_count(), item.get_healing_die_size()) + item.get_healing_bonus();
        }
        String note = noteField.getText().trim();
        String log = this.combatEngine.apply_external_healing(item.display_name, choice.combatant, healAmount, note);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name + "，治疗 ["
                + choice.combatant.display_name + "] " + healAmount + " 点生命值"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return log + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 魔法飞弹卷轴 ====================
    private String useMagicMissileScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可命中的敌方目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择魔法飞弹目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用魔法飞弹卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int damage = Dice_Util.roll_dice(3, 4) + 3;
        String note = noteField.getText().trim();
        String log = this.combatEngine.apply_external_damage(item.display_name, choice.combatant, damage, "力场", note);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，对 ["
                + choice.combatant.display_name + "] 造成 " + damage + " 点力场伤害"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return log + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 奥术洞察卷轴 ====================
    private String useArcaneInsightScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可洞察的敌方目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择洞察目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用奥术洞察卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        choice.combatant.apply_status(Combat_Status_Type.CURSED, 2);
        String note = noteField.getText().trim();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，洞察 ["
                + choice.combatant.display_name + "]，其破绽暴露 2 轮"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return item.display_name + " -> " + choice.combatant.display_name
                + "\n目标陷入诅咒/破绽暴露状态（2 轮）。"
                + "\n情报：AC " + choice.combatant.get_effective_armor_class()
                + "，HP " + choice.combatant.current_hp + "/" + choice.combatant.max_hp
                + "，状态 " + choice.combatant.get_status_summary()
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 状态施加道具（护盾/法师护甲/隐形） ====================
    private String useStatusItemInCombat(Combatant active,
                                         Equipment_Item item,
                                         Combat_Status_Type statusType,
                                         int rounds,
                                         Combatant.Side side,
                                         String detailText) {
        List<CombatantChoice> targets = getLivingCombatantChoices(side);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可施加该效果的目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用道具 - " + item.display_name, panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        choice.combatant.apply_status(statusType, rounds);
        String note = noteField.getText().trim();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用道具：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]，状态 " + statusType.label + " " + rounds + " 轮"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return item.display_name + " -> " + choice.combatant.display_name + "\n" + detailText
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 控制类卷轴（蛛网/定身术） ====================
    private String useControlScrollInCombat(Combatant active,
                                            Equipment_Item item,
                                            Combat_Status_Type statusType,
                                            int rounds,
                                            String saveAbility,
                                            int saveDc,
                                            String successText) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可作为控制目标的敌方单位。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择控制目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用卷轴 - " + item.display_name, panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int saveRoll = Dice_Util.roll_d20();
        int saveBonus = choice.combatant.get_saving_throw_bonus(saveAbility);
        int saveTotal = saveRoll + saveBonus;
        boolean resisted = saveTotal >= saveDc;
        String note = noteField.getText().trim();
        StringBuilder log = new StringBuilder();
        log.append(item.display_name).append(" -> ").append(choice.combatant.display_name).append("\n");
        log.append("进行 ").append(saveAbility).append(" 豁免：d20=").append(saveRoll)
                .append(saveBonus >= 0 ? "+" : "").append(saveBonus)
                .append(" = ").append(saveTotal).append("，对抗 DC ").append(saveDc).append("\n");
        if (resisted) {
            log.append("目标豁免成功，未陷入 ").append(statusType.label).append("。\n");
        } else {
            choice.combatant.apply_status(statusType, rounds);
            log.append(successText).append("（持续 ").append(rounds).append(" 轮）\n");
        }
        if (!note.isEmpty()) {
            log.append("备注：").append(note).append("\n");
        }

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]，" + saveAbility + " 豁免 " + saveTotal + "/" + saveDc
                + (resisted ? "，成功抵抗" : "，陷入" + statusType.label + " " + rounds + "轮")
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        if (remaining > 0) {
            log.append(active.display_name).append(" 的背包中还剩 ").append(remaining).append(" 件 [").append(item.display_name).append("]。");
        }
        return log.toString().trim();
    }

    // ==================== 火球术卷轴（多选目标） ====================
    private String useFireballScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices();
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可选中的火球术目标。");
            return null;
        }

        ObservableList<CombatantChoice> targetChoices = FXCollections.observableArrayList(targets);
        ListView<CombatantChoice> targetList = new ListView<>(targetChoices);
        targetList.setCellFactory(lv -> new ListCell<CombatantChoice>() {
            @Override
            protected void updateItem(CombatantChoice choice, boolean empty) {
                super.updateItem(choice, empty);
                if (empty || choice == null) {
                    setText("");
                } else {
                    setText(choice.label);
                }
            }
        });
        targetList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        targetList.setPrefHeight(Math.min(200, targets.size() * 28 + 40));

        TextField areaField = new TextField();
        areaField.setPromptText("爆心区域 / 命中位置");
        TextArea noteArea = new TextArea();
        noteArea.setWrapText(true);
        noteArea.setPromptText("备注（例如豁免成功、半伤、掩体）");
        noteArea.setPrefRowCount(3);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择被火球术波及的目标（可多选）"),
                targetList,
                new Label("爆心区域 / 命中位置"),
                areaField,
                new Label("备注（例如豁免成功、半伤、掩体）"),
                noteArea
        );

        Optional<ButtonType> result = showCustomDialogWithScrollPane("使用火球术卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        List<CombatantChoice> selected = targetList.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "请至少选择一个目标。");
            return null;
        }

        String area = areaField.getText().trim();
        String note = noteArea.getText().trim();
        String combinedNote = (area.isEmpty() ? "" : "爆心区域：" + area)
                + (note.isEmpty() ? "" : ((area.isEmpty() ? "" : "；") + note));
        int damage = Dice_Util.roll_dice(8, 6);
        StringBuilder log = new StringBuilder();
        List<String> names = new ArrayList<>();
        for (CombatantChoice choice : selected) {
            if (log.length() > 0) log.append("\n\n");
            log.append(this.combatEngine.apply_external_damage(item.display_name, choice.combatant, damage, "火焰", combinedNote));
            names.add(choice.combatant.display_name);
        }

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，影响 ["
                + String.join("、", names) + "]，统一伤害 " + damage + " 点"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        if (remaining > 0) {
            log.append("\n").append(active.display_name).append(" 的背包中还剩 ").append(remaining).append(" 件 [").append(item.display_name).append("]。");
        }
        return log.toString();
    }

    // ==================== 吐火药水 ====================
    private String useFireBreathPotionInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可喷吐火焰的敌方目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择火焰喷吐目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用吐火药水", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int damage = Dice_Util.roll_dice(3, 6);
        String note = noteField.getText().trim();
        String log = this.combatEngine.apply_external_damage(item.display_name, choice.combatant, damage, "火焰", note);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name + "，对 ["
                + choice.combatant.display_name + "] 造成 " + damage + " 点火焰伤害"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return log + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 圣水 ====================
    private String useHolyWaterInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可泼洒圣水的敌方目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择圣水目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用圣水", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int damage = Dice_Util.roll_dice(2, 6);
        String note = noteField.getText().trim();
        String log = this.combatEngine.apply_external_damage(item.display_name, choice.combatant, damage, "光耀", note);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name + "，对 ["
                + choice.combatant.display_name + "] 造成 " + damage + " 点光耀伤害"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return log + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 抗毒剂 ====================
    private String useAntitoxinInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.PLAYER);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可使用抗毒剂的友方目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择使用抗毒剂的目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用抗毒剂", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        boolean removed = removeStatusFromCombatant(choice.combatant, Combat_Status_Type.POISONED);
        String note = noteField.getText().trim();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]"
                + (removed ? "，已清除中毒状态" : "，未发现中毒状态")
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return item.display_name + " -> " + choice.combatant.display_name + "\n"
                + (removed ? "已清除中毒状态，并记录为获得额外抗毒保护。" : "目标当前没有中毒状态，记录为获得额外抗毒保护。")
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 侦测魔法卷轴 ====================
    private String useDetectMagicScrollInCombat(Combatant active, Equipment_Item item) {
        TextField areaField = new TextField();
        areaField.setPromptText("侦测区域 / 物件");
        ComboBox<String> schoolBox = new ComboBox<>(FXCollections.observableArrayList(
                "未识别", "防护", "咒法", "预言", "惑控", "塑能", "幻术", "死灵", "变化"));
        ComboBox<String> intensityBox = new ComboBox<>(FXCollections.observableArrayList(
                "微弱", "中等", "强烈", "压倒性"));
        TextArea resultArea = new TextArea();
        resultArea.setWrapText(true);
        resultArea.setPromptText("侦测结果");
        resultArea.setPrefRowCount(4);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("侦测区域 / 物件"),
                areaField,
                new Label("侦测到的学派"),
                schoolBox,
                new Label("灵光强度"),
                intensityBox,
                new Label("侦测结果"),
                resultArea
        );

        Optional<ButtonType> result = showCustomDialogWithScrollPane("使用侦测魔法卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        String area = areaField.getText().trim();
        String detectResult = resultArea.getText().trim();
        String school = schoolBox.getSelectionModel().getSelectedItem();
        String intensity = intensityBox.getSelectionModel().getSelectedItem();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，区域 ["
                + (area.isEmpty() ? "未指定" : area) + "]，学派 " + school + "，强度 " + intensity
                + "，结果：" + (detectResult.isEmpty() ? "未记录" : detectResult)
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 使用了 [" + item.display_name + "]。"
                + "\n侦测区域：" + (area.isEmpty() ? "未指定" : area)
                + "\n学派：" + school + " | 强度：" + intensity
                + "\n结果：" + (detectResult.isEmpty() ? "未记录" : detectResult)
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 迷踪步卷轴 ====================
    private String useMistyStepScrollInCombat(Combatant active, Equipment_Item item) {
        TextField destinationField = new TextField();
        destinationField.setPromptText("瞬移到的位置");
        TextArea noteArea = new TextArea();
        noteArea.setWrapText(true);
        noteArea.setPromptText("备注");
        noteArea.setPrefRowCount(3);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("瞬移到的位置"),
                destinationField,
                new Label("备注"),
                noteArea
        );

        Optional<ButtonType> result = showCustomDialog("使用迷踪步卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        String destination = destinationField.getText().trim();
        String note = noteArea.getText().trim();
        boolean removedRestrained = removeStatusFromCombatant(active, Combat_Status_Type.RESTRAINED);
        boolean removedProne = removeStatusFromCombatant(active, Combat_Status_Type.PRONE);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，瞬移到 ["
                + (destination.isEmpty() ? "未指定位置" : destination) + "]"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 使用了 [" + item.display_name + "]。"
                + "\n瞬移位置：" + (destination.isEmpty() ? "未指定位置" : destination)
                + ((removedRestrained || removedProne) ? "\n已移除状态：" : "")
                + (removedRestrained ? "束缚 " : "")
                + (removedProne ? "倒地" : "")
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 攀爬药水 ====================
    private String useClimbingPotionInCombat(Combatant active, Equipment_Item item) {
        TextField routeField = new TextField();
        routeField.setPromptText("攀爬目标 / 路线");
        TextArea noteArea = new TextArea();
        noteArea.setWrapText(true);
        noteArea.setPromptText("备注");
        noteArea.setPrefRowCount(3);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("攀爬目标 / 路线"),
                routeField,
                new Label("备注"),
                noteArea
        );

        Optional<ButtonType> result = showCustomDialog("使用攀爬药水", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        String route = routeField.getText().trim();
        String note = noteArea.getText().trim();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name + "，攀爬 ["
                + (route.isEmpty() ? "未指定路线" : route) + "]"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 使用了 [" + item.display_name + "]。"
                + "\n攀爬路线：" + (route.isEmpty() ? "未指定路线" : route)
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 炸弹 ====================
    private String useBombInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices();
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可投掷的目标。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField areaField = new TextField();
        areaField.setPromptText("爆炸区域 / 落点");
        TextArea noteArea = new TextArea();
        noteArea.setWrapText(true);
        noteArea.setPromptText("备注（可留空）");
        noteArea.setPrefRowCount(3);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择主目标"),
                targetBox,
                new Label("爆炸区域 / 落点"),
                areaField,
                new Label("备注（可留空）"),
                noteArea
        );

        Optional<ButtonType> result = showCustomDialogWithScrollPane("使用爆炸物 - " + item.display_name, panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        String area = areaField.getText().trim();
        String note = noteArea.getText().trim();
        String combinedNote = (area.isEmpty() ? "" : "爆炸区域：" + area)
                + (note.isEmpty() ? "" : ((area.isEmpty() ? "" : "；") + note));
        int damage = Dice_Util.roll_dice(item.get_bomb_dice_count(), item.get_bomb_die_size()) + item.get_bomb_bonus();
        String log = this.combatEngine.apply_external_damage(item.display_name, choice.combatant, damage, item.get_bomb_damage_type(), combinedNote);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用爆炸物：" + item.display_name + "，命中 ["
                + choice.combatant.display_name + "]，造成 " + damage + " 点" + item.get_bomb_damage_type() + "伤害"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return log + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 鉴定术卷轴 ====================
    private String useIdentifyScrollInCombat(Combatant active, Equipment_Item item) {
        TextField targetField = new TextField();
        targetField.setPromptText("本回合要鉴定的物品 / 法阵 / 现象");
        TextArea resultArea = new TextArea();
        resultArea.setWrapText(true);
        resultArea.setPromptText("鉴定结果");
        resultArea.setPrefRowCount(4);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("本回合要鉴定的物品 / 法阵 / 现象"),
                targetField,
                new Label("鉴定结果"),
                resultArea
        );

        Optional<ButtonType> result = showCustomDialogWithScrollPane("使用鉴定术卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        String target = targetField.getText().trim();
        String identifyResult = resultArea.getText().trim();
        if (target.isEmpty()) target = "未指定对象";
        if (identifyResult.isEmpty()) identifyResult = "尚未记录明确结果";

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，鉴定对象 ["
                + target + "]，结果：" + identifyResult
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 使用了 [" + item.display_name + "]。\n鉴定对象：" + target + "\n结果：" + identifyResult
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 其他卷轴（记录阅读结果） ====================
    private String useLoreScrollInCombat(Combatant active, Equipment_Item item) {
        TextField subjectField = new TextField();
        subjectField.setPromptText("卷轴主题 / 解读对象");
        TextArea insightArea = new TextArea();
        insightArea.setWrapText(true);
        insightArea.setPromptText("本回合获得的信息");
        insightArea.setPrefRowCount(4);

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("卷轴主题 / 解读对象"),
                subjectField,
                new Label("本回合获得的信息"),
                insightArea
        );

        Optional<ButtonType> result = showCustomDialogWithScrollPane("使用卷轴 - " + item.display_name, panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        String subject = subjectField.getText().trim();
        String insight = insightArea.getText().trim();
        if (subject.isEmpty()) subject = "未指定主题";
        if (insight.isEmpty()) insight = "尚未记录具体结果";

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中阅读卷轴：" + item.display_name + "，主题 [" + subject + "]，结果："
                + insight + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 阅读了 [" + item.display_name + "]。\n主题：" + subject + "\n结果：" + insight
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 睡眠术卷轴 ====================
    private String useSleepScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可作为睡眠术目标的敌方单位。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择睡眠术目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用睡眠术卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int sleepPool = Dice_Util.roll_dice(5, 8);
        boolean asleep = choice.combatant.current_hp <= sleepPool;
        if (asleep) {
            choice.combatant.apply_status(Combat_Status_Type.ASLEEP, 2);
        }
        String note = noteField.getText().trim();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]，睡眠值 " + sleepPool + "，目标当前 HP " + choice.combatant.current_hp
                + (asleep ? "，陷入沉睡 2 轮" : "，未被压制")
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return item.display_name + " -> " + choice.combatant.display_name
                + "\n睡眠值：5d8 = " + sleepPool + "，目标当前 HP " + choice.combatant.current_hp
                + (asleep ? "\n目标陷入沉睡状态（2 轮）。" : "\n目标生命值过高，没有进入沉睡。")
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 灼热射线卷轴 ====================
    private String useScorchingRayScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可作为灼热射线目标的敌方单位。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择灼热射线目标（默认三道射线集中）"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用灼热射线卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int spellAttackBonus = getScrollSpellAttackBonus(active);
        String note = noteField.getText().trim();
        StringBuilder log = new StringBuilder();
        log.append(item.display_name).append(" -> ").append(choice.combatant.display_name).append("\n");
        int totalDamage = 0;
        int hits = 0;
        for (int i = 1; i <= 3; i++) {
            int d20 = Dice_Util.roll_d20();
            int totalAttack = d20 + spellAttackBonus + active.get_effective_attack_modifier();
            boolean hit = d20 == 20 || totalAttack >= choice.combatant.get_effective_armor_class();
            log.append("第 ").append(i).append(" 道射线：d20=").append(d20)
                    .append(" + ").append(spellAttackBonus + active.get_effective_attack_modifier())
                    .append(" = ").append(totalAttack).append(hit ? "，命中" : "，未命中").append("\n");
            if (hit) {
                int damage = Dice_Util.roll_dice(2, 6);
                totalDamage += damage;
                hits++;
                choice.combatant.current_hp = Math.max(0, choice.combatant.current_hp - damage);
                log.append("造成 ").append(damage).append(" 点火焰伤害，目标剩余 HP ")
                        .append(choice.combatant.current_hp).append("/").append(choice.combatant.max_hp).append("\n");
                if (!choice.combatant.is_alive()) {
                    log.append(choice.combatant.display_name).append(" 倒下了。\n");
                    break;
                }
            }
        }
        if (!note.isEmpty()) {
            log.append("备注：").append(note).append("\n");
        }

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]，命中 " + hits + " 道射线，造成 " + totalDamage + " 点火焰伤害"
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        if (remaining > 0) {
            log.append(active.display_name).append(" 的背包中还剩 ").append(remaining).append(" 件 [").append(item.display_name).append("]。");
        }
        return log.toString().trim();
    }

    // ==================== 寒霜射线卷轴 ====================
    private String useRayOfFrostScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices(Combatant.Side.ENEMY);
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可作为寒霜射线目标的敌方单位。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择寒霜射线目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用寒霜射线卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        int spellAttackBonus = getScrollSpellAttackBonus(active);
        int d20 = Dice_Util.roll_d20();
        int totalAttack = d20 + spellAttackBonus + active.get_effective_attack_modifier();
        boolean hit = d20 == 20 || totalAttack >= choice.combatant.get_effective_armor_class();
        String note = noteField.getText().trim();
        StringBuilder log = new StringBuilder();
        log.append(item.display_name).append(" -> ").append(choice.combatant.display_name).append("\n");
        log.append("攻击检定：d20=").append(d20)
                .append(" + ").append(spellAttackBonus + active.get_effective_attack_modifier())
                .append(" = ").append(totalAttack).append(hit ? "，命中\n" : "，未命中\n");
        if (hit) {
            int damage = Dice_Util.roll_dice(1, 8);
            choice.combatant.current_hp = Math.max(0, choice.combatant.current_hp - damage);
            choice.combatant.apply_status(Combat_Status_Type.SLOWED, 1);
            log.append("造成 ").append(damage).append(" 点寒冷伤害，目标获得迟缓状态（1 轮），剩余 HP ")
                    .append(choice.combatant.current_hp).append("/").append(choice.combatant.max_hp).append("\n");
        }
        if (!note.isEmpty()) {
            log.append("备注：").append(note).append("\n");
        }

        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]，攻击检定 " + totalAttack
                + (hit ? "，命中并附加迟缓 1 轮" : "，未命中")
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        if (remaining > 0) {
            log.append(active.display_name).append(" 的背包中还剩 ").append(remaining).append(" 件 [").append(item.display_name).append("]。");
        }
        return log.toString().trim();
    }

    // ==================== 解除魔法卷轴 ====================
    private String useDispelMagicScrollInCombat(Combatant active, Equipment_Item item) {
        List<CombatantChoice> targets = getLivingCombatantChoices();
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "当前没有可作为解除魔法目标的单位。");
            return null;
        }

        ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targets));
        targetBox.setConverter(combatantChoiceConverter());
        TextField noteField = new TextField();
        noteField.setPromptText("备注");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("选择解除魔法目标"),
                targetBox,
                new Label("备注"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用解除魔法卷轴", panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        CombatantChoice choice = targetBox.getSelectionModel().getSelectedItem();
        if (choice == null || choice.combatant == null) return null;

        List<String> removedLabels = clearStatusesFromCombatant(choice.combatant);
        String note = noteField.getText().trim();
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中使用卷轴：" + item.display_name + "，目标 ["
                + choice.combatant.display_name + "]，移除状态：" + (removedLabels.isEmpty() ? "无" : String.join("、", removedLabels))
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return item.display_name + " -> " + choice.combatant.display_name
                + "\n已移除状态：" + (removedLabels.isEmpty() ? "无可移除效果" : String.join("、", removedLabels))
                + (note.isEmpty() ? "" : "\n备注：" + note)
                + (remaining > 0 ? "\n" + active.display_name + " 的背包中还剩 " + remaining + " 件 [" + item.display_name + "]。" : "");
    }

    // ==================== 任务物品/钥匙（不消耗） ====================
    private String useNonConsumingItemInCombat(Combatant active, Equipment_Item item) {
        TextField noteField = new TextField();
        noteField.setPromptText("记录本次使用方式");

        VBox panel = new VBox(8);
        panel.getChildren().addAll(
                new Label("记录本次使用方式"),
                noteField
        );

        Optional<ButtonType> result = showCustomDialog("使用物品 - " + item.display_name, panel);
        if (!result.isPresent() || result.get() != ButtonType.OK) return null;

        String note = noteField.getText().trim();
        active.linked_character.record_advancement("战斗中使用物品：" + item.display_name
                + (note.isEmpty() ? "" : "，说明：" + note));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 在战斗中使用了 [" + item.display_name + "]。"
                + (note.isEmpty() ? "" : "\n说明：" + note)
                + "\n该物品不会自动消耗。";
    }

    // ==================== 钱币物品（自动兑换） ====================
    private String useCoinItemInCombat(Combatant active, Equipment_Item item) {
        int gainedValue = item.get_currency_gain_cp();
        active.linked_character.add_currency_cp(gainedValue);
        active.linked_character.remove_item_from_inventory(item.key);
        int remaining = active.linked_character.get_item_count(item.key);
        active.linked_character.record_advancement("战斗中兑换钱币：" + item.display_name + "，获得 "
                + Equipment_Item.format_cp_value(gainedValue)
                + (remaining > 0 ? "，剩余 " + remaining + " 件" : ""));
        Character_DAO.update_character(active.linked_character);
        return active.display_name + " 在战斗中兑换了 [" + item.display_name + "]，获得 "
                + Equipment_Item.format_cp_value(gainedValue) + "。"
                + (remaining > 0 ? "\n背包中还剩 " + remaining + " 件。" : "");
    }

    // ==================== 辅助方法 ====================
    private List<CombatantChoice> getLivingCombatantChoices() {
        return getLivingCombatantChoices(null);
    }

    private List<CombatantChoice> getLivingCombatantChoices(Combatant.Side sideFilter) {
        List<CombatantChoice> choices = new ArrayList<>();
        if (this.combatEngine == null) return choices;
        for (Combatant combatant : this.combatEngine.get_initiative_order()) {
            if (combatant != null && combatant.is_alive()
                    && (sideFilter == null || combatant.side == sideFilter)) {
                choices.add(new CombatantChoice(combatant));
            }
        }
        return choices;
    }

    private boolean removeStatusFromCombatant(Combatant combatant, Combat_Status_Type statusType) {
        if (combatant == null || statusType == null) return false;
        boolean removed = false;
        for (int i = combatant.status_effects.size() - 1; i >= 0; i--) {
            Combat_Status_Effect effect = combatant.status_effects.get(i);
            if (effect.type == statusType) {
                combatant.status_effects.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private List<String> clearStatusesFromCombatant(Combatant combatant) {
        List<String> removedLabels = new ArrayList<>();
        if (combatant == null) return removedLabels;
        for (int i = combatant.status_effects.size() - 1; i >= 0; i--) {
            Combat_Status_Effect effect = combatant.status_effects.get(i);
            removedLabels.add(0, effect.get_label());
            combatant.status_effects.remove(i);
        }
        return removedLabels;
    }

    private int getScrollSpellAttackBonus(Combatant combatant) {
        if (combatant == null) return 5;
        int bestMentalScore = Math.max(combatant.intelligence, Math.max(combatant.wisdom, combatant.charisma));
        return combatant.proficiency_bonus + Combatant.get_modifier(bestMentalScore);
    }

    private StringConverter<CombatantChoice> combatantChoiceConverter() {
        return new StringConverter<CombatantChoice>() {
            @Override
            public String toString(CombatantChoice c) { return c == null ? "" : c.label; }
            @Override
            public CombatantChoice fromString(String s) { return null; }
        };
    }

    private String getResourceSummary(Combatant combatant) {
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
        if (combatant.bardic_inspiration_remaining > 0) {
            parts.add("吟游激励 " + combatant.bardic_inspiration_remaining);
        }
        if (combatant.superiority_dice_remaining > 0) {
            parts.add("卓越骰 " + combatant.superiority_dice_remaining + "d" + combatant.superiority_dice_size);
        }
        if (combatant.lay_on_hands_remaining > 0) {
            parts.add("圣疗池 " + combatant.lay_on_hands_remaining);
        }
        return parts.isEmpty() ? "无特殊消耗资源" : String.join(" | ", parts);
    }

    private void appendLog(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String current = logArea.getText();
        if (!current.trim().isEmpty()) {
            logArea.appendText("\n\n");
        }
        logArea.appendText(text);
        // 自动滚动到底部
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private void appendPendingSystemLog() {
        if (this.combatEngine == null) return;
        appendLog(this.combatEngine.get_and_clear_pending_log());
    }

    // ==================== 战后结算 ====================
    private void maybeShowSettlementDialog() {
        if (this.combatEngine == null
                || !this.combatEngine.is_combat_finished()
                || !this.combatEngine.did_players_win()
                || this.settlementShown) {
            return;
        }
        this.settlementShown = true;

        // 显示结算对话框
        SettlementDialog dialog = new SettlementDialog(this, this.combatEngine);
        dialog.showAndWait();

        // 询问是否导出 PDF
        Alert exportConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        exportConfirm.initOwner(this);
        exportConfirm.setTitle("导出战斗日志");
        exportConfirm.setHeaderText(null);
        exportConfirm.setContentText("是否导出本次战斗日志为 PDF 文件？");
        Optional<ButtonType> exportChoice = exportConfirm.showAndWait();

        if (exportChoice.isPresent() && exportChoice.get() == ButtonType.YES) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("导出战斗日志 PDF");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            fileChooser.setInitialFileName("combat_log_" + timestamp + ".pdf");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF 文件", "*.pdf"));
            File file = fileChooser.showSaveDialog(this);
            if (file != null) {
                try {
                    Encounter_Log_PDF.generate(
                            combatEngine.get_combat_log(),
                            combatEngine.get_initiative_order(),
                            combatEngine.get_current_round(),
                            combatEngine.get_total_damage_by_players(),
                            combatEngine.get_total_damage_by_enemies(),
                            combatEngine.get_total_healing(),
                            combatEngine.did_players_win(),
                            combatEngine.get_distributed_xp(),
                            file.getAbsolutePath()
                    );
                    showAlert(Alert.AlertType.INFORMATION, "战斗日志 PDF 已成功导出至：" + file.getAbsolutePath());
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "导出失败：" + ex.getMessage());
                }
            }
        }

        appendLog("战后结算完成。剩余未分配掉落：" + this.combatEngine.get_pending_loot_keys().size() + " 件。");
        refreshBattleUI();
    }

    // ==================== 战后结算对话框 ====================
    private static class SettlementDialog extends Dialog<Void> {
        private final Combat_Engine combatEngine;
        private final TextArea xpArea;
        private final ObservableList<String> lootModel = FXCollections.observableArrayList();
        private final ListView<String> lootList;

        SettlementDialog(Window owner, Combat_Engine combatEngine) {
            this.combatEngine = combatEngine;
            this.xpArea = new TextArea();
            this.lootList = new ListView<>(lootModel);

            initOwner(owner);
            setTitle("战斗结算");
            setWidth(820);
            setHeight(620);

            // 经验结算区域
            VBox xpPanel = new VBox(8);
            xpPanel.setPadding(new Insets(8, 8, 8, 8));
            xpPanel.setStyle("-fx-border-color: gray; -fx-border-width: 1;");
            Label xpTitle = new Label("经验结算");
            xpTitle.setStyle("-fx-font-weight: bold;");
            xpArea.setEditable(false);
            xpArea.setWrapText(true);
            xpArea.setPrefRowCount(5);
            xpPanel.getChildren().addAll(xpTitle, xpArea);

            // 掉落物区域
            VBox lootPanel = new VBox(8);
            lootPanel.setPadding(new Insets(8, 8, 8, 8));
            lootPanel.setStyle("-fx-border-color: gray; -fx-border-width: 1;");
            Label lootTitle = new Label("怪物掉落");
            lootTitle.setStyle("-fx-font-weight: bold;");
            TextArea hintArea = new TextArea(
                    "上方显示参战角色经验进度 a/b。\n下方掉落物支持单选或多选，左键选择，右键后把选中物品分配给指定角色。");
            hintArea.setEditable(false);
            hintArea.setWrapText(true);
            hintArea.setPrefRowCount(2);
            hintArea.setStyle("-fx-background-color: transparent;");

            lootList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            // 右键菜单分配掉落物
            lootList.setOnContextMenuRequested(e -> {
                List<String> pendingLoot = this.combatEngine.get_pending_loot_keys();
                ObservableList<Integer> selectedIndices = lootList.getSelectionModel().getSelectedIndices();
                if (selectedIndices.isEmpty() || pendingLoot.isEmpty()) return;

                ContextMenu contextMenu = new ContextMenu();
                for (Character_Sheet character : this.combatEngine.get_participating_characters()) {
                    MenuItem menuItem = new MenuItem("给予 " + character.name);
                    menuItem.setOnAction(ae -> assignSelectedLoot(character, selectedIndices, pendingLoot));
                    contextMenu.getItems().add(menuItem);
                }
                contextMenu.show(lootList, e.getScreenX(), e.getScreenY());
            });

            VBox.setVgrow(lootList, Priority.ALWAYS);
            lootPanel.getChildren().addAll(lootTitle, hintArea, lootList);

            // 整体布局
            VBox content = new VBox(12);
            content.setPadding(new Insets(12, 12, 12, 12));
            VBox.setVgrow(xpPanel, Priority.NEVER);
            VBox.setVgrow(lootPanel, Priority.ALWAYS);
            content.getChildren().addAll(xpPanel, lootPanel);

            ScrollPane scroll = new ScrollPane(content);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            getDialogPane().setContent(scroll);
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            refreshXpArea();
            refreshLootModel();
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
        }

        private void refreshLootModel() {
            lootModel.clear();
            List<String> lootKeys = this.combatEngine.get_pending_loot_keys();
            if (lootKeys.isEmpty()) {
                lootModel.add("本次无掉落物，或已全部分配完成。");
                lootList.setDisable(true);
                return;
            }
            lootList.setDisable(false);
            for (String lootKey : lootKeys) {
                Equipment_Item item = Equipment_Library.get_item(lootKey);
                lootModel.add(item == null ? lootKey : item.to_inventory_line());
            }
        }

        private void assignSelectedLoot(Character_Sheet receiver, ObservableList<Integer> selectedIndices, List<String> pendingLoot) {
            List<String> toAssign = new ArrayList<>();
            for (Integer selectedIndex : selectedIndices) {
                if (selectedIndex >= 0 && selectedIndex < pendingLoot.size()) {
                    toAssign.add(pendingLoot.get(selectedIndex));
                }
            }
            this.combatEngine.assign_loot(toAssign, receiver);
            refreshLootModel();
        }
    }

    // ==================== 内部类：道具选项 ====================
    private static class InventoryChoice {
        private final Equipment_Item item;
        private final String label;

        private InventoryChoice(Character_Sheet owner, Equipment_Item item) {
            this.item = item;
            int count = owner == null ? 1 : Math.max(1, owner.get_item_count(item.key));
            this.label = item.display_name + " x" + count + " | " + item.get_inventory_category();
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    // ==================== 内部类：战斗目标选项 ====================
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
}
