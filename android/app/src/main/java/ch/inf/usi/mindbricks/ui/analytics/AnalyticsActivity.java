package ch.inf.usi.mindbricks.ui.analytics;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.DailyRecommendation;
import ch.inf.usi.mindbricks.model.StudySession;
import ch.inf.usi.mindbricks.model.TimeSlotStats;
import ch.inf.usi.mindbricks.model.WeeklyStats;
import ch.inf.usi.mindbricks.ui.charts.SessionHistoryAdapter;
import ch.inf.usi.mindbricks.util.DataProcessor;
import ch.inf.usi.mindbricks.util.MockDataGenerator;

/**
 * Activity that displays comprehensive study analytics
 * SAFE VERSION: Doesn't crash if chart views are missing
 */
public class AnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "AnalyticsActivity";

    // Chart views (may be null if layouts don't exist yet)
    private View dailyTimelineChart;
    private View weeklyFocusChart;
    private View hourlyDistributionChart;

    private RecyclerView sessionHistoryRecycler;
    private SessionHistoryAdapter sessionAdapter;
    private ProgressBar loadingProgress;
    private LinearLayout chartContainer;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate started");

        try {
            setContentView(R.layout.activity_analytics);
            Log.d(TAG, "Layout inflated successfully");

            initViews();
            setupRecyclerView();
            loadData();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            showError("Failed to initialize Analytics: " + e.getMessage());
        }
    }

    private void initViews() {
        Log.d(TAG, "Initializing views...");

        try {
            // Try to find chart views (but don't crash if they don't exist)
            dailyTimelineChart = findViewById(R.id.dailyTimelineChart);
            weeklyFocusChart = findViewById(R.id.weeklyFocusChart);
            hourlyDistributionChart = findViewById(R.id.hourlyDistributionChart);

            sessionHistoryRecycler = findViewById(R.id.sessionHistoryRecycler);
            loadingProgress = findViewById(R.id.loadingProgress);
            chartContainer = findViewById(R.id.chartContainer);

            // Log which views were found
            Log.d(TAG, "Daily chart: " + (dailyTimelineChart != null ? "found" : "missing"));
            Log.d(TAG, "Weekly chart: " + (weeklyFocusChart != null ? "found" : "missing"));
            Log.d(TAG, "Hourly chart: " + (hourlyDistributionChart != null ? "found" : "missing"));
            Log.d(TAG, "Recycler: " + (sessionHistoryRecycler != null ? "found" : "missing"));

        } catch (Exception e) {
            Log.e(TAG, "Error finding views", e);
        }
    }

    private void setupRecyclerView() {
        if (sessionHistoryRecycler == null) {
            Log.w(TAG, "RecyclerView not found in layout");
            return;
        }

        try {
            sessionAdapter = new SessionHistoryAdapter(this::showSessionDetails);
            sessionHistoryRecycler.setLayoutManager(new LinearLayoutManager(this));
            sessionHistoryRecycler.setAdapter(sessionAdapter);
            Log.d(TAG, "RecyclerView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }

    private void loadData() {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Loading data...");

                // Load sessions
                List<StudySession> allSessions = loadSessionsFromDatabase();
                List<StudySession> recentSessions = DataProcessor.getRecentSessions(allSessions, 30);

                Log.d(TAG, "Loaded " + recentSessions.size() + " sessions");

                // Calculate statistics
                DailyRecommendation dailyRec = DataProcessor.generateDailyRecommendation(recentSessions);
                List<WeeklyStats> weeklyStats = DataProcessor.calculateWeeklyStats(recentSessions);
                List<TimeSlotStats> hourlyStats = DataProcessor.calculateHourlyStats(recentSessions);

                // Update UI on main thread
                runOnUiThread(() -> {
                    try {
                        updateCharts(dailyRec, weeklyStats, hourlyStats);

                        if (sessionAdapter != null) {
                            sessionAdapter.setData(recentSessions);
                            Log.d(TAG, "Session data updated");
                        }

                        Toast.makeText(this, "Loaded " + recentSessions.size() + " sessions", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI", e);
                        showError("Error displaying data: " + e.getMessage());
                    } finally {
                        if (loadingProgress != null) {
                            loadingProgress.setVisibility(View.GONE);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading data", e);
                runOnUiThread(() -> {
                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    showError("Failed to load data: " + e.getMessage());
                });
            }
        }).start();
    }

    private void updateCharts(DailyRecommendation dailyRec,
                              List<WeeklyStats> weeklyStats,
                              List<TimeSlotStats> hourlyStats) {

        Log.d(TAG, "Updating charts...");

        // Try to update each chart with reflection to avoid crashes
        updateChartSafely(dailyTimelineChart, "setData", dailyRec);
        updateChartSafely(weeklyFocusChart, "setData", weeklyStats);
        updateChartSafely(hourlyDistributionChart, "setData", hourlyStats);
    }

    /**
     * Safely call setData on a chart view using reflection
     * Won't crash if the view doesn't have the method
     */
    private void updateChartSafely(View chartView, String methodName, Object data) {
        if (chartView == null) {
            Log.w(TAG, "Chart view is null, skipping update");
            return;
        }

        try {
            java.lang.reflect.Method method = chartView.getClass().getMethod(methodName, data.getClass());
            method.invoke(chartView, data);
            Log.d(TAG, "Updated " + chartView.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update chart: " + chartView.getClass().getSimpleName(), e);
            // Don't crash, just log the error
        }
    }

    private List<StudySession> loadSessionsFromDatabase() {
        // TODO: Replace with actual Room database query
        Log.d(TAG, "Generating mock sessions...");
        return MockDataGenerator.generateMockSessions(50);
    }

    private void showSessionDetails(StudySession session) {
        if (session == null) {
            return;
        }

        try {
            // Build detailed session information
            StringBuilder details = new StringBuilder();
            details.append("📅 Session Details\n\n");

            // Time information
            details.append("⏰ Time\n");
            details.append("Started: ").append(dateFormat.format(session.getStartTime())).append("\n");
            details.append("Ended: ").append(dateFormat.format(session.getEndTime())).append("\n");
            details.append("Duration: ").append(session.getDurationMinutes()).append(" minutes\n");
            details.append("Completed: ").append(session.isSessionCompleted() ? "Yes ✓" : "No ✗").append("\n\n");

            // Performance metrics
            details.append("📊 Performance\n");
            details.append("Productivity Score: ").append(session.getProductivityScore()).append("%\n");
            details.append("AI Estimated Score: ").append(session.getAiEstimatedProdScore()).append("%\n");
            details.append("Self-Rated Focus: ").append(session.getSelfRatedFocus()).append("/10\n");
            details.append("Difficulty: ").append(session.getPerceivedDifficulty()).append("/5\n");
            details.append("Mood: ").append(session.getMood()).append("/5\n");
            details.append("Distractions: ").append(session.getDistractions()).append("\n\n");

            // Environmental factors
            details.append("🌍 Environment\n");
            details.append("Location: ").append(capitalizeFirst(session.getUserLocation())).append("\n");
            details.append("Noise Level: ").append(String.format(Locale.getDefault(), "%.1f dB", session.getNoiseAvgDb()));
            details.append(" (").append(categorizeNoise(session.getNoiseAvgDb())).append(")\n");
            details.append("Light Level: ").append(String.format(Locale.getDefault(), "%.0f lux", session.getLightLuxAvg()));
            details.append(" (").append(categorizeLight(session.getLightLuxAvg())).append(")\n");
            details.append("Phone Pickups: ").append(session.getPhonePickups()).append("\n\n");

            // Notes
            if (session.getNotes() != null && !session.getNotes().isEmpty()) {
                details.append("📝 Notes\n");
                details.append(session.getNotes()).append("\n");
            }

            // Show dialog
            new AlertDialog.Builder(this)
                    .setTitle("Study Session")
                    .setMessage(details.toString())
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing session details", e);
            Toast.makeText(this, "Error displaying session details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Also show in a TextView for longer visibility
        TextView errorView = new TextView(this);
        errorView.setText("❌ Error\n\n" + message + "\n\nCheck Logcat for details.");
        errorView.setTextSize(16);
        errorView.setPadding(32, 32, 32, 32);
        setContentView(errorView);
    }

    // Helper methods
    private String categorizeNoise(float noiseDb) {
        if (noiseDb < 30) return "Quiet";
        if (noiseDb < 45) return "Moderate";
        if (noiseDb < 60) return "Loud";
        return "Very Loud";
    }

    private String categorizeLight(float lightLux) {
        if (lightLux < 50) return "Dark";
        if (lightLux < 200) return "Dim";
        if (lightLux < 400) return "Bright";
        return "Very Bright";
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}