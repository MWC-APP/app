package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;

/**
 * Custom view that displays the "Energy Curve" - a smooth line showing
 * study quality throughout the day
 */
public class EnergyCurveChartView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint titlePaint;

    private List<HourlyQuality> dataPoints;

    private float chartHeight;
    private float chartWidth;
    private float padding = 80;
    private float topPadding = 100;

    // Theme colors
    private int colorGreen;
    private int colorGreenDark;
    private int colorGreenLight;
    private int colorGrid;
    private int colorTextPrimary;
    private int colorTextSecondary;

    public EnergyCurveChartView(Context context) {
        super(context);
        init();
    }

    public EnergyCurveChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Load theme colors
        Context context = getContext();
        colorGreen = ContextCompat.getColor(context, R.color.analytics_accent_green);
        colorGreenDark = ContextCompat.getColor(context, R.color.chart_scale_7);
        colorGreenLight = ContextCompat.getColor(context, R.color.chart_scale_1);
        colorGrid = ContextCompat.getColor(context, R.color.analytics_grid_line);
        colorTextPrimary = ContextCompat.getColor(context, R.color.analytics_text_primary);
        colorTextSecondary = ContextCompat.getColor(context, R.color.analytics_text_secondary);

        // Line paint
        linePaint = new Paint();
        linePaint.setColor(colorGreen);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setShadowLayer(4f, 0, 2f, Color.argb(64, 0, 0, 0));

        // Fill paint (gradient under curve)
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Point paint
        pointPaint = new Paint();
        pointPaint.setColor(colorGreenDark);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);
        pointPaint.setShadowLayer(4f, 0, 2f, Color.argb(64, 0, 0, 0));

        // Grid paint
        gridPaint = new Paint();
        gridPaint.setColor(colorGrid);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);

        // Text paint
        textPaint = new Paint();
        textPaint.setColor(colorTextSecondary);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);

        // Title paint
        titlePaint = new Paint();
        titlePaint.setColor(colorTextPrimary);
        titlePaint.setTextSize(48f);
        titlePaint.setAntiAlias(true);
        titlePaint.setFakeBoldText(true);
    }

    public void setData(List<HourlyQuality> data) {
        this.dataPoints = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints == null || dataPoints.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        chartWidth = getWidth() - (2 * padding);
        chartHeight = getHeight() - topPadding - padding;

        // Setup gradient for fill using theme colors
        LinearGradient gradient = new LinearGradient(
                0, topPadding,
                0, topPadding + chartHeight,
                Color.argb(128, Color.red(colorGreen), Color.green(colorGreen), Color.blue(colorGreen)),
                Color.argb(0, Color.red(colorGreen), Color.green(colorGreen), Color.blue(colorGreen)),
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);

        drawGrid(canvas);
        drawAxes(canvas);
        drawFillArea(canvas);
        drawCurve(canvas);
        drawPoints(canvas);
    }

    private void drawGrid(Canvas canvas) {
        // Horizontal lines (quality levels)
        for (int i = 0; i <= 4; i++) {
            float y = topPadding + (chartHeight * i / 4);
            canvas.drawLine(padding, y, padding + chartWidth, y, gridPaint);
        }

        // Vertical lines (every 3 hours)
        for (int hour = 0; hour <= 24; hour += 3) {
            float x = padding + (chartWidth * hour / 23);
            canvas.drawLine(x, topPadding, x, topPadding + chartHeight, gridPaint);
        }
    }

    private void drawAxes(Canvas canvas) {
        // X-axis labels (hours)
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int hour = 0; hour <= 24; hour += 3) {
            float x = padding + (chartWidth * hour / 23);
            String label = String.format(Locale.getDefault(), "%02d:00", hour);
            canvas.drawText(label, x, getHeight() - 20, textPaint);
        }

        // Y-axis labels (quality %)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 4; i++) {
            float y = topPadding + (chartHeight * i / 4) + 10;
            int quality = 100 - (i * 25);
            canvas.drawText(quality + "%", padding - 20, y, textPaint);
        }
    }

    private void drawFillArea(Canvas canvas) {
        Path fillPath = new Path();
        boolean firstPoint = true;

        for (HourlyQuality hq : dataPoints) {
            if (hq.getSessionCount() == 0) continue;

            float x = padding + (chartWidth * hq.getHour() / 23);
            float y = topPadding + (chartHeight * (100 - hq.getAvgQuality()) / 100);

            if (firstPoint) {
                fillPath.moveTo(x, y);
                firstPoint = false;
            } else {
                fillPath.lineTo(x, y);
            }
        }

        // Close the path to fill
        if (!firstPoint) {
            // Find last point's x
            HourlyQuality lastPoint = null;
            for (int i = dataPoints.size() - 1; i >= 0; i--) {
                if (dataPoints.get(i).getSessionCount() > 0) {
                    lastPoint = dataPoints.get(i);
                    break;
                }
            }

            if (lastPoint != null) {
                float lastX = padding + (chartWidth * lastPoint.getHour() / 23);
                fillPath.lineTo(lastX, topPadding + chartHeight);

                // Find first point's x
                HourlyQuality firstData = null;
                for (HourlyQuality hq : dataPoints) {
                    if (hq.getSessionCount() > 0) {
                        firstData = hq;
                        break;
                    }
                }

                if (firstData != null) {
                    float firstX = padding + (chartWidth * firstData.getHour() / 23);
                    fillPath.lineTo(firstX, topPadding + chartHeight);
                }
            }

            fillPath.close();
            canvas.drawPath(fillPath, fillPaint);
        }
    }

    private void drawCurve(Canvas canvas) {
        Path path = new Path();
        boolean firstPoint = true;

        for (HourlyQuality hq : dataPoints) {
            if (hq.getSessionCount() == 0) continue;

            float x = padding + (chartWidth * hq.getHour() / 23);
            float y = topPadding + (chartHeight * (100 - hq.getAvgQuality()) / 100);

            if (firstPoint) {
                path.moveTo(x, y);
                firstPoint = false;
            } else {
                path.lineTo(x, y);
            }
        }

        canvas.drawPath(path, linePaint);
    }

    private void drawPoints(Canvas canvas) {
        for (HourlyQuality hq : dataPoints) {
            if (hq.getSessionCount() == 0) continue;

            float x = padding + (chartWidth * hq.getHour() / 23);
            float y = topPadding + (chartHeight * (100 - hq.getAvgQuality()) / 100);

            // Draw outer circle
            canvas.drawCircle(x, y, 12f, pointPaint);

            // Draw inner circle (highlight)
            Paint innerPaint = new Paint(pointPaint);
            innerPaint.setColor(colorGreenLight);
            canvas.drawCircle(x, y, 6f, innerPaint);
        }
    }

    private void drawEmptyState(Canvas canvas) {
        Paint emptyTextPaint = new Paint(textPaint);
        emptyTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.empty_state_text));
        emptyTextPaint.setTextAlign(Paint.Align.CENTER);
        emptyTextPaint.setTextSize(40f);

        String text = "No data available";
        canvas.drawText(text, getWidth() / 2f, getHeight() / 2f, emptyTextPaint);

        emptyTextPaint.setTextSize(32f);
        canvas.drawText("Complete study sessions to see your energy curve",
                getWidth() / 2f, getHeight() / 2f + 50, emptyTextPaint);
    }
}