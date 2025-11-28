package ch.inf.usi.mindbricks.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;


/**
 * Model representing a completed study session with metrics
 */
@Entity(tableName = "study_sessions")
public class StudySession {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long timestamp; // When session started
    private int durationMinutes; // How long the session lasted
    private String tagTitle; // Which tag/subject was studied
    private int tagColor; // Color of the tag

    // Environmental metrics
    private float avgNoiseLevel; // Average noise during session (0-100)
    private float avgLightLevel; // Average light level (0-100)
    private int phonePickupCount; // How many times phone was picked up

    // Focus metrics
    private float focusScore; // Calculated focus score (0-100)
    private int coinsEarned; // Coins earned during session

    // Optional notes
    private String notes;

    @Ignore
    public StudySession() {
    }

    public StudySession(long timestamp, int durationMinutes, String tagTitle, int tagColor) {
        this.timestamp = timestamp;
        this.durationMinutes = durationMinutes;
        this.tagTitle = tagTitle;
        this.tagColor = tagColor;
        this.avgNoiseLevel = 0;
        this.avgLightLevel = 0;
        this.phonePickupCount = 0;
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

    public String getTagTitle() {
        return tagTitle;
    }

    public void setTagTitle(String tagTitle) {
        this.tagTitle = tagTitle;
    }

    public int getTagColor() {
        return tagColor;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = tagColor;
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

    public int getPhonePickupCount() {
        return phonePickupCount;
    }

    public void setPhonePickupCount(int phonePickupCount) {
        this.phonePickupCount = phonePickupCount;
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