package com.noctusoft.webviewbrowser.model;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Type converter for Room database to handle Date objects.
 */
public class DateConverter {
    
    /**
     * Converts a timestamp to a Date object.
     *
     * @param value The timestamp as a Long.
     * @return The Date object, or null if the input was null.
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }
    
    /**
     * Converts a Date object to a timestamp.
     *
     * @param date The Date object.
     * @return The timestamp as a Long, or null if the input was null.
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
