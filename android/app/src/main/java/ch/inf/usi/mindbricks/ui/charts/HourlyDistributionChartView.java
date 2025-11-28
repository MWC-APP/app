package ch.inf.usi.mindbricks.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.TimeSlotStats;

/**
 * Custom view that displays hourly distribution of study sessions
 */
public class HourlyDistributionChartView extends LinearLayout {

    private TextView titleText;
    private LineChart lineChart;

    public HourlyDistributionChartView(Context context) {
        super(context);
        init(context);
    }

    public HourlyDistributionChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HourlyDistributionChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_hourly_distribution_chart, this, true);

        titleText = findViewById(R.id.hourlyDistributionTitle);
        lineChart = findViewById(R.id.hourlyDistributionLineChart);

        setupChart();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setPinchZoom(false);
        lineChart.setScaleEnabled(false);
        lineChart.getLegend().setEnabled(true);

        // Configure X axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setLabelRotationAngle(-45f);

        // Configure Y axes
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(12f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public void setData(List<TimeSlotStats> hourlyStats) {
        if (hourlyStats == null || hourlyStats.isEmpty()) {
            lineChart.clear();
            return;
        }

        List<Entry> minutesEntries = new ArrayList<>();
        List<Entry> focusEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) { // Only show hours with data
                float hours = stats.getTotalMinutes() / 60f;
                minutesEntries.add(new Entry(index, hours));
                focusEntries.add(new Entry(index, stats.getAvgFocusScore()));
                labels.add(String.format("%dh", stats.getHourOfDay()));
                index++;
            }
        }

        // Study hours line
        LineDataSet minutesDataSet = new LineDataSet(minutesEntries, "Study Hours");
        minutesDataSet.setColor(Color.parseColor("#2196F3")); // Blue
        minutesDataSet.setCircleColor(Color.parseColor("#2196F3"));
        minutesDataSet.setLineWidth(2f);
        minutesDataSet.setCircleRadius(4f);
        minutesDataSet.setDrawValues(false);
        minutesDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        // Focus score line
        LineDataSet focusDataSet = new LineDataSet(focusEntries, "Focus Score");
        focusDataSet.setColor(Color.parseColor("#4CAF50")); // Green
        focusDataSet.setCircleColor(Color.parseColor("#4CAF50"));
        focusDataSet.setLineWidth(2f);
        focusDataSet.setCircleRadius(4f);
        focusDataSet.setDrawValues(false);
        focusDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineData lineData = new LineData(minutesDataSet, focusDataSet);

        lineChart.setData(lineData);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setLabelCount(Math.min(12, labels.size()));
        lineChart.invalidate(); // Refresh
    }
}