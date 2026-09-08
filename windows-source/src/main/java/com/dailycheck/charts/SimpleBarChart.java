package com.dailycheck.charts;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单柱状图（基于 JavaFX Canvas 自绘，不依赖外部图表库）
 * 移植自 Android 端 BarChartView。
 * 用于展示「按科目统计的学习时长」。
 *
 * 用法：调用 setData(labels, values) 后自动重绘；条目颜色由 PALETTE 轮转。
 */
public class SimpleBarChart extends Canvas {

    /** 配色板，与主色 #5B6CFF 相协调 */
    private static final Color[] PALETTE = {
            Color.web("#5B6CFF"),
            Color.web("#4ECDC4"),
            Color.web("#FFB84D"),
            Color.web("#FF6B81"),
            Color.web("#7ED957"),
            Color.web("#9D7BFF"),
            Color.web("#FF8C42"),
            Color.web("#36CFC9")
    };

    private static final Color TEXT_COLOR = Color.web("#3A3F5C");
    private static final Color AXIS_COLOR = Color.web("#E0E2EB");
    private static final Color VALUE_COLOR = Color.web("#5A6275");

    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private String unit = "分钟";
    private String emptyText = "暂无数据";

    public SimpleBarChart() {
        widthProperty().addListener((obs, o, n) -> draw());
        heightProperty().addListener((obs, o, n) -> draw());
    }

    /** 设置数据并重绘 */
    public void setData(List<String> labels, List<? extends Number> values) {
        this.labels.clear();
        this.values.clear();
        if (labels != null) {
            this.labels.addAll(labels);
        }
        if (values != null) {
            for (Number v : values) {
                this.values.add(v == null ? 0d : v.doubleValue());
            }
        }
        draw();
    }

    /** 设置单位标签（例如「分钟」「元」） */
    public void setUnit(String unit) {
        this.unit = unit;
        draw();
    }

    /** 空数据时显示的提示文案 */
    public void setEmptyText(String text) {
        this.emptyText = text;
        draw();
    }

    /** 让 Canvas 真正按其布局尺寸绘制 */
    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    /** 重绘 */
    public void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // 空数据
        if (labels.isEmpty() || values.isEmpty() || maxValue() <= 0) {
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font(14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(emptyText, w / 2.0, h / 2.0);
            return;
        }

        // 边距
        double padLeft = 56;
        double padRight = 20;
        double padTop = 24;
        double padBottom = 48;
        double chartW = Math.max(1, w - padLeft - padRight);
        double chartH = Math.max(1, h - padTop - padBottom);

        double maxV = maxValue();
        // 上取整一个漂亮刻度
        double niceMax = niceCeil(maxV);

        int n = labels.size();
        // 条宽与间隙
        double groupW = chartW / n;
        double barW = Math.min(groupW * 0.55, 60);
        if (barW < 6) barW = 6;

        // Y 轴刻度（5 段）
        gc.setStroke(AXIS_COLOR);
        gc.setLineWidth(1);
        gc.setFill(VALUE_COLOR);
        gc.setFont(Font.font(11));
        gc.setTextAlign(TextAlignment.RIGHT);
        int steps = 4;
        for (int i = 0; i <= steps; i++) {
            double v = niceMax * i / steps;
            double y = padTop + chartH - (v / niceMax) * chartH;
            gc.strokeLine(padLeft, y, padLeft + chartW, y);
            gc.fillText(formatNumber(v), padLeft - 6, y + 4);
        }

        // X 轴主线
        gc.setStroke(Color.web("#C0C4CC"));
        gc.setLineWidth(1.2);
        gc.strokeLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);

        // 柱子
        gc.setTextAlign(TextAlignment.CENTER);
        for (int i = 0; i < n; i++) {
            double v = values.get(i);
            double bh = v > 0 ? (v / niceMax) * chartH : 0;
            double bx = padLeft + i * groupW + (groupW - barW) / 2.0;
            double by = padTop + chartH - bh;

            // 渐变色填充（手画：用主色 + 半透明白色叠加）
            Color c = PALETTE[i % PALETTE.length];
            gc.setFill(c);
            gc.fillRect(bx, by, barW, bh);
            // 顶部高光
            gc.setFill(Color.rgb(255, 255, 255, 0.18));
            gc.fillRect(bx, by, barW, Math.min(6, bh));

            // 数值标签
            if (v > 0) {
                gc.setFill(TEXT_COLOR);
                gc.setFont(Font.font(12));
                gc.fillText(formatNumber(v), bx + barW / 2.0, by - 6);
            }

            // X 轴标签
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font(11));
            String lbl = ellipsize(labels.get(i), 6);
            gc.fillText(lbl, bx + barW / 2.0, padTop + chartH + 18);
        }

        // 单位
        gc.setFill(VALUE_COLOR);
        gc.setFont(Font.font(11));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("单位：" + unit, padLeft, padTop - 8);
    }

    private double maxValue() {
        double m = 0;
        for (Double v : values) {
            if (v != null && v > m) m = v;
        }
        return m;
    }

    /** 将数值上取整为一个「漂亮」的刻度上限 */
    private static double niceCeil(double v) {
        if (v <= 0) return 1;
        // 取整数数量级
        double pow = Math.pow(10, Math.floor(Math.log10(v)));
        double n = v / pow;
        double nice;
        if (n <= 1) nice = 1;
        else if (n <= 2) nice = 2;
        else if (n <= 5) nice = 5;
        else nice = 10;
        double result = nice * pow;
        // 至少留 10% 顶部空间
        if (result <= v * 1.05) {
            result = nice * pow * (nice == 10 ? 1.5 : (nice == 5 ? 2 : 2));
        }
        return result;
    }

    private static String formatNumber(double v) {
        if (v == 0) return "0";
        if (v < 10) {
            // 整数则无小数
            if (Math.abs(v - Math.round(v)) < 1e-6) return String.valueOf((long) v);
            return String.format("%.1f", v);
        }
        return String.valueOf((long) v);
    }

    private static String ellipsize(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(1, max - 1)) + "…";
    }
}
