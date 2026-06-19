package com.DMHelper.fx;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.menus.Character_Advancement_Helper;
import com.DMHelper.basic.playerclass.Character_Class;
import com.DMHelper.basic.playerclass.Fighter.Fighter_Class;
import com.DMHelper.basic.playerclass.bard.Bard_Class;
import com.DMHelper.basic.playerclass.paladin.Paladin_Class;
import com.DMHelper.basic.playerclass.sorcerer.Sorcerer_Class;
import com.DMHelper.basic.playerclass.warlock.Warlock_Class;
import com.DMHelper.basic.playerclass.wizard.Wizard_Class;
import com.DMHelper.basic.race.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.*;

/**
 * JavaFX 版角色创建窗口。
 * <p>
 * 功能与 Swing 版 Create_Character_UI 完全一致：
 * <ul>
 *   <li>基础信息表单（姓名、年龄、性别、种族、职业）</li>
 *   <li>六维属性（STR/DEX/CON/INT/WIS/CHA），范围 1-20，默认 10</li>
 *   <li>种族子类型选择（精灵/矮人/半身人/龙裔/侏儒/半精灵）</li>
 *   <li>角色背景与性格——使用第二个 Stage/Dialog</li>
 *   <li>创建后调用 Character_Advancement_Helper.configure_new_character()</li>
 *   <li>保存到 Global_Data.character_pool 和 Character_DAO</li>
 *   <li>创建成功后弹出摘要并关闭</li>
 * </ul>
 */
public class CharacterCreateWindow extends Stage {

    /* ==================== 基础信息控件 ==================== */

    /** 角色姓名输入框 */
    private final TextField nameField = new TextField();

    /** 角色年龄微调器，范围 1-1000，默认 20（与 Swing 版一致） */
    private final Spinner<Integer> ageSpinner = new Spinner<>(1, 1000, 20);

