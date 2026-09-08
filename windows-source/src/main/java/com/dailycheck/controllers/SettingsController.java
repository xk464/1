package com.dailycheck.controllers;

import com.dailycheck.utils.PrefsManager;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.io.File;

/**
 * 设置 Tab 控制器
 *
 * 功能：
 * - 每日学习目标（分钟）
 * - 每日消费上限（元）
 * - 打卡提醒开关 + 提醒时间
 * - 数据库存储路径展示
 */
public class SettingsController {

    @FXML private TextField studyGoalField;
    @FXML private TextField expenseLimitField;
    @FXML private CheckBox reminderCheck;
    @FXML private Spinner<Integer> hourSpinner;
    @FXML private Spinner<Integer> minSpinner;
    @FXML private Label dbPathLabel;

    private final PrefsManager prefs = PrefsManager.get();

    @FXML
    public void initialize() {
        // Spinner 取值范围
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 21));
        minSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        // 让 Spinner 可编辑且失焦时提交
        hourSpinner.setEditable(true);
        minSpinner.setEditable(true);
        // 刷新显示
        refresh();
    }

    public void onShown() {
        refresh();
    }

    /** 刷新输入框，显示当前偏好 */
    private void refresh() {
        studyGoalField.setText(String.valueOf(prefs.getDailyStudyGoal()));
        expenseLimitField.setText(String.valueOf(prefs.getDailyExpenseLimit()));
        reminderCheck.setSelected(prefs.isReminderEnabled());
        hourSpinner.getValueFactory().setValue(prefs.getReminderHour());
        minSpinner.getValueFactory().setValue(prefs.getReminderMin());
        // 数据库路径
        File dbFile = new File(System.getProperty("user.home") + "/.dailycheck/dailycheck.db");
        dbPathLabel.setText(dbFile.getAbsolutePath());
    }

    @FXML
    private void onSaveStudyGoal() {
        try {
            int v = Integer.parseInt(studyGoalField.getText().trim());
            prefs.setDailyStudyGoal(Math.max(0, v));
            flashSuccess(studyGoalField);
        } catch (NumberFormatException e) {
            flashFieldError(studyGoalField);
        }
    }

    @FXML
    private void onSaveExpenseLimit() {
        try {
            double v = Double.parseDouble(expenseLimitField.getText().trim());
            prefs.setDailyExpenseLimit(Math.max(0d, v));
            flashSuccess(expenseLimitField);
        } catch (NumberFormatException e) {
            flashFieldError(expenseLimitField);
        }
    }

    @FXML
    private void onSaveReminder() {
        prefs.setReminderEnabled(reminderCheck.isSelected());
        prefs.setReminderTime(
                hourSpinner.getValue() == null ? 21 : hourSpinner.getValue(),
                minSpinner.getValue() == null ? 0 : minSpinner.getValue());
        // 简单视觉反馈
        reminderCheck.setStyle("-fx-text-fill: #4CAF89;");
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.seconds(2),
                e -> reminderCheck.setStyle(null))).play();
    }

    /** 输入框校验失败闪烁 */
    private void flashFieldError(TextField field) {
        field.setStyle("-fx-border-color: #E53935; -fx-background-radius: 8; -fx-border-radius: 8;");
        field.requestFocus();
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.seconds(2),
                e -> field.setStyle(null))).play();
    }

    /** 保存成功后的短暂绿色边框反馈 */
    private void flashSuccess(TextField field) {
        field.setStyle("-fx-border-color: #4ECDC4; -fx-background-radius: 8; -fx-border-radius: 8;");
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(Duration.seconds(2),
                e -> field.setStyle(null))).play();
    }
}
