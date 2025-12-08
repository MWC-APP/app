package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.visual.DailyRecommendation;
import ch.inf.usi.mindbricks.model.visual.GoalRing;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.model.visual.StudySession;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.repository.StudySessionRepository;
import ch.inf.usi.mindbricks.util.database.DataProcessor;
/**
 * ViewModel for Analytics screen.
 */
public class AnalyticsViewModel extends AndroidViewModel {
    private static final String TAG = "AnalyticsViewModel";

    private final StudySessionRepository repository;
    private final Executor processingExecutor;

    //date ranges
    private DateRange currentDateRange;
    private final MutableLiveData<DateRange> dateRangeLiveData = new MutableLiveData<>();

    // LiveData for different chart types
    // MutableLiveData allows us to update values internally
    // External classes only see LiveData (read-only)
    private final MutableLiveData<WeeklyStats> weeklyStats = new MutableLiveData<>();
    private final MutableLiveData<List<TimeSlotStats>> hourlyStats = new MutableLiveData<>();
    private final MutableLiveData<DailyRecommendation> dailyRecommendation = new MutableLiveData<>();
    private final MutableLiveData<List<StudySession>> sessionHistory = new MutableLiveData<>();
    private final MutableLiveData<List<HourlyQuality>> energyCurveData = new MutableLiveData<>();
    private final MutableLiveData<List<HeatmapCell>> heatmapData = new MutableLiveData<>();
    private final MutableLiveData<List<StreakDay>> streakData = new MutableLiveData<>();
    private final MutableLiveData<List<GoalRing>> goalRingsData = new MutableLiveData<>();
    private final MutableLiveData<List<AIRecommendation>> aiRecommendations = new MutableLiveData<>();

    // ViewState for UI feedback
    private final MutableLiveData<ViewState> viewState = new MutableLiveData<>(ViewState.LOADING);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Cache for processed data to avoid reprocessing
    private List<StudySession> cachedSessions;
    private long lastLoadTime = 0;
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    // database handling
    private LiveData<List<StudySession>> sessionsSource;
    private final Observer<List<StudySession>> sessionsObserver = this::handleSessionsUpdate;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = new StudySessionRepository(application);
        this.processingExecutor = Executors.newSingleThreadExecutor();

