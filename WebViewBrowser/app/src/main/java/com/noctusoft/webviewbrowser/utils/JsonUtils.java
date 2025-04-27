package com.noctusoft.webviewbrowser.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Utility class for JSON operations.
 */
public class JsonUtils {

    /**
     * Format JSON string with proper indentation.
     *
     * @param jsonString The JSON string to format
     * @return Formatted JSON string
     */
    public static String formatJson(String jsonString) {
        try {
            if (jsonString.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(jsonString);
                return jsonObject.toString(2);
            } else if (jsonString.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonString);
                return jsonArray.toString(2);
            } else {
                return jsonString;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return jsonString;
        }
    }

    /**
     * Convert a JSONObject to a human-readable string.
     *
     * @param json The JSONObject to convert
     * @return A string representation
     */
    public static String stringify(JSONObject json) {
        try {
            return json.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return json.toString();
        }
    }

    /**
     * Convert a JSONArray to a human-readable string.
     *
     * @param json The JSONArray to convert
     * @return A string representation
     */
    public static String stringify(JSONArray json) {
        try {
            return json.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return json.toString();
        }
    }

    /**
     * Merge two JSONObjects.
     *
     * @param source The source JSONObject
     * @param target The target JSONObject to merge into
     * @return Merged JSONObject
     */
    public static JSONObject merge(JSONObject source, JSONObject target) throws JSONException {
        Iterator<String> keys = source.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = source.get(key);
            if (!target.has(key)) {
                target.put(key, value);
            } else if (value instanceof JSONObject) {
                if (target.get(key) instanceof JSONObject) {
                    target.put(key, merge((JSONObject) value, (JSONObject) target.get(key)));
                } else {
                    target.put(key, value);
                }
            } else {
                target.put(key, value);
            }
        }
        return target;
    }
    
    /**
     * Unescape JavaScript string from evaluateJavascript.
     * 
     * @param escapedJs JavaScript string often returned from evaluateJavascript
     * @return Unescaped string with quotes removed
     */
    public static String unescapeJavaScript(String escapedJs) {
        if (escapedJs == null || escapedJs.isEmpty()) {
            return "";
        }
        
        // Remove outer quotes if present (usually the case with evaluateJavascript results)
        if (escapedJs.startsWith("\"") && escapedJs.endsWith("\"")) {
            escapedJs = escapedJs.substring(1, escapedJs.length() - 1);
        }
        
        // Handle common escape sequences
        return escapedJs
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\f", "\f");
    }
}
