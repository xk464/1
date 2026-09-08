package com.dailycheck.charts;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单饼图（基于 JavaFX Canvas 自绘，不依赖外部图表库）
 * 移植自 Android 端 PieChartView。
 * 用于展示「按分类统计的支出占比」。
 *
 * 用法：调用 setData(labels, values) 后自动重绘；右侧绘制图例。
 */
public class SimplePieChart extends Canvas {

    /** 配色板 */
    private static final Color[] PALETTE = {
            Color.web("#5B6CFF"),
            Color.web("#FF6B81"),
            Color.web("#4ECDC4"),
            Color.web("#FFB84D"),
            Color.web("#7ED957"),
            Color.web("#9D7BFF"),
            Color.web("#FF8C42"),
            Color.web("#36CFC9")
    };

    private static final Color TEXT_COLOR = Color.web("#3A3F5C");
    private static final Color VALUE_COLOR = Color.web("#5A6275");

    private final List<String> labels = new ArrayList<>();
    private final List<Double> values = new ArrayList<>();
    private String unit = "元";
    private String emptyText = "暂无数据";

    public SimplePieChart() {
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

    public void setUnit(String unit) {
        this.unit = unit;
        draw();
    }

    public void setEmptyText(String text) {
        this.emptyText = text;
        draw();
    }

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
        if (labels.isEmpty() || values.isEmpty() || totalValue() <= 0) {
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font(14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(emptyText, w / 2.0, h / 2.0);
            return;
        }

        double total = totalValue();

        // 左侧饼图，右侧图例
        double pad = 16;
        double legendW = 160;
        double pieW = Math.max(60, w - legendW - pad * 2);
        double pieH = Math.max(60, h - pad * 2);
        double radius = Math.min(pieW, pieH) / 2.0;
        double cx = pad + pieW / 2.0;
        double cy = pad + pieH / 2.0;

        // 绘制扇形
        double startAngle = 90; // 从顶部开始
        int n = labels.size();
        for (int i = 0; i < n; i++) {
            double v = values.get(i);
            if (v <= 0) continue;
            double sweep = (v / total) * 360;
            Color c = PALETTE[i % PALETTE.length];

            // 扇形：JavaFX arc 角度顺时针为正；我们想让数据从顶部顺时针展开
            gc.setFill(c);
            gc.beginPath();
            gc.moveTo(cx, cy);
            // arc(cx, cy, radiusX, radiusY, startAngle, arcExtent, ArcClosure)
            gc.arc(cx, cy, radius, radius, startAngle, -sweep); // 负号让角度顺时针展开
            gc.closePath();
            gc.fill();

            // 扇形边界
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.stroke();

            // 扇形内百分比
            double midAngle = startAngle - sweep / 2;
            double pct = v / total * 100;
            if (pct >= 6) {
                double rad = Math.toRadians(midAngle);
                double lx = cx + Math.cos(rad) * (radius * 0.62);
                double ly = cy - Math.sin(rad) * (radius * 0.62);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font(12));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.format("%.0f%%", pct), lx, ly + 4);
            }

            startAngle -= sweep;
        }

        // 中心圆（环形效果）
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - radius * 0.42, cy - radius * 0.42, radius * 0.84, radius * 0.84);
        // 中心总计
        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font(13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("合计", cx, cy - 6);
        gc.setFont(Font.font(15));
        gc.fillText(formatNumber(total) + unit, cx, cy + 14);

        // 右侧图例
        double legendX = pad * 2 + pieW;
        double legendY = pad + 4;
        double lineH = 22;
        gc.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < n; i++) {
            double v = values.get(i);
            if (v <= 0) continue;
            double y = legendY + i * lineH;
            Color c = PALETTE[i % PALETTE.length];
            // 色块
            gc.setFill(c);
            gc.fillRect(legendX, y - 10, 12, 12);
            // 标签 + 数值
            gc.setFill(TEXT_COLOR);
            gc.setFont(Font.font(12));
            String label = labels.get(i) == null ? "" : labels.get(i);
            double pct = v / total * 100;
            String text = label + "  " + formatNumber(v) + unit + " (" + String.format("%.0f%%", pct) + ")";
            gc.fillText(ellipsize(text, 18), legendX + 18, y);
        }
    }

    private double totalValue() {
        double s = 0;
        for (Double v : values) {
            if (v != null) s += v;
        }
        return s;
    }

    private static String formatNumber(double v) {
        if (v == 0) return "0";
        if (Math.abs(v - Math.round(v)) < 1e-6) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }

    private static String ellipsize(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(1, max - 1)) + "…";
    }
}
