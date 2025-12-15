package ch.inf.usi.mindbricks.util.evaluation;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.inf.usi.mindbricks.database.AppDatabase;
import ch.inf.usi.mindbricks.model.evaluation.PAMScore;
import ch.inf.usi.mindbricks.util.database.CalendarIntegrationHelper;
import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;
import ch.inf.usi.mindbricks.model.visual.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.calendar.CalendarEvent;
import ch.inf.usi.mindbricks.util.UnifiedPreferencesManager;
import ch.inf.usi.mindbricks.util.database.DataProcessor;

/**
 * Adaptive AI Recommendation Engine
 *
 * Generates personalized daily schedules that allocate EXACTLY the specified
 * number of study hours based on the user's study plan.
 */
public class RecommendationEngine {

    private static final String TAG = "RecommendationEngine";
    private final UnifiedPreferencesManager preferencesManager;
    private final AppDatabase database;
    private final CalendarIntegrationHelper calendarHelper;

    public RecommendationEngine(Context context) {
        this.preferencesManager = new UnifiedPreferencesManager(context);
        this.database = AppDatabase.getInstance(context);
        this.calendarHelper = new CalendarIntegrationHelper(context);
    }

    public AIRecommendation generateAdaptiveSchedule(List<StudySessionWithStats> allSessions,
                                                     long targetDate) {

        Log.i(TAG, "Generating adaptive schedule for date: " + targetDate);

        AIRecommendation schedule = new AIRecommendation();
        UserPreferences prefs = preferencesManager.getAdvancedPreferences();
        String studyObjective = preferencesManager.getStudyObjective();

        // Get target study hours for this specific date
        int dailyGoalMinutes = preferencesManager.getDailyStudyMinutesGoalForDate(targetDate);
        float targetStudyHours = dailyGoalMinutes / 60.0f;

        // Get today's actual study topic from sessions
        String todayStudyTopic = DataProcessor.getTodayPrimaryStudyTopic(allSessions);

        List<CalendarEvent> calendarEvents = calendarHelper.getEventsInRange(getStartOfDay(targetDate), getEndOfDay(targetDate));

        schedule.setTotalSessions(allSessions.size());

        // Initialize 24-hour activity array
        AIRecommendation.ActivityType[] hourlyActivities = new AIRecommendation.ActivityType[24];

        // Step 1: Apply fixed constraints (calendar, sleep)
        applyCalendarConstraints(hourlyActivities, calendarEvents, prefs);
        applySleepSchedule(hourlyActivities, prefs);

        // Step 2: Apply scheduled activities (meals, work, exercise, social)
        applyMealTimes(hourlyActivities, prefs);
        applyWorkSchedule(hourlyActivities, prefs, targetDate);
        applyExerciseSchedule(hourlyActivities, prefs);
        applySocialTime(hourlyActivities, prefs, targetDate);

        // Step 3: Allocate EXACTLY the specified study hours
        allocateExactStudyHours(hourlyActivities, allSessions, prefs, targetStudyHours);

        // Step 4: Fill any remaining gaps with breaks
        fillRemainingSlots(hourlyActivities);

        // Step 5: Convert to activity blocks
        float avgProductivity = calculateAvgProductivity(allSessions);
        buildActivityBlocks(schedule, hourlyActivities, avgProductivity);

        // Step 6: Generate summary
        schedule.setSummaryMessage(generateSummaryMessage(
                schedule,
                prefs,
                calendarEvents,
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
    private void allocateExactStudyHours(AIRecommendation.ActivityType[] hourlyActivities,
                                         List<StudySessionWithStats> allSessions,
                                         UserPreferences prefs,
                                         float targetStudyHours) {

        // Calculate productivity scores for each hour
        float[] hourlyProductivity = new float[24];
        int[] hourlySessionCount = new int[24];

        for (StudySessionWithStats session : allSessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            if (hour >= 0 && hour < 24) {
                hourlyProductivity[hour] += session.getFocusScore();
                hourlySessionCount[hour]++;
            }
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
        Collections.sort(availableHours, (a, b) -> Float.compare(b.productivity, a.productivity));

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
                hourlyActivities[hour] = AIRecommendation.ActivityType.DEEP_STUDY;
            } else if (hourScore.productivity >= 60) {
                hourlyActivities[hour] = AIRecommendation.ActivityType.LIGHT_STUDY;
            } else {
                hourlyActivities[hour] = AIRecommendation.ActivityType.LIGHT_STUDY;
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

    private void applyCalendarConstraints(AIRecommendation.ActivityType[] hourlyActivities,
                                          List<CalendarEvent> events,
                                          UserPreferences prefs) {
        if (prefs == null || prefs.getCalendarIntegration() == null ||
                !prefs.getCalendarIntegration().isEnabled() || events == null) {
            return;
        }

        UserPreferences.CalendarIntegration calConfig = prefs.getCalendarIntegration();

        for (CalendarEvent event : events) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(event.getStartTime());
            int startHour = cal.get(Calendar.HOUR_OF_DAY);

            cal.setTimeInMillis(event.getEndTime());
            int endHour = cal.get(Calendar.HOUR_OF_DAY);
            if (endHour == 0) endHour = 24;

            // Add buffer time
            if (calConfig.getBufferBeforeEvent() > 0) {
                startHour = Math.max(0, startHour - (calConfig.getBufferBeforeEvent() / 60));
            }
            if (calConfig.getBufferAfterEvent() > 0) {
                endHour = Math.min(24, endHour + (calConfig.getBufferAfterEvent() / 60));
            }

            for (int h = startHour; h < endHour && h < 24; h++) {
                hourlyActivities[h] = AIRecommendation.ActivityType.CALENDAR_EVENT;
            }
        }
    }

    private void applySleepSchedule(AIRecommendation.ActivityType[] hourlyActivities,
                                    UserPreferences prefs) {
        if (prefs == null || prefs.getSleepSchedule() == null) return;

        UserPreferences.SleepSchedule sleep = prefs.getSleepSchedule();
        UserPreferences.TimeOfDay bedtime = sleep.getBedtime();
        UserPreferences.TimeOfDay wakeup = sleep.getWakeupTime();

        if (bedtime == null || wakeup == null) return;

        for (int h = 0; h < 24; h++) {
            if (hourlyActivities[h] == null) {
                UserPreferences.TimeOfDay current = new UserPreferences.TimeOfDay(h, 0);
                if (current.isBetween(bedtime, wakeup)) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.SLEEP;
                }
            }
        }
    }

    private void applyMealTimes(AIRecommendation.ActivityType[] hourlyActivities,
                                UserPreferences prefs) {
        if (prefs == null || prefs.getMealTimes() == null) return;

        UserPreferences.MealTimes meals = prefs.getMealTimes();
        List<UserPreferences.MealTime> enabledMeals = meals.getAllEnabledMeals();

        for (UserPreferences.MealTime meal : enabledMeals) {
            int hour = meal.getHour();
            if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                hourlyActivities[hour] = AIRecommendation.ActivityType.MEALS;
            }
        }
    }

    private void applyWorkSchedule(AIRecommendation.ActivityType[] hourlyActivities,
                                   UserPreferences prefs,
                                   long targetDate) {
        if (prefs == null || prefs.getWorkSchedule() == null ||
                !prefs.getWorkSchedule().isEnabled()) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);
        String dayOfWeek = getDayOfWeekString(cal);

        UserPreferences.WorkSchedule work = prefs.getWorkSchedule();
        if (!work.isWorkDay(dayOfWeek)) return;

        int startHour = work.getStartTime().getHour();
        int endHour = work.getEndTime().getHour();

        for (int h = startHour; h < endHour && h < 24; h++) {
            if (hourlyActivities[h] == null && !work.isAllowStudyDuringWork()) {
                hourlyActivities[h] = AIRecommendation.ActivityType.WORK;
            }
        }
    }

    private void applyExerciseSchedule(AIRecommendation.ActivityType[] hourlyActivities,
                                       UserPreferences prefs) {
        if (prefs == null || prefs.getExerciseSchedule() == null ||
                !prefs.getExerciseSchedule().isEnabled()) return;

        UserPreferences.ExerciseSchedule exercise = prefs.getExerciseSchedule();
        if (exercise.getPreferredTimes() == null) return;

        for (UserPreferences.ExerciseBlock block : exercise.getPreferredTimes()) {
            int hour = block.getHour();
            int duration = block.getDuration();

            for (int h = hour; h < hour + (duration / 60) && h < 24; h++) {
                if (hourlyActivities[h] == null) {
                    hourlyActivities[h] = AIRecommendation.ActivityType.EXERCISE;
                }
            }
        }
    }

    private void applySocialTime(AIRecommendation.ActivityType[] hourlyActivities,
                                 UserPreferences prefs,
                                 long targetDate) {
        if (prefs == null || prefs.getSocialTime() == null ||
                !prefs.getSocialTime().isEnabled() ||
                !prefs.getSocialTime().isProtectFromStudy()) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);
        String dayOfWeek = getDayOfWeekString(cal);

        UserPreferences.SocialTime social = prefs.getSocialTime();
        if (!social.getPreferredDays().contains(dayOfWeek.toUpperCase())) return;

        List<Integer> socialHours = social.getPreferredHours();
        if (socialHours == null) return;

        for (int hour : socialHours) {
            if (hour >= 0 && hour < 24 && hourlyActivities[hour] == null) {
                hourlyActivities[hour] = AIRecommendation.ActivityType.SOCIAL;
            }
        }
    }

