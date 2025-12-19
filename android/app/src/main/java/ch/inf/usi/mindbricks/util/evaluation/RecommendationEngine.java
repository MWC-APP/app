package ch.inf.usi.mindbricks.util.evaluation;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.model.recommendation.AIRecommendation;
import ch.inf.usi.mindbricks.model.recommendation.ActivityBlock;
import ch.inf.usi.mindbricks.model.recommendation.ActivityType;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;
import ch.inf.usi.mindbricks.repository.CalendarRepository;
import ch.inf.usi.mindbricks.util.PreferencesManager;
import ch.inf.usi.mindbricks.util.UserPreferenceLoader;
import ch.inf.usi.mindbricks.util.database.DataProcessor;

/**
 * Adaptive AI Recommendation Engine
 * <p>
 * Generates personalized daily schedules that allocate EXACTLY the specified
 * number of study hours based on the user's study plan.
 *
 * @author Marta Šafářová
 * Refactored by
 * @author Luca Di Bello
 */
public class RecommendationEngine {

    private static final String TAG = "RecommendationEngine";
    private final AppDatabase database;
    private final CalendarRepository calendarRepository;
    private final PreferencesManager preferencesManager;
    private final UserPreferenceLoader preferenceLoader;

    public RecommendationEngine(Context context) {
        this.preferencesManager = new PreferencesManager(context);
        this.preferenceLoader = UserPreferenceLoader.getInstance(context);
        this.database = AppDatabase.getInstance(context);
        this.calendarRepository = new CalendarRepository(context);
    }

    public AIRecommendation generateAdaptiveSchedule(List<StudySessionWithStats> allSessions,
                                                     long targetDate) {

        Log.i(TAG, "Generating adaptive schedule for date: " + targetDate);

        AIRecommendation schedule = new AIRecommendation();
        String studyObjective = preferencesManager.getStudyObjective();

        // Get target study hours for this specific date
        int dailyGoalMinutes = preferencesManager.getDailyStudyMinutesGoal(targetDate);
        float targetStudyHours = dailyGoalMinutes / 60.0f;

        // Get today's actual study topic from sessions
        String todayStudyTopic = DataProcessor.getTodayPrimaryStudyTopic(allSessions);

        List<CalendarEvent> calendarEvents = calendarRepository.getEventsInRangeSync(getStartOfDay(targetDate), getEndOfDay(targetDate));

        schedule.setTotalSessions(allSessions.size());

        // Initialize 24-hour activity array
        ActivityType[] hourlyActivities = new ActivityType[24];

        // Step 1: Apply fixed constraints (calendar, sleep)
        applyCalendarConstraints(hourlyActivities, calendarEvents);
        applySleepSchedule(hourlyActivities);

        // Step 2: Apply scheduled activities (meals, work, exercise, social)
        applyMealTimes(hourlyActivities);
        applyWorkSchedule(hourlyActivities, targetDate);
        applyExerciseSchedule(hourlyActivities);
        applySocialTime(hourlyActivities, targetDate);

        // Step 3: Allocate EXACTLY the specified study hours
        allocateExactStudyHours(hourlyActivities, allSessions, targetStudyHours);

        // Step 4: Fill any remaining gaps with breaks
        fillRemainingSlots(hourlyActivities);

        // Step 5: Convert to activity blocks
        buildActivityBlocks(schedule, hourlyActivities);

        // Step 6: Generate summary
        schedule.setSummaryMessage(generateSummaryMessage(
                schedule,
                studyObjective,
                todayStudyTopic,
                dailyGoalMinutes
        ));

        Log.i(TAG, "Schedule generated: " + schedule.getActivityBlocks().size() + " blocks, " +
                targetStudyHours + " hours of study allocated");
        return schedule;
    }

