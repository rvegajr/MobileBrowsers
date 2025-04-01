package com.noctusoft.webviewbrowser.model;

/**
 * Represents a favorite website bookmark.
 */
public class Favorite {
    private String title;
    private String url;
    private long timestamp;

    public Favorite(String title, String url) {
        this.title = title;
        this.url = url;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Favorite favorite = (Favorite) o;
        return url.equals(favorite.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
