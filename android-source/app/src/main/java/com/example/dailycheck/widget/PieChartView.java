package com.example.dailycheck.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 简易饼图：自包含、无第三方依赖
 * 传入标签与数值即可渲染，支持图例
 */
public class PieChartView extends View {

    private static final int[] PALETTE = {
            0xFF5B8FF9, 0xFF5AD8A6, 0xFFF6BD16, 0xFFE86452,
            0xFF6DC8EC, 0xFF945FB9, 0xFFFF9845, 0xFF1E9493,
            0xFFFF99C3, 0xFF8D7B68
    };

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private List<String> labels = new ArrayList<>();
    private List<Float> values = new ArrayList<>();
    private float total = 0f;
    private String centerText = "";

    public PieChartView(Context context) {
        this(context, null);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        labelPaint.setTextSize(11 * density);
        labelPaint.setColor(0xFF666666);
        centerPaint.setTextSize(16 * density);
        centerPaint.setColor(0xFF333333);
        centerPaint.setFakeBoldText(true);
        legendPaint.setTextSize(11 * density);
        legendPaint.setColor(0xFF666666);
    }

    /**
     * 设置饼图数据
     * @param labels 分类标签
     * @param values 数值
     * @param centerText 圆心文字（如总计）
     */
    public void setData(List<String> labels, List<Float> values, String centerText) {
        this.labels = labels == null ? new ArrayList<>() : labels;
        this.values = values == null ? new ArrayList<>() : values;
        this.total = 0f;
        for (float v : this.values) this.total += v;
        this.centerText = centerText == null ? "" : centerText;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.size();
        if (n == 0 || total <= 0) {
            labelPaint.setColor(0xFF999999);
            canvas.drawText("暂无数据", getWidth() / 2f - labelPaint.measureText("暂无数据") / 2f,
                    getHeight() / 2f, labelPaint);
            return;
        }

        int w = getWidth();
        int h = getHeight();
        float density = getResources().getDisplayMetrics().density;

        // 饼图在左半区，图例在右半区
        float pieSize = Math.min(w * 0.5f, h - 16 * density);
        float left = (w * 0.5f - pieSize) / 2f + 8 * density;
        float top = (h - pieSize) / 2f;
        arcRect.set(left, top, left + pieSize, top + pieSize);

        float startAngle = -90f;
        for (int i = 0; i < n; i++) {
            float v = values.get(i);
            if (v <= 0) continue;
            float sweep = v / total * 360f;
            slicePaint.setColor(PALETTE[i % PALETTE.length]);
            canvas.drawArc(arcRect, startAngle, sweep, true, slicePaint);
            startAngle += sweep;
        }

        // 圆心文字
        if (!centerText.isEmpty()) {
            float cx = arcRect.centerX();
            float cy = arcRect.centerY();
            float textW = centerPaint.measureText(centerText);
            canvas.drawText(centerText, cx - textW / 2f, cy + centerPaint.getTextSize() / 3f, centerPaint);
        }

        // 右侧图例
        float legendX = w * 0.5f + 12 * density;
        float legendY = top + 8 * density;
        float lineH = 20 * density;
        for (int i = 0; i < n; i++) {
            float v = values.get(i);
            if (v <= 0) continue;
            // 色块
            slicePaint.setColor(PALETTE[i % PALETTE.length]);
            canvas.drawRect(legendX, legendY, legendX + 10 * density, legendY + 10 * density, slicePaint);
            // 文字
            String label = labels.get(i);
            float pct = v / total * 100f;
            String text = String.format(java.util.Locale.CHINA, "%s  %.1f%%", label, pct);
            canvas.drawText(text, legendX + 14 * density, legendY + 9 * density, legendPaint);
            legendY += lineH;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int desired = (int) (200 * getResources().getDisplayMetrics().density);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
