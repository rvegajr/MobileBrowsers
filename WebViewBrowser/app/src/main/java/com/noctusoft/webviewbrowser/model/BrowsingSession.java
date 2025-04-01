package com.noctusoft.webviewbrowser.model;

import java.util.Date;

/**
 * Represents a browsing session with a URL, title, and timestamp.
 */
public class BrowsingSession {
    private String url;
    private String title;
    private Date lastVisited;

    /**
     * Creates a new browsing session.
     *
     * @param url The URL of the page.
     * @param title The title of the page.
     */
    public BrowsingSession(String url, String title) {
        this(url, title, new Date());
    }

    /**
     * Creates a new browsing session with a specific timestamp.
     *
     * @param url The URL of the page.
     * @param title The title of the page.
     * @param lastVisited The timestamp of the visit.
     */
    public BrowsingSession(String url, String title, Date lastVisited) {
        this.url = url;
        this.title = title;
        this.lastVisited = lastVisited;
    }

    /**
     * Gets the URL of the page.
     *
     * @return The URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the URL of the page.
     *
     * @param url The URL.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the title of the page.
     *
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the page.
     *
     * @param title The title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the timestamp of the visit.
     *
     * @return The timestamp.
     */
    public Date getLastVisited() {
        return lastVisited;
    }

    /**
     * Sets the timestamp of the visit.
     *
     * @param lastVisited The timestamp.
     */
    public void setLastVisited(Date lastVisited) {
        this.lastVisited = lastVisited;
    }
}
