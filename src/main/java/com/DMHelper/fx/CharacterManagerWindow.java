package com.DMHelper.fx;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Character_DAO;
import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.basic.equipment.Equipment_Item;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class CharacterManagerWindow extends Stage {

    private final ObservableList<Character_Sheet> characters =
            FXCollections.observableArrayList(Global_Data.character_pool);
    private Character_Sheet current;

    private final TextArea overviewArea = new TextArea();
    private final ListView<String> backpackList = new ListView<>();
    private final Label equipSummary = new Label();

    public CharacterManagerWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("角色管理 · JavaFX 版本");
        if (!characters.isEmpty()) {
            current = characters.get(0);
        }
        setScene(buildScene());
        sizeToScene();
        setMinWidth(1100);
        setMinHeight(700);
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-shell");
        root.setPadding(new Insets(32));

        VBox header = new VBox(6);
        Label title = new Label("角色管理");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("查看与维护装备、状态、背包，并执行短休/长休等操作。");
        subtitle.getStyleClass().add("dialog-subtitle");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        ComboBox<Character_Sheet> selector = new ComboBox<>(characters);
        selector.setCellFactory(cb -> new ListCell<Character_Sheet>() {
            @Override
            protected void updateItem(Character_Sheet item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name + " · " + item.job.class_name);
            }
        });
        selector.setButtonCell(selector.getCellFactory().call(null));
        selector.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            current = value;
            refreshPanels();
        });
        if (current != null) {
            selector.getSelectionModel().select(current);
        }

        overviewArea.setEditable(false);
        overviewArea.setPrefRowCount(16);
        overviewArea.getStyleClass().add("detail-area");

        backpackList.setPlaceholder(new Label("背包为空"));

        VBox leftCard = new VBox(16, new Label("选择角色"), selector, buildActionButtons(selector), buildRestButtons(selector));
        leftCard.getStyleClass().add("card");

        VBox centerCard = new VBox(12, new Label("简要概览"), overviewArea);
        centerCard.getStyleClass().add("card");

        VBox rightCard = new VBox(12);
        rightCard.getStyleClass().add("card");
        Label equipTitle = new Label("当前装备");
        equipTitle.getStyleClass().add("section-title");
        equipSummary.getStyleClass().add("equip-summary");
        Label backpackTitle = new Label("背包 (双击使用物品)");
        backpackTitle.getStyleClass().add("section-title");

        rightCard.getChildren().addAll(equipTitle, equipSummary, backpackTitle, backpackList);

        GridPane contentGrid = new GridPane();
        contentGrid.setHgap(18);
        contentGrid.setVgap(18);
        contentGrid.add(leftCard, 0, 0);
        contentGrid.add(centerCard, 1, 0);
        contentGrid.add(rightCard, 2, 0);
        GridPane.setVgrow(centerCard, Priority.ALWAYS);
        GridPane.setHgrow(centerCard, Priority.ALWAYS);
        GridPane.setVgrow(rightCard, Priority.ALWAYS);
        GridPane.setHgrow(rightCard, Priority.ALWAYS);

        root.setCenter(contentGrid);

        refreshPanels();

        Scene scene = new Scene(root, 1200, 720);
        FxThemes.apply(scene);
        return scene;
    }

    private VBox buildActionButtons(ComboBox<Character_Sheet> selector) {
        Label label = new Label("即刻操作");
        label.getStyleClass().add("section-title");
        Button saveBtn = new Button("保存更改");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.disableProperty().bind(selector.valueProperty().isNull());
        saveBtn.setOnAction(e -> {
            if (current != null) {
                Character_DAO.save_character(current);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "角色数据已保存。", ButtonType.OK);
                alert.initOwner(this);
                alert.showAndWait();
            }
        });

        VBox box = new VBox(8, label, saveBtn);
        return box;
    }

    private VBox buildRestButtons(ComboBox<Character_Sheet> selector) {
        Label label = new Label("恢复操作");
        label.getStyleClass().add("section-title");

        Button shortRest = new Button("短休");
        Button longRest = new Button("长休");
        shortRest.getStyleClass().add("ghost-button");
        longRest.getStyleClass().add("ghost-button");

        shortRest.disableProperty().bind(selector.valueProperty().isNull());
        longRest.disableProperty().bind(selector.valueProperty().isNull());

        shortRest.setOnAction(e -> {
            if (current != null) {
                current.job.restore_short_rest_resources();
                Character_DAO.save_character(current);
                refreshPanels();
            }
        });
        longRest.setOnAction(e -> {
            if (current != null) {
                current.job.restore_long_rest_resources();
                Character_DAO.save_character(current);
                refreshPanels();
            }
        });

        VBox box = new VBox(8, label, shortRest, longRest);
        return box;
    }

    private void refreshPanels() {
        if (current == null) {
            overviewArea.clear();
            equipSummary.setText("请选择角色");
            backpackList.getItems().clear();
            return;
        }
        overviewArea.setText(buildOverview(current));
        equipSummary.setText(buildEquipSummary(current));
        backpackList.getItems().setAll(buildBackpackRows(current));
    }

    private String buildOverview(Character_Sheet c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.name).append(" · ").append(c.job.class_name).append(" · ").append(c.race.race_name).append("\n");
        sb.append("等级：").append(c.job.current_level).append("    经验：").append(c.experience_points).append("\n");
        sb.append("HP：").append(c.current_hp).append("/").append(c.hp).append("\n\n");
        sb.append("属性： STR ").append(c.stats.str).append("  DEX ").append(c.stats.dex)
                .append("  CON ").append(c.stats.con).append("  INT ").append(c.stats.intel)
                .append("  WIS ").append(c.stats.wis).append("  CHA ").append(c.stats.cha).append("\n\n");
        sb.append("特性：\n");
        for (String summary : c.job.get_feature_summaries()) {
            sb.append(" • ").append(summary).append("\n");
        }
        return sb.toString();
    }

    private String buildEquipSummary(Character_Sheet c) {
        StringBuilder sb = new StringBuilder();
        sb.append("护甲：").append(resolveEquipName(c.equipped_armor_key)).append("\n");
        sb.append("主手：").append(resolveEquipName(c.equipped_main_hand_key)).append("\n");
        sb.append("副手/盾：").append(resolveEquipName(c.equipped_off_hand_key)).append("\n");
        sb.append("披风：").append(resolveEquipName(c.equipped_cloak_key)).append("\n");
        sb.append("护符：").append(resolveEquipName(c.equipped_accessory_key)).append("\n");
        return sb.toString();
    }

    private String resolveEquipName(String key) {
        if (key == null || key.isEmpty()) {
            return "未装备";
        }
        Equipment_Item item = com.DMHelper.basic.equipment.Equipment_Library.get_item(key);
        return item == null ? "未拥有" : item.display_name;
    }

    private java.util.List<String> buildBackpackRows(Character_Sheet c) {
        java.util.List<String> rows = new java.util.ArrayList<>();
        for (String key : c.owned_equipment_keys) {
            Equipment_Item item = com.DMHelper.basic.equipment.Equipment_Library.get_item(key);
            if (item == null) {
                continue;
            }
            int count = c.get_item_count(key);
            rows.add(item.display_name + (count > 1 ? " ×" + count : ""));
        }
        return rows;
    }
}
