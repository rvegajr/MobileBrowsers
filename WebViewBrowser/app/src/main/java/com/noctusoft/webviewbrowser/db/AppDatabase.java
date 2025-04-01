package com.noctusoft.webviewbrowser.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.noctusoft.webviewbrowser.model.DateConverter;
import com.noctusoft.webviewbrowser.model.HistoryEntry;

/**
 * Main database class for the application.
 */
@Database(entities = {HistoryEntry.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "webviewbrowser_db";
    private static volatile AppDatabase INSTANCE;

    /**
     * Get the DAO for history operations.
     *
     * @return The history DAO
     */
    public abstract HistoryDao historyDao();

    /**
     * Get the database instance, creating it if necessary.
     *
     * @param context Application context
     * @return The database instance
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
