package com.dailycheck.controllers;

import com.dailycheck.DatabaseHelper;
import com.dailycheck.models.StudyRecord;
import com.dailycheck.utils.DateUtils;
import com.dailycheck.utils.PrefsManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * 学习 Tab 控制器
 *
 * 功能：
 * - 日期导航
 * - 学习计时器（开始 / 暂停 / 重置），计时值可保存为学习记录
 * - 手动新增学习记录（科目 / 内容 / 时长）
 * - 列表展示当日学习记录，可删除
 * - 显示今日学习总时长，与设置中目标对比
 */
public class StudyController {

    @FXML private Label dateLabel;
    @FXML private Label totalLabel;
    @FXML private Label goalHintLabel;
    @FXML private Label timerLabel;
    @FXML private Button startBtn;
    @FXML private TextField subjectField;
    @FXML private TextField durationField;
    @FXML private TextField contentField;
    @FXML private VBox recordList;
    @FXML private Label recordCountLabel;

    private final DatabaseHelper db = DatabaseHelper.get();
    private String currentDate;

    // 计时器
    private Timeline timeline;
    private long elapsedSeconds = 0; // 已累计秒数
    private boolean running = false;

    @FXML
    public void initialize() {
        currentDate = DateUtils.today();
        // 每秒刷新计时显示
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            updateTimerLabel();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        refresh();
    }

    public void onShown() {
        // 计时器继续计时（不重置），但同步日期与列表
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

    /** 切换开始 / 暂停 */
    @FXML
    private void onToggleTimer() {
        if (running) {
            timeline.stop();
            running = false;
            startBtn.setText("继续");
        } else {
            timeline.play();
            running = true;
            startBtn.setText("暂停");
        }
    }

    @FXML
    private void onResetTimer() {
        timeline.stop();
        running = false;
        elapsedSeconds = 0;
        startBtn.setText("开始");
        updateTimerLabel();
    }

    /** 保存当前计时为一条学习记录（时长按分钟取整） */
    @FXML
    private void onSaveRecord() {
        String subject = subjectField.getText();
        String content = contentField.getText();
        // 优先使用手动输入的时长，否则使用计时器时长
        int minutes = 0;
        String durText = durationField.getText();
        if (durText != null && !durText.trim().isEmpty()) {
            try {
                minutes = Integer.parseInt(durText.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (minutes <= 0 && elapsedSeconds > 0) {
            minutes = (int) Math.round(elapsedSeconds / 60.0);
        }
        if (subject == null || subject.trim().isEmpty()) {
            flashFieldError(subjectField);
            return;
        }
        if (minutes <= 0) {
            flashFieldError(durationField);
            return;
        }
        StudyRecord record = new StudyRecord(subject.trim(), content == null ? "" : content.trim(),
                minutes, currentDate);
        db.insertStudyRecord(record);
        // 清空输入
        subjectField.clear();
        contentField.clear();
        durationField.clear();
        // 重置计时器
        onResetTimer();
        refresh();
    }

    /** 刷新当前日期视图 */
    private void refresh() {
        dateLabel.setText(DateUtils.toChinese(currentDate) + " " + DateUtils.weekdayChinese(currentDate));

        // 今日学习总时长
        int total = db.getStudyTotalMinutes(currentDate, currentDate);
        totalLabel.setText(String.valueOf(total));

        // 目标提示
        int goal = PrefsManager.get().getDailyStudyGoal();
        if (goal > 0) {
            goalHintLabel.setText("目标 " + goal + " 分钟，已完成 " + total + " 分钟");
        } else {
            goalHintLabel.setText("未设置目标，可在设置页配置");
        }

        // 学习记录列表
        List<StudyRecord> records = db.getStudyRecordsByDate(currentDate);
        recordCountLabel.setText("共 " + records.size() + " 条");
        recordList.getChildren().clear();
        if (records.isEmpty()) {
            Label empty = new Label("当日无学习记录");
            empty.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 13; -fx-padding: 24 0 24 0;");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            recordList.getChildren().add(empty);
            return;
        }
        for (StudyRecord r : records) {
            recordList.getChildren().add(buildStudyRow(r));
        }
    }

    /** 构建学习记录条目 */
    private HBox buildStudyRow(StudyRecord r) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-item");
        row.setPadding(new Insets(10, 14, 10, 14));

        // 科目标签
        Label subject = new Label(r.subject == null ? "" : r.subject);
        subject.setStyle("-fx-background-color: #EEF0FF; -fx-text-fill: #5B6CFF; " +
                "-fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12; -fx-font-weight: bold;");

        VBox info = new VBox(4);
        Label content = new Label(r.content == null || r.content.isEmpty() ? "（无内容描述）" : r.content);
        content.setStyle("-fx-font-size: 14; -fx-text-fill: #2A2E45;");
        Label time = new Label(formatTime(r.createdAt));
        time.setStyle("-fx-font-size: 11; -fx-text-fill: #9AA0B5;");
        info.getChildren().addAll(content, time);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dur = new Label(r.durationMinutes + " 分钟");
        dur.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #4ECDC4;");

        Button del = new Button("删除");
        del.getStyleClass().add("icon-btn");
        del.setOnAction(e -> {
            db.deleteStudyRecord(r.id);
            refresh();
        });

        row.getChildren().addAll(subject, info, spacer, dur, del);
        HBox.setHgrow(info, Priority.ALWAYS);
        return row;
    }

    /** 更新计时器显示 */
    private void updateTimerLabel() {
        long h = elapsedSeconds / 3600;
        long m = (elapsedSeconds % 3600) / 60;
        long s = elapsedSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
    }

    /** 输入框错误闪烁 */
    private void flashFieldError(TextField field) {
        field.setStyle("-fx-border-color: #E53935; -fx-background-radius: 8; -fx-border-radius: 8;");
        field.requestFocus();
        // 稍后恢复样式
        new Timeline(new KeyFrame(Duration.seconds(2), e ->
                field.setStyle(null))).play();
    }

    private static String formatTime(long ts) {
        if (ts <= 0) return "";
        java.util.Date d = new java.util.Date(ts);
        return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(d);
    }
}
