package ch.inf.usi.mindbricks.ui.nav.analytics;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import com.google.android.material.button.MaterialButton;
import ch.inf.usi.mindbricks.ui.settings.SettingsActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ch.inf.usi.mindbricks.R;
import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.ui.charts.AIRecommendationCardView;
import ch.inf.usi.mindbricks.ui.charts.DailyTimelineChartView;
import ch.inf.usi.mindbricks.ui.charts.GoalRingsView;
import ch.inf.usi.mindbricks.ui.charts.HourlyDistributionChartView;
import ch.inf.usi.mindbricks.ui.charts.QualityHeatmapChartView;
import ch.inf.usi.mindbricks.ui.charts.SessionHistoryAdapter;
import ch.inf.usi.mindbricks.ui.charts.StreakCalendarView;
import ch.inf.usi.mindbricks.ui.charts.TagUsageChartView;
import ch.inf.usi.mindbricks.ui.charts.WeeklyFocusChartView;
import ch.inf.usi.mindbricks.util.database.DataProcessor;

/**
 * Fragment that displays analytics and visualizations of study sessions.
 *
 */
public class AnalyticsFragment extends Fragment {
    private static final String TAG = "AnalyticsFragment";
    private static final int TEST_DATA_COUNT = 900;

    // ViewModel
    private AnalyticsViewModel viewModel;
    private List<StudySessionWithStats> calendarSessionsCache = new ArrayList<>();

    // Chart views
    private WeeklyFocusChartView weeklyFocusChart;
    private HourlyDistributionChartView hourlyDistributionChart;
    private DailyTimelineChartView dailyTimelineChart;
    private QualityHeatmapChartView qualityHeatmapChart;
    private StreakCalendarView streakCalendarView;
    private GoalRingsView goalRingsView;
    private AIRecommendationCardView aiRecommendationView;
    private LinearLayout aiLegendContainer;
    private TagUsageChartView tagUsagePieChart;
    private MaterialButton changePreferencesButton;


    // rings
    private View todayRingCard;
    private GoalRingsView todayGoalRingsView;
    private TextView todayDateText;
    private TextView todaySummaryText;
    private View historyDivider;
    private LinearLayout historyHeader;
    private RecyclerView dailyRingsRecyclerView;
    private DailyRingsAdapter dailyRingsAdapter;
    private MaterialButton expandRingsButton;
    private boolean isHistoryExpanded = false;


    // Session history
    private RecyclerView sessionHistoryRecycler;
    private SessionHistoryAdapter sessionHistoryAdapter;
    private TextView sessionCountText;

    // UI state views
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private View chartsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ExtendedFloatingActionButton filterFab;

    // Tab nav
    private TabLayout tabLayout;
    private View overviewContainer;
    private View insightsContainer;
    private View historyContainer;

    // Date formatters
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private boolean isRefreshing = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ViewModelProvider ensures same instance survives configuration changes
        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        // Initialize all views
        initializeViews(view);

        // Setup tab navigation
        setupTabs();

        // Setup RecyclerView for session history
        setupRecyclerView();

        // Observe ViewModel LiveData
        observeViewModel();

        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        Log.d(TAG, "Initial calendar load for current month: " + currentMonth + "/" + currentYear);
        loadStreakDataForMonth(currentMonth, currentYear);

        // Generate test data if database is empty
        //generateTestDataIfNeeded(); -> can use debug tools

        // Daily rings
        setupDailyRingsRecyclerView(view);

