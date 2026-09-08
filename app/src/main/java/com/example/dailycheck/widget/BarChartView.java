package com.example.dailycheck.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.dailycheck.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 简易柱状图：自包含、无第三方依赖
 * 传入标签与数值即可渲染
 */
public class BarChartView extends View {

    private static final int[] PALETTE = {
            0xFF5B8FF9, 0xFF5AD8A6, 0xFFF6BD16, 0xFFE86452,
            0xFF6DC8EC, 0xFF945FB9, 0xFFFF9845, 0xFF1E9493
    };

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<String> labels = new ArrayList<>();
    private List<Float> values = new ArrayList<>();

    private float max = 1f;
    private int barColor;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        barColor = getResources().getColor(R.color.primary, null);
        float density = getResources().getDisplayMetrics().density;
        textPaint.setTextSize(11 * density);
        textPaint.setColor(0xFF888888);
        valuePaint.setTextSize(11 * density);
        valuePaint.setColor(0xFF444444);
    }

    /**
     * 设置数据，每个柱子使用调色板循环取色
     */
    public void setData(List<String> labels, List<Float> values) {
        this.labels = labels == null ? new ArrayList<>() : labels;
        this.values = values == null ? new ArrayList<>() : values;
        this.max = 1f;
        for (float v : this.values) {
            if (v > max) max = v;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.size();
        if (n == 0) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        float padBottom = 28f * getResources().getDisplayMetrics().density;
        float topPad = 16f * getResources().getDisplayMetrics().density;
        float usableH = h - padBottom - topPad;
        float gap = 12f * getResources().getDisplayMetrics().density;
        float barWidth = (w - gap * (n + 1)) / n;

        for (int i = 0; i < n; i++) {
            float v = values.get(i);
            float ratio = max <= 0 ? 0 : v / max;
            float barH = ratio * usableH;
            float left = gap + i * (barWidth + gap);
            float top = topPad + (usableH - barH);
            float right = left + barWidth;
            float bottom = topPad + usableH;
            barPaint.setColor(PALETTE[i % PALETTE.length]);
            canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, barPaint);

            // 数值标签
            String valText = formatValue(v);
            float textW = valuePaint.measureText(valText);
            canvas.drawText(valText, left + (barWidth - textW) / 2f, top - 4f, valuePaint);

            // x 轴标签
            String label = labels.get(i);
            float labelW = textPaint.measureText(label);
            canvas.drawText(label, left + (barWidth - labelW) / 2f, bottom + 18f, textPaint);
        }
    }

    private String formatValue(float v) {
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        return String.format(java.util.Locale.CHINA, "%.1f", v);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 默认给定一个高度
        int desired = (int) (180 * getResources().getDisplayMetrics().density);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
