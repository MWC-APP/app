package ch.inf.usi.mindbricks.util;

import android.content.Context;

import ch.inf.usi.mindbricks.model.evaluation.UserPreferences;
import ch.inf.usi.mindbricks.util.evaluation.UserPreferencesManager;
import ch.inf.usi.mindbricks.model.plan.DayHours;

import java.util.Calendar;
import java.util.List;

/**
 * Unified Preferences Manager
 *
 * Single source of truth for all user preferences, study objectives, and AI settings.
 * Consolidates PreferencesManager and UserPreferencesManager for consistent access.
 */
public class UnifiedPreferencesManager {

    private final PreferencesManager basicPrefs;
    private final UserPreferencesManager advancedPrefs;

    private static final String[] DAY_KEYS = {
            "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
    };

    public UnifiedPreferencesManager(Context context) {
        this.basicPrefs = new PreferencesManager(context);
        this.advancedPrefs = new UserPreferencesManager(context);
    }

    // Study Objectives & Goals
    public String getStudyObjective() {
        return basicPrefs.getStudyObjective();
    }

    public void setStudyObjective(String objective) {
        basicPrefs.setStudyObjective(objective);
    }

    public List<DayHours> getStudyPlan() {
        return basicPrefs.getStudyPlan();
    }

    public void setStudyPlan(List<DayHours> plan) {
        basicPrefs.setStudyPlan(plan);
    }

    public boolean isStudyGoalSet() {
        return basicPrefs.isStudyGoalSet();
    }

    /**
     * Get today's study hours from the study plan
     * @return hours planned for today, or 0 if not set
     */
    public float getTodayStudyHours() {
        return getStudyHoursForDay(Calendar.getInstance());
    }

    /**
     * Get study hours for a specific date from the study plan
     * @param calendar The date to check
     * @return hours planned for that day, or 0 if not set
     */
    public float getStudyHoursForDay(Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String dayKey = DAY_KEYS[dayOfWeek - 1]; // Calendar.SUNDAY = 1, array index 0

        List<DayHours> studyPlan = getStudyPlan();
        for (DayHours dayHours : studyPlan) {
            if (dayHours.dayKey().equalsIgnoreCase(dayKey)) {
                return dayHours.hours();
            }
        }

        return 0f; // No plan for this day
    }

    /**
     * Get today's study minutes goal from the study plan
     * Prioritizes study plan over personalGoals in JSON
     * @return minutes planned for today
     */
    public int getDailyStudyMinutesGoal() {
        // First try to get from study plan for today
        float todayHours = getTodayStudyHours();
        if (todayHours > 0) {
            return (int) (todayHours * 60);
        }

        // Fallback to personalGoals from advanced preferences
        UserPreferences prefs = advancedPrefs.loadPreferences();
        if (prefs != null && prefs.getPersonalGoals() != null) {
            return prefs.getPersonalGoals().getDailyStudyMinutes();
        }

        return 120; // default
    }

    /**
     * Get daily study minutes goal for a specific date
     * @param targetDate timestamp of the target date
     * @return minutes planned for that day
     */
    public int getDailyStudyMinutesGoalForDate(long targetDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);

        float hoursForDay = getStudyHoursForDay(cal);
        if (hoursForDay > 0) {
            return (int) (hoursForDay * 60);
        }

        // Fallback to personalGoals
        UserPreferences prefs = advancedPrefs.loadPreferences();
        if (prefs != null && prefs.getPersonalGoals() != null) {
            return prefs.getPersonalGoals().getDailyStudyMinutes();
        }

        return 120; // default
    }

    public int getTargetFocusScore() {
        UserPreferences prefs = advancedPrefs.loadPreferences();
        if (prefs != null && prefs.getPersonalGoals() != null) {
            return prefs.getPersonalGoals().getTargetFocusScore();
        }
        return 70; // default
    }

    public int getWeeklyStudySessionsGoal() {
        UserPreferences prefs = advancedPrefs.loadPreferences();
        if (prefs != null && prefs.getPersonalGoals() != null) {
            return prefs.getPersonalGoals().getWeeklyStudySessions();
        }
        return 20; // default
    }

    // Advanced Preferences Access
    public UserPreferences getAdvancedPreferences() {
        return advancedPrefs.loadPreferences();
    }

    public void saveAdvancedPreferences(UserPreferences prefs) {
        advancedPrefs.savePreferences(prefs);
    }

    // Basic Preferences Access
    public PreferencesManager getBasicPreferences() {
        return basicPrefs;
    }

    // Timer Settings
    public int getTimerStudyDuration() {
        return basicPrefs.getTimerStudyDuration();
    }

    public int getTimerShortPauseDuration() {
        return basicPrefs.getTimerShortPauseDuration();
    }

    public int getTimerLongPauseDuration() {
        return basicPrefs.getTimerLongPauseDuration();
    }

    // Clear cache (force reload)
    public void clearCache() {
        advancedPrefs.clearCache();
    }
}