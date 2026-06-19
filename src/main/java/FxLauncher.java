import com.DMHelper.basic.database.Global_Data;
import com.DMHelper.fx.CharacterCreateWindow;
import com.DMHelper.fx.CharacterManagerWindow;
import com.DMHelper.fx.CharacterRosterWindow;
import com.DMHelper.fx.CombatConsoleWindow;
import com.DMHelper.fx.FxThemes;
import javafx.application.Application;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DMHelper JavaFX 主启动器
 * <p>
 * 所有子界面均已迁移为 JavaFX 原生实现，不再依赖 Swing。
 */
public final class FxLauncher extends Application {
    private static final AtomicBoolean launched = new AtomicBoolean(false);
    private static final String CSS_RESOURCE = "/com/DMHelper/basic/javafx/main-menu.css";

    private final IntegerProperty characterCount = new SimpleIntegerProperty();

    public static void launchApp(String[] args) {
        if (launched.compareAndSet(false, true)) {
            Application.launch(FxLauncher.class, args);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Main.ensure_core_bootstrapped();

        primaryStage.setTitle("DMHelper · JavaFX 控制台");
        Image icon = loadStageIcon();
        if (icon != null) {
            primaryStage.getIcons().add(icon);
        }

        Scene scene = new Scene(buildRoot(primaryStage), 1080, 760);
        String stylesheet = resolveResource(CSS_RESOURCE);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet);
        }
        FxThemes.apply(scene);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(620);
        primaryStage.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                refreshStats();
            }
        });
        primaryStage.show();
        refreshStats();
    }

    private BorderPane buildRoot(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("home-shell");

        VBox content = new VBox(24);
        content.getStyleClass().add("home-content");
        content.setPadding(new Insets(28, 28, 32, 28));
        content.getChildren().addAll(buildHero(), buildActionGrid());

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("home-scroll");
        root.setCenter(scrollPane);
        return root;
    }

    private VBox buildHero() {
        VBox hero = new VBox(10);
        hero.getStyleClass().add("hero-banner");

        Label badge = new Label("DM 工具箱");
        badge.getStyleClass().add("hero-badge");

        Text title = new Text("DMHelper");
        title.getStyleClass().add("hero-title");

        Text subtitle = new Text("角色、成长、装备与战斗管理");
        subtitle.getStyleClass().add("hero-description");

        Label summary = new Label();
        summary.textProperty().bind(characterCount.asString("当前已有 %s 个角色存档"));
        summary.getStyleClass().add("hero-summary");

        hero.getChildren().addAll(badge, title, subtitle, summary);
        return hero;
    }

    private FlowPane buildActionGrid() {
        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("action-grid");
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPrefWrapLength(980);

        grid.getChildren().add(buildActionCard(
                "创建角色",
                "创建新角色，填写基础信息、六维属性，并完成背景与性格设定。",
                () -> new CharacterCreateWindow(getActiveStage()).show()
        ));
        grid.getChildren().add(buildActionCard(
                "角色一览",
                "查看现有角色列表与详情，支持搜索、排序与导出角色卡 PDF。",
                () -> new CharacterRosterWindow(getActiveStage()).show()
        ));
        grid.getChildren().add(buildActionCard(
                "角色管理",
                "全功能角色管理：装备与背包、施法与法术、短休/长休、升级与成长、导出角色卡 PDF。",
                () -> {
                    if (ensureCharactersAvailable("角色管理")) {
                        new CharacterManagerWindow(getActiveStage()).show();
                    }
                }
        ));
        grid.getChildren().add(buildActionCard(
                "战斗系统",
                "完整战斗系统：选择参战角色与敌人、先攻排序、攻击结算、道具使用、战后结算与导出战斗日志 PDF。",
                () -> {
                    if (ensureCharactersAvailable("战斗系统")) {
                        new CombatConsoleWindow(getActiveStage()).show();
                    }
                }
        ));
        return grid;
    }

    private VBox buildActionCard(String title, String description, Runnable action) {
        VBox card = new VBox(16);
        card.getStyleClass().add("action-card");
        card.setPrefWidth(470);
        Label heading = new Label(title);
        heading.getStyleClass().add("action-title");
        Label body = new Label(description);
        body.getStyleClass().add("action-desc");
        body.setWrapText(true);

        Button openButton = new Button("打开");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(e -> {
            action.run();
            refreshStats();
        });

        HBox actions = new HBox(openButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(heading, body, actions);
        card.setOnMouseClicked(e -> openButton.fire());
        return card;
    }

    private boolean ensureCharactersAvailable(String featureName) {
        if (!Global_Data.character_pool.isEmpty()) {
            return true;
        }
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("暂无角色");
        alert.setHeaderText(featureName + " 需要至少一个角色存档");
        alert.setContentText("请先创建角色，再进入完整功能界面。");
        alert.showAndWait();
        return false;
    }

    private Stage getActiveStage() {
        // 获取当前 JavaFX 应用的主 Stage 或任意可见 Stage
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && ((Stage) window).isShowing()) {
                return (Stage) window;
            }
        }
        return new Stage();
    }

    private Image loadStageIcon() {
        String resourcePath = Main.getAppIconResourcePath();
        if (resourcePath == null) {
            return null;
        }
        URL resource = Main.class.getResource(resourcePath);
        if (resource == null) {
            return null;
        }
        try {
            return new Image(resource.toExternalForm());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveResource(String path) {
        URL resource = FxLauncher.class.getResource(path);
        return resource == null ? null : resource.toExternalForm();
    }

    private void refreshStats() {
        characterCount.set(Global_Data.character_pool.size());
    }
}
