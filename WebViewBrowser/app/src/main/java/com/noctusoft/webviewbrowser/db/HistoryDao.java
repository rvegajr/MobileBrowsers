package com.noctusoft.webviewbrowser.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.noctusoft.webviewbrowser.model.HistoryEntry;

import java.util.List;

/**
 * Data Access Object for History entries.
 */
@Dao
public interface HistoryDao {

    /**
     * Insert a new history entry
     * 
     * @param historyEntry The history entry to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntry historyEntry);
    
    /**
     * Delete a history entry
     * 
     * @param historyEntry The history entry to delete
     */
    @Delete
    void delete(HistoryEntry historyEntry);
    
    /**
     * Delete all history entries
     */
    @Query("DELETE FROM history_entries")
    void deleteAll();
    
    /**
     * Get all history entries sorted by timestamp descending (most recent first)
     * 
     * @return List of history entries
     */
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    List<HistoryEntry> getAllHistoryEntries();
    
    /**
     * Get all history entries as LiveData
     * 
     * @return LiveData list of history entries
     */
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    LiveData<List<HistoryEntry>> getAllHistoryEntriesLive();
    
    /**
     * Search history entries by title or URL
     * 
     * @param query The search query
     * @return List of matching history entries
     */
    @Query("SELECT * FROM history_entries WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<HistoryEntry> searchHistory(String query);

    /**
     * Get a history entry by URL
     * 
     * @param url The URL to search for
     * @return The history entry if found
     */
    @Query("SELECT * FROM history_entries WHERE url = :url LIMIT 1")
    HistoryEntry getEntryByUrl(String url);
}
