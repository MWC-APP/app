package ch.inf.usi.mindbricks.util.analytics;

import java.util.List;

import ch.inf.usi.mindbricks.model.recommendation.AIRecommendation;
import ch.inf.usi.mindbricks.model.visual.DailyRings;
import ch.inf.usi.mindbricks.model.visual.DateRange;
import ch.inf.usi.mindbricks.model.visual.HeatmapCell;
import ch.inf.usi.mindbricks.model.visual.HourlyQuality;
import ch.inf.usi.mindbricks.model.visual.StreakDay;
import ch.inf.usi.mindbricks.model.visual.StudySessionWithStats;
import ch.inf.usi.mindbricks.model.visual.TagUsage;
import ch.inf.usi.mindbricks.model.visual.TimeSlotStats;
import ch.inf.usi.mindbricks.model.visual.WeeklyStats;

/**
 * Analytics result cache to avoid recomputing the same data multiple times.
 *
 * @author Marta Šafářová
 */
public class ResultCache {
    final int sessionsHash;
    final DateRange dateRange;
    final long timestamp;

    // Cached results
    public WeeklyStats weeklyStats;
    public List<TimeSlotStats> hourlyStats;
    public AIRecommendation dailyRecommendation;
    public List<HourlyQuality> energyCurve;
    public List<HeatmapCell> heatmap;
    public List<StreakDay> streak;
    public List<DailyRings> dailyRings;
    public List<AIRecommendation> aiRecommendations;
    public List<StudySessionWithStats> filteredSessions;
    public List<TagUsage> tagUsage;

    public ResultCache(List<StudySessionWithStats> sessions, DateRange range) {
        this.sessionsHash = sessions.hashCode();
        this.dateRange = range;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isValid(List<StudySessionWithStats> sessions, DateRange range) {
        return sessions.hashCode() == this.sessionsHash
                && this.dateRange.equals(range)
                && (System.currentTimeMillis() - timestamp) < 300000; // 5 min validity
    }
}
