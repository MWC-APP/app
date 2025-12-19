package ch.inf.usi.mindbricks.model.recommendation;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ch.inf.usi.mindbricks.R;

/**
 * Model class that represents an AI-generated daily schedule.
 *
 * @author Marta Šafářová
 */
public class AIRecommendation extends DailyRecommendation{
    private final List<ActivityBlock> activityBlocks;
    private int totalSessions;
    private float averageProductivity;
    private String summaryMessage;
    private int calendarBlockedHours;

    public AIRecommendation() {
        this.activityBlocks = new ArrayList<>();
        this.totalSessions = 0;
        this.averageProductivity = 0f;
    }

    public void addActivityBlock(ActivityBlock block) {
        activityBlocks.add(block);

        if (block.getActivityType() == ActivityType.CALENDAR_EVENT) {
            calendarBlockedHours += block.getDurationHours();
        }
    }

    public List<ActivityBlock> getActivityBlocks() {
        return activityBlocks;
    }

    // Getters and Setters
    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int total) {
        this.totalSessions = total;
    }

    public float getAverageProductivity() {
        return averageProductivity;
    }

    public void setAverageProductivity(float avg) {
        this.averageProductivity = avg;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public void setSummaryMessage(String message) {
        this.summaryMessage = message;
    }

    public int getCalendarBlockedHours() {
        return calendarBlockedHours;
    }

    public int getAvailableHours() {
        int totalBlocked = 0;
        for (ActivityBlock block : activityBlocks) {
            ActivityType type = block.getActivityType();
            if (type == ActivityType.SLEEP || type == ActivityType.CALENDAR_EVENT) {
                totalBlocked += block.getDurationHours();
            }
        }
        return 24 - totalBlocked;
    }
}