    /** 角色性别下拉框 */
    private final ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList(
            "男", "女", "无性别", "其他"
    ));

    /** 种族下拉框，包含 9 个可选种族 */
    private final ComboBox<String> raceBox = new ComboBox<>(FXCollections.observableArrayList(
            "人类 (Human)", "精灵 (Elf)", "矮人 (Dwarf)", "半身人 (Halfling)",
            "龙裔 (Dragonborn)", "侏儒 (Gnome)", "半精灵 (Half-Elf)",
            "半兽人 (Half-Orc)", "提夫林 (Tiefling)"
    ));

    /** 职业下拉框，包含 6 个可选职业 */
    private final ComboBox<String> classBox = new ComboBox<>(FXCollections.observableArrayList(
            "战士 (Fighter)", "法师 (Wizard)", "术士 (Sorcerer)",
            "邪术士 (Warlock)", "圣武士 (Paladin)", "吟游诗人 (Bard)"
    ));

    /* ==================== 六维属性控件 ==================== */

    /** 六维属性 Spinner 映射，key 为属性英文缩写 */
    private final Map<String, Spinner<Integer>> abilitySpinners = new LinkedHashMap<>();

    /* ==================== 构造方法 ==================== */

    /**
     * 创建角色创建窗口。
     *
     * @param owner 父窗口，用于模态定位；可为 null
     */
    public CharacterCreateWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("创建新角色");
        setScene(buildScene());
        sizeToScene();
        setMinWidth(900);
        setMinHeight(640);
    }

    /* ==================== 界面构建 ==================== */

    /**
     * 构建主场景：BorderPane 布局，包含标题区、滚动表单区和底部按钮区。
     */
    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-shell");
        root.setPadding(new Insets(32));

        // ---- 顶部标题区 ----
        VBox header = new VBox(6);
        Label titleLabel = new Label("创建新角色");
        titleLabel.getStyleClass().add("dialog-title");
        Label subtitleLabel = new Label("填写基础信息与六维属性，然后继续完善背景与性格。");
        subtitleLabel.getStyleClass().add("dialog-subtitle");
        header.getChildren().addAll(titleLabel, subtitleLabel);
        root.setTop(header);

        // ---- 中间滚动内容区 ----
        VBox centerContent = new VBox(18);

        // 基础信息卡片
        centerContent.getChildren().add(buildBaseInfoSection());

        // 属性值卡片
        centerContent.getChildren().add(buildAbilitySection());

        ScrollPane scrollPane = new ScrollPane(centerContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        root.setCenter(scrollPane);

        // ---- 底部按钮区 ----
        Button submitButton = new Button("生成角色面板");
        submitButton.getStyleClass().add("primary-button");
        submitButton.setOnAction(evt -> handleCreate());

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getChildren().add(submitButton);
        root.setBottom(footer);

        Scene scene = new Scene(root, 980, 720);
        FxThemes.apply(scene);
        return scene;
    }

    /**
     * 构建基础信息区：姓名、年龄、性别、种族、职业，使用 GridPane 两列布局。
     */
    private VBox buildBaseInfoSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card");
        section.getChildren().add(buildSectionTitle("基础信息"));

        GridPane form = new GridPane();
        form.setHgap(18);
        form.setVgap(12);
        form.getColumnConstraints().addAll(
                new ColumnConstraints(), new ColumnConstraints(),
                new ColumnConstraints(), new ColumnConstraints()
        );
        // 让输入列自动拉伸
        ColumnConstraints stretchCol = new ColumnConstraints();
        stretchCol.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().set(1, stretchCol);
        form.getColumnConstraints().set(3, stretchCol);

        // 第 0 行：姓名 + 年龄
        form.add(buildLabel("角色姓名"), 0, 0);
        nameField.setPromptText("输入角色名称");
        form.add(nameField, 1, 0);
        form.add(buildLabel("角色年龄"), 2, 0);
        ageSpinner.setEditable(true);
        ageSpinner.setPrefWidth(140);
        form.add(ageSpinner, 3, 0);

        // 第 1 行：性别 + 种族
        form.add(buildLabel("角色性别"), 0, 1);
        genderBox.getSelectionModel().selectFirst();
        form.add(genderBox, 1, 1);
        form.add(buildLabel("选择种族"), 2, 1);
        raceBox.getSelectionModel().selectFirst();
        form.add(raceBox, 3, 1);

        // 第 2 行：职业（跨两列）
        form.add(buildLabel("选择职业"), 0, 2);
        classBox.getSelectionModel().selectFirst();
        GridPane.setColumnSpan(classBox, 3);
        form.add(classBox, 1, 2);

        section.getChildren().add(form);
        return section;
    }

    /**
     * 构建六维属性区：STR/DEX/CON/INT/WIS/CHA，范围 1-20，默认 10。
     * 使用 3 列 x 2 行网格布局。
     */
    private VBox buildAbilitySection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card");
        section.getChildren().add(buildSectionTitle("属性值"));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(10);

        // 六维属性定义：中文标签 + 英文键
        String[][] abilities = {
                {"力量 (STR)", "STR"},
                {"敏捷 (DEX)", "DEX"},
                {"体质 (CON)", "CON"},
                {"智力 (INT)", "INT"},
                {"感知 (WIS)", "WIS"},
                {"魅力 (CHA)", "CHA"}
        };

        for (int i = 0; i < abilities.length; i++) {
            String label = abilities[i][0];
            String key = abilities[i][1];
            int col = i % 3;
            int row = i / 3;

            Spinner<Integer> spinner = new Spinner<>(1, 20, 10);
            spinner.setEditable(true);
            spinner.setPrefWidth(100);
            abilitySpinners.put(key, spinner);

            grid.add(buildLabel(label), col * 2, row);
            grid.add(spinner, col * 2 + 1, row);
        }

        section.getChildren().add(grid);
        return section;
    }

    /* ==================== 创建角色主流程 ==================== */

    /**
     * 处理"生成角色面板"按钮点击事件。
     * <p>
     * 完整流程（与 Swing 版 build_character 一致）：
     * <ol>
     *   <li>校验姓名非空</li>
     *   <li>构建种族对象（含子种族选择对话框）</li>
     *   <li>构建职业对象</li>
     *   <li>创建 Character_Sheet</li>
     *   <li>打开背景与性格对话框（第二个 Stage）</li>
     *   <li>调用 Character_Advancement_Helper.configure_new_character()</li>
     *   <li>保存到 Global_Data.character_pool 和 Character_DAO</li>
     *   <li>弹出角色摘要对话框</li>
     *   <li>关闭本窗口</li>
     * </ol>
     */
    private void handleCreate() {
        // 1. 校验姓名
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showWarning("角色姓名不能为空！");
            return;
        }

        // 2. 解析种族（含子种族选择）
        Character_Race race = resolveRaceSelection();
        if (race == null) {
            showWarning("请选择一个有效的种族。");
            return;
        }

        // 3. 解析职业
        Character_Class characterClass = resolveClassSelection();
        if (characterClass == null) {
            showWarning("请选择一个有效的职业。");
            return;
        }

        // 4. 构建六维属性
        Stats rawStats = new Stats(
                abilitySpinners.get("STR").getValue(),
                abilitySpinners.get("DEX").getValue(),
                abilitySpinners.get("CON").getValue(),
                abilitySpinners.get("INT").getValue(),
                abilitySpinners.get("WIS").getValue(),
                abilitySpinners.get("CHA").getValue()
        );

        // 5. 创建角色卡
        int age = ageSpinner.getValue();
        String gender = genderBox.getValue();
        Character_Sheet newCharacter = Character_Sheet.create_new_character(
                name, age, gender, race, characterClass, rawStats
        );

        // 6. 打开背景与性格对话框（第二个 Stage，模态）
        Map<String, String> profileInputs = openProfileDialog();
        if (profileInputs == null) {
            // 用户点击了取消，中止创建
            return;
        }
        newCharacter.background_story = profileInputs.get("background_story");
        newCharacter.personality_traits = profileInputs.get("personality_traits");
        newCharacter.ideals = profileInputs.get("ideals");
        newCharacter.bonds = profileInputs.get("bonds");
        newCharacter.flaws = profileInputs.get("flaws");

        // 7. 调用职业起始配置（技能选择、子职业、法术等）
        //    传入 null 作为 Component 参数，因为 JavaFX 没有 AWT Component
        Character_Advancement_Helper.configure_new_character(null, newCharacter);

        // 8. 保存角色
        Global_Data.character_pool.add(newCharacter);
        Character_DAO.save_character(newCharacter);

        // 9. 弹出角色摘要
        showCharacterSummary(newCharacter);

        // 10. 关闭本窗口
        close();
    }

    /* ==================== 背景与性格对话框 ==================== */

    /**
     * 打开"最后一步：角色背景与性格"模态对话框。
     * <p>
     * 与 Swing 版 open_profile_dialog 功能一致，包含五个文本区域：
     * 背景故事、性格特点、理想信念、羁绊关系、缺陷弱点。
     *
     * @return 用户填写的字段映射；若用户取消则返回 null
     */
    private Map<String, String> openProfileDialog() {
        // 使用原子引用保存结果
        final Map<String, String>[] resultHolder = new Map[]{null};

        Stage dialog = new Stage();
        dialog.initOwner(this);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("最后一步：角色背景与性格");
        dialog.setMinWidth(600);
        dialog.setMinHeight(560);

        BorderPane dialogRoot = new BorderPane();
        dialogRoot.setPadding(new Insets(16));

        // ---- 标题 ----
        Label dialogTitle = new Label("角色背景与性格");
        dialogTitle.getStyleClass().add("dialog-title");
        dialogRoot.setTop(dialogTitle);

        // ---- 五个文本区域 ----
        VBox content = new VBox(14);

        TextArea backgroundArea = buildProfileArea("背景故事：可以写出身、经历、职业道路。");
        TextArea personalityArea = buildProfileArea("性格特点：比如冷静、暴躁、乐观、谨慎。");
        TextArea idealsArea = buildProfileArea("理想信念：角色最在意的原则、追求与目标。");
        TextArea bondsArea = buildProfileArea("羁绊关系：重要的人、组织、承诺或牵挂。");
        TextArea flawsArea = buildProfileArea("缺陷弱点：恐惧、偏执、贪念、固执等等。");

        content.getChildren().add(wrapProfileField("背景故事", backgroundArea));
        content.getChildren().add(wrapProfileField("性格特点", personalityArea));
        content.getChildren().add(wrapProfileField("理想信念", idealsArea));
        content.getChildren().add(wrapProfileField("羁绊关系", bondsArea));
        content.getChildren().add(wrapProfileField("缺陷弱点", flawsArea));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("content-scroll");
        dialogRoot.setCenter(scrollPane);

        // ---- 底部按钮 ----
        Button cancelButton = new Button("取消");
        cancelButton.getStyleClass().add("ghost-button");
        cancelButton.setOnAction(evt -> dialog.close());

        Button confirmButton = new Button("完成创建");
        confirmButton.getStyleClass().add("primary-button");
        confirmButton.setOnAction(evt -> {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("background_story", backgroundArea.getText().trim());
            values.put("personality_traits", personalityArea.getText().trim());
            values.put("ideals", idealsArea.getText().trim());
            values.put("bonds", bondsArea.getText().trim());
            values.put("flaws", flawsArea.getText().trim());
            resultHolder[0] = values;
            dialog.close();
        });

        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.getChildren().addAll(cancelButton, confirmButton);
        dialogRoot.setBottom(buttonBar);

        Scene dialogScene = new Scene(dialogRoot, 640, 620);
        FxThemes.apply(dialogScene);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return resultHolder[0];
    }

    /**
     * 创建带提示文本的 TextArea，用于背景与性格输入。
     *
     * @param placeholder 提示文本
     * @return 配置好的 TextArea
     */
    private static TextArea buildProfileArea(String placeholder) {
        TextArea area = new TextArea();
        area.setPromptText(placeholder);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.getStyleClass().add("solid-field");
        return area;
    }

    /**
     * 将 TextArea 包装为带标题的 VBox，模拟 Swing 版的 wrap_profile_field。
     *
     * @param title 字段标题
     * @param area  文本区域
     * @return 包装后的 VBox
     */
    private VBox wrapProfileField(String title, TextArea area) {
        VBox box = new VBox(6);
        box.getStyleClass().add("card");
        Label label = buildSectionTitle(title);
        box.getChildren().addAll(label, area);
        return box;
    }

    /* ==================== 角色摘要弹窗 ==================== */

    /**
     * 弹出角色创建成功的摘要对话框。
     * <p>
     * 与 Swing 版 Character_Summary_UI 的信息展示一致，
     * 使用 JavaFX Alert 展示角色关键数据。
     *
     * @param character 已创建的角色
     */
    private void showCharacterSummary(Character_Sheet character) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 基础信息 ===\n");
        sb.append("姓名: ").append(character.name).append("\n");
        sb.append("年龄/性别: ").append(character.age).append(" / ").append(character.gender).append("\n");
        sb.append("种族: ").append(character.race.race_name).append("\n");
        sb.append("职业: ").append(character.job.class_name)
                .append(" (等级 ").append(character.job.current_level).append(")\n");
        sb.append("子职业: ").append(character.job.get_subclass_name()).append("\n\n");

        sb.append("=== 战斗属性 ===\n");
        sb.append("生命值 (HP): ").append(character.get_hp_summary()).append("\n");
        sb.append("护甲等级 (AC): ").append(character.ac).append("\n");
        sb.append("熟练加值 (PB): +").append(character.get_proficiency_bonus()).append("\n\n");

        sb.append("=== 六维数据 ===\n");
        sb.append(String.format("力量: %d (调整值: %+d)\n",
                character.stats.str, character.stats.get_mod(character.stats.str)));
        sb.append(String.format("敏捷: %d (调整值: %+d)\n",
                character.stats.dex, character.stats.get_mod(character.stats.dex)));
        sb.append(String.format("体质: %d (调整值: %+d)\n",
                character.stats.con, character.stats.get_mod(character.stats.con)));
        sb.append(String.format("智力: %d (调整值: %+d)\n",
                character.stats.intel, character.stats.get_mod(character.stats.intel)));
        sb.append(String.format("感知: %d (调整值: %+d)\n",
                character.stats.wis, character.stats.get_mod(character.stats.wis)));
        sb.append(String.format("魅力: %d (调整值: %+d)\n",
                character.stats.cha, character.stats.get_mod(character.stats.cha)));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(this);
        alert.setTitle("角色创建成功");
        alert.setHeaderText(character.name + " 已成功创建！");
        alert.getDialogPane().setPrefWidth(560);
        alert.getDialogPane().setPrefHeight(480);

        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("solid-field");
        alert.getDialogPane().setContent(textArea);

        alert.showAndWait();
    }

    /* ==================== 种族解析 ==================== */

    /**
     * 根据下拉框选择构建对应的种族对象。
     * <p>
     * 对于有子种族的种族，会弹出 ChoiceDialog 让用户选择子类型。
     * 半精灵需要额外选择两项自选属性加值。
     *
     * @return 种族对象；若选择无效则返回 null
     */
    private Character_Race resolveRaceSelection() {
        String choice = raceBox.getValue();
        if (choice == null) {
            return null;
        }

        switch (choice) {
            case "精灵 (Elf)": {
                // 精灵有 3 种子种族：高等精灵、木精灵、卓尔精灵
                String selected = promptOption(
                        "选择精灵子种族",
                        "请选择精灵子种族：",
                        Arrays.asList("HIGH", "WOOD", "DROW"),
                        Arrays.asList("高等精灵 (High Elf)", "木精灵 (Wood Elf)", "卓尔精灵 (Drow)")
                );
                return new Elf_Race(selected);
            }
            case "矮人 (Dwarf)": {
                // 矮人有 2 种子种族：丘陵矮人、山地矮人
                String selected = promptOption(
                        "选择矮人子种族",
                        "请选择矮人子种族：",
                        Arrays.asList("HILL", "MOUNTAIN"),
                        Arrays.asList("丘陵矮人 (Hill Dwarf)", "山地矮人 (Mountain Dwarf)")
                );
                return new Dwarf_Race(selected);
            }
            case "半身人 (Halfling)": {
                // 半身人有 2 种子种族：轻足半身人、健壮半身人
                String selected = promptOption(
                        "选择半身人子种族",
                        "请选择半身人子种族：",
                        Arrays.asList("LIGHTFOOT", "STOUT"),
                        Arrays.asList("轻足半身人 (Lightfoot Halfling)", "健壮半身人 (Stout Halfling)")
                );
                return new Halfling_Race(selected);
            }
            case "龙裔 (Dragonborn)": {
                // 龙裔有 10 种血脉类型
                String selected = promptOption(
                        "选择龙裔血脉",
                        "请选择龙裔的龙族先祖：",
                        Arrays.asList("BLACK", "BLUE", "BRASS", "BRONZE", "COPPER",
                                "GOLD", "GREEN", "RED", "SILVER", "WHITE"),
                        Arrays.asList(
                                "黑龙 (Black) - 强酸", "蓝龙 (Blue) - 闪电",
                                "黄铜龙 (Brass) - 火焰", "青铜龙 (Bronze) - 闪电",
                                "赤铜龙 (Copper) - 强酸", "金龙 (Gold) - 火焰",
                                "绿龙 (Green) - 毒素", "红龙 (Red) - 火焰",
                                "银龙 (Silver) - 寒冷", "白龙 (White) - 寒冷"
                        )
                );
                return new Dragonborn_Race(selected);
            }
            case "侏儒 (Gnome)": {
                // 侏儒有 2 种子种族：森林侏儒、岩侏儒
                String selected = promptOption(
                        "选择侏儒子种族",
                        "请选择侏儒子种族：",
                        Arrays.asList("FOREST", "ROCK"),
                        Arrays.asList("森林侏儒 (Forest Gnome)", "岩侏儒 (Rock Gnome)")
                );
                return new Gnome_Race(selected);
            }
            case "半精灵 (Half-Elf)": {
                // 半精灵需要选择两项额外 +1 属性加值（不可重复，且不包含魅力）
                // 与 Swing 版一致：选项为力量、敏捷、体质、智力、感知（不含魅力）
                String firstLabel = promptOption(
                        "选择半精灵属性加值",
                        "请选择半精灵额外 +1 的第一项属性：",
                        Arrays.asList("STR", "DEX", "CON", "INT", "WIS"),
                        Arrays.asList("力量", "敏捷", "体质", "智力", "感知")
                );
                // 构建剩余选项（排除已选的第一项）
                List<String> remainingKeys = new ArrayList<>(Arrays.asList("STR", "DEX", "CON", "INT", "WIS"));
                remainingKeys.remove(firstLabel);
                List<String> remainingLabels = new ArrayList<>();
                for (String key : remainingKeys) {
                    remainingLabels.add(toHalfElfLabel(key));
                }
                String secondLabel = promptOption(
                        "选择半精灵属性加值",
                        "请选择半精灵额外 +1 的第二项属性：",
                        remainingKeys,
                        remainingLabels
                );
                return new Half_Elf_Race(firstLabel, secondLabel);
            }
            case "半兽人 (Half-Orc)":
                return new Half_Orc_Race();
            case "提夫林 (Tiefling)":
                return new Tiefling_Race();
            case "人类 (Human)":
            default:
                return new Human_Race();
        }
    }

    /* ==================== 职业解析 ==================== */

    /**
     * 根据下拉框选择构建对应的职业对象。
     *
     * @return 职业对象；若选择无效则返回 null
     */
    private Character_Class resolveClassSelection() {
        String choice = classBox.getValue();
        if (choice == null) {
            return null;
        }
        switch (choice) {
            case "战士 (Fighter)":
                return new Fighter_Class();
            case "法师 (Wizard)":
                return new Wizard_Class();
            case "术士 (Sorcerer)":
                return new Sorcerer_Class();
            case "邪术士 (Warlock)":
                return new Warlock_Class();
            case "圣武士 (Paladin)":
                return new Paladin_Class();
            case "吟游诗人 (Bard)":
                return new Bard_Class();
            default:
                return new Fighter_Class();
        }
    }

    /* ==================== 通用选择对话框 ==================== */

    /**
     * 弹出单选对话框，让用户从一组选项中选择一个。
     * <p>
     * 使用 JavaFX ChoiceDialog 实现，与 Swing 版 JOptionPane.showInputDialog 功能一致。
     *
     * @param title   对话框标题
     * @param content 对话框提示内容
     * @param values  内部值列表（如 "HIGH", "WOOD"）
     * @param labels  显示标签列表（如 "高等精灵", "木精灵"）
     * @return 用户选择的内部值；若取消则返回第一个值
     */
    private String promptOption(String title, String content, List<String> values, List<String> labels) {
        List<String> displayList = (labels != null) ? labels : values;
        ChoiceDialog<String> dialog = new ChoiceDialog<>(displayList.get(0), displayList);
        dialog.initOwner(this);
        dialog.setTitle(title);
        dialog.setHeaderText(content);
        dialog.setContentText(null);

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            // 用户取消，默认返回第一个值
            return values.get(0);
        }

        String selectedLabel = result.get();
        int idx = displayList.indexOf(selectedLabel);
        return values.get(Math.max(idx, 0));
    }

    /* ==================== 辅助工具方法 ==================== */

    /**
     * 创建带 CSS 样式的表单标签。
     *
     * @param text 标签文本
     * @return 配置好的 Label
     */
    private Label buildLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    /**
     * 创建带 CSS 样式的分区标题。
     *
     * @param text 标题文本
     * @return 配置好的 Label
     */
    private Label buildSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /**
     * 显示警告提示框。
     *
     * @param message 警告内容
     */
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(this);
        alert.setTitle("警告");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 将半精灵属性键转换为中文标签。
     *
     * @param key 属性英文缩写
     * @return 中文标签
     */
    private static String toHalfElfLabel(String key) {
        switch (key) {
            case "STR": return "力量";
            case "DEX": return "敏捷";
            case "CON": return "体质";
            case "INT": return "智力";
            case "WIS": return "感知";
            case "CHA": return "魅力";
            default:    return key;
        }
    }
}