    /**
     * Allocate EXACTLY the specified number of study hours
     * Uses historical productivity data to place study blocks in optimal times
     */
    private void allocateExactStudyHours(ActivityType[] hourlyActivities,
                                         List<StudySessionWithStats> allSessions,
                                         float targetStudyHours) {

        // Calculate productivity scores for each hour
        float[] hourlyProductivity = new float[24];
        int[] hourlySessionCount = new int[24];

        for (StudySessionWithStats session : allSessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            // NOTE: hour is for sure in range [0, 23]
            hourlyProductivity[hour] += session.getFocusScore();
            hourlySessionCount[hour]++;
        }

        // Calculate average productivity per hour
        for (int h = 0; h < 24; h++) {
            if (hourlySessionCount[h] > 0) {
                hourlyProductivity[h] /= hourlySessionCount[h];
            } else {
                // Default productivity based on time of day
                hourlyProductivity[h] = getDefaultProductivity(h);
            }
        }

        // Find available hours and sort by productivity
        List<HourScore> availableHours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            if (hourlyActivities[h] == null) {
                availableHours.add(new HourScore(h, hourlyProductivity[h]));
            }
        }

        // Sort by productivity (highest first)
        availableHours.sort((a, b) -> Float.compare(b.productivity, a.productivity));

        // Allocate exactly the target hours
        int hoursToAllocate = Math.round(targetStudyHours);
        int hoursAllocated = 0;

        for (HourScore hourScore : availableHours) {
            if (hoursAllocated >= hoursToAllocate) {
                break;
            }

            int hour = hourScore.hour;

            // Determine study type based on productivity
            if (hourScore.productivity >= 75 || hourlySessionCount[hour] >= 3) {
                hourlyActivities[hour] = ActivityType.DEEP_STUDY;
            } else if (hourScore.productivity >= 60) {
                hourlyActivities[hour] = ActivityType.LIGHT_STUDY;
            } else {
                hourlyActivities[hour] = ActivityType.LIGHT_STUDY;
            }

            hoursAllocated++;
        }