    private void fillRemainingSlots(AIRecommendation.ActivityType[] hourlyActivities) {
        for (int h = 0; h < 24; h++) {
            if (hourlyActivities[h] == null) {
                hourlyActivities[h] = AIRecommendation.ActivityType.BREAKS;
            }
        }
    }

    private float calculateAvgProductivity(List<StudySessionWithStats> sessions) {
        if (sessions.isEmpty()) return 0;

        float sum = 0;
        for (StudySessionWithStats session : sessions) {
            sum += session.getFocusScore();
        }
        return sum / sessions.size();
    }

    private void buildActivityBlocks(AIRecommendation schedule,
                                     AIRecommendation.ActivityType[] hourlyActivities,
                                     float avgProductivity) {
        AIRecommendation.ActivityType currentActivity = hourlyActivities[0];
        int blockStart = 0;

        for (int h = 1; h < 24; h++) {
            if (hourlyActivities[h] != currentActivity) {
                schedule.addActivityBlock(new AIRecommendation.ActivityBlock(
                        currentActivity,
                        blockStart,
                        h,
                        calculateConfidence(currentActivity),
                        getActivityReason(currentActivity, avgProductivity)
                ));

                currentActivity = hourlyActivities[h];
                blockStart = h;
            }
        }

        // Add final block
        schedule.addActivityBlock(new AIRecommendation.ActivityBlock(
                currentActivity,
                blockStart,
                24,
                calculateConfidence(currentActivity),
                getActivityReason(currentActivity, avgProductivity)
        ));
    }

