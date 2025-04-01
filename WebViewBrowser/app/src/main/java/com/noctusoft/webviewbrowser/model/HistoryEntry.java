package com.noctusoft.webviewbrowser.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity class representing a browser history entry for storage in Room database.
 */
@Entity(tableName = "history")
public class HistoryEntry {
    
    @PrimaryKey
    @NonNull
    private String id;
    
    @NonNull
    private String url;
    
    private String title;
    
    @TypeConverters(DateConverter.class)
    private Date timestamp;
    
    private byte[] iconData;
    
    /**
     * Creates a new history entry.
     *
     * @param url The URL of the page.
     * @param title The title of the page.
     */
    @Ignore
    public HistoryEntry(@NonNull String url, String title) {
        this(url, title, new Date(), null);
    }
    
    /**
     * Creates a new history entry with specific timestamp and icon data.
     *
     * @param url The URL of the page.
     * @param title The title of the page.
     * @param timestamp The timestamp of the visit.
     * @param iconData The favicon data as bytes.
     */
    public HistoryEntry(@NonNull String url, String title, Date timestamp, byte[] iconData) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.title = title;
        this.timestamp = timestamp;
        this.iconData = iconData;
    }
    
    @NonNull
    public String getId() {
        return id;
    }
    
    public void setId(@NonNull String id) {
        this.id = id;
    }
    
    @NonNull
    public String getUrl() {
        return url;
    }
    
    public void setUrl(@NonNull String url) {
        this.url = url;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    
    public byte[] getIconData() {
        return iconData;
    }
    
    public void setIconData(byte[] iconData) {
        this.iconData = iconData;
    }
    
    /**
     * Converts this history entry to a browsing session.
     *
     * @return A BrowsingSession object.
     */
    public BrowsingSession toBrowsingSession() {
        return new BrowsingSession(url, title, timestamp);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryEntry that = (HistoryEntry) o;
        return url.equals(that.url);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(url);
    }
}
