package com.dailycheck.controllers;

import com.dailycheck.DatabaseHelper;
import com.dailycheck.DatabaseHelper.SubjectTotal;
import com.dailycheck.DatabaseHelper.CategoryTotal;
import com.dailycheck.charts.SimpleBarChart;
import com.dailycheck.charts.SimplePieChart;
import com.dailycheck.utils.DateUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计 Tab 控制器
 *
 * 功能：
 * - 周期切换（本周 / 本月）
 * - 概览卡片：任务完成率、学习时长、支出金额、收入金额、结余
 * - 学习时长柱状图（按科目）
 * - 支出占比饼图（按分类）
 */
public class StatsController {

    @FXML private ToggleButton weekToggle;
    @FXML private ToggleButton monthToggle;
    @FXML private Label rangeLabel;

    @FXML private Label taskRateLabel;
    @FXML private Label taskCountSummary;
    @FXML private Label studyTotalLabel;
    @FXML private Label studyAvgLabel;
    @FXML private Label expenseTotalLabel;
    @FXML private Label expenseAvgLabel;
    @FXML private Label incomeTotalLabel;
    @FXML private Label balanceSummary;

    @FXML private SimpleBarChart barChart;
    @FXML private SimplePieChart pieChart;

    private final DatabaseHelper db = DatabaseHelper.get();
    private ToggleGroup periodGroup;
    private boolean monthly = true; // 默认本月

    @FXML
    public void initialize() {
        periodGroup = new ToggleGroup();
        weekToggle.setToggleGroup(periodGroup);
        monthToggle.setToggleGroup(periodGroup);
        monthToggle.setSelected(true);

        // 让图表自动随容器尺寸重绘
        setupChartResize();

        refresh();
    }

    public void onShown() {
        refresh();
    }

    @FXML
    private void onToggleWeek() {
        monthly = false;
        refresh();
    }

    @FXML
    private void onToggleMonth() {
        monthly = true;
        refresh();
    }

    /** 监听图表父容器尺寸变化以重绘 */
    private void setupChartResize() {
        // 监听父容器（图表的 parent）的宽高变化；Pane 继承自 Region 才有 widthProperty/heightProperty
        if (barChart != null) {
            barChart.parentProperty().addListener((obs, o, parent) -> {
                if (parent instanceof Region) {
                    Region r = (Region) parent;
                    r.widthProperty().addListener((obs2, oo, nn) -> barChart.draw());
                    r.heightProperty().addListener((obs2, oo, nn) -> barChart.draw());
                }
            });
        }
        if (pieChart != null) {
            pieChart.parentProperty().addListener((obs, o, parent) -> {
                if (parent instanceof Region) {
                    Region r = (Region) parent;
                    r.widthProperty().addListener((obs2, oo, nn) -> pieChart.draw());
                    r.heightProperty().addListener((obs2, oo, nn) -> pieChart.draw());
                }
            });
        }
    }

    /** 刷新统计 */
    private void refresh() {
        String today = DateUtils.today();
        String[] range = monthly ? DateUtils.monthRange(today) : DateUtils.weekRange(today);
        String start = range[0];
        String end = range[1];

        // 周期标签
        rangeLabel.setText((monthly ? "本月：" : "本周：") + DateUtils.toChinese(start)
                + " ~ " + DateUtils.toChinese(end));

        // 任务完成率
        int total = db.getTotalCountRange(start, end);
        int done = db.getCompletedCountRange(start, end);
        double rate = total > 0 ? (done * 100.0 / total) : 0;
        taskRateLabel.setText(String.format("%.0f%%", rate));
        taskCountSummary.setText(done + " / " + total + " 项");

        // 学习时长
        int studyMinutes = db.getStudyTotalMinutes(start, end);
        studyTotalLabel.setText(String.valueOf(studyMinutes));
        int days = countDays(start, end);
        int avg = days > 0 ? studyMinutes / days : 0;
        studyAvgLabel.setText("日均 " + avg + " 分钟");

        // 收支
        double expense = db.getTotalExpense(start, end);
        double income = db.getTotalIncome(start, end);
        double balance = income - expense;
        expenseTotalLabel.setText("¥" + String.format("%.2f", expense));
        expenseAvgLabel.setText("日均 ¥" + String.format("%.2f", days > 0 ? expense / days : 0));
        incomeTotalLabel.setText("¥" + String.format("%.2f", income));
        balanceSummary.setText("结余 ¥" + String.format("%.2f", balance));

        // 柱状图：按科目统计学习时长
        List<SubjectTotal> subjects = db.getStudyMinutesBySubject(start, end);
        List<String> subjectLabels = new ArrayList<>();
        List<Integer> subjectValues = new ArrayList<>();
        for (SubjectTotal st : subjects) {
            subjectLabels.add(st.subject);
            subjectValues.add(st.total);
        }
        barChart.setUnit("分钟");
        barChart.setEmptyText("当前周期内暂无学习记录");
        barChart.setData(subjectLabels, subjectValues);

        // 饼图：按分类统计支出
        List<CategoryTotal> categories = db.getExpenseByCategory(start, end);
        List<String> catLabels = new ArrayList<>();
        List<Double> catValues = new ArrayList<>();
        for (CategoryTotal ct : categories) {
            catLabels.add(ct.category);
            catValues.add(ct.total);
        }
        pieChart.setUnit("元");
        pieChart.setEmptyText("当前周期内暂无支出记录");
        pieChart.setData(catLabels, catValues);

        // 主动重绘一次（首次加载时尺寸已就绪）
        barChart.draw();
        pieChart.draw();
    }

    /** 计算 [start, end] 区间天数（含两端） */
    private static int countDays(String start, String end) {
        long diff = DateUtils.parse(end).getTime() - DateUtils.parse(start).getTime();
        if (diff < 0) return 1;
        return (int) (diff / (1000L * 60 * 60 * 24)) + 1;
    }
}
