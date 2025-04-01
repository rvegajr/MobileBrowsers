package com.noctusoft.webviewbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.noctusoft.webviewbrowser.model.Favorite;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages favorite websites storage and retrieval.
 */
public class FavoritesManager {
    private static final String TAG = "FavoritesManager";
    private static final String PREFS_NAME = "favorites_prefs";
    private static final String KEY_FAVORITES = "favorites_list";

    private static FavoritesManager instance;
    private final SharedPreferences preferences;
    private List<Favorite> favorites;

    private FavoritesManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFavorites();
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Add a new favorite site.
     * @param title The title of the website
     * @param url The URL of the website
     * @return true if added successfully, false if already exists
     */
    public boolean addFavorite(String title, String url) {
        Favorite newFavorite = new Favorite(title, url);
        // Check if URL already exists
        for (Favorite favorite : favorites) {
            if (favorite.getUrl().equals(url)) {
                return false; // Already exists
            }
        }
        
        favorites.add(newFavorite);
        saveFavorites();
        return true;
    }

    /**
     * Remove a favorite by URL.
     * @param url URL to remove
     * @return true if removed, false if not found
     */
    public boolean removeFavorite(String url) {
        for (int i = 0; i < favorites.size(); i++) {
            if (favorites.get(i).getUrl().equals(url)) {
                favorites.remove(i);
                saveFavorites();
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a URL is in favorites.
     * @param url URL to check
     * @return true if URL is a favorite
     */
    public boolean isFavorite(String url) {
        for (Favorite favorite : favorites) {
            if (favorite.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all saved favorites.
     * @return List of favorites
     */
    public List<Favorite> getAllFavorites() {
        // Return a sorted copy of the favorites list (newest first)
        List<Favorite> sortedList = new ArrayList<>(favorites);
        Collections.sort(sortedList, new Comparator<Favorite>() {
            @Override
            public int compare(Favorite f1, Favorite f2) {
                return Long.compare(f2.getTimestamp(), f1.getTimestamp());
            }
        });
        return sortedList;
    }

    /**
     * Load favorites from SharedPreferences.
     */
    private void loadFavorites() {
        favorites = new ArrayList<>();
        String favoritesJson = preferences.getString(KEY_FAVORITES, "");
        
        if (favoritesJson.isEmpty()) {
            // Add default favorites if no favorites exist
            addDefaultFavorites();
            return;
        }
        
        try {
            JSONArray jsonArray = new JSONArray(favoritesJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String title = jsonObject.getString("title");
                String url = jsonObject.getString("url");
                long timestamp = jsonObject.getLong("timestamp");
                
                Favorite favorite = new Favorite(title, url);
                favorite.setTimestamp(timestamp);
                favorites.add(favorite);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error loading favorites: " + e.getMessage());
        }
        
        // If no favorites loaded (possibly due to error), add defaults
        if (favorites.isEmpty()) {
            addDefaultFavorites();
        }
    }

    /**
     * Add default favorite websites
     */
    private void addDefaultFavorites() {
        // Add default favorites
        addFavorite("Allied Pilots", "https://alliedpilots.org");
        addFavorite("Allied Pilots Integration", "https://integ.alliedpilots.org");
        addFavorite("Allied Pilots Expense", "https://expense.integ.alliedpilots.org");
        
        // Save the defaults to SharedPreferences
        saveFavorites();
        Log.i(TAG, "Added default favorites");
    }

    /**
     * Save favorites to SharedPreferences.
     */
    private void saveFavorites() {
        JSONArray jsonArray = new JSONArray();
        try {
            for (Favorite favorite : favorites) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", favorite.getTitle());
                jsonObject.put("url", favorite.getUrl());
                jsonObject.put("timestamp", favorite.getTimestamp());
                jsonArray.put(jsonObject);
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_FAVORITES, jsonArray.toString());
            editor.apply();
        } catch (JSONException e) {
            Log.e(TAG, "Error saving favorites: " + e.getMessage());
        }
    }
}
