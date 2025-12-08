package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;

/**
 * Quality Heatmap:
 * Hour × Day grid showing study quality
 * -> Similar to GitHub contribution graph
 */
public class QualityHeatmapChartView extends View {

    private Paint cellPaint;
    private Paint borderPaint;
    private Paint textPaint;
    private Paint titlePaint;

    private List<HeatmapCell> cells;

    private float cellSize = 40f;
    private float cellSpacing = 4f;
    private float padding = 80f;
    private float topPadding = 100f;

    // Theme colors
    private int colorGrid;
    private int colorTextPrimary;
    private int colorTextSecondary;

    public QualityHeatmapChartView(Context context) {
        super(context);
        init();
    }

    public QualityHeatmapChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        Context context = getContext();
        colorGrid = ContextCompat.getColor(context, R.color.analytics_grid_line_major);
        colorTextPrimary = ContextCompat.getColor(context, R.color.analytics_text_primary);
        colorTextSecondary = ContextCompat.getColor(context, R.color.analytics_text_secondary);

        cellPaint = new Paint();
        cellPaint.setStyle(Paint.Style.FILL);
        cellPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(colorGrid);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f);
        borderPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(colorTextSecondary);
        textPaint.setTextSize(28f);
        textPaint.setAntiAlias(true);

        titlePaint = new Paint();
        titlePaint.setColor(colorTextPrimary);
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);
    }

    public void setData(List<HeatmapCell> cells) {
        this.cells = cells;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cells == null || cells.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        drawHourLabels(canvas);
        drawDayLabels(canvas);
        drawHeatmap(canvas);
        drawLegend(canvas);
    }

    private void drawHourLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.RIGHT);

        for (int hour = 0; hour < 24; hour += 2) {
            float y = topPadding + (hour * (cellSize + cellSpacing)) + cellSize / 2 + 10;
            String label = String.format(Locale.getDefault(), "%02d:00", hour);
            canvas.drawText(label, padding - 10, y, textPaint);
        }
    }

    private void drawDayLabels(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Get unique days from cells
        int minDay = Integer.MAX_VALUE;
        int maxDay = Integer.MIN_VALUE;

        for (HeatmapCell cell : cells) {
            if (cell.getDayOfMonth() < minDay) minDay = cell.getDayOfMonth();
            if (cell.getDayOfMonth() > maxDay) maxDay = cell.getDayOfMonth();
        }

        int dayCount = maxDay - minDay + 1;

        for (int i = 0; i < dayCount; i++) {
            int day = minDay + i;
            float x = padding + (i * (cellSize + cellSpacing)) + cellSize / 2;
            canvas.drawText(String.valueOf(day), x, topPadding - 20, textPaint);
        }
    }

    private void drawHeatmap(Canvas canvas) {
        for (HeatmapCell cell : cells) {
            if (cell.getSessionCount() == 0) continue;

            int dayIndex = cell.getDayOfMonth() - getMinDay();
            int hourIndex = cell.getHour();

            float x = padding + (dayIndex * (cellSize + cellSpacing));
            float y = topPadding + (hourIndex * (cellSize + cellSpacing));

            // Determine color based on quality
            int color = getColorForQuality(cell.getAvgQuality());
            cellPaint.setColor(color);

            // Draw cell
            RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
            canvas.drawRoundRect(rect, 8f, 8f, cellPaint);
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint);
        }
    }

    private void drawLegend(Canvas canvas) {
        float legendY = getHeight() - 60;
        float legendX = padding;

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(28f);
        textPaint.setColor(colorTextSecondary);
        canvas.drawText("Quality: ", legendX, legendY + 25, textPaint);

        legendX += 120;

        String[] labels = {"Low", "Medium", "High", "Excellent"};
        float[] qualities = {25f, 50f, 75f, 95f};

        for (int i = 0; i < labels.length; i++) {
            int color = getColorForQuality(qualities[i]);
            cellPaint.setColor(color);

            RectF rect = new RectF(legendX, legendY, legendX + 30, legendY + 30);
            canvas.drawRoundRect(rect, 4f, 4f, cellPaint);
            canvas.drawRoundRect(rect, 4f, 4f, borderPaint);

            textPaint.setTextSize(24f);
            canvas.drawText(labels[i], legendX + 40, legendY + 22, textPaint);

            legendX += 150;
        }
    }

    private int getColorForQuality(float quality) {
        Context context = getContext();

        if (quality >= 85) {
            return ContextCompat.getColor(context, R.color.chart_scale_7);
        }
        else if (quality >= 70) {
            return ContextCompat.getColor(context, R.color.chart_scale_5);
        }
        else if (quality >= 50) {
            return ContextCompat.getColor(context, R.color.chart_scale_3);
        }
        else if (quality >= 30) {
            return ContextCompat.getColor(context, R.color.chart_scale_2);
        }
        else {
            return ContextCompat.getColor(context, R.color.chart_scale_0);
        }
    }

    private int getMinDay() {
        if (cells == null || cells.isEmpty()) return 1;

        int min = Integer.MAX_VALUE;
        for (HeatmapCell cell : cells) {
            if (cell.getDayOfMonth() < min) {
                min = cell.getDayOfMonth();
            }
        }
        return min;
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyPaint = new Paint(textPaint);
        emptyPaint.setColor(ContextCompat.getColor(getContext(), R.color.empty_state_text));
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyPaint.setTextSize(40f);

        String text = "No heatmap data";
        canvas.drawText(text, getWidth() / 2f, getHeight() / 2f, emptyPaint);
    }
}