package ch.inf.usi.mindbricks.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags")
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Title/name of the tag
     */
    private String title;

    /**
     * Color representation (Android color int)
     */
    private int color;

    /**
     * Constructor for creating a new tag
     */
    public Tag(String title, int color) {
        this.title = title;
        this.color = color;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", color=" + color +
                '}';
    }
}
