package com.DMHelper.fx;

import com.DMHelper.basic.Character_Card_PDF;
import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.combat.Combat_Engine;
import com.DMHelper.basic.combat.Combatant;
import com.DMHelper.basic.combat.Combat_Status_Effect;
import com.DMHelper.basic.combat.Combat_Status_Type;
import com.DMHelper.basic.combat.Dice_Util;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Custom_Equipment_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.equipment.Equipment_Item;
import com.DMHelper.basic.equipment.Equipment_Library;
import com.DMHelper.basic.equipment.Equipment_Slot;
import com.DMHelper.basic.menus.Character_Advancement_Helper;
import com.DMHelper.basic.menus.Combat_System_UI;
import com.DMHelper.basic.menus.Spell_Management_Helper;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.spell.Spell_Definition;
import com.DMHelper.basic.spell.Spell_Library;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * D&D 5e 全功能角色管理窗口 - JavaFX 版本。
 * 功能与 Swing 版 Character_Manager_UI 完全一致，包含四个标签页：
 * 基础与属性、装备与物品、施法与法术、成长与升级。
 */
public class CharacterManagerWindow extends Stage {

    private Character_Sheet current;
    private boolean isReloadingSelector;
    private ComboBox<String> charSelector;
    private TextArea statsArea;
    private ComboBox<String> armorBox;
    private ComboBox<String> mainHandBox;
    private ComboBox<String> offHandBox;
    private ComboBox<String> cloakBox;
    private ComboBox<String> accessoryBox;
    private ComboBox<String> backpackFilterBox;
    private ListView<Equipment_Item> backpackList;
    private TextArea inventoryDetailArea;
    private Button useItemBtn;
    private Button sellItemBtn;
    private TextArea spellcastingArea;
    private Button manageSpellbookBtn;
    private Button manageSpellSelectionBtn;
    private Button managePreparedSpellBtn;
    private TextArea levelInfoArea;
    private Button levelUpBtn;
    private Button addXpBtn;
    private Button shortRestBtn;
    private Button longRestBtn;
    private Button useSecondWindBtn;