        Log.d(TAG, String.format("Allocated %d/%d study hours", hoursAllocated, hoursToAllocate));
    }

    /**
     * Get default productivity for an hour with no historical data
     */
    private float getDefaultProductivity(int hour) {
        if (hour >= 9 && hour <= 11) {
            return 85f; // Morning peak
        } else if (hour >= 14 && hour <= 16) {
            return 75f; // Afternoon
        } else if (hour >= 19 && hour <= 21) {
            return 70f; // Evening
        } else if (hour >= 6 && hour <= 8) {
            return 65f; // Early morning
        } else {
            return 50f; // Other times
        }
    }

    private static class HourScore {
        int hour;
        float productivity;

        HourScore(int hour, float productivity) {
            this.hour = hour;
            this.productivity = productivity;
        }
    }

    private void applyCalendarConstraints(ActivityType[] hourlyActivities,
                                          @NonNull List<CalendarEvent> events) {
        JsonObject calConfig = preferenceLoader.getCalendarIntegration();
        if (calConfig == null || !preferenceLoader.isEnabled(calConfig)) {
            return;
        }

        int bufferBefore = preferenceLoader.getInt(calConfig, "bufferBeforeEvent", 0);
        int bufferAfter = preferenceLoader.getInt(calConfig, "bufferAfterEvent", 0);

        for (CalendarEvent event : events) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getStartTime());
            int startHour = cal.get(Calendar.HOUR_OF_DAY);

            cal.setTimeInMillis(event.getEndTime());
            int endHour = cal.get(Calendar.HOUR_OF_DAY);
            if (endHour == 0) endHour = 24;

            // Add buffer time
            if (bufferBefore > 0) {
                startHour = Math.max(0, startHour - (bufferBefore / 60));
            }
            if (bufferAfter > 0) {
                endHour = Math.min(24, endHour + (bufferAfter / 60));
            }

            for (int h = startHour; h < endHour; h++) {
                hourlyActivities[h] = ActivityType.CALENDAR_EVENT;
            }
        }
    }

    private void applySleepSchedule(ActivityType[] hourlyActivities) {
        JsonObject sleep = preferenceLoader.getSleepSchedule();
        if (sleep == null) return;

        int bedtimeHour = preferenceLoader.getHour(sleep, "bedtime");
        int wakeupHour = preferenceLoader.getHour(sleep, "wakeupTime");

        for (int h = 0; h < 24; h++) {
            if (hourlyActivities[h] == null && isSleepHour(h, bedtimeHour, wakeupHour)) {
                hourlyActivities[h] = ActivityType.SLEEP;
            }
        }
    }

    /**
     * Check if an hour falls within sleep schedule.
     * Handles overnight sleep (e.g., 23:00 to 6:00)
     */
    private boolean isSleepHour(int hour, int bedtime, int wakeup) {
        if (bedtime < wakeup) {
            // Sleep doesn't wrap midnight (unusual case like 1am to 9am)
            return hour >= bedtime && hour < wakeup;
        } else {
            // Sleep wraps midnight (normal case like 11pm to 6am)
            return hour >= bedtime || hour < wakeup;
        }
    }

    private void applyMealTimes(ActivityType[] hourlyActivities) {
        JsonObject meals = preferenceLoader.getMealTimes();
        if (meals == null) return;

        // Process breakfast, lunch, dinner
        for (String mealType : new String[]{"breakfast", "lunch", "dinner"}) {
            JsonObject meal = meals.getAsJsonObject(mealType);
            if (meal != null && preferenceLoader.getBoolean(meal, "enabled", false)) {
                int hour = preferenceLoader.getInt(meal, "hour", -1);
                if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                    hourlyActivities[hour] = ActivityType.MEALS;
                }
            }
        }
    }

    private void applyWorkSchedule(ActivityType[] hourlyActivities,
                                   long targetDate) {
        JsonObject work = preferenceLoader.getWorkSchedule();
        if (work == null || !preferenceLoader.isEnabled(work)) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);
        String dayOfWeek = getDayOfWeekString(cal);

        List<String> workDays = preferenceLoader.getStringList(work, "workDays");
        if (!workDays.contains(dayOfWeek.toUpperCase())) return;

        int startHour = preferenceLoader.getHour(work, "startTime");
        int endHour = preferenceLoader.getHour(work, "endTime");
        boolean allowStudy = preferenceLoader.getBoolean(work, "allowStudyDuringWork", false);

        for (int h = startHour; h < endHour && h < 24; h++) {
            if (hourlyActivities[h] == null && !allowStudy) {
                hourlyActivities[h] = ActivityType.WORK;
            }
        }
    }

    private void applyExerciseSchedule(ActivityType[] hourlyActivities) {
        JsonObject exercise = preferenceLoader.getExerciseSchedule();
        if (exercise == null || !preferenceLoader.isEnabled(exercise)) return;

        if (!exercise.has("preferredTimes")) return;

        for (JsonElement element : exercise.getAsJsonArray("preferredTimes")) {
            JsonObject block = element.getAsJsonObject();
            int hour = preferenceLoader.getInt(block, "hour", -1);
            int duration = preferenceLoader.getInt(block, "duration", 0);

            if (hour >= 0 && hour < 24) {
                for (int h = hour; h < hour + (duration / 60) && h < 24; h++) {
                    if (hourlyActivities[h] == null) {
                        hourlyActivities[h] = ActivityType.EXERCISE;
                    }
                }
            }
        }
    }

    private void applySocialTime(ActivityType[] hourlyActivities,
                                 long targetDate) {
        JsonObject social = preferenceLoader.getSocialTime();
        if (social == null || !preferenceLoader.isEnabled(social)) return;

        boolean protectFromStudy = preferenceLoader.getBoolean(social, "protectFromStudy", false);
        if (!protectFromStudy) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);
        String dayOfWeek = getDayOfWeekString(cal);

        List<String> preferredDays = preferenceLoader.getStringList(social, "preferredDays");
        if (!preferredDays.contains(dayOfWeek.toUpperCase())) return;

        List<Integer> socialHours = preferenceLoader.getIntList(social, "preferredHours");
        for (int hour : socialHours) {
            if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                hourlyActivities[hour] = ActivityType.SOCIAL;
            }
        }
    }

    private void fillRemainingSlots(ActivityType[] hourlyActivities) {
        for (int h = 0; h < 24; h++) {
            if (hourlyActivities[h] == null) {
                hourlyActivities[h] = ActivityType.BREAKS;
            }
        }
    }

    private void buildActivityBlocks(AIRecommendation schedule,
                                     ActivityType[] hourlyActivities) {
        ActivityType currentActivity = hourlyActivities[0];
        int blockStart = 0;

        for (int h = 1; h < 24; h++) {
            if (hourlyActivities[h] != currentActivity) {
                schedule.addActivityBlock(new ActivityBlock(
                        currentActivity,
                        blockStart,
                        h,
                        calculateConfidence(currentActivity),
                        getActivityReason(currentActivity)
                ));

                currentActivity = hourlyActivities[h];
                blockStart = h;
            }
        }

        // Add final block
        schedule.addActivityBlock(new ActivityBlock(
                currentActivity,
                blockStart,
                24,
                calculateConfidence(currentActivity),
                getActivityReason(currentActivity)
        ));
    }

    private String generateSummaryMessage(AIRecommendation schedule,
                                          String studyObjective,
                                          String todayStudyTopic,
                                          int dailyGoalMinutes) {
        int totalSessions = schedule.getTotalSessions();
        int availableHours = schedule.getAvailableHours();
        int calendarBlocked = schedule.getCalendarBlockedHours();

        StringBuilder summary = new StringBuilder();

        if (totalSessions == 0) {
            summary.append("Start tracking sessions to build personalized recommendations. ");
        } else {
            summary.append(String.format("Based on %d sessions: ", totalSessions));
        }

        // Today's actual study topic (from sessions)
        if (todayStudyTopic != null && !todayStudyTopic.isEmpty()) {
            summary.append(String.format("Studying %s today. ", todayStudyTopic));
        } else if (studyObjective != null && !studyObjective.isEmpty()) {
            // Fallback to general objective if no sessions today
            summary.append(String.format("Focus on %s. ", studyObjective));
        }

        // Daily goal
        if (dailyGoalMinutes > 0) {
            int hours = dailyGoalMinutes / 60;
            int minutes = dailyGoalMinutes % 60;
            if (hours > 0 && minutes > 0) {
                summary.append(String.format("Target: %dh %dm. ", hours, minutes));
            } else if (hours > 0) {
                summary.append(String.format("Target: %dh. ", hours));
            } else {
                summary.append(String.format("Target: %dm. ", minutes));
            }
        }

        // Available time
        if (availableHours < 6) {
            summary.append(String.format("%dh free (busy day). ", availableHours));
        } else {
            summary.append(String.format("%dh available. ", availableHours));
        }

        if (calendarBlocked > 0) {
            summary.append(String.format("%dh in calendar. ", calendarBlocked));
        }

        // Recent energy levels
        List<PAMScore> recentScores = database.pamScoreDao().getLastNScores(3);
        if (recentScores != null && !recentScores.isEmpty()) {
            float avgRecent = 0;
            for (PAMScore score : recentScores) {
                avgRecent += score.getTotalScore();
            }
            avgRecent /= recentScores.size();

            // Use PAM thresholds from JSON or defaults
            JsonObject personalGoals = preferenceLoader.getPersonalGoals();
            int lowThreshold = 15;
            int highThreshold = 35;

            if (personalGoals != null) {
                lowThreshold = preferenceLoader.getInt(personalGoals, "lowThreshold", 15);
                highThreshold = preferenceLoader.getInt(personalGoals, "highThreshold", 35);
            }

            if (avgRecent < lowThreshold) {
                summary.append("Low energy—schedule breaks.");
            } else if (avgRecent > highThreshold) {
                summary.append("High energy—maximize focus.");
            }
        }

        return summary.toString();
    }

    private int calculateConfidence(ActivityType type) {
        return switch (type) {
            case SLEEP, CALENDAR_EVENT -> 100;
            case MEALS, EXERCISE -> 95;
            case DEEP_STUDY, LIGHT_STUDY -> 90; // Higher confidence for exact allocation
            case WORK, SOCIAL -> 80;
            case BREAKS -> 70;
        };
    }

    private String getActivityReason(ActivityType type) {
        return switch (type) {
            case DEEP_STUDY -> "Optimal time for intensive focus work";
            case LIGHT_STUDY -> "Good for review, reading, practice";
            case WORK -> "Work hours";
            case EXERCISE -> "Exercise for energy and focus";
            case SOCIAL -> "Protected social time";
            case MEALS -> "Meal time";
            case BREAKS -> "Break to maintain energy";
            case SLEEP -> "Sleep for optimal rest";
            case CALENDAR_EVENT -> "Calendar commitment";
        };
    }

    private String getDayOfWeekString(Calendar cal) {
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String[] days = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};
        return days[dayOfWeek - 1];
    }

    private long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}