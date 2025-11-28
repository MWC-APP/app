package ch.inf.usi.mindbricks.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing recommended study times for a day based on historical performance
 */
public class DailyRecommendation {

    private List<TimeSlot> recommendedSlots;
    private String reasonSummary;
    private int confidenceScore; // 0-100

    public DailyRecommendation() {
        this.recommendedSlots = new ArrayList<>();
        this.reasonSummary = "";
        this.confidenceScore = 0;
    }

    public List<TimeSlot> getRecommendedSlots() {
        return recommendedSlots;
    }

    public void setRecommendedSlots(List<TimeSlot> recommendedSlots) {
        this.recommendedSlots = recommendedSlots;
    }

    public void addRecommendedSlot(TimeSlot slot) {
        this.recommendedSlots.add(slot);
    }

    public String getReasonSummary() {
        return reasonSummary;
    }

    public void setReasonSummary(String reasonSummary) {
        this.reasonSummary = reasonSummary;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    /**
     * Inner class representing a recommended time slot
     */
    public static class TimeSlot {
        private int hourOfDay; // 0-23
        private float expectedFocusScore; // 0-100
        private String label; // e.g., "Morning Peak", "Afternoon Focus"

        public TimeSlot(int hourOfDay, float expectedFocusScore, String label) {
            this.hourOfDay = hourOfDay;
            this.expectedFocusScore = expectedFocusScore;
            this.label = label;
        }

        public int getHourOfDay() {
            return hourOfDay;
        }

        public void setHourOfDay(int hourOfDay) {
            this.hourOfDay = hourOfDay;
        }

        public float getExpectedFocusScore() {
            return expectedFocusScore;
        }

        public void setExpectedFocusScore(float expectedFocusScore) {
            this.expectedFocusScore = expectedFocusScore;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getTimeRange() {
            int endHour = (hourOfDay + 1) % 24;
            return String.format("%02d:00 - %02d:00", hourOfDay, endHour);
        }
    }
}