package com.noctusoft.webviewbrowser;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.noctusoft.webviewbrowser.model.DateConverter;
import com.noctusoft.webviewbrowser.model.HistoryEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager class for browser history.
 */
public class HistoryManager {
    private static final String TAG = "HistoryManager";
    private static final String DATABASE_NAME = "browser_history";
    private static HistoryManager instance;

    private final HistoryDatabase database;
    private final ExecutorService executorService;
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private final int maxHistoryEntries = 1000;

    /**
     * Room database definition for history entries.
     */
    @Database(entities = {HistoryEntry.class}, version = 1, exportSchema = false)
    @TypeConverters({DateConverter.class})
    public abstract static class HistoryDatabase extends RoomDatabase {
        public abstract HistoryDao historyDao();
    }

    /**
     * Gets the singleton instance of HistoryManager.
     *
     * @param context The application context.
     * @return The HistoryManager instance.
     */
    public static synchronized HistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private HistoryManager(Context context) {
        database = Room.databaseBuilder(context, HistoryDatabase.class, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build();
        executorService = Executors.newSingleThreadExecutor();
        loadHistory();
    }

    /**
     * Load history entries from database.
     */
    private void loadHistory() {
        executorService.execute(() -> {
            try {
                historyEntries = database.historyDao().getAllEntries();
                Log.d(TAG, "Loaded " + historyEntries.size() + " history entries");
            } catch (Exception e) {
                Log.e(TAG, "Error loading history", e);
                historyEntries = new ArrayList<>();
            }
        });
    }

    /**
     * Adds a new entry to the history.
     *
     * @param url The URL of the page.
     * @param title The title of the page.
     * @param iconData Optional favicon data.
     */
    public void addEntry(final String url, final String title, final byte[] iconData) {
        executorService.execute(() -> {
            try {
                // Create a new history entry
                HistoryEntry newEntry = new HistoryEntry(url, title, new Date(), iconData);

                // Remove any existing entries with the same URL
                database.historyDao().deleteByUrl(url);

                // Add the new entry
                database.historyDao().insert(newEntry);

                // Trim the history if needed
                if (getEntries().size() > maxHistoryEntries) {
                    List<HistoryEntry> entries = database.historyDao().getOldestEntriesExceedingLimit(maxHistoryEntries);
                    for (HistoryEntry entry : entries) {
                        database.historyDao().delete(entry);
                    }
                }

                // Reload history entries
                historyEntries = database.historyDao().getAllEntries();
                
                Log.d(TAG, "Added history entry: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Error adding history entry", e);
            }
        });
    }

    /**
     * Gets all history entries.
     *
     * @return A list of all history entries.
     */
    public List<HistoryEntry> getEntries() {
        return historyEntries;
    }

    /**
     * Gets history entries filtered by date.
     *
     * @param since Optional date to filter entries after.
     * @param limit Optional maximum number of entries to return.
     * @return A filtered list of history entries.
     */
    public List<HistoryEntry> getEntries(final Date since, final Integer limit) {
        List<HistoryEntry> result = new ArrayList<>();
        
        // Make a copy to avoid concurrency issues
        List<HistoryEntry> entriesCopy = new ArrayList<>(historyEntries);
        
        // Apply date filter if provided
        if (since != null) {
            for (HistoryEntry entry : entriesCopy) {
                if (entry.getTimestamp().after(since) || entry.getTimestamp().equals(since)) {
                    result.add(entry);
                }
            }
        } else {
            result.addAll(entriesCopy);
        }
        
        // Apply limit if provided
        if (limit != null && limit < result.size()) {
            result = result.subList(0, limit);
        }
        
        return result;
    }

    /**
     * Clears all history entries.
     */
    public void clearHistory() {
        executorService.execute(() -> {
            try {
                database.historyDao().deleteAll();
                historyEntries.clear();
                Log.d(TAG, "History cleared");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing history", e);
            }
        });
    }

    /**
     * Deletes a specific entry.
     *
     * @param url The URL of the entry to delete.
     */
    public void deleteEntry(final String url) {
        executorService.execute(() -> {
            try {
                database.historyDao().deleteByUrl(url);
                
                // Update in-memory list
                List<HistoryEntry> updatedList = new ArrayList<>();
                for (HistoryEntry entry : historyEntries) {
                    if (!entry.getUrl().equals(url)) {
                        updatedList.add(entry);
                    }
                }
                historyEntries = updatedList;
                
                Log.d(TAG, "Deleted history entry: " + url);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting history entry", e);
            }
        });
    }

    /**
     * Interface for DAO (Data Access Object) operations.
     */
    @androidx.room.Dao
    public interface HistoryDao {
        @androidx.room.Query("SELECT * FROM history ORDER BY timestamp DESC")
        List<HistoryEntry> getAllEntries();

        @androidx.room.Query("SELECT * FROM history WHERE url = :url LIMIT 1")
        HistoryEntry getByUrl(String url);

        @androidx.room.Insert
        void insert(HistoryEntry historyEntry);

        @androidx.room.Delete
        void delete(HistoryEntry historyEntry);

        @androidx.room.Query("DELETE FROM history WHERE url = :url")
        void deleteByUrl(String url);

        @androidx.room.Query("DELETE FROM history")
        void deleteAll();
        
        @androidx.room.Query("SELECT * FROM history ORDER BY timestamp ASC LIMIT :count")
        List<HistoryEntry> getOldestEntriesExceedingLimit(int count);
    }
}
