package com.DMHelper.fx;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Global_Data;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Comparator;

public class CharacterRosterWindow extends Stage {

    private final ObservableList<Character_Sheet> characters =
            FXCollections.observableArrayList(Global_Data.character_pool);

    public CharacterRosterWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("角色一览 · JavaFX 版本");
        setScene(buildScene());
        sizeToScene();
        setMinWidth(960);
        setMinHeight(640);
    }

    private Scene buildScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("dialog-shell");
        root.setPadding(new Insets(32));

        VBox header = new VBox(6);
        Label title = new Label("角色一览");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("快速搜索、过滤和查看所有已创建角色的详细资料。");
        subtitle.getStyleClass().add("dialog-subtitle");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        FilteredList<Character_Sheet> filtered = new FilteredList<>(characters, c -> true);

        TextField searchField = new TextField();
        searchField.setPromptText("按名称、职业、种族关键字过滤...");
        searchField.textProperty().addListener((obs, old, val) -> {
            String keyword = val == null ? "" : val.trim().toLowerCase();
            filtered.setPredicate(character -> {
                if (keyword.isEmpty()) {
                    return true;
                }
                return character.name.toLowerCase().contains(keyword)
                        || character.job.class_name.toLowerCase().contains(keyword)
                        || character.race.race_name.toLowerCase().contains(keyword);
            });
        });

        TableView<Character_Sheet> tableView = new TableView<>(filtered);
        TableColumn<Character_Sheet, String> nameCol = new TableColumn<>("姓名");
        nameCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().name));
        nameCol.setPrefWidth(160);

        TableColumn<Character_Sheet, String> classCol = new TableColumn<>("职业");
        classCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().job.class_name));
        classCol.setPrefWidth(160);

        TableColumn<Character_Sheet, String> raceCol = new TableColumn<>("种族");
        raceCol.setCellValueFactory(data -> Bindings.createStringBinding(() -> data.getValue().race.race_name));
        raceCol.setPrefWidth(160);

        TableColumn<Character_Sheet, Number> levelCol = new TableColumn<>("等级");
        levelCol.setCellValueFactory(data -> Bindings.createIntegerBinding(() -> data.getValue().job.current_level));
        levelCol.setPrefWidth(80);

        tableView.getColumns().addAll(nameCol, classCol, raceCol, levelCol);
        tableView.getStyleClass().add("roster-table");

        VBox tableCard = new VBox(12, searchField, tableView);
        tableCard.getStyleClass().add("card");

        TextArea detailArea = new TextArea();
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefRowCount(20);
        detailArea.getStyleClass().add("detail-area");

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value == null) {
                detailArea.clear();
            } else {
                detailArea.setText(buildCharacterSummary(value));
            }
        });
        if (!characters.isEmpty()) {
            tableView.getSelectionModel().select(characters.get(0));
        }

        VBox detailCard = new VBox(8);
        detailCard.getStyleClass().add("card");
        Label detailTitle = new Label("角色详情");
        detailTitle.getStyleClass().add("section-title");
        detailCard.getChildren().addAll(detailTitle, detailArea);

        SplitPane splitPane = new SplitPane(tableCard, detailCard);
        splitPane.setDividerPositions(0.55);
        SplitPane.setResizableWithParent(tableCard, Boolean.TRUE);
        SplitPane.setResizableWithParent(detailCard, Boolean.TRUE);

        root.setCenter(splitPane);

        Button refreshButton = new Button("刷新列表");
        refreshButton.getStyleClass().add("ghost-button");
        refreshButton.setOnAction(e -> {
            characters.setAll(Global_Data.character_pool);
            characters.sort(Comparator.comparing(c -> c.name));
        });

        HBox footer = new HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.getChildren().add(refreshButton);
        root.setBottom(footer);

        Scene scene = new Scene(root, 1100, 680);
        FxThemes.apply(scene);
        return scene;
    }

    private String buildCharacterSummary(Character_Sheet sheet) {
        StringBuilder sb = new StringBuilder();
        sb.append("姓名：").append(sheet.name).append("\n");
        sb.append("等级：").append(sheet.job.current_level).append("       职业：").append(sheet.job.class_name).append("\n");
        sb.append("种族：").append(sheet.race.race_name).append("\n\n");
        sb.append("属性：\n");
        sb.append(String.format("STR %d   DEX %d   CON %d   INT %d   WIS %d   CHA %d\n",
                sheet.stats.str, sheet.stats.dex, sheet.stats.con, sheet.stats.intel, sheet.stats.wis, sheet.stats.cha));
        sb.append("\n");
        if (sheet.background_story != null && !sheet.background_story.isEmpty()) {
            sb.append("背景：").append(sheet.background_story).append("\n\n");
        }
        if (sheet.personality_traits != null && !sheet.personality_traits.isEmpty()) {
            sb.append("性格：").append(sheet.personality_traits).append("\n\n");
        }
        if (sheet.ideals != null && !sheet.ideals.isEmpty()) {
            sb.append("理想：").append(sheet.ideals).append("\n\n");
        }
        if (sheet.bonds != null && !sheet.bonds.isEmpty()) {
            sb.append("羁绊：").append(sheet.bonds).append("\n\n");
        }
        if (sheet.flaws != null && !sheet.flaws.isEmpty()) {
            sb.append("缺陷：").append(sheet.flaws).append("\n\n");
        }
        sb.append("特性：\n");
        for (String feature : sheet.job.get_feature_summaries()) {
            sb.append(" • ").append(feature).append("\n");
        }
        return sb.toString();
    }
}
