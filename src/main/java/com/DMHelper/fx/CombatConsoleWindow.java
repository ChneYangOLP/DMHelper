package com.DMHelper.fx;

import com.DMHelper.basic.Character_Sheet;
import com.DMHelper.basic.database.Global_Data;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Comparator;
import java.util.Random;

public class CombatConsoleWindow extends Stage {

    private final ObservableList<Character_Sheet> available =
            FXCollections.observableArrayList(Global_Data.character_pool);
    private final ObservableList<Participant> participants = FXCollections.observableArrayList();
    private final Random random = new Random();

    private final TableView<Participant> tableView = new TableView<>();
    private final TextArea logArea = new TextArea();

    public CombatConsoleWindow(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE);
        setTitle("战斗系统 · JavaFX 版本");
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
        Label title = new Label("战斗控制台");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label("配置参与者、投掷先攻并追踪轮次。");
        subtitle.getStyleClass().add("dialog-subtitle");
        header.getChildren().addAll(title, subtitle);
        root.setTop(header);

        ListView<Character_Sheet> rosterList = new ListView<>(available);
        rosterList.setPlaceholder(new Label("暂无角色"));
        rosterList.setCellFactory(list -> new ListCell<Character_Sheet>() {
            @Override
            protected void updateItem(Character_Sheet item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.name + " · " + item.job.class_name);
            }
        });

        Button addButton = new Button("加入战斗");
        addButton.getStyleClass().add("primary-button");
        addButton.disableProperty().bind(rosterList.getSelectionModel().selectedItemProperty().isNull());
        addButton.setOnAction(e -> {
            Character_Sheet sheet = rosterList.getSelectionModel().getSelectedItem();
            if (sheet != null && participants.stream().noneMatch(p -> p.sheet == sheet)) {
                participants.add(new Participant(sheet));
                log("添加参战者：" + sheet.name);
            }
        });

        VBox left = new VBox(12, new Label("存档角色"), rosterList, addButton);
        left.getStyleClass().add("card");
        VBox.setVgrow(rosterList, Priority.ALWAYS);

        setupTable();

        VBox center = new VBox(12, tableView, buildCombatControls());
        center.getStyleClass().add("card");
        VBox.setVgrow(tableView, Priority.ALWAYS);

        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(20);
        VBox right = new VBox(12, new Label("战斗日志"), logArea);
        right.getStyleClass().add("card");

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.add(left, 0, 0);
        grid.add(center, 1, 0);
        grid.add(right, 2, 0);
        GridPane.setHgrow(center, Priority.ALWAYS);
        GridPane.setVgrow(center, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.ALWAYS);
        GridPane.setVgrow(right, Priority.ALWAYS);

        root.setCenter(grid);

        Scene scene = new Scene(root, 1280, 720);
        FxThemes.apply(scene);
        return scene;
    }

    private void setupTable() {
        TableColumn<Participant, String> nameCol = new TableColumn<>("角色");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sheet.name));
        nameCol.setPrefWidth(200);

        TableColumn<Participant, Number> initiativeCol = new TableColumn<>("先攻");
        initiativeCol.setCellValueFactory(data -> data.getValue().initiativeProperty());
        initiativeCol.setPrefWidth(80);

        TableColumn<Participant, Number> hpCol = new TableColumn<>("HP");
        hpCol.setCellValueFactory(data -> new SimpleIntegerProperty(
                data.getValue().sheet.current_hp));
        hpCol.setPrefWidth(120);

        TableColumn<Participant, Boolean> activeCol = new TableColumn<>("当前回合");
        activeCol.setCellValueFactory(data -> data.getValue().activeProperty());
        activeCol.setCellFactory(col -> new TableCell<Participant, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (item ? "▶" : ""));
            }
        });
        activeCol.setPrefWidth(120);

        tableView.getColumns().addAll(nameCol, initiativeCol, hpCol, activeCol);
        tableView.setItems(participants);
        tableView.setPlaceholder(new Label("请添加参战者"));
    }

    private HBox buildCombatControls() {
        Button rollBtn = new Button("掷先攻");
        Button advanceBtn = new Button("下一回合");
        Button removeBtn = new Button("移除选中");

        rollBtn.getStyleClass().add("primary-button");
        advanceBtn.getStyleClass().add("ghost-button");
        removeBtn.getStyleClass().add("ghost-button");

        rollBtn.setOnAction(e -> {
            participants.forEach(p -> {
                p.setInitiative(rollInitiative(p.sheet));
                p.setActive(false);
            });
            FXCollections.sort(participants, Comparator.comparingInt(Participant::getInitiative).reversed());
            if (!participants.isEmpty()) {
                participants.get(0).setActive(true);
            }
            log("重新掷先攻，顺序已更新。");
        });

        advanceBtn.setOnAction(e -> {
            if (participants.isEmpty()) return;
            Participant current = participants.stream().filter(Participant::isActive).findFirst().orElse(null);
            if (current != null) {
                current.setActive(false);
                int idx = participants.indexOf(current);
                int nextIdx = (idx + 1) % participants.size();
                participants.get(nextIdx).setActive(true);
                log("轮到 " + participants.get(nextIdx).sheet.name);
            }
        });

        removeBtn.setOnAction(e -> {
            Participant selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                participants.remove(selected);
                log("移除参战者：" + selected.sheet.name);
            }
        });

        HBox box = new HBox(12, rollBtn, advanceBtn, removeBtn);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private int rollInitiative(Character_Sheet sheet) {
        return random.nextInt(20) + 1 + sheet.get_initiative_modifier();
    }

    private void log(String message) {
        logArea.appendText("• " + message + "\n");
    }

    private static class Participant {
        private final Character_Sheet sheet;
        private final IntegerProperty initiative = new SimpleIntegerProperty(0);
        private final BooleanProperty active = new SimpleBooleanProperty(false);

        Participant(Character_Sheet sheet) {
            this.sheet = sheet;
        }

        int getInitiative() {
            return initiative.get();
        }

        void setInitiative(int value) {
            initiative.set(value);
        }

        IntegerProperty initiativeProperty() {
            return initiative;
        }

        boolean isActive() {
            return active.get();
        }

        void setActive(boolean value) {
            active.set(value);
        }

        BooleanProperty activeProperty() {
            return active;
        }
    }
}
