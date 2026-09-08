package com.dailycheck.controllers;

import com.dailycheck.DatabaseHelper;
import com.dailycheck.models.Task;
import com.dailycheck.utils.DateUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;

/**
 * 打卡 Tab 控制器
 *
 * 功能：
 * - 日期导航（前一天 / 后一天 / 今天）
 * - 显示连续打卡天数
 * - 显示今日任务完成进度
 * - 添加 / 删除 / 切换任务完成状态
 */
public class TasksController {

    @FXML private Label dateLabel;
    @FXML private Label streakLabel;
    @FXML private Label streakHint;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressHint;
    @FXML private VBox taskList;
    @FXML private Label taskCountLabel;

    private final DatabaseHelper db = DatabaseHelper.get();
    private String currentDate;

    @FXML
    public void initialize() {
        currentDate = DateUtils.today();
        refresh();
    }

    /** 由 Main 在切换到此 Tab 时调用 */
    public void onShown() {
        currentDate = DateUtils.today();
        refresh();
    }

    @FXML
    private void onPrevDay() {
        currentDate = DateUtils.plusDays(currentDate, -1);
        refresh();
    }

    @FXML
    private void onNextDay() {
        currentDate = DateUtils.plusDays(currentDate, 1);
        refresh();
    }

    @FXML
    private void onToday() {
        currentDate = DateUtils.today();
        refresh();
    }

    @FXML
    private void onAddTask() {
        showAddTaskPopup();
    }

    /** 刷新当前日期视图 */
    private void refresh() {
        // 日期 + 星期
        dateLabel.setText(DateUtils.toChinese(currentDate) + " " + DateUtils.weekdayChinese(currentDate));

        // 连续打卡
        int streak = db.getStreak();
        streakLabel.setText(String.valueOf(streak));
        if (streak == 0) {
            streakHint.setText("开始今日打卡吧");
        } else if (streak < 7) {
            streakHint.setText("加油，坚持就是胜利");
        } else if (streak < 30) {
            streakHint.setText("已坚持 " + streak + " 天，非常棒！");
        } else {
            streakHint.setText("太厉害了，已坚持 " + streak + " 天！");
        }

        // 今日进度
        int total = db.getTotalCount(currentDate);
        int done = db.getCompletedCount(currentDate);
        progressLabel.setText(done + " / " + total);
        progressBar.setProgress(total > 0 ? (double) done / total : 0);
        if (total == 0) {
            progressHint.setText("今天还没有任务，点击右上角新增");
        } else if (done == total) {
            progressHint.setText("全部完成，今天打卡成功！");
        } else {
            progressHint.setText("还差 " + (total - done) + " 项即可完成今日打卡");
        }

        // 任务列表
        List<Task> tasks = db.getTasksByDate(currentDate);
        taskCountLabel.setText("共 " + tasks.size() + " 项");
        taskList.getChildren().clear();
        if (tasks.isEmpty()) {
            Label empty = new Label("当日无任务");
            empty.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 13; -fx-padding: 24 0 24 0;");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            taskList.getChildren().add(empty);
            return;
        }
        for (Task t : tasks) {
            taskList.getChildren().add(buildTaskRow(t));
        }
    }

    /** 构建单个任务条目 */
    private HBox buildTaskRow(Task task) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-item");
        if (task.isCompleted) {
            row.getStyleClass().add("completed");
        }
        row.setPadding(new Insets(10, 14, 10, 14));

        // 完成复选框
        CheckBox check = new CheckBox();
        check.setSelected(task.isCompleted);
        check.getStyleClass().add("task-check");
        check.setOnAction(e -> {
            db.setTaskCompleted(task.id, check.isSelected());
            refresh();
        });

        // 标题与时间
        VBox info = new VBox(4);
        Label title = new Label(task.title == null ? "" : task.title);
        title.getStyleClass().add("task-title");
        if (task.isCompleted) {
            title.getStyleClass().add("task-title-done");
        }
        Label time = new Label(formatTime(task.createdAt));
        time.getStyleClass().add("task-time");
        info.getChildren().addAll(title, time);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 删除按钮
        Button del = new Button("删除");
        del.getStyleClass().add("icon-btn");
        del.setOnAction(e -> {
            db.deleteTask(task.id);
            refresh();
        });

        row.getChildren().addAll(check, info, spacer, del);
        HBox.setHgrow(info, Priority.ALWAYS);
        return row;
    }

    /** 添加任务弹窗（使用 Popup 而非 Stage，便于居中且无需新窗体） */
    private void showAddTaskPopup() {
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);

        VBox box = new VBox(10);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0, 0, 4); " +
                "-fx-padding: 18 18 18 18;");
        box.setPrefWidth(360);

        Label title = new Label("新增打卡任务");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2A2E45;");

        Label dateInfo = new Label("归属日期：" + DateUtils.toChinese(currentDate));
        dateInfo.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 12;");

        TextField input = new TextField();
        input.setPromptText("输入任务标题，回车保存");
        input.getStyleClass().add("text-field");
        input.setPrefWidth(320);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("取消");
        cancel.getStyleClass().add("ghost-btn");
        Button save = new Button("保存");
        save.getStyleClass().add("primary-btn");
        buttons.getChildren().addAll(cancel, save);

        Separator sep = new Separator();
        box.getChildren().addAll(title, dateInfo, input, sep, buttons);
        popup.getContent().add(box);

        Runnable doSave = () -> {
            String text = input.getText();
            if (text == null || text.trim().isEmpty()) {
                input.setStyle("-fx-border-color: #E53935;");
                input.requestFocus();
                return;
            }
            db.insertTask(new Task(text.trim(), currentDate));
            popup.hide();
            refresh();
        };

        save.setOnAction(e -> doSave.run());
        cancel.setOnAction(e -> popup.hide());
        input.setOnAction(e -> doSave.run());

        popup.show(dateLabel.getScene().getWindow());
        // 简单居中到窗口中心
        Platform.runLater(() -> {
            double x = dateLabel.getScene().getWindow().getX()
                    + (dateLabel.getScene().getWidth() - box.getWidth()) / 2.0;
            double y = dateLabel.getScene().getWindow().getY()
                    + (dateLabel.getScene().getHeight() - box.getHeight()) / 2.0;
            popup.setX(x);
            popup.setY(y);
            input.requestFocus();
        });
    }

    /** 格式化时间戳为「MM-dd HH:mm」 */
    private static String formatTime(long ts) {
        if (ts <= 0) return "";
        java.util.Date d = new java.util.Date(ts);
        return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(d);
    }
}
