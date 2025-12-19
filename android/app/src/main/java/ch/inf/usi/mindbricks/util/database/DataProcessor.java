package ch.inf.usi.mindbricks.util.database;

import android.content.Context;
import android.util.Log;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.recommendation.DailyRecommendation;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TagUsage;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;
import ch.inf.usi.mindbricks.model.recommendation.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.model.visual.GoalRing;
import ch.inf.usi.mindbricks.util.PreferencesManager;

/**
 * Utility class for processing and analyzing study session data.
 * Contains methods to transform raw session data into chart-ready statistics.
 *
 * @author Luca Di Bello
 * @author Marta Šafářová
 */
public class DataProcessor {

    public static WeeklyStats calculateWeeklyStats(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        WeeklyStats stats = new WeeklyStats();
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            return stats;
        }

        int[] minutesPerDay = new int[7];
        float[] focusScoreSum = new float[7];
        int[] sessionCountPerDay = new int[7];

        Calendar calendar = Calendar.getInstance();

        // Process each session
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());

            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int dayIndex = convertCalendarDayToIndex(dayOfWeek);

            // Accumulate data
            minutesPerDay[dayIndex] += session.getDurationMinutes();
            focusScoreSum[dayIndex] += session.getFocusScore();
            sessionCountPerDay[dayIndex]++;
        }

        // Calculate averages and set data
        int totalMinutes = 0;
        float totalFocusScore = 0;
        int totalSessions = 0;
        int daysWithSessions = 0;

        for (int i = 0; i < 7; i++) {
            stats.setDayMinutes(i,0);
            stats.setDaySessionCount(i, sessionCountPerDay[i]);

            if (sessionCountPerDay[i] > 0) {
                float avgFocusScore = focusScoreSum[i] / sessionCountPerDay[i];
                stats.setDayMinutes(i, minutesPerDay[i]/sessionCountPerDay[i]);
                stats.setDayFocusScore(i, avgFocusScore);

                totalMinutes += minutesPerDay[i];
                totalFocusScore += avgFocusScore;
                totalSessions += sessionCountPerDay[i];
                daysWithSessions++;
            } else {
                stats.setDayFocusScore(i, 0);
            }
        }

        stats.setTotalMinutes(totalMinutes);
        stats.setTotalSessions(totalSessions);

        if (daysWithSessions > 0) {
            stats.setAverageFocusScore(totalFocusScore / daysWithSessions);
        }

        return stats;
    }

    public static List<TimeSlotStats> calculateHourlyDistribution(List<StudySessionWithStats> allSessions, DateRange dateRange) {        // Create 24 time slots (one for each hour)
        List<TimeSlotStats> hourlyStats = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyStats.add(new TimeSlotStats(hour));
        }

        // Filter sessions to only include those within the date range
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            return hourlyStats;
        }

        Calendar calendar = Calendar.getInstance();

        // Process each session
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            // Add session data to the appropriate hour slot
            TimeSlotStats hourSlot = hourlyStats.get(hour);
            hourSlot.addSession(
                    session.getDurationMinutes(),
                    session.getFocusScore(),
                    session.getAvgNoiseLevel(),
                    session.getAvgLightLevel()
            );
        }

        // Log statistics for debugging
        int activeHours = 0;
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                activeHours++;
            }
        }
        return hourlyStats;
    }

    public static DailyRecommendation generateDailyRecommendation(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        DailyRecommendation recommendation = new DailyRecommendation();
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            recommendation.setReasonSummary(
                    "Not enough data yet. Complete more sessions to get personalized recommendations.");
            recommendation.setConfidenceScore(0);
            recommendation.setRecommendedSlots(new ArrayList<>());
            return recommendation;
        }

        // Calculate hourly distribution to find best times
        List<TimeSlotStats> hourlyStats = calculateHourlyDistribution(allSessions, dateRange);

        // Find hours with sessions and rank by focus score
        List<TimeSlotStats> rankedHours = new ArrayList<>();
        for (TimeSlotStats stats : hourlyStats) {
            if (stats.getSessionCount() > 0) {
                rankedHours.add(stats);
            }
        }

        // Sort by focus score (descending) - best hours first
        Collections.sort(rankedHours, (a, b) ->
                Float.compare(b.getAverageFocusScore(), a.getAverageFocusScore()));

        // Create recommended time slots (top 3)
        List<DailyRecommendation.TimeSlot> recommendedSlots = new ArrayList<>();
        int slotsToRecommend = Math.min(3, rankedHours.size());

        for (int i = 0; i < slotsToRecommend; i++) {
            TimeSlotStats hourStat = rankedHours.get(i);
            DailyRecommendation.TimeSlot slot = new DailyRecommendation.TimeSlot();
            slot.setStartTime(hourStat.getFormattedTime());
            slot.setEndTime(getEndTime(hourStat.getHourOfDay()));
            slot.setProductivityScore((int) hourStat.getAverageFocusScore());
            slot.setReason(generateSlotReason(hourStat));
            recommendedSlots.add(slot);
        }

        recommendation.setRecommendedSlots(recommendedSlots);

        // Generate summary
        if (!rankedHours.isEmpty()) {
            TimeSlotStats bestHour = rankedHours.get(0);
            String summary = String.format(
                    "Based on %d sessions, you're most productive around %s with %.0f%% focus.",
                    sessions.size(),
                    bestHour.getFormattedTime(),
                    bestHour.getAverageFocusScore()
            );
            recommendation.setReasonSummary(summary);

            int confidence = Math.min(100, (sessions.size() * 10));
            recommendation.setConfidenceScore(confidence);
        }

        return recommendation;
    }

    public static List<HourlyQuality> calculateEnergyCurve(List<StudySessionWithStats> sessions) {
        List<HourlyQuality> hourlyData = new ArrayList<>();

        // Initialize 24-hour structure
        float[] totalQuality = new float[24];
        int[] sessionCounts = new int[24];

        Calendar calendar = Calendar.getInstance();

        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            totalQuality[hour] += session.getFocusScore();
            sessionCounts[hour]++;
        }

        // Calculate averages for each hour
        for (int hour = 0; hour < 24; hour++) {
            float avgQuality = sessionCounts[hour] > 0
                    ? totalQuality[hour] / sessionCounts[hour]
                    : 0;

            hourlyData.add(new HourlyQuality(hour, avgQuality, sessionCounts[hour]));
        }

        return hourlyData;
    }

    public static List<HeatmapCell> calculateQualityHeatmap(
            List<StudySessionWithStats> sessions,
            DateRange dateRange
    ) {
        Log.d("DataProcessor", "calculateQualityHeatmap START - range: " + dateRange.getDisplayName());

        // Cap to prevent excessive memory usage
        DateRange cappedRange = dateRange;
        if (dateRange.getRangeType() == DateRange.RangeType.ALL_TIME) {
            Log.d("DataProcessor", "ALL_TIME detected, capping to last 365 days for heatmap");
            cappedRange = DateRange.lastNDays(365);
        } else if (dateRange.getDurationInDays() > 365) {
            Log.d("DataProcessor", "Range > 365 days, capping to 365 days for performance");
            long cappedStart = dateRange.getEndTimestamp() - (365L * 24 * 60 * 60 * 1000);
            cappedRange = DateRange.custom(cappedStart, dateRange.getEndTimestamp());
        }

        // Filter sessions to the (possibly capped) range
        List<StudySessionWithStats> filteredSessions = filterSessionsInRange(sessions, cappedRange);
        Log.d("DataProcessor", "Filtered sessions for heatmap: " + filteredSessions.size());

        if (filteredSessions.isEmpty()) {
            Log.d("DataProcessor", "No sessions for heatmap, returning empty list");
            return new ArrayList<>();
        }

        // OPTIMIZED: Only create cells for hours with actual sessions (not every hour in range)
        Map<String, HeatmapCell> cellMap = new HashMap<>();
        Calendar calendar = Calendar.getInstance();

        // Process each session and create cells only for hours that have data
        for (StudySessionWithStats session : filteredSessions) {
            calendar.setTimeInMillis(session.getTimestamp());

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

            String hourKey = year + "-" + month + "-" + dayOfMonth + "-" + hourOfDay;

            HeatmapCell cell = cellMap.get(hourKey);
            if (cell == null) {
                // Create new cell for this hour
                cell = createHeatmapCell(calendar);
                cellMap.put(hourKey, cell);
            }

            // Accumulate session data
            cell.setSessionCount(cell.getSessionCount() + 1);
            cell.setAvgQuality(cell.getAvgQuality() + session.getFocusScore());
            cell.setTotalMinutes(cell.getTotalMinutes() + session.getDurationMinutes());
        }

        Log.d("DataProcessor", "Created " + cellMap.size() + " hourly cells (only for hours with sessions)");

        // Calculate averages and convert to list
        List<HeatmapCell> result = new ArrayList<>(cellMap.values());
        for (HeatmapCell cell : result) {
            if (cell.getSessionCount() > 0) {
                // Calculate average quality
                cell.setAvgQuality(cell.getAvgQuality() / cell.getSessionCount());
            }
        }

        // Sort chronologically (oldest to newest)
        Collections.sort(result, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

        Log.d("DataProcessor", "Heatmap complete: " + result.size() + " hourly cells with " +
                filteredSessions.size() + " sessions");

        return result;
    }

    /**
     * Helper method to create a HeatmapCell from a Calendar instance.
     * Sets timestamp to the start of the hour.
     */
    private static HeatmapCell createHeatmapCell(Calendar cal) {
        HeatmapCell cell = new HeatmapCell();
        cell.setYear(cal.get(Calendar.YEAR));
        cell.setMonth(cal.get(Calendar.MONTH));
        cell.setDayOfMonth(cal.get(Calendar.DAY_OF_MONTH));
        cell.setDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
        cell.setHourOfDay(cal.get(Calendar.HOUR_OF_DAY));

        // Set timestamp to start of hour
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cell.setTimestamp(cal.getTimeInMillis());

        cell.setSessionCount(0);
        cell.setAvgQuality(0);
        cell.setTotalMinutes(0);
        return cell;
    }

    public static List<StreakDay> calculateStreakCalendar(List<StudySessionWithStats> sessions,
                                                          int targetMinutes,
                                                          int month,
                                                          int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1);

        // Get the number of days in this month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        Map<String, StreakDay> dayMap = new HashMap<>();
        for (int day = 1; day <= daysInMonth; day++) {
            String key = year + "-" + month + "-" + day;
            StreakDay streakDay = new StreakDay(day, month, year);
            streakDay.setStatus(StreakDay.StreakStatus.NONE);
            dayMap.put(key, streakDay);
        }

        // Filter sessions to only this month
        List<StudySessionWithStats> monthSessions = new ArrayList<>();
        for (StudySessionWithStats session : sessions) {
            calendar.setTimeInMillis(session.getTimestamp());
            if (calendar.get(Calendar.MONTH) == month &&
                    calendar.get(Calendar.YEAR) == year) {
                monthSessions.add(session);
            }
        }

        for (StudySessionWithStats session : monthSessions) {
            calendar.setTimeInMillis(session.getTimestamp());

            int day = calendar.get(Calendar.DAY_OF_MONTH);
            String key = year + "-" + month + "-" + day;

            StreakDay streakDay = dayMap.get(key);
            if (streakDay != null) {
                // Accumulate session data
                streakDay.setTotalMinutes(streakDay.getTotalMinutes() + session.getDurationMinutes());
                streakDay.setAvgQuality(streakDay.getAvgQuality() + session.getFocusScore());
                streakDay.setSessionCount(streakDay.getSessionCount() + 1);
            }
        }

        // Calculate status for each day
        List<StreakDay> result = new ArrayList<>();
        for (StreakDay day : dayMap.values()) {
            if (day.getSessionCount() > 0) {
                day.setAvgQuality(day.getAvgQuality() / day.getSessionCount());
            }

            if (day.getTotalMinutes() == 0) {
                day.setStatus(StreakDay.StreakStatus.NONE);
            } else if (day.getTotalMinutes() < targetMinutes * 0.5) {
                day.setStatus(StreakDay.StreakStatus.PARTIAL);
            } else if (day.getTotalMinutes() >= targetMinutes * 1.5) {
                day.setStatus(StreakDay.StreakStatus.EXCEPTIONAL);
            } else {
                day.setStatus(StreakDay.StreakStatus.HIT_TARGET);
            }

            result.add(day);
        }

        Collections.sort(result, (a, b) -> {
            if (a.getYear() != b.getYear()) return a.getYear() - b.getYear();
            if (a.getMonth() != b.getMonth()) return a.getMonth() - b.getMonth();
            return a.getDayOfMonth() - b.getDayOfMonth();
        });

        return result;
    }

    public static List<DailyRings> calculateDailyRingsHistory(
            Context context,
            List<StudySessionWithStats> allSessions,
            DateRange dateRange,
            int dailyMinutesTarget,
            int minSessionsForRing
    ) {
        Log.d("DataProcessor", "calculateDailyRingsHistory START");
        Log.d("DataProcessor", "  Range: " + dateRange.getDisplayName());
        Log.d("DataProcessor", "  Total sessions: " + allSessions.size());

        long rangeMs = dateRange.getEndTimestamp() - dateRange.getStartTimestamp();
        long maxMs = 365L * 24 * 60 * 60 * 1000;

        DateRange cappedRange = dateRange;
        if (rangeMs > maxMs) {
            Log.w("DataProcessor", "  Range too large (" + (rangeMs / (24*60*60*1000)) + " days), capping to 365 days");
            long cappedStart = dateRange.getEndTimestamp() - maxMs;
            cappedRange = DateRange.custom(cappedStart, dateRange.getEndTimestamp());
        }

        List<DailyRings> result = new ArrayList<>();

        // Filter sessions to capped range
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, cappedRange);
        Log.d("DataProcessor", "  Filtered sessions: " + sessions.size());

        if (sessions.isEmpty()) {
            Log.d("DataProcessor", "  No sessions, creating empty ring for today");
            // Create an empty ring for today to show current goal status
            List<GoalRing> emptyRings = calculateGoalRings(context, new ArrayList<>(), dailyMinutesTarget, 70);
            DailyRings todayRings = new DailyRings(LocalDate.now(), emptyRings);
            result.add(todayRings);
            return result;
        }

        // Group sessions by day
        Map<String, List<StudySessionWithStats>> sessionsByDay = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        for (StudySessionWithStats session : sessions) {
            cal.setTimeInMillis(session.getTimestamp());

            String dayKey = String.format(Locale.US, "%d-%02d-%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
            );

            if (!sessionsByDay.containsKey(dayKey)) {
                sessionsByDay.put(dayKey, new ArrayList<>());
            }
            Objects.requireNonNull(sessionsByDay.get(dayKey)).add(session);
        }

        Log.d("DataProcessor", "  Days with sessions: " + sessionsByDay.size());

        // Get today's date for comparison
        Calendar today = Calendar.getInstance();
        String todayKey = String.format(Locale.US, "%d-%02d-%02d",
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH)
        );

        for (Map.Entry<String, List<StudySessionWithStats>> entry : sessionsByDay.entrySet()) {
            List<StudySessionWithStats> daySessions = entry.getValue();

            // Always include today
            boolean isToday = entry.getKey().equals(todayKey);

            if (!isToday && daySessions.size() < minSessionsForRing) {
                continue;
            }

            long dayTimestamp = daySessions.get(0).getTimestamp();

            List<GoalRing> rings = calculateGoalRings(context, daySessions, dailyMinutesTarget, 70);

            DailyRings dailyRings = new DailyRings(LocalDate.ofEpochDay(dayTimestamp / 86400000), rings);
            result.add(dailyRings);
        }

        // If today has no sessions -> add empty DailyRings
        boolean todayIncluded = result.stream()
                .anyMatch(dr -> dr.getDate().equals(LocalDate.now()));

        if (!todayIncluded) {
            List<GoalRing> emptyRings = calculateGoalRings(context, new ArrayList<>(), dailyMinutesTarget, 70);
            DailyRings todayRings = new DailyRings(LocalDate.now(), emptyRings);
            result.add(todayRings);
            Log.d("DataProcessor", "  Added empty ring for today (no sessions yet)");
        }

        result.sort((a, b) -> Long.compare(b.getDate().toEpochDay(), a.getDate().toEpochDay()));

        Log.d("DataProcessor", "  Daily rings created: " + result.size());
        return result;
    }
    
    private static Calendar getStartCalendar(DateRange dateRange) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(dateRange.getStartTimestamp());

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }


    public static List<GoalRing> calculateGoalRings(Context context,
                                                    List<StudySessionWithStats> sessions,
                                                    int dailyMinutesTarget,
                                                    float dailyFocusTarget) {
        PreferencesManager manager = new PreferencesManager(context);

        // Use unified preferences for targets if not provided
        if (dailyMinutesTarget <= 0) {
            dailyMinutesTarget = manager.getDailyStudyMinutesGoal(System.currentTimeMillis());
        }

        List<GoalRing> rings = new ArrayList<>();

        // Calculate totals from the passed sessions (already filtered by caller)
        int totalMinutes = 0;
        float totalFocus = 0;
        int sessionCount = sessions.size();

        for (StudySessionWithStats session : sessions) {
            totalMinutes += session.getDurationMinutes();
            totalFocus += session.getFocusScore();
        }

        float avgFocus = sessionCount > 0 ? totalFocus / sessionCount : 0;

        // Get today's primary study topic
        String todayTopic = getTodayPrimaryStudyTopic(sessions);

        // Create rings with study topic if available
        String timeRingTitle = "Study Time";
        if (todayTopic != null && !todayTopic.isEmpty()) {
            timeRingTitle = todayTopic + " Time";
        }

        rings.add(new GoalRing(
                timeRingTitle,
                totalMinutes,
                dailyMinutesTarget,
                "min"
        ));

        rings.add(new GoalRing(
                "Focus Quality",
                avgFocus,
                dailyFocusTarget,
                "%"
        ));

        // Session target
        int dailySessionTarget = Math.max(3, dailyMinutesTarget / 60);
        rings.add(new GoalRing(
                "Sessions",
                sessionCount,
                dailySessionTarget,
                "sessions"
        ));

        return rings;
    }

    public static List<TagUsage> calculateTagUsage(List<StudySessionWithStats> allSessions,
                                                       DateRange dateRange,
                                                       int topN) {
        List<StudySessionWithStats> sessions = filterSessionsInRange(allSessions, dateRange);

        if (sessions.isEmpty()) {
            return new ArrayList<>();
        }

        // Count sessions per tag
        Map<String, TagInfo> tagStats = new HashMap<>();
        int totalSessions = 0;

        for (StudySessionWithStats session : sessions) {
            String tagTitle = session.getTagTitle();
            if (tagTitle == null || tagTitle.isEmpty()) {
                tagTitle = "No tag";
            }

            TagInfo info = tagStats.get(tagTitle);
            if (info == null) {
                info = new TagInfo(tagTitle, session.getTagColor());
                tagStats.put(tagTitle, info);
            }

            info.sessionCount++;
            info.totalMinutes += session.getDurationMinutes();
            totalSessions++;
        }

        // Convert to list and sort
        List<TagUsage> usageList = new ArrayList<>();
        for (TagInfo info : tagStats.values()) {
            float percentage = (float) info.sessionCount / totalSessions * 100f;
            usageList.add(new TagUsage(
                    info.title,
                    info.color,
                    info.sessionCount,
                    info.totalMinutes,
                    percentage
            ));
        }

        Collections.sort(usageList);

        // Keep top N, group rest as "Other"
        if (usageList.size() > topN) {
            List<TagUsage> topTags = new ArrayList<>(usageList.subList(0, topN));

            int otherCount = 0;
            int otherMinutes = 0;
            for (int i = topN; i < usageList.size(); i++) {
                TagUsage data = usageList.get(i);
                otherCount += data.getSessionCount();
                otherMinutes += data.getTotalMinutes();
            }

            float otherPercentage = (float) otherCount / totalSessions * 100f;
            topTags.add(new TagUsage(
                    "Other",
                    0xFF808080, // Gray
                    otherCount,
                    otherMinutes,
                    otherPercentage
            ));

            return topTags;
        }

        return usageList;
    }

    /**
     * Generate AI recommendations by delegating to RecommendationEngine.
     * This method now simply wraps the RecommendationEngine to maintain API compatibility.
     */
    public static AIRecommendation generateAIRecommendations(
            List<StudySessionWithStats> allSessions, Context context, DateRange dateRange) {

        // Delegate to RecommendationEngine for all schedule generation logic
        ch.inf.usi.mindbricks.util.evaluation.RecommendationEngine engine =
                new ch.inf.usi.mindbricks.util.evaluation.RecommendationEngine(context);

        return engine.generateAdaptiveSchedule(allSessions, System.currentTimeMillis());
    }

    public static String getTodayPrimaryStudyTopic(List<StudySessionWithStats> sessions) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfToday = today.getTimeInMillis();

        today.add(Calendar.DAY_OF_MONTH, 1);
        long startOfTomorrow = today.getTimeInMillis();

        // Count tags from today's sessions
        Map<String, Integer> tagCounts = new HashMap<>();
        int maxCount = 0;
        String primaryTopic = null;

        for (StudySessionWithStats session : sessions) {
            if (session.getTimestamp() >= startOfToday && session.getTimestamp() < startOfTomorrow) {
                String tagTitle = session.getTagTitle();
                if (tagTitle != null && !tagTitle.isEmpty() && !tagTitle.equals("No tag")) {
                    int count = tagCounts.getOrDefault(tagTitle, 0) + 1;
                    tagCounts.put(tagTitle, count);

                    if (count > maxCount) {
                        maxCount = count;
                        primaryTopic = tagTitle;
                    }
                }
            }
        }

        return primaryTopic;
    }

    public static int getTodayTotalMinutes(List<StudySessionWithStats> sessions) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long startOfToday = today.getTimeInMillis();

        today.add(Calendar.DAY_OF_MONTH, 1);
        long startOfTomorrow = today.getTimeInMillis();

        int totalMinutes = 0;
        for (StudySessionWithStats session : sessions) {
            if (session.getTimestamp() >= startOfToday && session.getTimestamp() < startOfTomorrow) {
                totalMinutes += session.getDurationMinutes();
            }
        }

        return totalMinutes;
    }


    // helpers
    public static List<StudySessionWithStats> filterSessionsInRange(List<StudySessionWithStats> allSessions, DateRange dateRange) {
        if (allSessions == null || allSessions.isEmpty()) {
            return new ArrayList<>();
        }

        if (dateRange == null) {
            return new ArrayList<>(allSessions);
        }

        List<StudySessionWithStats> filtered = new ArrayList<>();
        int outOfRange = 0;

        for (StudySessionWithStats session : allSessions) {
            if (dateRange.contains(session.getTimestamp())) {
                filtered.add(session);
            } else {
                outOfRange++;
            }
        }

        return filtered;
    }

    // ---> Helper methods
    private static int convertCalendarDayToIndex(int calendarDay) {
        switch (calendarDay) {
            case Calendar.MONDAY: return 0;
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return 0;
        }
    }

    private static String getEndTime(int hour) {
        int endHour = (hour + 1) % 24;
        if (endHour == 0) {
            return "12:00 AM";
        } else if (endHour < 12) {
            return endHour + ":00 AM";
        } else if (endHour == 12) {
            return "12:00 PM";
        } else {
            return (endHour - 12) + ":00 PM";
        }
    }

    private static String generateSlotReason(TimeSlotStats hourStat) {
        float focusScore = hourStat.getAverageFocusScore();
        int sessionCount = hourStat.getSessionCount();

        String productivity;
        if (focusScore >= 80) {
            productivity = "excellent";
        } else if (focusScore >= 60) {
            productivity = "good";
        } else {
            productivity = "moderate";
        }

        return String.format(
                "Based on %d session%s with %s focus (%.0f%%)",
                sessionCount,
                sessionCount == 1 ? "" : "s",
                productivity,
                focusScore
        );
    }

    private static class HourScore {
        int hour;
        float focusScore;
        int sessionCount;

        HourScore(int hour, float focusScore, int sessionCount) {
            this.hour = hour;
            this.focusScore = focusScore;
            this.sessionCount = sessionCount;
        }
    }

    private static class TagInfo {
        String title;
        int color;
        int sessionCount;
        int totalMinutes;

        TagInfo(String title, int color) {
            this.title = title;
            this.color = color;
            this.sessionCount = 0;
            this.totalMinutes = 0;
        }
    }
}