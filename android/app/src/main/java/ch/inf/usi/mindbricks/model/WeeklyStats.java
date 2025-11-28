package ch.inf.usi.mindbricks.model;

/**
 * Model representing statistics for a single day in weekly view
 */
public class WeeklyStats {

    private String dayLabel; // "Mon", "Tue", etc.
    private int dayOfWeek; // 1-7 (Sunday = 1)
    private int totalMinutes; // Total study time for this day
    private float avgFocusScore; // Average focus score (0-100)
    private int sessionCount; // Number of sessions
    private long date; // Timestamp for this day (midnight)

    public WeeklyStats(String dayLabel, int dayOfWeek, long date) {
        this.dayLabel = dayLabel;
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.totalMinutes = 0;
        this.avgFocusScore = 0;
        this.sessionCount = 0;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(int totalMinutes) {
        this.totalMinutes = totalMinutes;
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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    /**
     * Get total study time in hours (formatted)
     */
    public float getTotalHours() {
        return totalMinutes / 60f;
    }
}