        Log.d(TAG, "All setup complete, now loading initial data");
        viewModel.loadLastNDays(30);
    }

    private void setupDailyRingsRecyclerView(View view) {
        // Find views
        todayRingCard = view.findViewById(R.id.todayRingCard);
        todayGoalRingsView = todayRingCard.findViewById(R.id.goalRingsView);
        todayDateText = todayRingCard.findViewById(R.id.dateText);
        todaySummaryText = todayRingCard.findViewById(R.id.summaryText);

        historyDivider = view.findViewById(R.id.historyDivider);
        historyHeader = view.findViewById(R.id.historyHeader);
        dailyRingsRecyclerView = view.findViewById(R.id.dailyRingsRecyclerView);
        expandRingsButton = view.findViewById(R.id.expandRingsButton);

        // Setup horizontal RecyclerView for previous days
        dailyRingsAdapter = new DailyRingsAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        dailyRingsRecyclerView.setAdapter(dailyRingsAdapter);
        dailyRingsRecyclerView.setLayoutManager(layoutManager);

        // Handle item clicks
        dailyRingsAdapter.setOnDayClickListener((position, data) -> {
            Toast.makeText(getContext(), getString(R.string.analytics_toast_clicked, data.getDisplayDate()), Toast.LENGTH_SHORT).show();
        });
        expandRingsButton.setOnClickListener(v -> {
            isHistoryExpanded = !isHistoryExpanded;
            updateHistoryVisibility();
        });

        dailyRingsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Update scrolling state
                isScrolling = (newState != RecyclerView.SCROLL_STATE_IDLE);

                Log.d(TAG, "Daily rings scroll state: " +
                        (isScrolling ? "SCROLLING" : "IDLE"));
            }
        });

        dailyRingsAdapter.setOnDayClickListener((position, data) -> {
            if (isScrolling) {
                Log.d(TAG, "Click ignored - currently scrolling");
                return; // Ignore clicks while scrolling
            }
            Toast.makeText(getContext(), "Clicked: " + data.getDisplayDate(), Toast.LENGTH_SHORT).show();
        });
    }


    private void updateHistoryVisibility() {
        if (isHistoryExpanded) {
            dailyRingsRecyclerView.setVisibility(View.VISIBLE);
            expandRingsButton.setText(R.string.analytics_history_hide);
            expandRingsButton.setIconResource(R.drawable.ic_expand_less);
        } else {
            dailyRingsRecyclerView.setVisibility(View.GONE);
            expandRingsButton.setText(R.string.analytics_history_show);
            expandRingsButton.setIconResource(R.drawable.ic_expand_more);
        }
    }

    /**
     * Initialize all views from the layout.
     *
     * @param view Root view
     */
    private void initializeViews(View view) {
        Log.d(TAG, "Initializing views...");

        changePreferencesButton = view.findViewById(R.id.changePreferencesButton);
        if (changePreferencesButton != null) {
            changePreferencesButton.setOnClickListener(v -> navigateToPreferences());
        }

        // Tab navigation
        tabLayout = view.findViewById(R.id.analyticsTabLayout);
        overviewContainer = view.findViewById(R.id.overviewContainer);
        insightsContainer = view.findViewById(R.id.insightsContainer);
        historyContainer = view.findViewById(R.id.historyContainer);

        // Chart views
        weeklyFocusChart = view.findViewById(R.id.weeklyFocusChart);
        hourlyDistributionChart = view.findViewById(R.id.hourlyDistributionChart);
        qualityHeatmapChart = view.findViewById(R.id.qualityHeatmapChart);
        streakCalendarView = view.findViewById(R.id.streakCalendarView);
        goalRingsView = view.findViewById(R.id.goalRingsView);
        aiRecommendationView = view.findViewById(R.id.aiRecommendationView);
        aiLegendContainer = view.findViewById(R.id.legendContainer);
        tagUsagePieChart = view.findViewById(R.id.tagUsageChart);

        // History
        sessionHistoryRecycler = view.findViewById(R.id.sessionHistoryRecycler);
        sessionCountText = view.findViewById(R.id.sessionCountText);

        // UI state views
        progressBar = view.findViewById(R.id.analyticsProgressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        chartsContainer = view.findViewById(R.id.chartsContainer);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Swipe refresh triggered");
            viewModel.refreshData();
        });

        // Daily Rings
        RecyclerView dailyRingsRecyclerView = view.findViewById(R.id.dailyRingsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );
        dailyRingsRecyclerView.setLayoutManager(layoutManager);

        // Force scrollbar to always show
        dailyRingsRecyclerView.setScrollbarFadingEnabled(false);
        dailyRingsRecyclerView.setHorizontalScrollBarEnabled(true);


        // Setup filter FAB
        filterFab = view.findViewById(R.id.analyticsFilterFab);
        if (filterFab != null) {
            filterFab.setOnClickListener(v -> showFilterDialog());
        }

        Log.d(TAG, "Views initialized successfully");
    }

    /**
     * Setup tab navigation
     */
    private void setupTabs() {
        Log.d(TAG, "Setting up tabs...");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchContent(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Nothing so far
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Nothing as well
            }
        });

        // Default to Overview tab
        switchContent(0);
    }

    /**
     * Switch between different tab content
     */
    private void switchContent(int position) {
        Log.d(TAG, "Switching to tab position: " + position);

        // Hide all containers first
        overviewContainer.setVisibility(View.GONE);
        insightsContainer.setVisibility(View.GONE);
        historyContainer.setVisibility(View.GONE);

        // Show selected container
        switch (position) {
            case 0: // Overview
                Log.d(TAG, "Showing Overview tab");
                overviewContainer.setVisibility(View.VISIBLE);
                break;

            case 1: // Insights
                Log.d(TAG, "Showing Insights tab");
                insightsContainer.setVisibility(View.VISIBLE);
                break;

            case 2: // History
                Log.d(TAG, "Showing History tab");
                historyContainer.setVisibility(View.VISIBLE);
                break;
        }
    }

    private boolean isScrolling = false;
    /**
     * Setup RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        sessionHistoryAdapter = new SessionHistoryAdapter(new SessionHistoryAdapter.OnSessionClickListener() {
            @Override
            public void onSessionClick(StudySessionWithStats session) {
                if (isScrolling) {
                    Log.d(TAG, "Click ignored - currently scrolling");
                    return;
                }
                showSessionDetails(session);
            }

            @Override
            public void onSessionLongClick(StudySessionWithStats session) {
                if (isScrolling) {
                    Log.d(TAG, "Long click ignored - currently scrolling");
                    return;
                }
                showSessionOptionsDialog(session);
            }
        });

        // Set layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        sessionHistoryRecycler.setLayoutManager(layoutManager);

        // Set adapter
        sessionHistoryRecycler.setAdapter(sessionHistoryAdapter);

        // Add divider
        sessionHistoryRecycler.addItemDecoration(
                new androidx.recyclerview.widget.DividerItemDecoration(
                        requireContext(),
                        layoutManager.getOrientation()
                )
        );

        sessionHistoryRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Update scrolling state
                isScrolling = (newState != RecyclerView.SCROLL_STATE_IDLE);

                Log.d(TAG, "Session history scroll state: " +
                        (isScrolling ? "SCROLLING" : "IDLE"));
            }
        });

        Log.d(TAG, "RecyclerView setup complete");
    }

    /**
     * Observe all LiveData from ViewModel.
     * This is where the Fragment reacts to data changes.
     */
    private void observeViewModel() {
        // Observe date range changes
        viewModel.getDateRange().observe(getViewLifecycleOwner(), dateRange -> {
            if (dateRange != null) {
                String displayText = dateRange.getDisplayName();

                // Update FAB text
                if (filterFab != null) {
                    filterFab.setText(displayText);
                }
            }
        });

        // Observe view state for loading/error/success
        viewModel.getViewState().observe(getViewLifecycleOwner(), this::updateUIState);

        // Observe weekly stats
        viewModel.getWeeklyStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && weeklyFocusChart != null) {
                weeklyFocusChart.setData(stats);
            }
        });

        // Observe hourly distribution
        viewModel.getHourlyStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null && hourlyDistributionChart != null) {
                hourlyDistributionChart.setData(stats);
            }
        });

        // Observe daily recommendations
        viewModel.getDailyRecommendation().observe(getViewLifecycleOwner(), recommendation -> {
            if (recommendation != null && dailyTimelineChart != null) {
                dailyTimelineChart.setData(recommendation);
            }
        });

        // Observe heatmap
        viewModel.getHeatmapData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && qualityHeatmapChart != null) {
                qualityHeatmapChart.setData(data);
            }
        });

        if (streakCalendarView != null) {
            streakCalendarView.setOnDayClickListener(this::showSessionsForDay);
            streakCalendarView.setOnMonthChangeListener(this::loadStreakDataForMonth);
            Log.d(TAG, "Calendar listeners configured");
        }

        // Observe goal rings
        viewModel.getDailyRingsHistory().observe(getViewLifecycleOwner(), this::updateDailyRingsDisplay);

        // Observe AI Recommendations
        viewModel.getDailyRecommendation().observe(getViewLifecycleOwner(), recommendation -> {

            if (recommendation != null && aiRecommendationView != null) {
                aiRecommendationView.setData(recommendation);

                // Post to ensure view has been laid out
                aiRecommendationView.post(this::updateAILegend);
            } else {
                Log.w(TAG, "Cannot update AI recommendation: recommendation=" +
                        (recommendation != null) + ", view=" + (aiRecommendationView != null));
            }
        });

        // Observe tag usage data
        viewModel.getTagUsageData().observe(getViewLifecycleOwner(), data -> {
            if (data != null && tagUsagePieChart != null) {
                tagUsagePieChart.setData(data);
            }
        });

        // Observe session history
        viewModel.getSessionHistory().observe(getViewLifecycleOwner(), sessions -> {
            Log.d(TAG, "Session history received: " + (sessions != null ? sessions.size() + " items" : "null"));

            if (sessions == null) return;

            new Handler(Looper.getMainLooper()).post(() -> {
                sessionHistoryAdapter.setData(sessions);

                if (sessionCountText != null) {
                    String countText = getString(R.string.analytics_session_count_format,
                            sessions.size(), sessions.size() == 1 ? "" : "s");

                    // Show if data is truncated
                    DateRange currentRange = viewModel.getCurrentDateRange();
                    if (currentRange != null &&
                            currentRange.getRangeType() == DateRange.RangeType.ALL_TIME &&
                            sessions.size() >= 200) {
                        countText += getString(R.string.analytics_session_count_truncated);
                    }

                    sessionCountText.setText(countText);
                }
            });
        });

        Log.d(TAG, "=== All observers registered ===");
    }

    private void updateDailyRingsList(List<DailyRings> history) {
        if (history == null || history.isEmpty()) {
            dailyRingsAdapter.submitList(new ArrayList<>());
            return;
        }

        boolean isExpanded = Boolean.TRUE.equals(viewModel.isRingsExpanded().getValue());

        if (isExpanded) {
            // Show all days
            dailyRingsAdapter.submitList(history);
        } else {
            // Show only today (first item)
            dailyRingsAdapter.submitList(history.subList(0, Math.min(1, history.size())));
        }
    }

    private void updateDailyRingsDisplay(List<DailyRings> history) {
        if (history == null || history.isEmpty()) {
            todayRingCard.setVisibility(View.GONE);
            historyDivider.setVisibility(View.GONE);
            historyHeader.setVisibility(View.GONE);
            dailyRingsRecyclerView.setVisibility(View.GONE);
            return;
        }

        // Show today's card (first item)
        DailyRings today = history.get(0);
        todayRingCard.setVisibility(View.VISIBLE);
        todayDateText.setText(today.getDisplayDate());
        todaySummaryText.setText(today.getSummary());
        todayGoalRingsView.setData(today.getRings(), false);

        // Show previous days in horizontal list
        if (history.size() > 1) {
            historyDivider.setVisibility(View.VISIBLE);
            historyHeader.setVisibility(View.VISIBLE);

            // Get all days except today
            List<DailyRings> previousDays = history.subList(1, history.size());
            dailyRingsAdapter.submitList(previousDays);

            // Update visibility based on expand state
            updateHistoryVisibility();
        } else {
            historyDivider.setVisibility(View.GONE);
            historyHeader.setVisibility(View.GONE);
            dailyRingsRecyclerView.setVisibility(View.GONE);
        }
    }

    private void updateExpandButton(boolean isExpanded) {
        if (expandRingsButton == null) return;

        if (isExpanded) {
            expandRingsButton.setText(R.string.analytics_history_hide_full);
            expandRingsButton.setIconResource(R.drawable.ic_expand_less);
        } else {
            expandRingsButton.setText(R.string.analytics_history_show_full);
            expandRingsButton.setIconResource(R.drawable.ic_expand_more);
        }
    }

    /**
     * Updates the AI recommendation legend with themed colors and styling
     */
    private void updateAILegend() {
        if (aiLegendContainer == null || aiRecommendationView == null) {
            Log.w(TAG, "Legend container or chart view is null");
            return;
        }

        aiLegendContainer.removeAllViews();

        List<AIRecommendationCardView.LegendItem> items = aiRecommendationView.getLegendItems();

        if (items.isEmpty()) {
            Log.d(TAG, "No legend items to display");
            return;
        }

        Log.d(TAG, "Updating legend with " + items.size() + " items");

        // Add legend title with themed color
        TextView legendTitle = new TextView(requireContext());
        legendTitle.setText(R.string.analytics_legend_activity_types);
        legendTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        legendTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.analytics_text_primary));
        legendTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, dpToPx(8));
        legendTitle.setLayoutParams(titleParams);
        aiLegendContainer.addView(legendTitle);

        // Create rows for legend items
        LinearLayout row = null;
        int itemsPerRow = 3;

        for (int i = 0; i < items.size(); i++) {
            if (i % itemsPerRow == 0) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));
                aiLegendContainer.addView(row);
            }

            View legendItem = createAILegendItem(items.get(i));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.0f  // Equal weight
            );
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            legendItem.setLayoutParams(params);
            row.addView(legendItem);
        }

        // Add summary message with themed color
        String summary = aiRecommendationView.getSummaryMessage();
        if (summary != null && !summary.isEmpty()) {
            TextView summaryView = new TextView(requireContext());
            summaryView.setText(summary);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            summaryView.setTextColor(ContextCompat.getColor(requireContext(), R.color.analytics_text_secondary));
            summaryView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            summaryView.setPadding(dpToPx(8), dpToPx(16), dpToPx(8), 0);
            aiLegendContainer.addView(summaryView);
        }

        Log.d("Fragment", "=== All observers registered ===");
    }


    private View createAILegendItem(AIRecommendationCardView.LegendItem item) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

        // Create rounded color box
        View colorBox = new View(requireContext());

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(item.colorHex()));
        drawable.setCornerRadius(dpToPx(4));
        colorBox.setBackground(drawable);

        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                dpToPx(16), dpToPx(16)
        );
        boxParams.setMargins(0, 0, dpToPx(8), 0);
        colorBox.setLayoutParams(boxParams);
        layout.addView(colorBox);

        // Create label with themed text color
        TextView label = new TextView(requireContext());
        label.setText(item.name());
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.analytics_text_primary));
        layout.addView(label);

        return layout;
    }

    private void showSessionsForDay(StreakDay day) {
        // Format the date nicely
        Calendar cal = Calendar.getInstance();
        cal.set(day.getYear(), day.getMonth(), day.getDayOfMonth());
        String dateStr = dateFormat.format(cal.getTime());

        // Build a nicely formatted message
        StringBuilder message = new StringBuilder();
        message.append(getString(R.string.analytics_summary_date_prefix, dateStr)).append("\n\n");

        // Show study status with icon
        // FIXME: I don't like emojis as they seem a bit cheap, but we need to see if we have enough time
        String statusIcon;
        String statusText = switch (day.getStatus()) {
            case HIT_TARGET -> {
                statusIcon = "✅";
                yield getString(R.string.analytics_status_goal_completed);
            }
            case PARTIAL -> {
                statusIcon = "⏳";
                yield getString(R.string.analytics_status_partially_completed);
            }
            case EXCEPTIONAL -> {
                statusIcon = "⭐";
                yield getString(R.string.analytics_status_exceptional);
            }
            case NONE -> {
                statusIcon = "❌";
                yield getString(R.string.analytics_status_no_study);
            }
            default -> {
                statusIcon = "❓";
                yield getString(R.string.analytics_status_unknown);
            }
        };
        message.append(statusIcon).append(" ").append(statusText).append("\n\n");

        // Show duration details
        int totalMinutes = day.getTotalMinutes();
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        message.append(getString(R.string.analytics_time_total_prefix));
        if (hours > 0) {
            message.append(getString(R.string.analytics_time_format_hours_mins, hours, minutes)).append("\n");
        } else {
            message.append(getString(R.string.analytics_time_format_mins, minutes)).append("\n");
        }

        // Add motivational message based on status
        message.append("\n");
        if (day.getStatus() == StreakDay.StreakStatus.HIT_TARGET) {
            message.append(getString(R.string.analytics_motivation_goal_met));
        } else if (day.getStatus() == StreakDay.StreakStatus.EXCEPTIONAL) {
            message.append(getString(R.string.analytics_motivation_exceptional));
        } else if (day.getStatus() == StreakDay.StreakStatus.PARTIAL) {
            message.append(getString(R.string.analytics_motivation_partial));
        } else if (day.getStatus() == StreakDay.StreakStatus.NONE) {
            message.append(getString(R.string.analytics_motivation_none));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.analytics_dialog_summary_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.analytics_close, null)
                .show();
    }

    /**
     * Update UI based on view state.
     * Shows/hides loading, error, empty, and content views.
     *
     * @param state Current view state
     */
    private void updateUIState(AnalyticsViewModel.ViewState state) {
        Log.d("Fragment", "=== updateUIState called with: " + state + " ===");

        // Stop refresh animation if running
        swipeRefreshLayout.setRefreshing(false);

        switch (state) {
            case LOADING:
                Log.d(TAG, "Showing LOADING state");
                progressBar.setVisibility(View.VISIBLE);
                chartsContainer.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.GONE);
                break;

            case SUCCESS:
                Log.d(TAG, "Showing SUCCESS state - charts visible");
                progressBar.setVisibility(View.GONE);
                chartsContainer.setVisibility(View.VISIBLE);
                emptyStateText.setVisibility(View.GONE);
                break;

            case EMPTY:
                Log.d(TAG, "Showing EMPTY state");
                progressBar.setVisibility(View.GONE);
                chartsContainer.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);

                emptyStateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.empty_state_text));

                // Show message based on current range
                DateRange currentRange = viewModel.getCurrentDateRange();
                if (currentRange != null) {
                    String message = getString(R.string.analytics_empty_range, currentRange.getDisplayName());
                    emptyStateText.setText(message);
                } else {
                    emptyStateText.setText(R.string.analytics_empty_all);
                }
                break;

            case ERROR:
                Log.d(TAG, "Showing ERROR state");
                progressBar.setVisibility(View.GONE);
                chartsContainer.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_error));
                emptyStateText.setText(R.string.analytics_error_loading);
                Toast.makeText(getContext(), R.string.analytics_error_loading_toast, Toast.LENGTH_SHORT).show();
                break;
        }

        Log.d(TAG, "UI state update complete");
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.analytics_filter_title);

        String[] options = {
                getString(R.string.analytics_range_7_days),
                getString(R.string.analytics_range_30_days),
                getString(R.string.analytics_range_90_days),
                getString(R.string.analytics_range_this_month),
                getString(R.string.analytics_range_last_month),
                getString(R.string.analytics_range_all_time)
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    viewModel.loadLastNDays(7);
                    break;

                case 1:
                    viewModel.loadLastNDays(30);
                    break;

                case 2:
                    viewModel.loadLastNDays(90);
                    break;

                case 3:
                    viewModel.loadCurrentMonth();
                    break;

                case 4:
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MONTH, -1);
                    int lastMonth = cal.get(Calendar.MONTH);
                    int lastMonthYear = cal.get(Calendar.YEAR);
                    viewModel.loadMonth(lastMonthYear, lastMonth);
                    break;

                case 5:
                    viewModel.loadAllTime();
                    break;
            }
        });

        builder.setNegativeButton(R.string.analytics_cancel, null);
        builder.show();
    }

    /**
     * Show detailed dialog for a study session.
     *
     * @param session Session to show
     */
    private void showSessionDetails(StudySessionWithStats session) {
        // Inflate custom dialog layout
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_session_details, null);

        // Find views in dialog
        TextView dateText = dialogView.findViewById(R.id.sessionDetailDate);
        TextView timeText = dialogView.findViewById(R.id.sessionDetailTime);
        TextView durationText = dialogView.findViewById(R.id.sessionDetailDuration);
        TextView tagText = dialogView.findViewById(R.id.sessionDetailTag);
        TextView focusScoreText = dialogView.findViewById(R.id.sessionDetailFocusScore);
        TextView noiseText = dialogView.findViewById(R.id.sessionDetailNoise);
        TextView lightText = dialogView.findViewById(R.id.sessionDetailLight);
        TextView pickupsText = dialogView.findViewById(R.id.sessionDetailPickups);
        TextView notesText = dialogView.findViewById(R.id.sessionDetailNotes);

        // Populate with session data
        Date date = new Date(session.getTimestamp());
        dateText.setText(dateFormat.format(date));
        timeText.setText(timeFormat.format(date));
        durationText.setText(formatDuration(session.getDurationMinutes()));
        tagText.setText(session.getTagTitle());
        focusScoreText.setText(String.format(Locale.getDefault(),
                getString(R.string.analytics_detail_focus_score), session.getFocusScore()));
        noiseText.setText(String.format(Locale.getDefault(),
                getString(R.string.analytics_detail_noise), session.getAvgNoiseLevel()));
        lightText.setText(String.format(Locale.getDefault(),
                getString(R.string.analytics_detail_light), session.getAvgLightLevel()));
        pickupsText.setText(String.format(Locale.getDefault(),
                getString(R.string.analytics_detail_pickups), session.getPhonePickupCount()));

        // Show notes if available
        if (session.getNotes() != null && !session.getNotes().isEmpty()) {
            notesText.setText(session.getNotes());
            notesText.setVisibility(View.VISIBLE);
        } else {
            notesText.setVisibility(View.GONE);
        }

        // Show dialog
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.analytics_detail_title)
                .setView(dialogView)
                .setPositiveButton(R.string.analytics_close, null)
                .show();
    }

    /**
     * Show options dialog for long-press on session.
     *
     * @param session Session to show options for
     */
    private void showSessionOptionsDialog(StudySessionWithStats session) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.analytics_options_title)
                .setItems(new String[]{
                        getString(R.string.analytics_action_view_details),
                        getString(R.string.analytics_action_delete_session)},
                        (dialog, which) -> {
                    if (which == 0) {
                        showSessionDetails(session);
                    } else if (which == 1) {
                        confirmDeleteSession(session);
                    }
                })
                .setNegativeButton(R.string.analytics_cancel, null)
                .show();
    }

    /**
     * Show confirmation dialog before deleting session.
     *
     * @param session Session to delete
     */
    private void confirmDeleteSession(StudySessionWithStats session) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.analytics_delete_confirm_title)
                .setMessage(R.string.analytics_delete_confirm_message)
                .setPositiveButton(R.string.analytics_action_delete, (dialog, which) -> {
                    viewModel.deleteSession(session);
                    Toast.makeText(getContext(), R.string.analytics_toast_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.analytics_cancel, null)
                .show();
    }

    /**
     * Format duration in human-readable format.
     *
     * @param totalMinutes Total minutes
     * @return Formatted string
     */
    private String formatDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        if (hours > 0) {
            return getString(R.string.analytics_format_hours_mins_long,
                    hours, hours == 1 ? "" : "s", minutes);
        } else {
            return getString(R.string.analytics_format_mins_long, minutes);
        }
    }

    private void navigateToPreferences() {
        Intent intent = new Intent(requireContext(), SettingsActivity.class);
        intent.putExtra("fragment_set", 1);
        startActivity(intent);
    }

    private void loadStreakDataForMonth(int month, int year) {
        Log.d(TAG, "=== LOADING CALENDAR: " + month + "/" + year + " ===");

        // If we have cached calendar data, use it immediately
        if (!calendarSessionsCache.isEmpty()) {
            Log.d(TAG, "Using cached calendar data: " + calendarSessionsCache.size() + " sessions");
            computeAndDisplayCalendar(calendarSessionsCache, month, year);
            return;
        }

        // Otherwise, load ALL sessions from database
        Log.d(TAG, "Loading ALL sessions from database for calendar...");
        viewModel.loadAllSessionsForCalendar(allSessions -> {
            Log.d(TAG, "Calendar data received: " + allSessions.size() + " total sessions");

            // Cache for future month navigation
            calendarSessionsCache = allSessions;

            // Compute and display
            computeAndDisplayCalendar(allSessions, month, year);
        });
    }

    /**
     * Compute calendar data and update the view
     */
    private void computeAndDisplayCalendar(List<StudySessionWithStats> allSessions, int month, int year) {
        // Count sessions in this specific month (for logging)
        int sessionsInMonth = 0;
        Calendar cal = Calendar.getInstance();
        for (StudySessionWithStats session : allSessions) {
            cal.setTimeInMillis(session.getTimestamp());
            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                sessionsInMonth++;
            }
        }

        Log.d(TAG, "Computing calendar:");
        Log.d(TAG, "  Total sessions: " + allSessions.size());
        Log.d(TAG, "  Sessions in " + month + "/" + year + ": " + sessionsInMonth);

        // Compute streak data
        List<StreakDay> monthData = DataProcessor.calculateStreakCalendar(
                allSessions,
                60,
                month,
                year
        );

        Log.d(TAG, "  Streak days computed: " + monthData.size());

        // Update calendar view
        if (streakCalendarView != null) {
            streakCalendarView.setData(monthData);
            Log.d(TAG, "Calendar updated successfully");
        } else {
            Log.e(TAG, "Calendar view is null!");
        }
    }


    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "Fragment resumed");

        // Only reload calendar if it was previously cleared
        if (calendarSessionsCache.isEmpty() && streakCalendarView != null) {
            Calendar cal = Calendar.getInstance();
            int currentMonth = cal.get(Calendar.MONTH);
            int currentYear = cal.get(Calendar.YEAR);

            Log.d(TAG, "Reloading calendar for current month");
            loadStreakDataForMonth(currentMonth, currentYear);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        Log.d(TAG, "onDestroyView");
    }
}