package com.dailycheck.controllers;

import com.dailycheck.DatabaseHelper;
import com.dailycheck.models.ExpenseRecord;
import com.dailycheck.utils.DateUtils;
import com.dailycheck.utils.PrefsManager;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * 记账 Tab 控制器
 *
 * 功能：
 * - 日期导航
 * - 显示当日收入 / 支出 / 结余，及消费上限提示
 * - 新增收支记录（类型 / 分类 / 金额 / 备注）
 * - 列表展示当日记录，可删除
 */
public class ExpenseController {

    @FXML private Label dateLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expenseLabel;
    @FXML private Label balanceLabel;
    @FXML private Label limitHintLabel;

    @FXML private RadioButton expenseRadio;
    @FXML private RadioButton incomeRadio;
    @FXML private TextField categoryField;
    @FXML private TextField amountField;
    @FXML private TextField noteField;

    @FXML private VBox recordList;
    @FXML private Label recordCountLabel;

    private final DatabaseHelper db = DatabaseHelper.get();
    private String currentDate;
    private ToggleGroup typeGroup;

    @FXML
    public void initialize() {
        currentDate = DateUtils.today();
        typeGroup = new ToggleGroup();
        expenseRadio.setToggleGroup(typeGroup);
        incomeRadio.setToggleGroup(typeGroup);
        refresh();
    }

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

    /** 添加记录 */
    @FXML
    private void onAddRecord() {
        int type = expenseRadio.isSelected() ? ExpenseRecord.TYPE_EXPENSE : ExpenseRecord.TYPE_INCOME;
        String category = categoryField.getText();
        String note = noteField.getText();
        double amount = 0;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException e) {
            flashFieldError(amountField);
            return;
        }
        if (category == null || category.trim().isEmpty()) {
            flashFieldError(categoryField);
            return;
        }
        if (amount <= 0) {
            flashFieldError(amountField);
            return;
        }
        ExpenseRecord record = new ExpenseRecord(type, category.trim(), amount,
                note == null ? "" : note.trim(), currentDate);
        db.insertExpenseRecord(record);
        categoryField.clear();
        amountField.clear();
        noteField.clear();
        refresh();
    }

    /** 刷新当前日期视图 */
    private void refresh() {
        dateLabel.setText(DateUtils.toChinese(currentDate) + " " + DateUtils.weekdayChinese(currentDate));

        double income = db.getTotalIncome(currentDate, currentDate);
        double expense = db.getTotalExpense(currentDate, currentDate);
        double balance = income - expense;
        incomeLabel.setText("¥" + String.format("%.2f", income));
        expenseLabel.setText("¥" + String.format("%.2f", expense));
        balanceLabel.setText("¥" + String.format("%.2f", balance));
        // 结余着色
        if (balance > 0) {
            balanceLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #4CAF89;");
        } else if (balance < 0) {
            balanceLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #FF6B81;");
        } else {
            balanceLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #2A2E45;");
        }

        // 消费上限提示
        double limit = PrefsManager.get().getDailyExpenseLimit();
        if (limit > 0) {
            double ratio = expense / limit;
            if (ratio >= 1) {
                limitHintLabel.setText("已支出 ¥" + String.format("%.2f", expense) + "，超出上限 ¥" + String.format("%.2f", limit));
                limitHintLabel.setStyle("-fx-text-fill: #FF6B81; -fx-font-size: 12;");
            } else if (ratio >= 0.8) {
                limitHintLabel.setText("已支出 ¥" + String.format("%.2f", expense) + " / " + String.format("%.2f", limit) + "，接近上限");
                limitHintLabel.setStyle("-fx-text-fill: #FFB84D; -fx-font-size: 12;");
            } else {
                limitHintLabel.setText("已支出 ¥" + String.format("%.2f", expense) + " / 上限 ¥" + String.format("%.2f", limit));
                limitHintLabel.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 12;");
            }
        } else {
            limitHintLabel.setText("未设置上限，可在设置页配置");
            limitHintLabel.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 12;");
        }

        // 记录列表
        List<ExpenseRecord> records = db.getExpenseRecordsByDate(currentDate);
        recordCountLabel.setText("共 " + records.size() + " 条");
        recordList.getChildren().clear();
        if (records.isEmpty()) {
            Label empty = new Label("当日无收支记录");
            empty.setStyle("-fx-text-fill: #9AA0B5; -fx-font-size: 13; -fx-padding: 24 0 24 0;");
            empty.setAlignment(Pos.CENTER);
            empty.setMaxWidth(Double.MAX_VALUE);
            recordList.getChildren().add(empty);
            return;
        }
        for (ExpenseRecord r : records) {
            recordList.getChildren().add(buildExpenseRow(r));
        }
    }

    /** 构建收支记录条目 */
    private HBox buildExpenseRow(ExpenseRecord r) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-item");
        row.setPadding(new Insets(10, 14, 10, 14));

        // 类型标签
        Label typeLabel = new Label(r.isIncome() ? "收入" : "支出");
        typeLabel.setStyle(r.isIncome()
                ? "-fx-background-color: #E7F7EE; -fx-text-fill: #4CAF89; -fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12; -fx-font-weight: bold;"
                : "-fx-background-color: #FFEBEE; -fx-text-fill: #FF6B81; -fx-background-radius: 8; -fx-padding: 3 10 3 10; -fx-font-size: 12; -fx-font-weight: bold;");

        VBox info = new VBox(4);
        Label content = new Label((r.category == null ? "" : r.category)
                + (r.note == null || r.note.isEmpty() ? "" : " · " + r.note));
        content.setStyle("-fx-font-size: 14; -fx-text-fill: #2A2E45;");
        Label time = new Label(formatTime(r.createdAt));
        time.setStyle("-fx-font-size: 11; -fx-text-fill: #9AA0B5;");
        info.getChildren().addAll(content, time);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label amount = new Label((r.isIncome() ? "+" : "-") + "¥" + String.format("%.2f", r.amount));
        amount.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: " +
                (r.isIncome() ? "#4CAF89" : "#FF6B81") + ";");

        javafx.scene.control.Button del = new javafx.scene.control.Button("删除");
        del.getStyleClass().add("icon-btn");
        del.setOnAction(e -> {
            db.deleteExpenseRecord(r.id);
            refresh();
        });

        row.getChildren().addAll(typeLabel, info, spacer, amount, del);
        HBox.setHgrow(info, Priority.ALWAYS);
        return row;
    }

    private void flashFieldError(TextField field) {
        field.setStyle("-fx-border-color: #E53935; -fx-background-radius: 8; -fx-border-radius: 8;");
        field.requestFocus();
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2),
                e -> field.setStyle(null))).play();
    }

    private static String formatTime(long ts) {
        if (ts <= 0) return "";
        java.util.Date d = new java.util.Date(ts);
        return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA).format(d);
    }
}