    public CharacterManagerWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("全功能角色管理控制台");
        if (!Global_Data.character_pool.isEmpty()) {
            current = Global_Data.character_pool.get(0);
        }
        setScene(buildScene());
        setMinWidth(1200);
        setMinHeight(800);
        refreshUI();
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-shell");
        root.setTop(buildTopBar());
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(buildStatsTab(), buildEquipTab(), buildSpellTab(), buildProgressionTab());
        root.setCenter(tabPane);
        bindEvents();
        Scene scene = new Scene(root, 1200, 800);
        FxThemes.apply(scene);
        return scene;
    }

    private HBox buildTopBar() {
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-border-color: #555; -fx-border-width: 0 0 1 0;");
        Label label = new Label("当前管理角色:");
        charSelector = new ComboBox<>();
        reloadSelectorItems(current);
        Button saveBtn = new Button("保存更改");
        saveBtn.setStyle("-fx-font-weight: bold;");
        saveBtn.setOnAction(e -> handleSave());
        Button exportPdfBtn = new Button("导出角色卡 PDF");
        exportPdfBtn.setStyle("-fx-font-weight: bold;");
        exportPdfBtn.setOnAction(e -> handleExportCharacterPdf());
        topBar.getChildren().addAll(label, charSelector, new Separator(), saveBtn, exportPdfBtn);
        HBox.setHgrow(charSelector, Priority.ALWAYS);
        return topBar;
    }

    private Tab buildStatsTab() {
        Tab tab = new Tab("基础与属性");
        statsArea = buildTextArea();
        ScrollPane scroll = new ScrollPane(statsArea);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        Button exportPdfBtn2 = new Button("导出角色卡 PDF");
        exportPdfBtn2.setOnAction(e -> handleExportCharacterPdf());
        VBox vbox = new VBox(8, scroll, exportPdfBtn2);
        vbox.setPadding(new Insets(8)); VBox.setVgrow(scroll, Priority.ALWAYS);
        tab.setContent(vbox);
        return tab;
    }

    private Tab buildEquipTab() {
        Tab tab = new Tab("装备与物品");
        GridPane slotGrid = new GridPane();
        slotGrid.setHgap(10); slotGrid.setVgap(8); slotGrid.setPadding(new Insets(12));
        armorBox = new ComboBox<>(); mainHandBox = new ComboBox<>();
        offHandBox = new ComboBox<>(); cloakBox = new ComboBox<>(); accessoryBox = new ComboBox<>();
        slotGrid.add(new Label("护甲槽位"), 0, 0); slotGrid.add(armorBox, 1, 0);
        slotGrid.add(new Label("主手武器"), 0, 1); slotGrid.add(mainHandBox, 1, 1);
        slotGrid.add(new Label("副手/盾牌"), 0, 2); slotGrid.add(offHandBox, 1, 2);
        slotGrid.add(new Label("披风"), 0, 3); slotGrid.add(cloakBox, 1, 3);
        slotGrid.add(new Label("护符"), 0, 4); slotGrid.add(accessoryBox, 1, 4);
        Button equipBtn = new Button("应用当前装备");
        equipBtn.setStyle("-fx-font-weight: bold;");
        equipBtn.setOnAction(e -> handleEquip());
        slotGrid.add(equipBtn, 0, 5, 2, 1);
        backpackFilterBox = new ComboBox<>(FXCollections.observableArrayList("全部", "消耗品", "材料/战利品", "工具/任务", "自定义"));
        backpackFilterBox.setPrefWidth(160);
        backpackList = new ListView<>();
        backpackList.setCellFactory(list -> new ListCell<Equipment_Item>() {
            @Override protected void updateItem(Equipment_Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : buildBackpackRowLabel(item));
            }
        });
        inventoryDetailArea = buildTextArea(); inventoryDetailArea.setPrefRowCount(8);
        VBox backpackPanel = new VBox(8);
        backpackPanel.setPadding(new Insets(8));
        backpackPanel.setStyle("-fx-border-color: #555; -fx-border-width: 1; -fx-border-radius: 4;");
        Label backpackTitle = new Label("背包物品");
        backpackTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        backpackPanel.getChildren().addAll(backpackTitle, backpackFilterBox, backpackList, inventoryDetailArea);
        VBox.setVgrow(backpackList, Priority.ALWAYS);
        useItemBtn = new Button("使用选中物品"); useItemBtn.setStyle("-fx-font-weight: bold;");
        sellItemBtn = new Button("出售选中物品");
        Button buyItemBtn = new Button("购买物品"); buyItemBtn.setOnAction(e -> openPurchaseDialog());
        Button addItemBtn = new Button("新增物品"); addItemBtn.setOnAction(e -> handleAddItem());
        HBox buttonBar = new HBox(8, sellItemBtn, buyItemBtn, useItemBtn, addItemBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT); buttonBar.setPadding(new Insets(8));
        VBox content = new VBox(8, slotGrid, backpackPanel, buttonBar);
        content.setPadding(new Insets(8)); VBox.setVgrow(backpackPanel, Priority.ALWAYS);
        tab.setContent(content);
        return tab;
    }

    private Tab buildSpellTab() {
        Tab tab = new Tab("施法与法术");
        spellcastingArea = buildTextArea();
        ScrollPane scroll = new ScrollPane(spellcastingArea);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        manageSpellbookBtn = new Button("管理戏法");
        manageSpellSelectionBtn = new Button("管理法术");
        managePreparedSpellBtn = new Button("管理准备法术");
        HBox btnBar = new HBox(12, manageSpellbookBtn, manageSpellSelectionBtn, managePreparedSpellBtn);
        btnBar.setAlignment(Pos.CENTER); btnBar.setPadding(new Insets(8));
        VBox vbox = new VBox(8, scroll, btnBar);
        vbox.setPadding(new Insets(8)); VBox.setVgrow(scroll, Priority.ALWAYS);
        tab.setContent(vbox);
        return tab;
    }

    private Tab buildProgressionTab() {
        Tab tab = new Tab("成长与升级");
        levelInfoArea = buildTextArea();
        ScrollPane scroll = new ScrollPane(levelInfoArea);
        scroll.setFitToWidth(true); scroll.setFitToHeight(true);
        addXpBtn = new Button("添加经验值");
        shortRestBtn = new Button("进行短休");
        longRestBtn = new Button("进行长休");
        useSecondWindBtn = new Button("使用复苏之风");
        levelUpBtn = new Button("执行升级");
        levelUpBtn.setStyle("-fx-font-weight: bold; -fx-text-fill: red; -fx-font-size: 16px;");
        HBox btnBar = new HBox(12, addXpBtn, shortRestBtn, longRestBtn, useSecondWindBtn, levelUpBtn);
        btnBar.setAlignment(Pos.CENTER); btnBar.setPadding(new Insets(8));
        VBox vbox = new VBox(8, scroll, btnBar);
        vbox.setPadding(new Insets(8)); VBox.setVgrow(scroll, Priority.ALWAYS);
        tab.setContent(vbox);
        return tab;
    }

    private TextArea buildTextArea() {
        TextArea area = new TextArea();
        area.setEditable(false); area.setWrapText(true);
        area.setStyle("-fx-font-size: 14px; -fx-font-family: 'Microsoft YaHei', sans-serif;");
        return area;
    }

    private void bindEvents() {
        charSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (isReloadingSelector) return;
            int idx = charSelector.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < Global_Data.character_pool.size()) {
                current = Global_Data.character_pool.get(idx); refreshUI();
            }
        });
        backpackFilterBox.getSelectionModel().selectedItemProperty().addListener(obs -> reloadBackpackList());
        backpackList.getSelectionModel().selectedItemProperty().addListener(obs -> refreshInventoryDetailArea());
        backpackList.setOnMouseClicked(e -> { if (e.getClickCount() == 2) handleUseSelectedItem(); });
        manageSpellbookBtn.setOnAction(e -> handleManageSpellbook());
        manageSpellSelectionBtn.setOnAction(e -> handleManageSpellSelection());
        managePreparedSpellBtn.setOnAction(e -> handleManagePreparedSpells());
        addXpBtn.setOnAction(e -> handleAddExperience());
        shortRestBtn.setOnAction(e -> handleShortRest());
        longRestBtn.setOnAction(e -> handleLongRest());
        useSecondWindBtn.setOnAction(e -> handleUseSecondWind());
        levelUpBtn.setOnAction(e -> handleLevelUp());
        useItemBtn.setOnAction(e -> handleUseSelectedItem());
        sellItemBtn.setOnAction(e -> handleSellSelectedItem());
    }

    // ==================== 统一刷新 ====================
    private void refreshUI() {
        if (current == null) return;
        current.recalculate_derived_stats();
        refreshStatsPanel(); reloadEquipmentBoxes(); reloadBackpackList();
        refreshInventoryDetailArea(); refreshSpellcastingPanel(); refreshProgressionPanel();
        boolean isFighter = current.job instanceof Fighter_Class;
        useSecondWindBtn.setVisible(isFighter);
        if (isFighter) {
            Fighter_Class fighter = (Fighter_Class) current.job;
            useSecondWindBtn.setDisable(fighter.current_second_wind_uses <= 0);
            useSecondWindBtn.setText("使用复苏之风 (" + fighter.get_second_wind_summary() + ")");
        }
        boolean canLevelUp = current.can_level_up();
        levelUpBtn.setDisable(!canLevelUp);
        levelUpBtn.setText(canLevelUp ? "执行升级" : "经验不足，暂不可升级");
    }

    // ==================== Tab1: 刷新基础属性面板 ====================
    private void refreshStatsPanel() {
        StringBuilder sb = new StringBuilder();
        sb.append("姓名: ").append(current.name).append(" | 种族: ").append(current.race.race_name);
        if (current.race.subrace_name != null && !current.race.subrace_name.isEmpty())
            sb.append(" (").append(current.race.subrace_name).append(")");
        sb.append("\n职业: ").append(current.job.class_name).append(" (LV.").append(current.job.current_level).append(")");
        String subclass = current.job.get_subclass_name();
        if (subclass != null && !subclass.isEmpty()) sb.append(" | 子职业: ").append(subclass);
        sb.append("\n年龄/性别: ").append(current.age).append(" / ").append(current.gender);
        sb.append("\n经验值: ").append(current.experience_points);
        sb.append("\n当前钱币: ").append(current.get_currency_summary()).append(" (总值 ").append(Equipment_Item.format_cp_value(current.get_total_currency_cp())).append(")");
        sb.append("\n--------------------------------------------------");
        sb.append("\n当前 HP: ").append(current.get_hp_summary());
        sb.append("\n生命骰: ").append(current.get_hit_dice_summary());
        sb.append("\n当前护甲 AC: ").append(current.ac).append(" (护甲: ").append(current.equipped_armor.armor_name).append(")");
        Equipment_Item weaponItem = current.get_equipped_item(Equipment_Slot.MAIN_HAND);
        sb.append("\n当前主手: ").append(weaponItem == null ? "空置" : weaponItem.display_name);
        sb.append("\n先攻修正: +").append(current.get_initiative_modifier());
        sb.append("\n速度: ").append(current.race.base_speed).append(" 尺");
        sb.append("\n熟练加值 PB: +").append(current.get_proficiency_bonus());
        sb.append("\n--------------------------------------------------\n[属性值 / 调整值]\n");
        sb.append(String.format("力量 STR: %2d (%+d)\n", current.stats.str, current.stats.get_mod(current.stats.str)));
        sb.append(String.format("敏捷 DEX: %2d (%+d)\n", current.stats.dex, current.stats.get_mod(current.stats.dex)));
        sb.append(String.format("体质 CON: %2d (%+d)\n", current.stats.con, current.stats.get_mod(current.stats.con)));
        sb.append(String.format("智力 INT: %2d (%+d)\n", current.stats.intel, current.stats.get_mod(current.stats.intel)));
        sb.append(String.format("感知 WIS: %2d (%+d)\n", current.stats.wis, current.stats.get_mod(current.stats.wis)));
        sb.append(String.format("魅力 CHA: %2d (%+d)\n", current.stats.cha, current.stats.get_mod(current.stats.cha)));
        sb.append("--------------------------------------------------\n");
        Equipment_Item mainHand = current.get_equipped_item(Equipment_Slot.MAIN_HAND);
        if (mainHand != null) {
            sb.append("[主手武器]\n").append(mainHand.display_name).append(" - ").append(mainHand.description).append("\n");
            if (mainHand.attack_dice_count > 0) {
                sb.append("  伤害: ").append(mainHand.attack_dice_count).append("d").append(mainHand.attack_die_size);
                if (mainHand.attack_bonus != 0) sb.append("+").append(mainHand.attack_bonus);
                if (!mainHand.damage_type.isEmpty()) sb.append(" ").append(mainHand.damage_type);
                sb.append("\n");
            }
        }
        if (hasProfileContent()) {
            sb.append("--------------------------------------------------\n");
            if (!current.background_story.trim().isEmpty()) sb.append("背景故事: ").append(current.background_story).append("\n");
            if (!current.personality_traits.trim().isEmpty()) sb.append("性格特点: ").append(current.personality_traits).append("\n");
            if (!current.ideals.trim().isEmpty()) sb.append("理想信念: ").append(current.ideals).append("\n");
            if (!current.bonds.trim().isEmpty()) sb.append("羁绊关系: ").append(current.bonds).append("\n");
            if (!current.flaws.trim().isEmpty()) sb.append("缺陷弱点: ").append(current.flaws).append("\n");
        }
        sb.append("--------------------------------------------------\n[种族特性]\n");
        for (String feature : current.race.get_feature_summaries()) sb.append("- ").append(feature).append("\n");
        sb.append("--------------------------------------------------\n[豁免检定加值]\n");
        sb.append(String.format("力量: %+d | 敏捷: %+d | 体质: %+d\n",
                current.get_saving_throw_bonus("Strength"), current.get_saving_throw_bonus("Dexterity"),
                current.get_saving_throw_bonus("Constitution")));
        sb.append(String.format("智力: %+d | 感知: %+d | 魅力: %+d\n",
                current.get_saving_throw_bonus("Intelligence"), current.get_saving_throw_bonus("Wisdom"),
                current.get_saving_throw_bonus("Charisma")));
        statsArea.setText(sb.toString());
    }

    private boolean hasProfileContent() {
        return !(current.background_story.trim().isEmpty() && current.personality_traits.trim().isEmpty()
                && current.ideals.trim().isEmpty() && current.bonds.trim().isEmpty() && current.flaws.trim().isEmpty());
    }

    // ==================== Tab2: 装备相关 ====================
    private void reloadEquipmentBoxes() {
        reloadSlotBox(armorBox, Equipment_Slot.ARMOR, current.equipped_armor_key, false);
        reloadSlotBox(mainHandBox, Equipment_Slot.MAIN_HAND, current.equipped_main_hand_key, false);
        reloadSlotBox(offHandBox, Equipment_Slot.OFF_HAND, current.equipped_off_hand_key, true);
        reloadSlotBox(cloakBox, Equipment_Slot.CLOAK, current.equipped_cloak_key, true);
        reloadSlotBox(accessoryBox, Equipment_Slot.ACCESSORY, current.equipped_accessory_key, true);
    }

    private void reloadSlotBox(ComboBox<String> box, Equipment_Slot slot, String equippedKey, boolean allowEmpty) {
        isReloadingSelector = true;
        box.getItems().clear();
        if (allowEmpty) box.getItems().add("[空置]");
        for (Equipment_Item item : current.get_owned_items_for_slot(slot))
            box.getItems().add(item.key + " | " + item.display_name);
        String targetKey = equippedKey == null ? "" : equippedKey;
        for (int i = 0; i < box.getItems().size(); i++) {
            String entry = box.getItems().get(i);
            if (entry.startsWith(targetKey + " |") || (targetKey.isEmpty() && entry.startsWith("[空置]"))) {
                box.getSelectionModel().select(i); break;
            }
        }
        if (box.getSelectionModel().getSelectedIndex() < 0 && !box.getItems().isEmpty()) box.getSelectionModel().select(0);
        isReloadingSelector = false;
    }

    private String getSelectedEquipmentKey(ComboBox<String> box) {
        String selected = box.getSelectionModel().getSelectedItem();
        if (selected == null || selected.startsWith("[空置]")) return "";
        int sep = selected.indexOf(" | ");
        return sep > 0 ? selected.substring(0, sep) : "";
    }

    private void handleEquip() {
        current.equip_item(Equipment_Slot.ARMOR, getSelectedEquipmentKey(armorBox));
        current.equip_item(Equipment_Slot.MAIN_HAND, getSelectedEquipmentKey(mainHandBox));
        current.equip_item(Equipment_Slot.OFF_HAND, getSelectedEquipmentKey(offHandBox));
        current.equip_item(Equipment_Slot.CLOAK, getSelectedEquipmentKey(cloakBox));
        current.equip_item(Equipment_Slot.ACCESSORY, getSelectedEquipmentKey(accessoryBox));
        Character_DAO.update_character(current); refreshUI();
        showAlert("装备已更新，当前 AC 为 " + current.ac + "。");
    }

    // ==================== Tab2: 背包相关 ====================
    private void reloadBackpackList() {
        Equipment_Item selected = backpackList.getSelectionModel().getSelectedItem();
        String selectedKey = selected == null ? "" : selected.key;
        String filterCategory = backpackFilterBox.getSelectionModel().getSelectedItem();
        if (filterCategory == null) filterCategory = "全部";
        ObservableList<Equipment_Item> items = FXCollections.observableArrayList();
        for (Equipment_Item item : current.get_owned_items_for_slot(Equipment_Slot.BACKPACK)) {
            if ("全部".equals(filterCategory) || item.get_inventory_category().equals(filterCategory)) items.add(item);
        }
        backpackList.setItems(items);
        int selectIdx = 0;
        for (int i = 0; i < items.size(); i++) { if (items.get(i).key.equals(selectedKey)) { selectIdx = i; break; } }
        if (!items.isEmpty()) backpackList.getSelectionModel().select(selectIdx);
        useItemBtn.setDisable(items.isEmpty()); sellItemBtn.setDisable(items.isEmpty());
    }

    private void refreshInventoryDetailArea() {
        StringBuilder detail = new StringBuilder();
        Equipment_Item item = backpackList.getSelectionModel().getSelectedItem();
        if (item == null) {
            detail.append("【背包说明】\n请选择一个背包物品，可查看详情并执行\"使用 / 出售\"。\n");
        } else {
            detail.append("【所选物品】\n").append(buildBackpackRowLabel(item)).append("\n");
            detail.append("分类: ").append(item.get_inventory_category()).append("\n");
            detail.append("价值: ").append(item.get_value_summary()).append("\n");
            detail.append("出售参考价: ").append(Equipment_Item.format_cp_value(item.get_sale_value_cp())).append("\n");
            detail.append("用途: ").append(item.get_use_hint()).append("\n");
        }
        detail.append("\n【当前钱包】\n").append(current.get_currency_summary()).append(" | 总值 ")
                .append(Equipment_Item.format_cp_value(current.get_total_currency_cp())).append("\n");
        detail.append("\n【武器/护甲熟练】\n");
        detail.append(current.job.equipment_proficiencies.isEmpty() ? "暂无\n" : String.join("、", current.job.equipment_proficiencies) + "\n");
        detail.append("\n【技能熟练】\n");
        detail.append(current.job.skill_proficiencies.isEmpty() ? "尚未选择\n" : String.join("、", current.job.skill_proficiencies) + "\n");
        inventoryDetailArea.setText(detail.toString());
        useItemBtn.setDisable(item == null); sellItemBtn.setDisable(item == null);
    }

    private String buildBackpackRowLabel(Equipment_Item item) {
        if (item == null) return "";
        int count = Math.max(1, current.get_item_count(item.key));
        return item.display_name + " x" + count + " | " + item.get_inventory_category()
                + " | 价值 " + item.get_value_summary() + " | " + item.description;
    }

    // ==================== Tab2: 新增物品 ====================
    private void handleAddItem() {
        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>("从物品库搜索", "从物品库搜索", "自定义普通物品", "自定义武器/护甲");
        choiceDialog.setTitle("新增物品"); choiceDialog.setHeaderText("请选择新增物品的方式："); choiceDialog.initOwner(this);
        Optional<String> result = choiceDialog.showAndWait();
        if (!result.isPresent()) return;
        String choice = result.get();
        if ("从物品库搜索".equals(choice)) openLibraryItemPicker();
        else if ("自定义普通物品".equals(choice)) openCustomMiscItemDialog();
        else if ("自定义武器/护甲".equals(choice)) openCustomEquipmentDialog();
    }

    private void openLibraryItemPicker() {
        TextField searchField = new TextField();
        ListView<Equipment_Item> resultList = new ListView<>();
        resultList.setCellFactory(list -> new ListCell<Equipment_Item>() {
            @Override protected void updateItem(Equipment_Item item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? "" : item.to_inventory_line());
            }
        });
        resultList.setPrefHeight(300);
        Runnable refreshResults = () -> {
            ObservableList<Equipment_Item> items = FXCollections.observableArrayList();
            for (Equipment_Item item : Equipment_Library.search_items(searchField.getText(), null, true)) {
                if (!current.owned_equipment_keys.contains(item.key) || item.is_stackable()) items.add(item);
            }
            resultList.setItems(items);
            if (!items.isEmpty()) resultList.getSelectionModel().select(0);
        };
        searchField.textProperty().addListener(obs -> refreshResults.run());
        Label hintLabel = new Label("这里是直接发放/调试用入口，不会扣除钱币。\n只显示当前角色尚未拥有的内置物品；可堆叠物品允许重复加入。");
        hintLabel.setStyle("-fx-text-fill: gray;");
        VBox content = new VBox(8, new Label("搜索物品库（支持名称、描述、槽位关键词）："), searchField, resultList, hintLabel);
        content.setPadding(new Insets(12)); content.setPrefWidth(760);
        refreshResults.run();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("从物品库添加"); dialog.initOwner(this);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> dialogResult = dialog.showAndWait();
        if (dialogResult.orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        Equipment_Item selected = resultList.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("请先选择一个物品。"); return; }
        current.add_item_to_inventory(selected.key);
        current.record_advancement("从物品库获得物品：" + selected.display_name);
        Character_DAO.update_character(current); refreshUI();
        showAlert("已将 [" + selected.display_name + "] 加入 " + current.name + " 的物品列表。");
    }

    private void openCustomMiscItemDialog() {
        TextField nameField = new TextField();
        ComboBox<String> slotBox = new ComboBox<>(FXCollections.observableArrayList("背包杂物", "副手/法器", "披风", "护符"));
        TextArea descriptionArea = new TextArea(); descriptionArea.setPrefRowCount(3); descriptionArea.setWrapText(true);
        Spinner<Integer> valueSpinner = new Spinner<>(0, 1000000, 10);
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(6);
        grid.add(new Label("物品名称"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("物品槽位"), 0, 1); grid.add(slotBox, 1, 1);
        grid.add(new Label("价值（cp）"), 0, 2); grid.add(valueSpinner, 1, 2);
        grid.add(new Label("功能/描述"), 0, 3); grid.add(descriptionArea, 1, 3);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("创建自定义普通物品"); dialog.initOwner(this);
        dialog.getDialogPane().setContent(new VBox(8, grid));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        String displayName = nameField.getText().trim();
        if (displayName.isEmpty()) { showAlert("物品名称不能为空。"); return; }
        String slotStr = slotBox.getSelectionModel().getSelectedItem();
        Equipment_Slot slot = Equipment_Slot.BACKPACK;
        if ("副手/法器".equals(slotStr)) slot = Equipment_Slot.OFF_HAND;
        else if ("披风".equals(slotStr)) slot = Equipment_Slot.CLOAK;
        else if ("护符".equals(slotStr)) slot = Equipment_Slot.ACCESSORY;
        Equipment_Item item = buildCustomItem(slot, displayName, descriptionArea.getText().trim(), "", 0, 0, 0, 0, 0, "", false, false, valueSpinner.getValue());
        persistCustomItem(item, "创建自定义物品");
    }

    private void openCustomEquipmentDialog() {
        ComboBox<String> templateBox = new ComboBox<>(FXCollections.observableArrayList(
                "轻甲模板", "中甲模板", "重甲模板", "单手武器模板", "双手武器模板", "灵巧武器模板", "远程武器模板", "盾牌模板"));
        TextField nameField = new TextField();
        TextArea descriptionArea = new TextArea(); descriptionArea.setPrefRowCount(3); descriptionArea.setWrapText(true);
        Spinner<Integer> armorAcSpinner = new Spinner<>(0, 30, 10);
        Spinner<Integer> shieldBonusSpinner = new Spinner<>(0, 10, 2);
        Spinner<Integer> diceCountSpinner = new Spinner<>(0, 10, 1);
        Spinner<Integer> dieSizeSpinner = new Spinner<>(0, 20, 6);
        Spinner<Integer> attackBonusSpinner = new Spinner<>(-10, 20, 0);
        Spinner<Integer> valueSpinner = new Spinner<>(0, 10000000, 100);
        TextField damageTypeField = new TextField();
        CheckBox finesseBox = new CheckBox("灵巧"); CheckBox rangedBox = new CheckBox("远程");
        Label slotHintLabel = new Label();
        Runnable applyTemplate = () -> {
            String tmpl = templateBox.getSelectionModel().getSelectedItem();
            if (tmpl == null) return;
            switch (tmpl) {
                case "轻甲模板": slotHintLabel.setText("当前模板槽位：护甲"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义轻甲"); armorAcSpinner.getValueFactory().setValue(11); break;
                case "中甲模板": slotHintLabel.setText("当前模板槽位：护甲"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义中甲"); armorAcSpinner.getValueFactory().setValue(14); break;
                case "重甲模板": slotHintLabel.setText("当前模板槽位：护甲"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义重甲"); armorAcSpinner.getValueFactory().setValue(16); break;
                case "单手武器模板": slotHintLabel.setText("当前模板槽位：主手武器"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义单手武器"); diceCountSpinner.getValueFactory().setValue(1); dieSizeSpinner.getValueFactory().setValue(8); damageTypeField.setText("挥砍"); break;
                case "双手武器模板": slotHintLabel.setText("当前模板槽位：主手武器"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义双手武器"); diceCountSpinner.getValueFactory().setValue(2); dieSizeSpinner.getValueFactory().setValue(6); damageTypeField.setText("挥砍"); break;
                case "灵巧武器模板": slotHintLabel.setText("当前模板槽位：主手武器"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义灵巧武器"); diceCountSpinner.getValueFactory().setValue(1); dieSizeSpinner.getValueFactory().setValue(6); damageTypeField.setText("穿刺"); finesseBox.setSelected(true); break;
                case "远程武器模板": slotHintLabel.setText("当前模板槽位：主手武器"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义远程武器"); diceCountSpinner.getValueFactory().setValue(1); dieSizeSpinner.getValueFactory().setValue(8); damageTypeField.setText("穿刺"); rangedBox.setSelected(true); break;
                case "盾牌模板": slotHintLabel.setText("当前模板槽位：副手/盾牌"); if (nameField.getText().trim().isEmpty()) nameField.setText("自定义盾牌"); shieldBonusSpinner.getValueFactory().setValue(2); break;
            }
        };
        templateBox.getSelectionModel().selectedItemProperty().addListener(obs -> applyTemplate.run());
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(6);
        grid.add(new Label("模板类型"), 0, 0); grid.add(templateBox, 1, 0);
        grid.add(slotHintLabel, 0, 1, 2, 1);
        grid.add(new Label("装备名称"), 0, 2); grid.add(nameField, 1, 2);
        grid.add(new Label("功能/描述"), 0, 3); grid.add(descriptionArea, 1, 3);
        grid.add(new Label("护甲 AC"), 0, 4); grid.add(armorAcSpinner, 1, 4);
        grid.add(new Label("盾牌 AC 加值"), 0, 5); grid.add(shieldBonusSpinner, 1, 5);
        grid.add(new Label("伤害骰个数"), 0, 6); grid.add(diceCountSpinner, 1, 6);
        grid.add(new Label("伤害骰面数"), 0, 7); grid.add(dieSizeSpinner, 1, 7);
        grid.add(new Label("攻击加值"), 0, 8); grid.add(attackBonusSpinner, 1, 8);
        grid.add(new Label("价值（cp）"), 0, 9); grid.add(valueSpinner, 1, 9);
        grid.add(new Label("伤害类型"), 0, 10); grid.add(damageTypeField, 1, 10);
        grid.add(finesseBox, 0, 11); grid.add(rangedBox, 1, 11);
        applyTemplate.run();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("创建自定义武器/护甲"); dialog.initOwner(this);
        dialog.getDialogPane().setContent(new VBox(8, grid));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        String tmpl = templateBox.getSelectionModel().getSelectedItem();
        if (tmpl == null) { showAlert("请选择一个装备模板。"); return; }
        String displayName = nameField.getText().trim();
        if (displayName.isEmpty()) { showAlert("装备名称不能为空。"); return; }
        Equipment_Slot slot; String armorType = "";
        if (tmpl.contains("甲")) { slot = Equipment_Slot.ARMOR; if (tmpl.contains("轻")) armorType = "Light"; else if (tmpl.contains("中")) armorType = "Medium"; else armorType = "Heavy"; }
        else if (tmpl.contains("盾")) { slot = Equipment_Slot.OFF_HAND; }
        else { slot = Equipment_Slot.MAIN_HAND; }
        Equipment_Item item = buildCustomItem(slot, displayName, descriptionArea.getText().trim(), armorType, armorAcSpinner.getValue(), shieldBonusSpinner.getValue(),
                diceCountSpinner.getValue(), dieSizeSpinner.getValue(), attackBonusSpinner.getValue(), damageTypeField.getText().trim(), finesseBox.isSelected(), rangedBox.isSelected(), valueSpinner.getValue());
        persistCustomItem(item, "创建自定义装备");
    }

    private Equipment_Item buildCustomItem(Equipment_Slot slot, String displayName, String description,
                                            String armorType, int baseAc, int shieldBonus, int adc, int ads, int ab,
                                            String damageType, boolean finesse, boolean ranged, int valueInCp) {
        ensureCharacterHasDatabaseId();
        String itemKey = Equipment_Library.build_custom_key(current.database_id, displayName);
        return new Equipment_Item(itemKey, displayName, slot,
                (description == null || description.trim().isEmpty()) ? "玩家自定义物品。" : description.trim(),
                armorType == null ? "" : armorType, baseAc, shieldBonus, adc, ads, ab,
                damageType == null ? "" : damageType.trim(), finesse, ranged, valueInCp);
    }

    private void persistCustomItem(Equipment_Item item, String actionLabel) {
        if (item == null) return;
        ensureCharacterHasDatabaseId();
        if (!Custom_Equipment_DAO.save_custom_item(current.database_id, item)) { showAlert("自定义物品保存失败。"); return; }
        current.add_item_to_inventory(item.key);
        current.record_advancement(actionLabel + "：" + item.display_name);
        Character_DAO.update_character(current); refreshUI();
        showAlert("已创建并加入物品列表：[" + item.display_name + "]。");
    }

    private void ensureCharacterHasDatabaseId() { if (current.database_id > 0) return; Character_DAO.update_character(current); }

    // ==================== Tab2: 购买物品 ====================
    private void openPurchaseDialog() {
        TextField searchField = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList("全部", "药水", "卷轴", "工具/任务", "武器/护甲", "材料/其他"));
        ListView<Equipment_Item> resultList = new ListView<>();
        resultList.setCellFactory(list -> new ListCell<Equipment_Item>() {
            @Override protected void updateItem(Equipment_Item item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? "" : item.to_inventory_line());
            }
        });
        resultList.setPrefHeight(250);
        Label walletLabel = new Label("当前钱包：" + current.get_currency_summary() + " | 总值 " + Equipment_Item.format_cp_value(current.get_total_currency_cp()));
        Spinner<Integer> quantitySpinner = new Spinner<>(1, 99, 1);
        Label totalCostLabel = new Label("总价：0 cp");
        Runnable refreshResults = () -> {
            ObservableList<Equipment_Item> items = FXCollections.observableArrayList();
            String category = categoryBox.getSelectionModel().getSelectedItem();
            for (Equipment_Item item : Equipment_Library.search_items(searchField.getText(), null, true)) {
                if ((!current.owned_equipment_keys.contains(item.key) || item.is_stackable()) && matchesPurchaseCategory(item, category)) items.add(item);
            }
            resultList.setItems(items);
            if (!items.isEmpty()) resultList.getSelectionModel().select(0);
            refreshPurchaseTotalLabel(resultList.getSelectionModel().getSelectedItem(), quantitySpinner.getValue(), totalCostLabel);
        };
        Runnable refreshTotal = () -> refreshPurchaseTotalLabel(resultList.getSelectionModel().getSelectedItem(), quantitySpinner.getValue(), totalCostLabel);
        searchField.textProperty().addListener(obs -> refreshResults.run());
        categoryBox.getSelectionModel().selectedItemProperty().addListener(obs -> refreshResults.run());
        resultList.getSelectionModel().selectedItemProperty().addListener(obs -> refreshTotal.run());
        quantitySpinner.valueProperty().addListener(obs -> refreshTotal.run());
        VBox topPanel = new VBox(6, new Label("搜索物品库并购买"), searchField, categoryBox, walletLabel);
        HBox bottomPanel = new HBox(8, new Label("购买数量"), quantitySpinner, totalCostLabel);
        bottomPanel.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(8, topPanel, resultList, bottomPanel);
        content.setPadding(new Insets(12)); content.setPrefWidth(760);
        refreshResults.run();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("购买物品"); dialog.initOwner(this);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> dialogResult = dialog.showAndWait();
        if (dialogResult.orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        Equipment_Item selected = resultList.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("请先选择一个物品。"); return; }
        int quantity = Math.max(1, quantitySpinner.getValue());
        if (!selected.is_stackable()) quantity = 1;
        int totalCostCp = selected.value_in_cp * quantity;
        if (totalCostCp <= 0) { showAlert("该物品当前没有可购买价格。"); return; }
        if (!current.spend_currency_cp(totalCostCp)) { showAlert("钱币不足。"); return; }
        for (int i = 0; i < quantity; i++) current.add_item_to_inventory(selected.key);
        current.record_advancement("购买物品：" + selected.display_name + " x" + quantity + "，花费 " + Equipment_Item.format_cp_value(totalCostCp));
        Character_DAO.update_character(current); refreshUI();
        showAlert("已购买 [" + selected.display_name + "] x" + quantity + "。\n花费：" + Equipment_Item.format_cp_value(totalCostCp) + "\n剩余钱包：" + current.get_currency_summary());
    }

    private void refreshPurchaseTotalLabel(Equipment_Item item, int quantity, Label totalCostLabel) {
        if (item == null) { totalCostLabel.setText("总价：0 cp"); return; }
        int safeQuantity = item.is_stackable() ? Math.max(1, quantity) : 1;
        totalCostLabel.setText("总价：" + Equipment_Item.format_cp_value(item.value_in_cp * safeQuantity) + (item.is_stackable() ? "" : "（此物品每次只能购入 1 件）"));
    }

    private boolean matchesPurchaseCategory(Equipment_Item item, String category) {
        if (item == null || category == null || "全部".equals(category)) return true;
        if ("药水".equals(category)) { String text = (item.display_name + " " + item.description).toLowerCase(); return text.contains("药水") || text.contains("药剂") || text.contains("抗毒剂"); }
        if ("卷轴".equals(category)) return item.is_scroll_item();
        if ("工具/任务".equals(category)) return "工具/任务".equals(item.get_inventory_category()) || item.is_coin_item();
        if ("武器/护甲".equals(category)) return item.slot == Equipment_Slot.ARMOR || item.slot == Equipment_Slot.MAIN_HAND || item.slot == Equipment_Slot.OFF_HAND;
        if ("材料/其他".equals(category)) return !item.is_scroll_item() && !"工具/任务".equals(item.get_inventory_category()) && item.slot == Equipment_Slot.BACKPACK && !((item.display_name + " " + item.description).toLowerCase().contains("药水"));
        return true;
    }

    // ==================== Tab2: 出售物品 ====================
    private void handleSellSelectedItem() {
        Equipment_Item item = backpackList.getSelectionModel().getSelectedItem();
        if (item == null) { showAlert("请先选择一个背包物品。"); return; }
        int currentCount = Math.max(1, current.get_item_count(item.key));
        Spinner<Integer> quantitySpinner = new Spinner<>(1, currentCount, 1);
        Label totalLabel = new Label();
        quantitySpinner.valueProperty().addListener(obs -> totalLabel.setText("本次出售可获得：" + Equipment_Item.format_cp_value(item.get_sale_value_cp() * Math.max(1, quantitySpinner.getValue()))));
        totalLabel.setText("本次出售可获得：" + Equipment_Item.format_cp_value(item.get_sale_value_cp()));
        VBox panel = new VBox(8, new Label("出售物品：" + item.display_name), new Label("单件参考价：" + Equipment_Item.format_cp_value(item.get_sale_value_cp())), new Label("出售数量"), quantitySpinner, totalLabel);
        panel.setPadding(new Insets(12));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("出售物品"); dialog.initOwner(this);
        dialog.getDialogPane().setContent(panel);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        int quantity = Math.max(1, quantitySpinner.getValue());
        int totalGain = item.get_sale_value_cp() * quantity;
        for (int i = 0; i < quantity; i++) { if (!current.remove_item_from_inventory(item.key)) break; }
        current.add_currency_cp(totalGain);
        current.record_advancement("出售物品：" + item.display_name + " x" + quantity + "，获得 " + Equipment_Item.format_cp_value(totalGain));
        Character_DAO.update_character(current); refreshUI();
        showAlert("已出售 [" + item.display_name + "] x" + quantity + "。\n获得：" + Equipment_Item.format_cp_value(totalGain) + "\n当前钱包：" + current.get_currency_summary());
    }

    // ==================== Tab2: 使用物品（主入口） ====================
    private void handleUseSelectedItem() {
        Equipment_Item item = backpackList.getSelectionModel().getSelectedItem();
        if (item == null) { showAlert("请先从背包列表中选择一个物品。"); return; }
        if (item.is_healing_item()) {
            int healAmount = item.get_flat_healing_amount();
            if (healAmount <= 0 && item.get_healing_dice_count() > 0)
                healAmount = Dice_Util.roll_dice(item.get_healing_dice_count(), item.get_healing_die_size()) + item.get_healing_bonus();
            int beforeHp = current.current_hp;
            current.set_current_hp(current.current_hp + healAmount);
            current.remove_item_from_inventory(item.key);
            int remainingCount = current.get_item_count(item.key);
            current.record_advancement("使用物品：" + item.display_name + "，恢复生命值 " + (current.current_hp - beforeHp) + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
            Character_DAO.update_character(current); refreshUI();
            showAlert(current.name + " 使用了 [" + item.display_name + "]，生命值从 " + beforeHp + " 提升到 " + current.current_hp + "/" + current.hp + "。" + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
            return;
        }
        if (handleSpecialInventoryItemUse(item)) { refreshUI(); return; }
        if (item.is_scroll_item()) { if (handleScrollItemUse(item)) refreshUI(); return; }
        if (item.is_bomb_item()) { if (handleBombItemUse(item)) refreshUI(); return; }
        if (item.is_coin_item()) {
            int gainedValue = item.get_currency_gain_cp();
            current.add_currency_cp(gainedValue);
            current.remove_item_from_inventory(item.key);
            int remainingCount = current.get_item_count(item.key);
            current.record_advancement("兑换钱币：" + item.display_name + "，获得 " + Equipment_Item.format_cp_value(gainedValue) + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
            Character_DAO.update_character(current); refreshUI();
            showAlert("已将 [" + item.display_name + "] 兑换为 " + Equipment_Item.format_cp_value(gainedValue) + "。\n当前钱包：" + current.get_currency_summary());
            return;
        }
        if (item.is_key_item()) { current.record_advancement("检查钥匙：" + item.display_name); Character_DAO.update_character(current); showAlert("钥匙已准备好使用，但不会自动消耗。"); return; }
        if (item.is_quest_item()) { current.record_advancement("查看任务物品：" + item.display_name); Character_DAO.update_character(current); showAlert("已查看任务物品。"); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "当前物品还没有自动结算效果。\n是否将 [" + item.display_name + "] 标记为已使用并从背包移除？", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("使用物品"); confirm.initOwner(this);
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.orElse(ButtonType.NO) != ButtonType.YES) return;
        current.remove_item_from_inventory(item.key);
        int remainingCount = current.get_item_count(item.key);
        current.record_advancement("使用物品：" + item.display_name + (remainingCount > 0 ? "，剩余 " + remainingCount + " 件" : ""));
        Character_DAO.update_character(current); refreshUI();
        showAlert("已将 [" + item.display_name + "] 标记为已使用。" + (remainingCount > 0 ? "\n背包中还剩 " + remainingCount + " 件。" : ""));
    }

    // ==================== 特殊物品使用 ====================
    private boolean handleSpecialInventoryItemUse(Equipment_Item item) {
        if (item == null) return false;
        if ("potion_of_fire_breath".equals(item.key)) return handleFireBreathPotionUse(item);
        if ("potion_of_invisibility".equals(item.key)) return handleInvisibilityPotionUse(item);
        if ("potion_of_climbing".equals(item.key)) return handleClimbingPotionUse(item);
        if ("antitoxin".equals(item.key)) return handleAntitoxinUse(item);
        if ("holy_water".equals(item.key)) return handleHolyWaterUse(item);
        return false;
    }

    private boolean handleFireBreathPotionUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可喷吐火焰的敌方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择火焰喷吐目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("使用药水 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            int damage = Dice_Util.roll_dice(3, 6); String note = noteField.getText().trim();
            String log = combatEngine.apply_external_damage(item.display_name, tc.combatant, damage, "火焰", note);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用物品：" + item.display_name + "，对 [" + tc.combatant.display_name + "] 造成 " + damage + " 点火焰伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log);
            showAlert(item.display_name + " 已生效，造成 " + damage + " 点火焰伤害。"); return true;
        }
        TextField targetField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("喷吐火焰命中的目标 / 区域"), targetField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("使用药水 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        int damage = Dice_Util.roll_dice(3, 6); String target = targetField.getText().trim(); String note = noteArea.getText().trim();
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用物品：" + item.display_name + "，目标 [" + (target.isEmpty() ? "未指定" : target) + "]，造成 " + damage + " 点火焰伤害" + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert(item.display_name + " 已记录，伤害：" + damage + " 点火焰伤害。"); return true;
    }

    private boolean handleInvisibilityPotionUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.PLAYER);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可饮用隐形药水的友方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择隐形目标（持续 2 轮）"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("使用药水 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            tc.combatant.apply_status(Combat_Status_Type.INVISIBLE, 2); String note = noteField.getText().trim();
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用物品：" + item.display_name + "，使 [" + tc.combatant.display_name + "] 获得隐形状态 2 轮" + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current);
            combatUI.refresh_after_external_effect(item.display_name + " -> " + tc.combatant.display_name + "\n目标进入隐形状态（2 轮）。" + (note.isEmpty() ? "" : "\n备注：" + note));
            return true;
        }
        TextField noteField = new TextField(); VBox panel = new VBox(8, new Label("记录本次隐形用途"), noteField); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("使用药水 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用物品：" + item.display_name + "，获得隐形效果" + (noteField.getText().trim().isEmpty() ? "" : "，备注：" + noteField.getText().trim()) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("已记录隐形药水效果。"); return true;
    }

    private boolean handleClimbingPotionUse(Equipment_Item item) {
        TextField routeField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("攀爬目标 / 路线"), routeField, new Label("效果备注"), noteArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("使用药水 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        String route = routeField.getText().trim(); String note = noteArea.getText().trim();
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用物品：" + item.display_name + "，用于攀爬 [" + (route.isEmpty() ? "未指定路线" : route) + "]" + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("已记录攀爬药水效果。"); return true;
    }

    private boolean handleAntitoxinUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.PLAYER);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可使用抗毒剂的友方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择使用抗毒剂的目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("使用抗毒剂", panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            boolean removed = removeStatusFromCombatant(tc.combatant, Combat_Status_Type.POISONED); String note = noteField.getText().trim();
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用物品：" + item.display_name + "，目标 [" + tc.combatant.display_name + "]" + (removed ? "，已清除中毒状态" : "，未发现中毒状态") + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current);
            combatUI.refresh_after_external_effect(item.display_name + " -> " + tc.combatant.display_name + "\n" + (removed ? "已清除中毒状态。" : "未发现中毒状态。") + (note.isEmpty() ? "" : "\n备注：" + note));
            return true;
        }
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用物品：" + item.display_name + "，获得抗毒保护" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("已记录抗毒剂效果。"); return true;
    }

    private boolean handleHolyWaterUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可泼洒圣水的敌方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择圣水目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("使用圣水", panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            int damage = Dice_Util.roll_dice(2, 6); String note = noteField.getText().trim();
            String log = combatEngine.apply_external_damage(item.display_name, tc.combatant, damage, "光耀", note);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用物品：" + item.display_name + "，对 [" + tc.combatant.display_name + "] 造成 " + damage + " 点光耀伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log); return true;
        }
        TextField targetField = new TextField(); VBox panel = new VBox(8, new Label("圣水目标 / 净化对象"), targetField); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("使用圣水", panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用物品：" + item.display_name + "，目标 [" + (targetField.getText().trim().isEmpty() ? "未指定" : targetField.getText().trim()) + "]" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("已记录圣水使用。"); return true;
    }

    // ==================== 卷轴使用（路由） ====================
    private boolean handleScrollItemUse(Equipment_Item item) {
        if ("scroll_of_healing_touch".equals(item.key)) return handleHealingScrollUse(item);
        if ("scroll_of_fireball".equals(item.key)) return handleFireballScrollUse(item);
        if ("scroll_of_identify".equals(item.key)) return handleIdentifyScrollUse(item);
        if ("scroll_of_arcane_insight".equals(item.key)) return handleArcaneInsightScrollUse(item);
        if ("scroll_of_magic_missile".equals(item.key)) return handleMagicMissileScrollUse(item);
        if ("scroll_of_shield".equals(item.key)) return handleDefensiveStatusScrollUse(item, "护盾术卷轴", Combat_Status_Type.SHIELDED, 1, "获得护盾状态，+5 AC，持续 1 轮。");
        if ("scroll_of_detect_magic".equals(item.key)) return handleDetectMagicScrollUse(item);
        if ("scroll_of_mage_armor".equals(item.key)) return handleDefensiveStatusScrollUse(item, "法师护甲卷轴", Combat_Status_Type.SHIELDED, 3, "获得法师护甲保护，持续 3 轮。");
        if ("scroll_of_misty_step".equals(item.key)) return handleMistyStepScrollUse(item);
        if ("scroll_of_web".equals(item.key)) return handleControlScrollUse(item, Combat_Status_Type.RESTRAINED, 2, "Dexterity", 13, "蛛网缠住了目标，进入束缚状态。");
        if ("scroll_of_hold_person".equals(item.key)) return handleControlScrollUse(item, Combat_Status_Type.PARALYZED, 2, "Wisdom", 14, "人类定身术生效，目标陷入麻痹。");
        if ("scroll_of_sleep".equals(item.key)) return handleSleepScrollUse(item);
        if ("scroll_of_scorching_ray".equals(item.key)) return handleScorchingRayScrollUse(item);
        if ("scroll_of_ray_of_frost".equals(item.key)) return handleRayOfFrostScrollUse(item);
        if ("scroll_of_dispel_magic".equals(item.key)) return handleDispelMagicScrollUse(item);
        return handleLoreScrollUse(item);
    }

    // 由于文件长度限制，其余卷轴处理方法、炸弹处理、施法面板、成长面板、以及所有辅助方法
    // 均已在此文件中实现。以下为剩余的核心方法。

    private boolean handleLoreScrollUse(Equipment_Item item) {
        TextField subjectField = new TextField(); TextArea resultArea = new TextArea(); resultArea.setPrefRowCount(4); resultArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("研究主题/目标地点"), subjectField, new Label("从卷轴中获得的具体情报"), resultArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("阅读卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        String subject = subjectField.getText().trim(); if (subject.isEmpty()) subject = "未指定主题";
        String insight = resultArea.getText().trim(); if (insight.isEmpty()) insight = "尚未记录具体结果";
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("阅读卷轴：" + item.display_name + "，主题 [" + subject + "]，结果：" + insight + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("卷轴内容已记录。\n主题: " + subject + "\n结果: " + insight); return true;
    }

    private boolean handleHealingScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可治疗的有效目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("疗伤术卷轴 - 选择治疗目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) { showAlert("请先选择一个治疗目标。"); return false; }
            int healAmount = Dice_Util.roll_dice(1, 8) + 3; String note = noteField.getText().trim();
            String log = combatEngine.apply_external_healing(item.display_name, tc.combatant, healAmount, note);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，为 [" + tc.combatant.display_name + "] 恢复 " + healAmount + " 点生命值" + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log);
            showAlert(tc.combatant.display_name + " 恢复 " + healAmount + " 点生命值。"); return true;
        }
        int healAmount = Dice_Util.roll_dice(1, 8) + 3; int beforeHp = current.current_hp;
        current.set_current_hp(current.current_hp + healAmount);
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，恢复生命值 " + (current.current_hp - beforeHp) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current);
        showAlert(current.name + " 使用了 [" + item.display_name + "]，生命值从 " + beforeHp + " 提升到 " + current.current_hp + "/" + current.hp + "。"); return true;
    }

    private boolean handleFireballScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可选中的目标。"); return false; }
            ListView<CombatantChoice> targetList = new ListView<>(FXCollections.observableArrayList(targetChoices));
            targetList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); targetList.setPrefHeight(200);
            TextField areaField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
            VBox panel = new VBox(8, new Label("火球术卷轴 - 选择被波及的目标（可多选）"), targetList, new Label("爆心区域"), areaField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            List<CombatantChoice> selectedChoices = targetList.getSelectionModel().getSelectedItems();
            if (selectedChoices.isEmpty()) { showAlert("请至少选择一个目标。"); return false; }
            String area = areaField.getText().trim(); String note = noteArea.getText().trim();
            String combinedNote = (area.isEmpty() ? "" : "爆心区域：" + area) + (note.isEmpty() ? "" : "；" + note);
            int damage = Dice_Util.roll_dice(8, 6);
            StringBuilder battleLog = new StringBuilder(); List<String> targetNames = new ArrayList<>();
            for (CombatantChoice choice : selectedChoices) {
                if (battleLog.length() > 0) battleLog.append("\n\n");
                battleLog.append(combatEngine.apply_external_damage(item.display_name, choice.combatant, damage, "火焰", combinedNote));
                targetNames.add(choice.combatant.display_name);
            }
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，对 [" + String.join("、", targetNames) + "] 造成 " + damage + " 点火焰伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(battleLog.toString());
            showAlert(item.display_name + " 已施放。\n受影响目标：" + String.join("、", targetNames) + "\n统一伤害：" + damage + " 点火焰伤害"); return true;
        }
        TextField areaField = new TextField(); TextField targetField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("火球术卷轴"), new Label("爆心区域"), areaField, new Label("受影响目标"), targetField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        int damage = Dice_Util.roll_dice(8, 6);
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，在 [" + areaField.getText().trim() + "] 释放火球术，造成约 " + damage + " 点火焰伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("火球术已记录。\n伤害: " + damage + " 点火焰伤害"); return true;
    }

    private boolean handleIdentifyScrollUse(Equipment_Item item) {
        TextField targetField = new TextField(); TextArea propertyArea = new TextArea(); propertyArea.setPrefRowCount(4); propertyArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("被鉴定的物品 / 法阵 / 现象"), targetField, new Label("识别出的魔法性质 / 特殊效果"), propertyArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        String target = targetField.getText().trim(); if (target.isEmpty()) target = "未指定对象";
        String property = propertyArea.getText().trim(); if (property.isEmpty()) property = "尚未记录具体鉴定结果";
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，鉴定对象 [" + target + "]，结果：" + property + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("鉴定结果已记录。\n对象: " + target + "\n结果: " + property); return true;
    }

    private boolean handleMagicMissileScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可命中的敌方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择魔法飞弹目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            int damage = Dice_Util.roll_dice(3, 4) + 3; String note = noteField.getText().trim();
            String log = combatEngine.apply_external_damage(item.display_name, tc.combatant, damage, "力场", note);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，对 [" + tc.combatant.display_name + "] 造成 " + damage + " 点力场伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log); return true;
        }
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        int damage = Dice_Util.roll_dice(3, 4) + 3;
        current.record_advancement("使用卷轴：" + item.display_name + "，造成 " + damage + " 点力场伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("魔法飞弹已记录，伤害 " + damage + " 点力场伤害。"); return true;
    }

    private boolean handleArcaneInsightScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可洞察的敌方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择洞察目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            tc.combatant.apply_status(Combat_Status_Type.CURSED, 2); String note = noteField.getText().trim();
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，洞察 [" + tc.combatant.display_name + "]，弱点暴露 2 轮" + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current);
            combatUI.refresh_after_external_effect(item.display_name + " -> " + tc.combatant.display_name + "\n目标陷入诅咒/破绽暴露状态（2 轮）。"); return true;
        }
        TextField subjectField = new TextField(); TextArea insightArea = new TextArea(); insightArea.setPrefRowCount(4); insightArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("研究对象 / 主题"), subjectField, new Label("洞察到的弱点 / 线索"), insightArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，研究 [" + (subjectField.getText().trim().isEmpty() ? "未指定" : subjectField.getText().trim()) + "]" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("奥术洞察结果已记录。"); return true;
    }

    private boolean handleDefensiveStatusScrollUse(Equipment_Item item, String title, Combat_Status_Type statusType, int durationRounds, String detailText) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.PLAYER);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可施加防护效果的友方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + title, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            tc.combatant.apply_status(statusType, durationRounds); String note = noteField.getText().trim();
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，为 [" + tc.combatant.display_name + "] 提供防护效果 " + durationRounds + " 轮" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current);
            combatUI.refresh_after_external_effect(item.display_name + " -> " + tc.combatant.display_name + "\n" + detailText + (note.isEmpty() ? "" : "\n备注：" + note));
            return true;
        }
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，效果：" + detailText + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert(detailText); return true;
    }

    private boolean handleDetectMagicScrollUse(Equipment_Item item) {
        TextField areaField = new TextField();
        ComboBox<String> schoolBox = new ComboBox<>(FXCollections.observableArrayList("未识别", "防护", "咒法", "预言", "惑控", "塑能", "幻术", "死灵", "变化"));
        ComboBox<String> intensityBox = new ComboBox<>(FXCollections.observableArrayList("微弱", "中等", "强烈", "压倒性"));
        TextArea resultArea = new TextArea(); resultArea.setPrefRowCount(4); resultArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("侦测区域 / 物件"), areaField, new Label("侦测到的学派"), schoolBox, new Label("灵光强度"), intensityBox, new Label("侦测到的魔法灵光 / 细节"), resultArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，区域 [" + (areaField.getText().trim().isEmpty() ? "未指定" : areaField.getText().trim()) + "]，学派 " + schoolBox.getSelectionModel().getSelectedItem() + "，强度 " + intensityBox.getSelectionModel().getSelectedItem() + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("侦测魔法结果已记录。"); return true;
    }

    private boolean handleMistyStepScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.PLAYER);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可瞬移的友方目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField destinationField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
            VBox panel = new VBox(8, new Label("选择瞬移目标"), targetBox, new Label("瞬移到的位置"), destinationField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            String dest = destinationField.getText().trim(); String note = noteArea.getText().trim();
            removeStatusFromCombatant(tc.combatant, Combat_Status_Type.RESTRAINED);
            removeStatusFromCombatant(tc.combatant, Combat_Status_Type.PRONE);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，令 [" + tc.combatant.display_name + "] 瞬移到 [" + (dest.isEmpty() ? "未指定" : dest) + "]" + (note.isEmpty() ? "" : "，备注：" + note) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current);
            combatUI.refresh_after_external_effect(item.display_name + " -> " + tc.combatant.display_name + "\n目标瞬移到 [" + (dest.isEmpty() ? "未指定" : dest) + "]。"); return true;
        }
        TextField destinationField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("瞬移到的位置"), destinationField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，瞬移到 [" + (destinationField.getText().trim().isEmpty() ? "未指定" : destinationField.getText().trim()) + "]" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("迷踪步卷轴效果已记录。"); return true;
    }

    private boolean handleControlScrollUse(Equipment_Item item, Combat_Status_Type statusType, int durationRounds, String saveAbility, int saveDc, String successText) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可作为控制目标的敌方单位。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择控制目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            int saveRoll = Dice_Util.roll_d20(); int saveBonus = tc.combatant.get_saving_throw_bonus(saveAbility);
            int saveTotal = saveRoll + saveBonus; boolean resisted = saveTotal >= saveDc; String note = noteField.getText().trim();
            StringBuilder log = new StringBuilder();
            log.append(item.display_name).append(" -> ").append(tc.combatant.display_name).append("\n");
            log.append(saveAbility).append(" 豁免：d20=").append(saveRoll).append(saveBonus >= 0 ? "+" : "").append(saveBonus).append(" = ").append(saveTotal).append(" vs DC ").append(saveDc).append("\n");
            if (resisted) log.append("目标豁免成功。\n");
            else { tc.combatant.apply_status(statusType, durationRounds); log.append(successText).append("\n"); }
            if (!note.isEmpty()) log.append("备注：").append(note);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + tc.combatant.display_name + "]，" + saveAbility + " 豁免 " + saveTotal + "/" + saveDc + (resisted ? "，成功抵抗" : "，陷入" + statusType.label) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log.toString().trim()); return true;
        }
        TextField targetField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("控制目标"), targetField, new Label("结果备注"), noteArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + (targetField.getText().trim().isEmpty() ? "未指定" : targetField.getText().trim()) + "]" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("控制卷轴效果已记录。"); return true;
    }

    private boolean handleSleepScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可作为睡眠术目标的敌方单位。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择睡眠术目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            int sleepPool = Dice_Util.roll_dice(5, 8); boolean asleep = tc.combatant.current_hp <= sleepPool;
            if (asleep) tc.combatant.apply_status(Combat_Status_Type.ASLEEP, 2); String note = noteField.getText().trim();
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + tc.combatant.display_name + "]，睡眠值 " + sleepPool + "，HP " + tc.combatant.current_hp + (asleep ? "，陷入沉睡" : "，未被压制") + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current);
            combatUI.refresh_after_external_effect(item.display_name + " -> " + tc.combatant.display_name + "\n睡眠值 5d8=" + sleepPool + "，HP " + tc.combatant.current_hp + (asleep ? "，陷入沉睡。" : "，未进入沉睡。")); return true;
        }
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        int sleepPool = Dice_Util.roll_dice(5, 8);
        current.record_advancement("使用卷轴：" + item.display_name + "，睡眠值 5d8=" + sleepPool + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("睡眠术卷轴已记录。\n睡眠值：5d8 = " + sleepPool); return true;
    }

    private boolean handleScorchingRayScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        int spellAttackBonus = current.get_proficiency_bonus() + Math.max(current.stats.get_mod(current.stats.intel), Math.max(current.stats.get_mod(current.stats.wis), current.stats.get_mod(current.stats.cha)));
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可作为灼热射线目标的敌方单位。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择灼热射线目标（三道射线集中）"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            StringBuilder log = new StringBuilder(); log.append(item.display_name).append(" -> ").append(tc.combatant.display_name).append("\n");
            int totalDamage = 0; int hits = 0;
            for (int i = 1; i <= 3; i++) {
                int d20 = Dice_Util.roll_d20(); int totalAttack = d20 + spellAttackBonus;
                boolean hit = d20 == 20 || totalAttack >= tc.combatant.get_effective_armor_class();
                log.append("第").append(i).append("道射线 d20=").append(d20).append(hit ? " 命中" : " 未命中").append("\n");
                if (hit) { int dmg = Dice_Util.roll_dice(2, 6); totalDamage += dmg; hits++; tc.combatant.current_hp = Math.max(0, tc.combatant.current_hp - dmg); log.append("造成 ").append(dmg).append(" 点火焰伤害，剩余 HP ").append(tc.combatant.current_hp).append("/").append(tc.combatant.max_hp).append("\n"); if (!tc.combatant.is_alive()) { log.append(tc.combatant.display_name).append(" 倒下了。\n"); break; } }
            }
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + tc.combatant.display_name + "]，命中 " + hits + " 道射线，造成 " + totalDamage + " 点火焰伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log.toString().trim()); return true;
        }
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        int totalDamage = 0; int hits = 0;
        for (int i = 1; i <= 3; i++) { int d20 = Dice_Util.roll_d20(); if (d20 + spellAttackBonus >= 13) { hits++; totalDamage += Dice_Util.roll_dice(2, 6); } }
        current.record_advancement("使用卷轴：" + item.display_name + "，命中 " + hits + " 道射线，造成约 " + totalDamage + " 点火焰伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("灼热射线已记录。\n命中 " + hits + " 道射线，总伤害 " + totalDamage + " 点火焰伤害"); return true;
    }

    private boolean handleRayOfFrostScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        int spellAttackBonus = current.get_proficiency_bonus() + Math.max(current.stats.get_mod(current.stats.intel), Math.max(current.stats.get_mod(current.stats.wis), current.stats.get_mod(current.stats.cha)));
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine, Combatant.Side.ENEMY);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可作为寒霜射线目标的敌方单位。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择寒霜射线目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            int d20 = Dice_Util.roll_d20(); int totalAttack = d20 + spellAttackBonus;
            boolean hit = d20 == 20 || totalAttack >= tc.combatant.get_effective_armor_class(); String note = noteField.getText().trim();
            StringBuilder log = new StringBuilder(); log.append(item.display_name).append(" -> ").append(tc.combatant.display_name).append("\n攻击 d20=").append(d20).append("+").append(spellAttackBonus).append("=").append(totalAttack).append(hit ? " 命中\n" : " 未命中\n");
            int damage = 0;
            if (hit) { damage = Dice_Util.roll_dice(1, 8); tc.combatant.current_hp = Math.max(0, tc.combatant.current_hp - damage); tc.combatant.apply_status(Combat_Status_Type.SLOWED, 1); log.append("造成 ").append(damage).append(" 点寒冷伤害，附加迟缓 1 轮\n"); }
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + tc.combatant.display_name + "]，攻击 " + totalAttack + (hit ? "，命中 " + damage + " 点寒冷伤害+迟缓" : "，未命中") + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log.toString().trim()); return true;
        }
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        int d20 = Dice_Util.roll_d20(); boolean hit = d20 + spellAttackBonus >= 13; int damage = hit ? Dice_Util.roll_dice(1, 8) : 0;
        current.record_advancement("使用卷轴：" + item.display_name + "，d20=" + d20 + (hit ? "，命中 " + damage + " 点寒冷伤害" : "，未命中") + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("寒霜射线已记录。\n" + (hit ? "命中 " + damage + " 点寒冷伤害" : "未命中")); return true;
    }

    private boolean handleDispelMagicScrollUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可作为解除魔法目标的单位。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField noteField = new TextField();
            VBox panel = new VBox(8, new Label("选择解除魔法目标"), targetBox, new Label("备注"), noteField); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) return false;
            List<String> removedLabels = clearStatusesFromCombatant(tc.combatant); String note = noteField.getText().trim();
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + tc.combatant.display_name + "]，移除状态：" + (removedLabels.isEmpty() ? "无" : String.join("、", removedLabels)) + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect("解除魔法 -> " + tc.combatant.display_name + "\n已移除：" + (removedLabels.isEmpty() ? "无" : String.join("、", removedLabels))); return true;
        }
        TextField targetField = new TextField(); TextArea resultArea = new TextArea(); resultArea.setPrefRowCount(3); resultArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("解除魔法目标 / 法阵"), targetField, new Label("移除结果"), resultArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("施放卷轴 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用卷轴：" + item.display_name + "，目标 [" + (targetField.getText().trim().isEmpty() ? "未指定" : targetField.getText().trim()) + "]" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert("解除魔法卷轴效果已记录。"); return true;
    }

    // ==================== 炸弹使用 ====================
    private boolean handleBombItemUse(Equipment_Item item) {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        Combat_Engine combatEngine = getActiveCombatEngine();
        if (combatUI != null && combatEngine != null) {
            List<CombatantChoice> targetChoices = getLivingCombatantChoices(combatEngine);
            if (targetChoices.isEmpty()) { showAlert("当前战斗中没有可投掷的有效目标。"); return false; }
            ComboBox<CombatantChoice> targetBox = new ComboBox<>(FXCollections.observableArrayList(targetChoices));
            TextField areaField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
            VBox panel = new VBox(8, new Label("选择炸弹命中的主目标"), targetBox, new Label("爆炸区域 / 落点"), areaField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
            Optional<ButtonType> result = showCustomDialog("使用爆炸物 - " + item.display_name, panel);
            if (!result.isPresent()) return false;
            CombatantChoice tc = targetBox.getSelectionModel().getSelectedItem();
            if (tc == null || tc.combatant == null) { showAlert("请先选择一个目标。"); return false; }
            String area = areaField.getText().trim(); String note = noteArea.getText().trim();
            String combinedNote = (area.isEmpty() ? "" : "爆炸区域：" + area) + (note.isEmpty() ? "" : "；" + note);
            int damage = Dice_Util.roll_dice(item.get_bomb_dice_count(), item.get_bomb_die_size()) + item.get_bomb_bonus();
            String log = combatEngine.apply_external_damage(item.display_name, tc.combatant, damage, item.get_bomb_damage_type(), combinedNote);
            current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
            current.record_advancement("使用爆炸物：" + item.display_name + "，命中 [" + tc.combatant.display_name + "]，造成 " + damage + " 点" + item.get_bomb_damage_type() + "伤害" + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
            Character_DAO.update_character(current); combatUI.refresh_after_external_effect(log);
            showAlert(item.display_name + " 已投掷，伤害 " + damage + " 点" + item.get_bomb_damage_type()); return true;
        }
        TextField targetField = new TextField(); TextArea noteArea = new TextArea(); noteArea.setPrefRowCount(3); noteArea.setWrapText(true);
        VBox panel = new VBox(8, new Label("命中目标 / 区域"), targetField, new Label("备注"), noteArea); panel.setPadding(new Insets(12));
        Optional<ButtonType> result = showCustomDialog("使用爆炸物 - " + item.display_name, panel);
        if (!result.isPresent()) return false;
        String target = targetField.getText().trim(); if (target.isEmpty()) target = "未指定目标";
        String note = noteArea.getText().trim(); if (note.isEmpty()) note = "无额外备注";
        int damage = Dice_Util.roll_dice(item.get_bomb_dice_count(), item.get_bomb_die_size()) + item.get_bomb_bonus();
        current.remove_item_from_inventory(item.key); int rc = current.get_item_count(item.key);
        current.record_advancement("使用爆炸物：" + item.display_name + "，目标 [" + target + "]，造成约 " + damage + " 点" + item.get_bomb_damage_type() + "伤害，备注：" + note + (rc > 0 ? "，剩余 " + rc + " 件" : ""));
        Character_DAO.update_character(current); showAlert(item.display_name + " 已投掷。\n目标: " + target + "\n伤害: " + damage + " 点" + item.get_bomb_damage_type()); return true;
    }

    // ==================== Tab3: 施法面板刷新 ====================
    private void refreshSpellcastingPanel() {
        StringBuilder sb = new StringBuilder();
        manageSpellbookBtn.setVisible(false); manageSpellSelectionBtn.setVisible(false); managePreparedSpellBtn.setVisible(false);
        if (current.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current.job;
            manageSpellbookBtn.setVisible(true); manageSpellSelectionBtn.setVisible(true); managePreparedSpellBtn.setVisible(true);
            manageSpellbookBtn.setText("管理戏法"); manageSpellSelectionBtn.setText("管理法术书"); managePreparedSpellBtn.setText("管理准备法术");
            sb.append("【法师施法资源】\n").append(wizard.get_spell_slot_summary()).append("\n");
            sb.append("戏法已知数: ").append(wizard.cantrips_known).append("\n法术书容量: ").append(wizard.spells_in_spellbook).append("\n");
            sb.append("可准备法术数: ").append(wizard.get_prepared_spell_count(current.stats.get_mod(current.stats.intel))).append("\n");
            sb.append("奥术回能额度: ").append(wizard.arcane_recovery_level).append("\n奥术回能状态: ").append(wizard.get_arcane_recovery_status()).append("\n\n");
            appendListSection(sb, "已知戏法", wizard.get_known_cantrip_lines());
            appendListSection(sb, "法术书", wizard.get_spellbook_lines());
            appendListSection(sb, "准备法术", wizard.get_prepared_spell_lines());
        } else if (current.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current.job;
            manageSpellbookBtn.setVisible(true); manageSpellSelectionBtn.setVisible(true);
            manageSpellbookBtn.setText("管理戏法"); manageSpellSelectionBtn.setText("管理已知法术");
            sb.append("【术士施法资源】\n").append(sorcerer.get_spell_slot_summary()).append("\n术法点: ").append(sorcerer.get_sorcery_point_summary()).append("\n");
            sb.append("戏法已知数: ").append(sorcerer.cantrips_known).append("\n法术已知数: ").append(sorcerer.spells_known_count).append("\n\n");
            appendListSection(sb, "已知戏法", sorcerer.get_known_cantrip_lines());
            appendListSection(sb, "已知法术", sorcerer.get_known_spell_lines());
        } else if (current.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current.job;
            manageSpellbookBtn.setVisible(true); manageSpellSelectionBtn.setVisible(true);
            manageSpellbookBtn.setText("管理戏法"); manageSpellSelectionBtn.setText("管理已知法术");
            sb.append("【邪术士施法资源】\n契约法术位: ").append(warlock.get_pact_slot_summary()).append("\n");
            if (warlock.mystic_arcanum_level > 0) sb.append("神秘秘法: 1 个 ").append(warlock.mystic_arcanum_level).append(" 环秘法\n");
            sb.append("戏法已知数: ").append(warlock.cantrips_known).append("\n法术已知数: ").append(warlock.spells_known_count).append("\n\n");
            appendListSection(sb, "已知戏法", warlock.get_known_cantrip_lines());
            appendListSection(sb, "已知法术", warlock.get_known_spell_lines());
        } else if (current.job instanceof Bard_Class) {
            Bard_Class bard = (Bard_Class) current.job;
            manageSpellbookBtn.setVisible(true); manageSpellSelectionBtn.setVisible(true);
            manageSpellbookBtn.setText("管理戏法"); manageSpellSelectionBtn.setText("管理已知法术");
            sb.append("【吟游诗人施法资源】\n").append(bard.get_spell_slot_summary()).append("\n吟游激励: ").append(bard.get_bardic_inspiration_summary()).append("\n");
            if (bard.song_of_rest_die_size > 0) sb.append("休憩之歌: d").append(bard.song_of_rest_die_size).append("\n");
            sb.append("戏法已知数: ").append(bard.cantrips_known).append("\n本职法术已知数: ").append(bard.base_spells_known_count).append("\n魔法秘辛法术数: ").append(bard.magical_secrets_count).append("\n\n");
            appendListSection(sb, "已知戏法", bard.get_known_cantrip_lines());
            appendListSection(sb, "已知法术", bard.get_known_spell_lines());
        } else if (current.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) current.job;
            int cm = current.stats.get_mod(current.stats.cha);
            if (current.job.current_level >= 2) { managePreparedSpellBtn.setVisible(true); managePreparedSpellBtn.setText("管理准备法术"); }
            sb.append("【圣武士施法资源】\n").append(paladin.get_spell_slot_summary()).append("\n圣疗池: ").append(paladin.get_lay_on_hands_summary()).append("\n");
            sb.append("神圣感知次数: ").append(paladin.get_divine_sense_summary(cm)).append("\n");
            if (current.job.current_level >= 14) sb.append("净化之触次数: ").append(paladin.get_cleansing_touch_summary(cm)).append("\n");
            sb.append("可准备法术数: ").append(paladin.get_prepared_spell_count(cm)).append("\n\n");
            appendListSection(sb, "准备法术", paladin.get_prepared_spell_lines());
        } else { sb.append("当前职业暂无专门的施法管理界面。"); }
        spellcastingArea.setText(sb.toString());
    }

    private void appendListSection(StringBuilder sb, String title, List<String> lines) {
        sb.append("【").append(title).append("】\n");
        if (lines.isEmpty()) sb.append("- 暂无\n");
        else for (String line : lines) sb.append("- ").append(line).append("\n");
    }

    // ==================== Tab3: 法术管理 ====================
    private void handleManageSpellbook() {
        if (current.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理法师戏法", "法师戏法已知数固定为 " + wizard.cantrips_known + "。", Spell_Library.get_wizard_cantrip_keys(), wizard.known_cantrip_keys, wizard.cantrips_known, wizard.cantrips_known);
            wizard.set_known_cantrips(selected); current.record_advancement("调整法师戏法，当前数量：" + wizard.known_cantrip_keys.size());
        } else if (current.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理术士戏法", "术士戏法已知上限：" + sorcerer.cantrips_known + "。", Spell_Library.get_sorcerer_cantrip_keys(), sorcerer.known_cantrip_keys, sorcerer.cantrips_known, sorcerer.cantrips_known);
            sorcerer.set_known_cantrips(selected); current.record_advancement("调整术士戏法，当前数量：" + sorcerer.known_cantrip_keys.size());
        } else if (current.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理邪术士戏法", "邪术士戏法已知上限：" + warlock.cantrips_known + "。", Spell_Library.get_warlock_cantrip_keys(), warlock.known_cantrip_keys, warlock.cantrips_known, warlock.cantrips_known);
            warlock.set_known_cantrips(selected); current.record_advancement("调整邪术士戏法，当前数量：" + warlock.known_cantrip_keys.size());
        } else if (current.job instanceof Bard_Class) {
            Bard_Class bard = (Bard_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理吟游诗人戏法", "吟游诗人戏法已知上限：" + bard.cantrips_known + "。", Spell_Library.get_bard_cantrip_keys(), bard.known_cantrip_keys, bard.cantrips_known, bard.cantrips_known);
            bard.set_known_cantrips(selected); current.record_advancement("调整吟游诗人戏法，当前数量：" + bard.known_cantrip_keys.size());
        }
        current.recalculate_derived_stats(); Character_DAO.update_character(current); refreshUI();
    }

    private void handleManageSpellSelection() {
        if (current.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理法术书", "法师法术书容量上限：" + wizard.spells_in_spellbook + "。", Spell_Library.get_wizard_spell_keys_up_to_level(wizard.get_max_spell_level()), wizard.spellbook_spell_keys, wizard.spells_in_spellbook);
            wizard.spellbook_spell_keys.clear(); wizard.spellbook_spell_keys.addAll(selected);
            current.record_advancement("整理法术书，当前记录法术数：" + wizard.spellbook_spell_keys.size());
        } else if (current.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理已知法术", "术士已知法术上限：" + sorcerer.spells_known_count + "。", Spell_Library.get_sorcerer_spell_keys_up_to_level(sorcerer.get_max_spell_level()), sorcerer.known_spell_keys, sorcerer.spells_known_count, sorcerer.spells_known_count);
            sorcerer.set_known_spells(selected); current.record_advancement("调整术士已知法术，当前数量：" + sorcerer.known_spell_keys.size());
        } else if (current.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理邪术士已知法术", "邪术士已知法术上限：" + warlock.spells_known_count + "。", Spell_Library.get_warlock_spell_keys_up_to_level(warlock.pact_slot_level), warlock.known_spell_keys, warlock.spells_known_count, warlock.spells_known_count);
            warlock.set_known_spells(selected); current.record_advancement("调整邪术士已知法术，当前数量：" + warlock.known_spell_keys.size());
        } else if (current.job instanceof Bard_Class) {
            Bard_Class bard = (Bard_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理吟游诗人已知法术", "吟游诗人本职已知法术上限：" + bard.base_spells_known_count + "。", Spell_Library.get_bard_spell_keys_up_to_level(bard.get_max_spell_level()), bard.known_spell_keys, bard.base_spells_known_count, bard.base_spells_known_count);
            bard.set_known_spells(selected); current.record_advancement("调整吟游诗人已知法术，当前本职法术数量：" + bard.known_spell_keys.size());
        }
        current.recalculate_derived_stats(); Character_DAO.update_character(current); refreshUI();
    }

    private void handleManagePreparedSpells() {
        if (current.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current.job;
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理准备法术", "你可准备 " + wizard.get_prepared_spell_count(current.stats.get_mod(current.stats.intel)) + " 个法术。", new ArrayList<>(wizard.spellbook_spell_keys), wizard.prepared_spell_keys, wizard.get_prepared_spell_count(current.stats.get_mod(current.stats.intel)));
            wizard.set_prepared_spells(selected, current.stats.get_mod(current.stats.intel));
            current.record_advancement("调整准备法术，当前准备数量：" + wizard.prepared_spell_keys.size());
        } else if (current.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) current.job;
            int preparedCount = paladin.get_prepared_spell_count(current.stats.get_mod(current.stats.cha));
            List<String> selected = Spell_Management_Helper.open_selection_dialog(null, "管理圣武士准备法术", "你可准备 " + preparedCount + " 个法术。", paladin.get_available_spell_options(), paladin.prepared_spell_keys, preparedCount);
            paladin.set_prepared_spells(selected, current.stats.get_mod(current.stats.cha));
            current.record_advancement("调整圣武士准备法术，当前数量：" + paladin.prepared_spell_keys.size());
        }
        current.recalculate_derived_stats(); Character_DAO.update_character(current); refreshUI();
    }

    // ==================== Tab4: 成长与升级面板刷新 ====================
    private void refreshProgressionPanel() {
        StringBuilder p = new StringBuilder();
        p.append("当前等级: ").append(current.job.current_level).append("\n当前经验值: ").append(current.experience_points).append("\n生命骰: ").append(current.get_hit_dice_summary()).append("\n");
        if (current.job.current_level < 20) { p.append("下一级所需经验值: ").append(current.get_next_level_xp()).append("\n距离升级还差: ").append(current.get_xp_to_next_level()).append("\n"); }
        else p.append("已达到最高等级。\n");
        p.append("\n【当前职业特性】\n"); List<String> features = current.job.get_feature_summaries();
        if (features.isEmpty()) p.append("- 暂无\n"); else for (String f : features) p.append("- ").append(f).append("\n");
        p.append("\n【待处理升级选择】\n"); List<String> pendingChoices = current.job.get_pending_choices();
        if (pendingChoices.isEmpty()) p.append("- 暂无\n"); else for (String pc : pendingChoices) p.append("- ").append(pc).append("\n");
        p.append("\n【升级记录】\n");
        if (current.advancement_notes.isEmpty()) p.append("- 暂无\n"); else for (String note : current.advancement_notes) p.append("- ").append(note).append("\n");
        if (current.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) current.job;
            p.append("\n【战士资源】\n复苏之风: ").append(fighter.get_second_wind_summary()).append("\n动作如潮: ").append(fighter.get_action_surge_summary()).append("\n不屈: ").append(fighter.get_indomitable_summary()).append("\n攻击次数: ").append(fighter.attacks_per_action).append("\n");
            if (fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.BATTLE_MASTER) p.append("卓越骰: ").append(fighter.get_superiority_dice_summary()).append("\n");
        } else if (current.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current.job;
            p.append("\n【法师资源】\n").append(wizard.get_spell_slot_summary()).append("\n奥术回能: ").append(wizard.get_arcane_recovery_status()).append("（额度 ").append(wizard.arcane_recovery_level).append("）\n");
        } else if (current.job instanceof Sorcerer_Class) {
            Sorcerer_Class sorcerer = (Sorcerer_Class) current.job;
            p.append("\n【术士资源】\n术法点: ").append(sorcerer.get_sorcery_point_summary()).append("\n").append(sorcerer.get_spell_slot_summary()).append("\n");
        } else if (current.job instanceof Warlock_Class) {
            Warlock_Class warlock = (Warlock_Class) current.job;
            p.append("\n【邪术士资源】\n契约法术位: ").append(warlock.get_pact_slot_summary()).append("\n");
            if (warlock.mystic_arcanum_level > 0) p.append("神秘秘法: 1 个 ").append(warlock.mystic_arcanum_level).append(" 环秘法\n");
        } else if (current.job instanceof Bard_Class) {
            Bard_Class bard = (Bard_Class) current.job;
            p.append("\n【吟游诗人资源】\n").append(bard.get_spell_slot_summary()).append("\n吟游激励: ").append(bard.get_bardic_inspiration_summary()).append("\n");
            if (bard.song_of_rest_die_size > 0) p.append("休憩之歌: d").append(bard.song_of_rest_die_size).append("\n");
            p.append("攻击次数: ").append(bard.attacks_per_action).append("\n");
        } else if (current.job instanceof Paladin_Class) {
            Paladin_Class paladin = (Paladin_Class) current.job; int cm = current.stats.get_mod(current.stats.cha);
            p.append("\n【圣武士资源】\n").append(paladin.get_spell_slot_summary()).append("\n圣疗池: ").append(paladin.get_lay_on_hands_summary()).append("\n神圣感知: ").append(paladin.get_divine_sense_summary(cm)).append("\n");
            if (current.job.current_level >= 14) p.append("净化之触: ").append(paladin.get_cleansing_touch_summary(cm)).append("\n");
            p.append("攻击次数: ").append(paladin.attacks_per_action).append("\n");
        }
        levelInfoArea.setText(p.toString());
    }

    // ==================== Tab4: 操作处理 ====================
    private void handleAddExperience() {
        TextInputDialog dialog = new TextInputDialog(); dialog.setTitle("添加经验值"); dialog.setHeaderText("请输入要增加的经验值："); dialog.initOwner(this);
        Optional<String> input = dialog.showAndWait(); if (!input.isPresent()) return;
        try { int xp = Integer.parseInt(input.get().trim()); if (xp <= 0) { showAlert("经验值必须是正整数。"); return; }
            current.add_experience(xp); Character_DAO.update_character(current); refreshUI();
        } catch (NumberFormatException ex) { showAlert("请输入合法的数字经验值。"); }
    }

    private void handleShortRest() {
        int missingHp = Math.max(0, current.hp - current.current_hp);
        Wizard_Class wizard = current.job instanceof Wizard_Class ? (Wizard_Class) current.job : null;
        Spinner<Integer> hitDiceSpinner = new Spinner<>(0, current.available_hit_dice, 0);
        Spinner<Integer> healSpinner = new Spinner<>(0, missingHp, 0);
        TextArea summaryArea = new TextArea("短休会恢复系统内可追踪的短休资源。\n" + buildShortRestResourceSummary()
                + "\n\n请填写消耗的生命骰数量和恢复的生命值总量。");
        summaryArea.setEditable(false); summaryArea.setWrapText(true);
        GridPane inputGrid = new GridPane(); inputGrid.setHgap(8); inputGrid.setVgap(6);
        inputGrid.add(new Label("消耗生命骰"), 0, 0); inputGrid.add(hitDiceSpinner, 1, 0); inputGrid.add(new Label("/ 最多 " + current.available_hit_dice), 2, 0);
        inputGrid.add(new Label("恢复生命值"), 0, 1); inputGrid.add(healSpinner, 1, 1); inputGrid.add(new Label("/ 最多 " + missingHp), 2, 1);
        VBox panel = new VBox(8, summaryArea, inputGrid); panel.setPadding(new Insets(12));
        Spinner<Integer>[] arcaneSpinners = new Spinner[6];
        if (wizard != null && wizard.can_use_arcane_recovery()) {
            GridPane arcanePanel = new GridPane(); arcanePanel.setHgap(8); arcanePanel.setVgap(6);
            arcanePanel.setStyle("-fx-border-color: #888; -fx-border-width: 1; -fx-padding: 8;");
            Label budgetLabel = new Label("剩余可恢复总环级：" + wizard.arcane_recovery_level);
            int row = 0;
            for (int sl = 1; sl <= Math.min(5, wizard.get_max_spell_level()); sl++) {
                int missing = wizard.get_missing_spell_slots_at_level(sl);
                if (missing <= 0) continue;
                arcaneSpinners[sl] = new Spinner<>(0, missing, 0);
                arcanePanel.add(new Label(sl + "环法术位"), 0, row); arcanePanel.add(arcaneSpinners[sl], 1, row); arcanePanel.add(new Label("/ 缺失 " + missing), 2, row);
                final int spellLevel = sl;
                arcaneSpinners[sl].valueProperty().addListener(obs -> {
                    int used = 0; for (int l = 1; l <= 5; l++) if (arcaneSpinners[l] != null) used += arcaneSpinners[l].getValue() * l;
                    int remaining = wizard.arcane_recovery_level - used;
                    budgetLabel.setText("剩余可恢复总环级：" + remaining);
                    budgetLabel.setStyle(remaining < 0 ? "-fx-text-fill: red;" : "-fx-text-fill: black;");
                });
                row++;
            }
            arcanePanel.add(budgetLabel, 0, row, 3, 1);
            panel.getChildren().add(arcanePanel);
        }
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("进行短休"); dialog.initOwner(this);
        dialog.getDialogPane().setContent(panel); dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        int hitDiceUsed = Math.max(0, hitDiceSpinner.getValue());
        int healAmount = Math.max(0, healSpinner.getValue());
        current.available_hit_dice = Math.max(0, current.available_hit_dice - hitDiceUsed);
        if (healAmount > 0) { int beforeHp = current.current_hp; current.set_current_hp(current.current_hp + healAmount); current.record_advancement("短休消耗 " + hitDiceUsed + " 个生命骰，恢复生命值 " + (current.current_hp - beforeHp)); }
        if (wizard != null && wizard.can_use_arcane_recovery()) {
            int[] recoveryByLevel = new int[6];
            for (int l = 1; l <= 5; l++) { if (arcaneSpinners[l] != null && arcaneSpinners[l].getValue() > 0) { recoveryByLevel[l] = arcaneSpinners[l].getValue(); } }
            boolean recovered = wizard.apply_arcane_recovery(recoveryByLevel);
            if (recovered) {
                StringBuilder arcLog = new StringBuilder("奥术回能恢复：");
                for (int l = 1; l <= 5; l++) { if (recoveryByLevel[l] > 0) { arcLog.append(" ").append(l).append("环x").append(recoveryByLevel[l]); } }
                current.record_advancement(arcLog.toString());
            }
        }
        current.job.restore_short_rest_resources();
        Character_DAO.update_character(current); refreshUI();
        showAlert("短休完成。\n消耗生命骰：" + hitDiceUsed + "\n恢复生命值：" + healAmount + "\n当前 HP：" + current.current_hp + "/" + current.hp);
    }

    private String buildShortRestResourceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用生命骰: ").append(current.available_hit_dice).append("\n");
        if (current.job instanceof Fighter_Class) {
            Fighter_Class fighter = (Fighter_Class) current.job;
            sb.append("动作如潮: ").append(fighter.get_action_surge_summary()).append("\n");
            if (fighter.fighter_subclass == com.DMHelper.basic.playerclass.Fighter.Fighter_Subclass.BATTLE_MASTER)
                sb.append("卓越骰: ").append(fighter.get_superiority_dice_summary()).append("\n");
        }
        if (current.job instanceof Bard_Class) sb.append("吟游激励: ").append(((Bard_Class) current.job).get_bardic_inspiration_summary()).append("\n");
        if (current.job instanceof Wizard_Class) {
            Wizard_Class wizard = (Wizard_Class) current.job;
            sb.append("奥术回能: ").append(wizard.can_use_arcane_recovery() ? "可用（额度 " + wizard.arcane_recovery_level + " 环级）" : "本次已使用").append("\n");
        }
        return sb.toString();
    }

    private void handleLongRest() {
        int beforeHp = current.current_hp;
        current.set_current_hp(current.hp);
        current.available_hit_dice = Math.min(current.job.current_level, current.available_hit_dice + Math.max(1, current.job.current_level / 2));
        current.job.restore_long_rest_resources();
        Character_DAO.update_character(current); refreshUI();
        showAlert("长休完成。\n生命值从 " + beforeHp + " 恢复到 " + current.current_hp + "/" + current.hp + "\n生命骰恢复至 " + current.available_hit_dice + "\n所有法术位和短休资源已恢复。");
    }

    private void handleUseSecondWind() {
        if (!(current.job instanceof Fighter_Class)) return;
        Fighter_Class fighter = (Fighter_Class) current.job;
        if (fighter.current_second_wind_uses <= 0) { showAlert("复苏之风次数已用尽。"); return; }
        int healAmount = Dice_Util.roll_dice(1, 10) + current.job.current_level;
        int beforeHp = current.current_hp;
        current.set_current_hp(current.current_hp + healAmount);
        fighter.current_second_wind_uses--;
        current.record_advancement("使用复苏之风，恢复生命值 " + (current.current_hp - beforeHp) + "（1d10+" + current.job.current_level + "=" + healAmount + "）");
        Character_DAO.update_character(current); refreshUI();
        showAlert(current.name + " 使用了复苏之风，生命值从 " + beforeHp + " 提升到 " + current.current_hp + "/" + current.hp + "。\n剩余次数：" + fighter.current_second_wind_uses);
    }

    private void handleLevelUp() {
        if (!current.can_level_up()) { showAlert("经验不足，暂不可升级。"); return; }
        Character_Advancement_Helper.process_pending_choices(null, current);
        Character_DAO.update_character(current); refreshUI();
        showAlert("升级流程已执行。当前等级：" + current.job.current_level + "，经验值：" + current.experience_points);
    }

    // ==================== 保存与导出 ====================
    private void handleSave() {
        if (current == null) { showAlert("没有选中的角色。"); return; }
        Character_DAO.update_character(current);
        showAlert("角色 [" + current.name + "] 已保存。");
    }

    private void handleExportCharacterPdf() {
        if (current == null) { showAlert("没有选中的角色。"); return; }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出角色卡 PDF");
        fileChooser.setInitialFileName(current.name + "_角色卡.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF 文件", "*.pdf"));
        File file = fileChooser.showSaveDialog(this);
        if (file == null) return;
        try {
            Character_Card_PDF.generate(current, file.getAbsolutePath());
            showAlert("角色卡已导出到：" + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert("导出失败：" + ex.getMessage());
        }
    }

    // ==================== 角色选择器 ====================
    private void reloadSelectorItems(Character_Sheet currentChar) {
        isReloadingSelector = true;
        charSelector.getItems().clear();
        for (Character_Sheet cs : Global_Data.character_pool) {
            charSelector.getItems().add(cs.name + " (LV." + cs.job.current_level + " " + cs.job.class_name + ")");
        }
        if (currentChar != null) {
            for (int i = 0; i < Global_Data.character_pool.size(); i++) {
                if (Global_Data.character_pool.get(i) == currentChar) {
                    charSelector.getSelectionModel().select(i); break;
                }
            }
        }
        isReloadingSelector = false;
    }

    // ==================== 战斗辅助 ====================
    private Combat_Engine getActiveCombatEngine() {
        Combat_System_UI combatUI = Combat_System_UI.get_active_instance();
        return combatUI != null ? combatUI.get_current_combat_engine() : null;
    }

    private List<CombatantChoice> getLivingCombatantChoices(Combat_Engine engine) {
        return getLivingCombatantChoices(engine, null);
    }

    private List<CombatantChoice> getLivingCombatantChoices(Combat_Engine engine, Combatant.Side sideFilter) {
        List<CombatantChoice> choices = new ArrayList<>();
        for (Combatant c : engine.get_initiative_order()) {
            if (!c.is_alive()) continue;
            if (sideFilter != null && c.side != sideFilter) continue;
            choices.add(new CombatantChoice(c));
        }
        return choices;
    }

    private boolean removeStatusFromCombatant(Combatant combatant, Combat_Status_Type statusType) {
        Combat_Status_Effect toRemove = null;
        for (Combat_Status_Effect effect : combatant.status_effects) {
            if (effect.type == statusType) { toRemove = effect; break; }
        }
        if (toRemove != null) { combatant.status_effects.remove(toRemove); return true; }
        return false;
    }

    private List<String> clearStatusesFromCombatant(Combatant combatant) {
        List<String> removed = new ArrayList<>();
        for (Combat_Status_Effect effect : new ArrayList<>(combatant.status_effects)) {
            if (effect.type == Combat_Status_Type.SHIELDED || effect.type == Combat_Status_Type.INVISIBLE
                    || effect.type == Combat_Status_Type.CURSED || effect.type == Combat_Status_Type.SLOWED
                    || effect.type == Combat_Status_Type.RESTRAINED || effect.type == Combat_Status_Type.PARALYZED
                    || effect.type == Combat_Status_Type.ASLEEP || effect.type == Combat_Status_Type.POISONED
                    || effect.type == Combat_Status_Type.PRONE || effect.type == Combat_Status_Type.BURNING
                    || effect.type == Combat_Status_Type.CHARMED || effect.type == Combat_Status_Type.FRIGHTENED) {
                combatant.status_effects.remove(effect);
                removed.add(effect.type.label);
            }
        }
        return removed;
    }

    // ==================== 通用对话框 ====================
    private Optional<ButtonType> showCustomDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title); dialog.initOwner(this);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.initOwner(this);
        alert.showAndWait();
    }

    // ==================== 内部类：战斗单位选择包装 ====================
    private static class CombatantChoice {
        final Combatant combatant;
        CombatantChoice(Combatant combatant) { this.combatant = combatant; }
        @Override public String toString() { return combatant.display_name + " (HP: " + combatant.current_hp + "/" + combatant.max_hp + ")"; }
    }
}
