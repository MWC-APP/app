package ch.inf.usi.mindbricks.model.recommendation;

public class ActivityBlock {
    private final ActivityType activityType;
    private final int startHour;
    private final int endHour;
    private final int confidenceScore;
    private final String reason;
    private String eventTitle;

    public ActivityBlock(ActivityType type, int startHour, int endHour,
                         int confidence, String reason) {
        this.activityType = type;
        this.startHour = startHour;
        this.endHour = endHour;
        this.confidenceScore = confidence;
        this.reason = reason;
    }

    public ActivityBlock(ActivityType type, int startHour, int endHour,
                         String eventTitle, String reason) {
        this.activityType = type;
        this.startHour = startHour;
        this.endHour = endHour;
        this.eventTitle = eventTitle;
        this.reason = reason;
        this.confidenceScore = 100;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public int getStartHour() {
        return startHour;
    }

    public int getEndHour() {
        return endHour;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public String getReason() {
        return reason;
    }

    public int getDurationHours() {
        return endHour - startHour;
    }

    public String getTimeRange() {
        return formatHour(startHour) + " - " + formatHour(endHour);
    }

    private String formatHour(int hour) {
        if (hour == 0) return "12 AM";
        if (hour < 12) return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }

    public String getDisplayName() {
        if (activityType == ActivityType.CALENDAR_EVENT && eventTitle != null) {
            return eventTitle;
        }
        return activityType.getDisplayName();
    }
}