    private String generateSummaryMessage(AIRecommendation schedule,
                                          UserPreferences prefs,
                                          List<CalendarEvent> events,
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
        if (recentScores != null && !recentScores.isEmpty() && prefs != null &&
                prefs.getPamThresholds() != null) {
            float avgRecent = 0;
            for (PAMScore score : recentScores) {
                avgRecent += score.getTotalScore();
            }
            avgRecent /= recentScores.size();

            if (avgRecent < prefs.getPamThresholds().getLowThreshold()) {
                summary.append("Low energy—schedule breaks.");
            } else if (avgRecent > prefs.getPamThresholds().getHighThreshold()) {
                summary.append("High energy—maximize focus.");
            }
        }

        return summary.toString();
    }

    private int calculateConfidence(AIRecommendation.ActivityType type) {
        switch (type) {
            case SLEEP:
            case CALENDAR_EVENT:
                return 100;
            case MEALS:
            case EXERCISE:
                return 95;
            case DEEP_STUDY:
            case LIGHT_STUDY:
                return 90; // Higher confidence for exact allocation
            case WORK:
            case SOCIAL:
                return 80;
            case BREAKS:
                return 70;
            default:
                return 50;
        }
    }

    private String getActivityReason(AIRecommendation.ActivityType type, float avgProductivity) {
        switch (type) {
            case DEEP_STUDY:
                return "Optimal time for intensive focus work";
            case LIGHT_STUDY:
                return "Good for review, reading, practice";
            case WORK:
                return "Work hours";
            case EXERCISE:
                return "Exercise for energy and focus";
            case SOCIAL:
                return "Protected social time";
            case MEALS:
                return "Meal time";
            case BREAKS:
                return "Break to maintain energy";
            case SLEEP:
                return "Sleep for optimal rest";
            case CALENDAR_EVENT:
                return "Calendar commitment";
            default:
                return "Flexible time";
        }
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