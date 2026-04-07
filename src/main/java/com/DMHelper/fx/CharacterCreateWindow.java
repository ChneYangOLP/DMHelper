package com.DMHelper.fx;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.Stats;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Global_Data;
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

public class CharacterCreateWindow extends Stage {

    private final TextField nameField = new TextField();
    private final Spinner<Integer> ageSpinner = new Spinner<>(1, 500, 20);
    private final ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("男", "女", "无性别", "其他"));
    private final ComboBox<String> raceBox = new ComboBox<>(FXCollections.observableArrayList(
            "人类 (Human)", "精灵 (Elf)", "矮人 (Dwarf)", "半身人 (Halfling)",
            "龙裔 (Dragonborn)", "侏儒 (Gnome)", "半精灵 (Half-Elf)",
            "半兽人 (Half-Orc)", "提夫林 (Tiefling)"
    ));
    private final ComboBox<String> classBox = new ComboBox<>(FXCollections.observableArrayList(
            "战士 (Fighter)", "法师 (Wizard)", "术士 (Sorcerer)",
            "邪术士 (Warlock)", "圣武士 (Paladin)", "吟游诗人 (Bard)"
    ));

    private final Map<String, Spinner<Integer>> abilitySpinners = new LinkedHashMap<>();
    private final TextArea backgroundArea = buildMultiLineArea("背景故事、出身、主线动机……");
    private final TextArea personalityArea = buildMultiLineArea("性格特点、语气、常态行为……");
    private final TextArea idealsArea = buildMultiLineArea("理想、信念、价值观……");
    private final TextArea bondsArea = buildMultiLineArea("羁绊、承诺、重要人物或组织……");
    private final TextArea flawsArea = buildMultiLineArea("缺陷、恐惧、弱点……");

    public CharacterCreateWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("创建角色 · JavaFX 版本");
        setScene(buildScene());
        sizeToScene();
        setMinWidth(900);
        setMinHeight(640);
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-shell");
        root.setPadding(new Insets(32));

        VBox header = new VBox(6);
        Label title = new Label("创建新角色");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("填写基础信息、六维属性与背景设定，保存后即可出现在角色列表中。");
        subtitle.getStyleClass().add("dialog-subtitle");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.setHgap(18);
        form.setVgap(12);
        form.add(buildLabel("角色姓名"), 0, 0);
        form.add(nameField, 1, 0);
        form.add(buildLabel("角色年龄"), 2, 0);
        ageSpinner.setEditable(true);
        form.add(ageSpinner, 3, 0);

        form.add(buildLabel("角色性别"), 0, 1);
        genderBox.getSelectionModel().selectFirst();
        form.add(genderBox, 1, 1);
        form.add(buildLabel("种族"), 2, 1);
        raceBox.getSelectionModel().selectFirst();
        form.add(raceBox, 3, 1);

        form.add(buildLabel("职业"), 0, 2);
        classBox.getSelectionModel().selectFirst();
        form.add(classBox, 1, 2);

        VBox abilityBox = new VBox(8);
        abilityBox.getStyleClass().add("card");
        abilityBox.getChildren().add(buildSectionTitle("能力值"));
        GridPane abilityGrid = new GridPane();
        abilityGrid.setHgap(12);
        abilityGrid.setVgap(8);
        String[][] abilities = {
                {"力量 STR", "STR"},
                {"敏捷 DEX", "DEX"},
                {"体质 CON", "CON"},
                {"智力 INT", "INT"},
                {"感知 WIS", "WIS"},
                {"魅力 CHA", "CHA"}
        };
        for (int i = 0; i < abilities.length; i++) {
            String label = abilities[i][0];
            String key = abilities[i][1];
            Spinner<Integer> spinner = new Spinner<>(1, 20, 10);
            spinner.setEditable(true);
            abilitySpinners.put(key, spinner);
            abilityGrid.add(buildLabel(label), i % 3 * 2, i / 3);
            abilityGrid.add(spinner, i % 3 * 2 + 1, i / 3);
        }
        abilityBox.getChildren().add(abilityGrid);

        VBox loreBox = new VBox(12);
        loreBox.getChildren().add(buildSectionTitle("背景设定"));
        loreBox.getChildren().add(buildStackedField("背景故事", backgroundArea));
        loreBox.getChildren().add(buildStackedField("性格特点", personalityArea));
        loreBox.getChildren().add(buildStackedField("理想信念", idealsArea));
        loreBox.getChildren().add(buildStackedField("羁绊关系", bondsArea));
        loreBox.getChildren().add(buildStackedField("缺陷弱点", flawsArea));

        VBox center = new VBox(18);
        center.getChildren().add(form);
        center.getChildren().add(abilityBox);
        center.getChildren().add(loreBox);

        ScrollPane scrollPane = new ScrollPane(center);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        root.setCenter(scrollPane);

        Button saveButton = new Button("生成角色");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(evt -> handleCreate());
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getChildren().add(saveButton);
        root.setBottom(footer);

        Scene scene = new Scene(root, 980, 720);
        FxThemes.apply(scene);
        return scene;
    }

    private void handleCreate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showWarning("请填写角色姓名。");
            return;
        }
        Character_Race race = resolveRaceSelection();
        if (race == null) {
            showWarning("请选择一个有效的种族。");
            return;
        }
        Character_Class characterClass = resolveClassSelection();
        if (characterClass == null) {
            showWarning("请选择一个有效的职业。");
            return;
        }

        Stats stats = new Stats(
                abilitySpinners.get("STR").getValue(),
                abilitySpinners.get("DEX").getValue(),
                abilitySpinners.get("CON").getValue(),
                abilitySpinners.get("INT").getValue(),
                abilitySpinners.get("WIS").getValue(),
                abilitySpinners.get("CHA").getValue()
        );
        Character_Sheet character = Character_Sheet.create_new_character(
                name,
                ageSpinner.getValue(),
                genderBox.getValue(),
                race,
                characterClass,
                stats
        );
        applyLoreFields(character);
        character.recalculate_derived_stats();
        Global_Data.character_pool.add(character);
        Character_DAO.save_character(character);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(getOwner());
        alert.setTitle("创建成功");
        alert.setHeaderText("角色已创建");
        alert.setContentText("“" + name + "” 已加入存档，可在角色列表中查看。");
        alert.showAndWait();
        close();
    }

    private void applyLoreFields(Character_Sheet character) {
        character.background_story = backgroundArea.getText().trim();
        character.personality_traits = personalityArea.getText().trim();
        character.ideals = idealsArea.getText().trim();
        character.bonds = bondsArea.getText().trim();
        character.flaws = flawsArea.getText().trim();
    }

    private Character_Race resolveRaceSelection() {
        String choice = raceBox.getValue();
        if (choice == null) {
            return null;
        }
        switch (choice) {
            case "精灵 (Elf)":
                return new Elf_Race(promptOption("选择精灵子种族", "请选择精灵子种族",
                        Arrays.asList("HIGH", "WOOD", "DROW"), Arrays.asList("高等精灵", "木精灵", "卓尔")));
            case "矮人 (Dwarf)":
                return new Dwarf_Race(promptOption("选择矮人子种族", "请选择矮人子种族",
                        Arrays.asList("HILL", "MOUNTAIN"), Arrays.asList("丘陵矮人", "山地矮人")));
            case "半身人 (Halfling)":
                return new Halfling_Race(promptOption("选择半身人子种族", "请选择半身人子种族",
                        Arrays.asList("LIGHTFOOT", "STOUT"), Arrays.asList("轻足半身人", "健壮半身人")));
            case "龙裔 (Dragonborn)":
                return new Dragonborn_Race(promptOption("选择龙裔血脉", "请选择龙裔血脉",
                        Arrays.asList("BLACK", "BLUE", "BRASS", "BRONZE", "COPPER", "GOLD", "GREEN", "RED", "SILVER", "WHITE"),
                        Arrays.asList("黑龙 (腐蚀)", "蓝龙 (电击)", "黄铜龙 (火焰)", "青铜龙 (电击)", "赤铜龙 (腐蚀)",
                                "金龙 (火焰)", "绿龙 (毒素)", "红龙 (火焰)", "银龙 (寒冷)", "白龙 (寒冷)")));
            case "侏儒 (Gnome)":
                return new Gnome_Race(promptOption("选择侏儒子种族", "请选择侏儒子种族",
                        Arrays.asList("FOREST", "ROCK"), Arrays.asList("森林侏儒", "岩侏儒")));
            case "半精灵 (Half-Elf)":
                String first = promptAttribute("请选择第一项 +1 属性");
                String second = promptAttribute("请选择第二项 +1 属性（不同于第一项）", first);
                return new Half_Elf_Race(first, second);
            case "半兽人 (Half-Orc)":
                return new Half_Orc_Race();
            case "提夫林 (Tiefling)":
                return new Tiefling_Race();
            case "人类 (Human)":
            default:
                return new Human_Race();
        }
    }

    private Character_Class resolveClassSelection() {
        String choice = classBox.getValue();
        if (choice == null) {
            return null;
        }
        switch (choice) {
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
            case "战士 (Fighter)":
            default:
                return new Fighter_Class();
        }
    }

    private String promptOption(String title, String content, List<String> values, List<String> labels) {
        List<String> labelList = labels == null ? values : labels;
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labelList.get(0), labelList);
        dialog.initOwner(this);
        dialog.setTitle(title);
        dialog.setHeaderText(content);
        dialog.setContentText(null);
        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            return values.get(0);
        }
        String selectedLabel = result.get();
        int idx = labelList.indexOf(selectedLabel);
        return values.get(Math.max(idx, 0));
    }

    private String promptAttribute(String title) {
        return promptAttribute(title, null);
    }

    private String promptAttribute(String title, String exclude) {
        List<String> attributes = new ArrayList<>(Arrays.asList("STR", "DEX", "CON", "INT", "WIS", "CHA"));
        if (exclude != null) {
            attributes.remove(exclude);
        }
        List<String> labels = new ArrayList<>();
        for (String attr : attributes) {
            labels.add(toAttributeLabel(attr));
        }
        String chosenKey = promptOption(title, "请选择属性", attributes, labels);
        return chosenKey;
    }

    private Label buildLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private VBox buildStackedField(String title, TextArea area) {
        Label label = buildLabel(title);
        VBox box = new VBox(4, label, area);
        return box;
    }

    private Label buildSectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private static TextArea buildMultiLineArea(String placeholder) {
        TextArea area = new TextArea();
        area.setPromptText(placeholder);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.getStyleClass().add("solid-field");
        return area;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.initOwner(this);
        alert.setTitle("缺少信息");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String toAttributeLabel(String key) {
        switch (key) {
            case "STR":
                return "力量 STR";
            case "DEX":
                return "敏捷 DEX";
            case "CON":
                return "体质 CON";
            case "INT":
                return "智力 INT";
            case "WIS":
                return "感知 WIS";
            case "CHA":
            default:
                return "魅力 CHA";
        }
    }
}
