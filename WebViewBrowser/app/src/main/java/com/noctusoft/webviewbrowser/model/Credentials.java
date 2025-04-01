package com.noctusoft.webviewbrowser.model;

/**
 * Represents a set of credentials (username and password) for a website.
 */
public class Credentials {
    private String username;
    private String password;

    /**
     * Creates a new credentials object.
     *
     * @param username The username.
     * @param password The password.
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password The password.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
