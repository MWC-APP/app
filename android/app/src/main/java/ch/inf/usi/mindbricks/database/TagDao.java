package ch.inf.usi.mindbricks.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ch.inf.usi.mindbricks.model.Tag;

@Dao
public interface TagDao {

    @Insert
    long insert(Tag tag);

    @Insert
    List<Long> insertAll(List<Tag> tags);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);

    @Query("DELETE FROM tags WHERE id = :tagId")
    void deleteById(long tagId);

    @Query("DELETE FROM tags")
    void deleteAll();

    // Query operations

    @Query("SELECT * FROM tags ORDER BY title ASC")
    List<Tag> getAllTags();

    @Query("SELECT * FROM tags ORDER BY title ASC")
    LiveData<List<Tag>> observeAllTags();

    @Query("SELECT * FROM tags WHERE id = :tagId")
    Tag getTagById(long tagId);

    @Query("SELECT * FROM tags WHERE id = :tagId")
    LiveData<Tag> observeTagById(long tagId);

    @Query("SELECT * FROM tags WHERE title = :title LIMIT 1")
    Tag getTagByTitle(String title);

    // Aggregation queries

    @Query("SELECT t.id, t.title, t.color, " +
           "COALESCE(SUM(s.durationMinutes), 0) as totalMinutes " +
           "FROM tags t " +
           "LEFT JOIN study_sessions s ON t.id = s.tagId " +
           "GROUP BY t.id, t.title, t.color " +
           "ORDER BY totalMinutes DESC")
    List<TagWithStats> getTagsWithTotalTime();

    @Query("SELECT t.id, t.title, t.color, " +
           "COALESCE(SUM(s.durationMinutes), 0) as totalMinutes " +
           "FROM tags t " +
           "LEFT JOIN study_sessions s ON t.id = s.tagId " +
           "GROUP BY t.id, t.title, t.color " +
           "ORDER BY totalMinutes DESC")
    LiveData<List<TagWithStats>> observeTagsWithTotalTime();

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) " +
           "FROM study_sessions " +
           "WHERE tagId = :tagId")
    int getTotalTimeForTag(long tagId);

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) " +
           "FROM study_sessions " +
           "WHERE tagId = :tagId")
    LiveData<Integer> observeTotalTimeForTag(long tagId);

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) " +
           "FROM study_sessions " +
           "WHERE tagId = :tagId AND timestamp >= :startTime AND timestamp <= :endTime")
    int getTotalTimeForTagInRange(long tagId, long startTime, long endTime);

    @Query("SELECT t.id, t.title, t.color, " +
           "COUNT(s.id) as sessionCount " +
           "FROM tags t " +
           "LEFT JOIN study_sessions s ON t.id = s.tagId " +
           "GROUP BY t.id, t.title, t.color " +
           "ORDER BY sessionCount DESC")
    List<TagWithSessionCount> getTagsWithSessionCount();

    @Query("SELECT t.id, t.title, t.color, " +
           "COALESCE(AVG(s.focusScore), 0) as avgFocusScore " +
           "FROM tags t " +
           "LEFT JOIN study_sessions s ON t.id = s.tagId " +
           "GROUP BY t.id, t.title, t.color " +
           "ORDER BY avgFocusScore DESC")
    List<TagWithAvgScore> getTagsWithAvgFocusScore();

    // Result classes for aggregation queries
    class TagWithStats {
        public long id;
        public String title;
        public int color;
        public int totalMinutes;
    }

    class TagWithSessionCount {
        public long id;
        public String title;
        public int color;
        public int sessionCount;
    }

    class TagWithAvgScore {
        public long id;
        public String title;
        public int color;
        public float avgFocusScore;
    }
}
