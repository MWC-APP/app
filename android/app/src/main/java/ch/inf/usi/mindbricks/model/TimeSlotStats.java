package ch.inf.usi.mindbricks.model;

/**
 * Model representing statistics for a specific hour of the day across all sessions
 */
public class TimeSlotStats {

    private int hourOfDay; // 0-23
    private int totalMinutes; // Total minutes studied during this hour across all days
    private float avgFocusScore; // Average focus score for this hour
    private int sessionCount; // Number of sessions during this hour
    private float avgNoiseLevel; // Average noise level
    private float avgLightLevel; // Average light level

    public TimeSlotStats(int hourOfDay) {
        this.hourOfDay = hourOfDay;
        this.totalMinutes = 0;
        this.avgFocusScore = 0;
        this.sessionCount = 0;
        this.avgNoiseLevel = 0;
        this.avgLightLevel = 0;
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public void setHourOfDay(int hourOfDay) {
        this.hourOfDay = hourOfDay;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public void addMinutes(int minutes) {
        this.totalMinutes += minutes;
    }

    public float getAvgFocusScore() {
        return avgFocusScore;
    }

    public void setAvgFocusScore(float avgFocusScore) {
        this.avgFocusScore = avgFocusScore;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(int sessionCount) {
        this.sessionCount = sessionCount;
    }

    public void incrementSessionCount() {
        this.sessionCount++;
    }

    public float getAvgNoiseLevel() {
        return avgNoiseLevel;
    }

    public void setAvgNoiseLevel(float avgNoiseLevel) {
        this.avgNoiseLevel = avgNoiseLevel;
    }

    public float getAvgLightLevel() {
        return avgLightLevel;
    }

    public void setAvgLightLevel(float avgLightLevel) {
        this.avgLightLevel = avgLightLevel;
    }

    /**
     * Get hour label (e.g., "9 AM", "2 PM")
     */
    public String getHourLabel() {
        if (hourOfDay == 0) return "12 AM";
        if (hourOfDay == 12) return "12 PM";
        if (hourOfDay < 12) return hourOfDay + " AM";
        return (hourOfDay - 12) + " PM";
    }

    /**
     * Get time range (e.g., "09:00-10:00")
     */
    public String getTimeRange() {
        int endHour = (hourOfDay + 1) % 24;
        return String.format("%02d:00-%02d:00", hourOfDay, endHour);
    }
}