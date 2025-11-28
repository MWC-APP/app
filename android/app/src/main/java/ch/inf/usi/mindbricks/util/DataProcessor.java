package ch.inf.usi.mindbricks.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.inf.usi.mindbricks.model.DailyRecommendation;
import ch.inf.usi.mindbricks.model.StudySession;
import ch.inf.usi.mindbricks.model.TimeSlotStats;
import ch.inf.usi.mindbricks.model.WeeklyStats;

/**
 * Utility class for processing and analyzing study session data
 */
public class DataProcessor {

    /**
     * Get sessions from the last N days
     */
    public static List<StudySession> getRecentSessions(List<StudySession> allSessions, int days) {
        if (allSessions == null || allSessions.isEmpty()) {
            return new ArrayList<>();
        }

        long cutoffTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);
        List<StudySession> recent = new ArrayList<>();

        for (StudySession session : allSessions) {
            if (session.getTimestamp() >= cutoffTime) {
                recent.add(session);
            }
        }

        return recent;
    }

    /**
     * Generate daily recommendations based on historical performance
     */
    public static DailyRecommendation generateDailyRecommendation(List<StudySession> sessions) {
        DailyRecommendation recommendation = new DailyRecommendation();

        if (sessions == null || sessions.isEmpty()) {
            recommendation.setReasonSummary("Not enough data yet. Complete more sessions to get personalized recommendations.");
            recommendation.setConfidenceScore(0);
            return recommendation;
        }

        // Calculate average focus score by hour
        Map<Integer, List<Float>> focusScoresByHour = new HashMap<>();
        for (StudySession session : sessions) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(session.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            if (!focusScoresByHour.containsKey(hour)) {
                focusScoresByHour.put(hour, new ArrayList<>());
            }
            focusScoresByHour.get(hour).add(session.getFocusScore());
        }

        // Find top 3 hours with best average focus
        List<HourScore> hourScores = new ArrayList<>();
        for (Map.Entry<Integer, List<Float>> entry : focusScoresByHour.entrySet()) {
            int hour = entry.getKey();
            List<Float> scores = entry.getValue();

            if (scores.size() >= 3) {
                // Only consider hours with enough data
                float avgScore = 0;
                for (float score : scores) {
                    avgScore += score;
                }
                avgScore /= scores.size();
                hourScores.add(new HourScore(hour, avgScore));
            }
        }

        Collections.sort(hourScores, (a, b) -> Float.compare(b.score, a.score));

        // Add top 3 recommendations
        String[] labels = {"Peak Performance", "High Focus", "Good Focus"};
        for (int i = 0; i < Math.min(3, hourScores.size()); i++) {
            HourScore hs = hourScores.get(i);
            recommendation.addRecommendedSlot(
                    new DailyRecommendation.TimeSlot(hs.hour, hs.score, labels[i])
            );
        }

        // Set confidence based on data amount
        int confidence = Math.min(100, (sessions.size() * 100) / 30); // Max at 30 sessions
        recommendation.setConfidenceScore(confidence);

        if (hourScores.size() >= 3) {
            recommendation.setReasonSummary(
                    String.format("Based on %d sessions, you focus best around %s",
                            sessions.size(),
                            formatHour(hourScores.get(0).hour))
            );
        } else {
            recommendation.setReasonSummary("Complete more sessions for better recommendations.");
        }

        return recommendation;
    }

    /**
     * Calculate weekly statistics for the last 7 days
     */
    public static List<WeeklyStats> calculateWeeklyStats(List<StudySession> sessions) {
        List<WeeklyStats> weeklyStats = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String[] dayLabels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_MONTH, -i);

            int dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK);
            String label = dayLabels[dayOfWeek - 1];

            WeeklyStats stats = new WeeklyStats(label, dayOfWeek, dayCal.getTimeInMillis());
            weeklyStats.add(stats);
        }

        // Populate stats with session data
        if (sessions != null) {
            for (StudySession session : sessions) {
                Calendar sessionCal = Calendar.getInstance();
                sessionCal.setTimeInMillis(session.getTimestamp());
                sessionCal.set(Calendar.HOUR_OF_DAY, 0);
                sessionCal.set(Calendar.MINUTE, 0);
                sessionCal.set(Calendar.SECOND, 0);
                sessionCal.set(Calendar.MILLISECOND, 0);

                long sessionDate = sessionCal.getTimeInMillis();

                for (WeeklyStats stats : weeklyStats) {
                    if (stats.getDate() == sessionDate) {
                        stats.setTotalMinutes(stats.getTotalMinutes() + session.getDurationMinutes());

                        // Update average focus score
                        float currentAvg = stats.getAvgFocusScore();
                        int count = stats.getSessionCount();
                        float newAvg = (currentAvg * count + session.getFocusScore()) / (count + 1);
                        stats.setAvgFocusScore(newAvg);

                        stats.setSessionCount(count + 1);
                        break;
                    }
                }
            }
        }

        return weeklyStats;
    }

    /**
     * Calculate hourly distribution statistics
     */
    public static List<TimeSlotStats> calculateHourlyStats(List<StudySession> sessions) {
        // Create stats for each hour of the day
        Map<Integer, TimeSlotStats> hourlyStatsMap = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyStatsMap.put(hour, new TimeSlotStats(hour));
        }

        if (sessions != null) {
            // Aggregate session data by hour
            for (StudySession session : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(session.getTimestamp());
                int hour = cal.get(Calendar.HOUR_OF_DAY);

                TimeSlotStats stats = hourlyStatsMap.get(hour);
                stats.addMinutes(session.getDurationMinutes());

                // Update average focus score
                float currentAvg = stats.getAvgFocusScore();
                int count = stats.getSessionCount();
                float newAvg = (currentAvg * count + session.getFocusScore()) / (count + 1);
                stats.setAvgFocusScore(newAvg);

                // Update average noise and light
                float currentNoise = stats.getAvgNoiseLevel();
                float newNoise = (currentNoise * count + session.getAvgNoiseLevel()) / (count + 1);
                stats.setAvgNoiseLevel(newNoise);

                float currentLight = stats.getAvgLightLevel();
                float newLight = (currentLight * count + session.getAvgLightLevel()) / (count + 1);
                stats.setAvgLightLevel(newLight);

                stats.incrementSessionCount();
            }
        }

        List<TimeSlotStats> statsList = new ArrayList<>(hourlyStatsMap.values());
        Collections.sort(statsList, (a, b) -> Integer.compare(a.getHourOfDay(), b.getHourOfDay()));

        return statsList;
    }

    private static String formatHour(int hour) {
        if (hour == 0) return "midnight";
        if (hour == 12) return "noon";
        if (hour < 12) return hour + " AM";
        return (hour - 12) + " PM";
    }

    /**
     * Helper class for sorting hours by score
     */
    private static class HourScore {
        int hour;
        float score;

        HourScore(int hour, float score) {
            this.hour = hour;
            this.score = score;
        }
    }
}