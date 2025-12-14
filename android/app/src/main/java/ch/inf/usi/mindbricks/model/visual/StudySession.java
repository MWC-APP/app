package ch.inf.usi.mindbricks.model.visual;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import ch.inf.usi.mindbricks.model.Tag;


/**
 * Model representing a completed study session with metrics
 */
@Entity(tableName = "study_sessions",
        foreignKeys = @ForeignKey(
                entity = Tag.class,
                parentColumns = "id",
                childColumns = "tagId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {@Index("tagId")})
public class StudySession {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Timestamp of when the session started
     */
    private long timestamp;

    /**
     * Total duration of the session in minutes
     */
    private int durationMinutes;

    /**
     * Foreign key reference to the Tag table
     */
    private Long tagId;

    // Focus metrics

    /**
     * Calculated focus score (0-100)
     */
    private float focusScore;

    /**
     * Number of coins earned during this study session
     */
    private int coinsEarned;

    // Optional notes
    private String notes;

    public StudySession(long timestamp, int durationMinutes, Long tagId) {
        this.timestamp = timestamp;
        this.durationMinutes = durationMinutes;
        this.tagId = tagId;
        this.focusScore = 0;
        this.coinsEarned = 0;
        this.notes = "";
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public float getFocusScore() {
        return focusScore;
    }

    public void setFocusScore(float focusScore) {
        this.focusScore = focusScore;
    }

    public int getCoinsEarned() {
        return coinsEarned;
    }

    public void setCoinsEarned(int coinsEarned) {
        this.coinsEarned = coinsEarned;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}