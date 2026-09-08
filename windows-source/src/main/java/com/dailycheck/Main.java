package com.dailycheck;

import com.dailycheck.controllers.ExpenseController;
import com.dailycheck.controllers.SettingsController;
import com.dailycheck.controllers.StatsController;
import com.dailycheck.controllers.StudyController;
import com.dailycheck.controllers.TasksController;
import com.dailycheck.utils.DateUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * DailyCheck 桌面版入口
 *
 * 主窗口布局：左侧侧边栏导航（5 个 Tab：打卡 / 学习 / 记账 / 统计 / 设置），
 * 右侧内容区根据当前选中 Tab 加载对应 FXML 视图。
 *
 * 视图采用「懒加载 + 缓存」策略，首次切换时加载 FXML 并缓存对应的 Controller 实例，
 * 切换 Tab 时调用 controller.onShown() 让其刷新数据。
 */
public class Main extends Application {

    private static final String PRIMARY_COLOR = "#5B6CFF";
    private static final String SIDEBAR_BG = "#FFFFFF";
    private static final String PAGE_BG = "#F5F6F8";

    /** 侧边栏 Tab 项 */
    private static final class TabDef {
        final String id;
        final String label;     // 中文标签
        final String fxml;      // FXML 资源路径
        TabDef(String id, String label, String fxml) {
            this.id = id; this.label = label; this.fxml = fxml;
        }
    }

    private static final TabDef[] TABS = {
            new TabDef("tasks",   "打卡", "/fxml/tasks.fxml"),
            new TabDef("study",   "学习", "/fxml/study.fxml"),
            new TabDef("expense", "记账", "/fxml/expense.fxml"),
            new TabDef("stats",   "统计", "/fxml/stats.fxml"),
            new TabDef("settings","设置", "/fxml/settings.fxml"),
    };

    private final DatabaseHelper dbHelper = DatabaseHelper.get();

    // 缓存已加载的视图与控制器
    private final Node[] cachedViews = new Node[TABS.length];
    private final Object[] cachedControllers = new Object[TABS.length];

    private StackPane contentPane;
    private ToggleGroup navGroup;

    @Override
    public void start(Stage primaryStage) {
        // 触发数据库初始化（在 DBHelper.get() 中已建表）
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + PAGE_BG + ";");

        // 左侧栏
        Node sidebar = buildSidebar(primaryStage);
        root.setLeft(sidebar);

        // 右侧内容
        contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: " + PAGE_BG + ";");
        root.setCenter(contentPane);

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setTitle("每日打卡 DailyCheck");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);

        // 应用图标（可选，找不到则忽略）
        try (InputStream is = getClass().getResourceAsStream("/icon.png")) {
            if (is != null) {
                primaryStage.getIcons().add(new Image(is));
            }
        } catch (Exception e) {
            // 忽略图标缺失
        }

        primaryStage.setOnCloseRequest(e -> {
            try { dbHelper.close(); } catch (Exception ignored) {}
            Platform.exit();
        });

        primaryStage.show();

        // 默认选中第一个 Tab
        selectTab(0);
    }

    /** 构建左侧导航栏 */
    private Node buildSidebar(Stage stage) {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(208);
        sidebar.setStyle("-fx-background-color: " + SIDEBAR_BG + "; " +
                "-fx-border-color: #ECEEF3; -fx-border-width: 0 1 0 0; " +
                "-fx-padding: 18 0 12 0;");

        // 顶部 logo / 标题
        HBox logoBox = new HBox(8);
        logoBox.setAlignment(Pos.CENTER_LEFT);
        logoBox.setPadding(new Insets(0, 22, 14, 22));
        Label dot = new Label();
        dot.setPrefSize(12, 12);
        dot.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-background-radius: 6; -fx-background-insets: 4 0 0 0;");
        Label title = new Label("每日打卡");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2A2E45;");
        logoBox.getChildren().addAll(dot, title);
        sidebar.getChildren().add(logoBox);

        // 日期副标题
        Label dateLabel = new Label(DateUtils.toChinese(DateUtils.today()) + " " + DateUtils.weekdayChinese(DateUtils.today()));
        dateLabel.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 12; -fx-padding: 0 0 16 22;");
        sidebar.getChildren().add(dateLabel);

        navGroup = new ToggleGroup();
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            ToggleButton btn = new ToggleButton(TABS[i].label);
            btn.setToggleGroup(navGroup);
            btn.getStyleClass().add("nav-item");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> selectTab(idx));
            // 让按钮占满宽度
            VBox.setMargin(btn, new Insets(2, 12, 2, 12));
            sidebar.getChildren().add(btn);
        }

        // 弹性间隔
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // 底部版本信息
        Label ver = new Label("DailyCheck v1.0.0");
        ver.setStyle("-fx-text-fill: #B5BAD0; -fx-font-size: 11; -fx-padding: 0 0 0 22;");
        sidebar.getChildren().add(ver);

        return sidebar;
    }

    /** 选中并展示指定 Tab，必要时懒加载其 FXML */
    private void selectTab(int index) {
        try {
            navGroup.getToggles().get(index).setSelected(true);
        } catch (Exception ignored) {}

        if (cachedViews[index] == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(TABS[index].fxml));
                cachedViews[index] = loader.load();
                cachedControllers[index] = loader.getController();
            } catch (Exception e) {
                e.printStackTrace();
                Label err = new Label("加载视图失败：" + TABS[index].fxml + "\n" + e.getMessage());
                err.setWrapText(true);
                err.setStyle("-fx-text-fill: #C0392B; -fx-padding: 24;");
                cachedViews[index] = err;
            }
        }

        contentPane.getChildren().setAll(cachedViews[index]);
        notifyShown(index);
    }

    /** 切换到指定 Tab 时调用控制器的 onShown 刷新数据 */
    private void notifyShown(int index) {
        Object ctrl = cachedControllers[index];
        try {
            if (ctrl instanceof TasksController) {
                ((TasksController) ctrl).onShown();
            } else if (ctrl instanceof StudyController) {
                ((StudyController) ctrl).onShown();
            } else if (ctrl instanceof ExpenseController) {
                ((ExpenseController) ctrl).onShown();
            } else if (ctrl instanceof StatsController) {
                ((StatsController) ctrl).onShown();
            } else if (ctrl instanceof SettingsController) {
                ((SettingsController) ctrl).onShown();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