        // Initialize with default range: Last 30 days
        this.currentDateRange = DateRange.lastNDays(30);
        this.dateRangeLiveData.setValue(currentDateRange);
    }


    // loading data
    public void loadData() {
        loadDataForRange(currentDateRange);
    }

    public void loadLastNDays(int days) {
        DateRange range = DateRange.lastNDays(days);
        loadDataForRange(range);
    }

    public void loadMonth(int year, int month) {
        DateRange range = DateRange.forMonth(year, month);
        loadDataForRange(range);
    }

    public void loadCurrentMonth() {
        DateRange range = DateRange.currentMonth();
        loadDataForRange(range);
    }

    public void loadAllTime() {
        DateRange range = DateRange.allTime();
        loadDataForRange(range);
    }

    public void loadDataForRange(DateRange dateRange) {
        if (dateRange == null) {
            Log.e(TAG, "Cannot load data with null DateRange");
            return;
        }

        // Check if range actually changed to avoid unnecessary work
        if (currentDateRange != null && currentDateRange.equals(dateRange)) {
            Log.d(TAG, "Range unchanged, skipping reload: " + dateRange.getDisplayName());
            return;
        }

        Log.d(TAG, "=== Loading data for range: " + dateRange.getDisplayName() + " ===");

        currentDateRange = dateRange;
        dateRangeLiveData.setValue(currentDateRange);

        viewState.setValue(ViewState.LOADING);

        long queryStartTime = calculateQueryStartTime(dateRange);

        Log.d(TAG, "Database query start time: " + queryStartTime);

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
        }

        sessionsSource = repository.getSessionsSince(queryStartTime);
        sessionsSource.observeForever(sessionsObserver);
    }

    // calculation

    private long calculateQueryStartTime(DateRange range) {
        // For ALL_TIME, query from epoch
        if (range.getRangeType() == DateRange.RangeType.ALL_TIME) {
            return 0L;
        }

        // For specific ranges, add a buffer but ensure we include the entire range
        long bufferMs = 7L * 24 * 60 * 60 * 1000;
        long queryStart = range.getStartTimestamp() - bufferMs;

        Log.d(TAG, "Query start: " + new Date(queryStart) +
                " (Range start: " + new Date(range.getStartTimestamp()) + ")");

        return Math.max(0, queryStart);
    }

    // navigation
    public void previousMonth() {
        if (currentDateRange.getRangeType() != DateRange.RangeType.SPECIFIC_MONTH) {
            Log.w(TAG, "previousMonth() only works for SPECIFIC_MONTH ranges");
            return;
        }

        try {
            DateRange prevMonth = currentDateRange.previousMonth();
            loadDataForRange(prevMonth);
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "Error navigating to previous month", e);
        }
    }

    public void nextMonth() {
        if (currentDateRange.getRangeType() != DateRange.RangeType.SPECIFIC_MONTH) {
            Log.w(TAG, "nextMonth() only works for SPECIFIC_MONTH ranges");
            return;
        }

        DateRange nextMonth = currentDateRange.nextMonth();
        loadDataForRange(nextMonth);
    }

    private void handleSessionsUpdate(List<StudySession> sessions) {
        Log.d(TAG, "Sessions updated from database: " +
                (sessions != null ? sessions.size() + " sessions" : "null"));

        if (sessions == null) {
            errorMessage.setValue("Error loading sessions from database");
            viewState.setValue(ViewState.ERROR);
            return;
        }

        cachedSessions = sessions;
        lastLoadTime = System.currentTimeMillis();

        List<StudySession> filteredSessions = DataProcessor.filterSessionsInRange(
                sessions, currentDateRange
        );

        if (filteredSessions.isEmpty()) {
            Log.w(TAG, "No sessions in current range: " + currentDateRange.getDisplayName());
            sessionHistory.setValue(filteredSessions);
            viewState.setValue(ViewState.EMPTY);
            return;
        }

        Log.d(TAG, String.format("Processing %d sessions (filtered from %d) for range: %s",
                filteredSessions.size(), sessions.size(),
                currentDateRange.getDisplayName()));

        processAllData(sessions, currentDateRange);
    }

    private void processAllData(List<StudySession> allSessions, DateRange dateRange) {
        Log.d(TAG, "=== processAllData START ===");
        Log.d(TAG, "All sessions count: " + (allSessions != null ? allSessions.size() : "null"));
        Log.d(TAG, "Date range: " + dateRange.getDisplayName());

        processingExecutor.execute(() -> {
            try {
                // Filter first to check if we have data
                List<StudySession> filtered = DataProcessor.filterSessionsInRange(
                        allSessions, dateRange);

                Log.d(TAG, "Filtered sessions: " + filtered.size());

                if (filtered.isEmpty()) {
                    Log.w(TAG, "No sessions in range, setting EMPTY state");
                    sessionHistory.postValue(filtered);
                    viewState.postValue(ViewState.EMPTY);
                    return; // Don't process if no data
                }

                // Process data
                WeeklyStats weekly = DataProcessor.calculateWeeklyStats(allSessions, dateRange);
                weeklyStats.postValue(weekly);
                Log.d(TAG, "Weekly stats computed: " + weekly.getTotalMinutes() + " mins");


                List<TimeSlotStats> hourly = DataProcessor.calculateHourlyDistribution(allSessions, dateRange);
                hourlyStats.postValue(hourly);
                Log.d(TAG, "Hourly stats computed: " + hourly.size() + " slots");

                DailyRecommendation recommendation = DataProcessor.generateDailyRecommendation(allSessions, dateRange);
                dailyRecommendation.postValue(recommendation);
                Log.d(TAG, "Recommendations computed");


                Log.d("ViewModel", "Calculating energy curve data...");
                List<HourlyQuality> energyCurve = DataProcessor.calculateEnergyCurve(filtered);
                energyCurveData.postValue(energyCurve);

                Log.d("ViewModel", "Calculating heatmap data...");
                List<HeatmapCell> heatmap = DataProcessor.calculateQualityHeatmap(filtered);
                heatmapData.postValue(heatmap);

                Log.d("ViewModel", "Calculating streak data...");
                Calendar cal = Calendar.getInstance();
                int currentMonth = cal.get(Calendar.MONTH);
                int currentYear = cal.get(Calendar.YEAR);

                List<StreakDay> streak = DataProcessor.calculateStreakCalendar(
                        allSessions,
                        60,
                        currentMonth,
                        currentYear
                );
                streakData.postValue(streak);

                List<StudySession> todaySessions = DataProcessor.filterSessionsInRange(
                        allSessions, DateRange.lastNDays(1));
                List<GoalRing> rings = DataProcessor.calculateGoalRings(todaySessions, 120, 70);
                goalRingsData.postValue(rings);

                List<AIRecommendation> recommendations = new ArrayList<>();
                recommendations.add(DataProcessor.generateAIRecommendations(filtered, dateRange));
                aiRecommendations.postValue(recommendations);

                sessionHistory.postValue(filtered);
                viewState.postValue(ViewState.SUCCESS);

                // Set SUCCESS only after all data is posted
                viewState.postValue(ViewState.SUCCESS);
                Log.d(TAG, "=== processAllData COMPLETE - ViewState.SUCCESS ===");

            } catch (Exception e) {
                Log.e(TAG, "ERROR in processAllData", e);
                errorMessage.postValue("Error processing data: " + e.getMessage());
                viewState.postValue(ViewState.ERROR);
            }
        });
    }

    public void deleteSession(StudySession session) {
        repository.deleteSession(session, this::refreshData);
    }

    // loaders

    public void loadWeeklyStats() {
        if (cachedSessions != null && isCacheValid()) {
            processingExecutor.execute(() -> {
                WeeklyStats stats = DataProcessor.calculateWeeklyStats(cachedSessions, currentDateRange);
                weeklyStats.postValue(stats);
            });
        } else {
            loadData();
        }
    }

    public void loadRecentSessions(int limit) {
        LiveData<List<StudySession>> sessionsLiveData = repository.getRecentSessions(limit);
        observeOnce(sessionsLiveData, sessionHistory::setValue);
    }
    public void refreshData() {
        Log.d(TAG, "Refreshing data (cache invalidated)");
        cachedSessions = null;
        lastLoadTime = 0;
        loadData();
    }

    // getters
    public LiveData<DateRange> getDateRange() {
        return dateRangeLiveData;
    }

    public DateRange getCurrentDateRange() {
        return currentDateRange;
    }

    public LiveData<WeeklyStats> getWeeklyStats() {
        return weeklyStats;
    }

    public LiveData<List<TimeSlotStats>> getHourlyStats() {
        return hourlyStats;
    }

    public LiveData<DailyRecommendation> getDailyRecommendation() {
        return dailyRecommendation;
    }

    public LiveData<List<StudySession>> getSessionHistory() {
        return sessionHistory;
    }

    public LiveData<ViewState> getViewState() {
        return viewState;
    }

    public LiveData<List<HourlyQuality>> getEnergyCurveData() {
        return energyCurveData;
    }

    public LiveData<List<HeatmapCell>> getHeatmapData() {
        return heatmapData;
    }

    public LiveData<List<StreakDay>> getStreakData() {
        return streakData;
    }

    public LiveData<List<GoalRing>> getGoalRingsData() {
        return goalRingsData;
    }

    public LiveData<List<AIRecommendation>> getAiRecommendations() {
        return aiRecommendations;
    }


    private boolean isCacheValid() {
        return cachedSessions != null &&
                (System.currentTimeMillis() - lastLoadTime) < CACHE_VALIDITY_MS;
    }

    private <T> void observeOnce(LiveData<T> liveData, OnDataCallback<T> callback) {
        liveData.observeForever(new Observer<T>() {
            @Override
            public void onChanged(T data) {
                liveData.removeObserver(this);
                callback.onData(data);
            }
        });
    }

    private interface OnDataCallback<T> {
        void onData(T data);
    }

    public enum ViewState {
        LOADING,
        SUCCESS,
        ERROR,
        EMPTY
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        Log.d(TAG, "ViewModel cleared");

        if (sessionsSource != null) {
            sessionsSource.removeObserver(sessionsObserver);
        }

        if (processingExecutor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) processingExecutor).shutdown();
        }
    }
}
