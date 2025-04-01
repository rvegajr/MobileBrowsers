package com.noctusoft.webviewbrowser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manager class for user-defined variables that can be inserted into web forms.
 */
public class VariablesManager {
    private static final String TAG = "VariablesManager";
    private static final String SHARED_PREFS_NAME = "WebViewBrowserVariables";
    private static VariablesManager instance;

    private final SharedPreferences sharedPreferences;
    private Map<String, String> variables = new HashMap<>();

    /**
     * Gets the singleton instance of VariablesManager.
     *
     * @param context The application context.
     * @return The VariablesManager instance.
     */
    public static synchronized VariablesManager getInstance(Context context) {
        if (instance == null) {
            instance = new VariablesManager(context.getApplicationContext());
        }
        return instance;
    }

    private VariablesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        loadVariables();
        setupDefaultVariables();
    }

    /**
     * Sets a variable value.
     *
     * @param name The variable name.
     * @param value The variable value.
     */
    public void setValue(String name, String value) {
        variables.put(name, value);
        saveVariables();
        Log.d(TAG, "Set variable: " + name + " = " + value);
    }

    /**
     * Gets a variable value by name.
     *
     * @param name The variable name.
     * @return The variable value, or null if not found.
     */
    public String getValue(String name) {
        return variables.get(name);
    }

    /**
     * Removes a variable.
     *
     * @param name The variable name to remove.
     */
    public void removeVariable(String name) {
        variables.remove(name);
        saveVariables();
        Log.d(TAG, "Removed variable: " + name);
    }

    /**
     * Gets all variable names.
     *
     * @return A list of all variable names.
     */
    public List<String> getAllVariableNames() {
        List<String> names = new ArrayList<>(variables.keySet());
        java.util.Collections.sort(names);
        return names;
    }

    /**
     * Gets all variables.
     *
     * @return A map of all variables.
     */
    public Map<String, String> getAllVariables() {
        return new HashMap<>(variables);
    }

    /**
     * Sets up default variables if none exist.
     */
    private void setupDefaultVariables() {
        if (variables.isEmpty()) {
            setValue("username", "user123");
            setValue("password", "password123");
            setValue("email", "john.doe@example.com");
            setValue("phone", "123-456-7890");
            Log.d(TAG, "Set up default variables");
        }
    }

    /**
     * Loads variables from SharedPreferences.
     */
    private void loadVariables() {
        try {
            Map<String, ?> allEntries = sharedPreferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getValue() instanceof String) {
                    variables.put(entry.getKey(), (String) entry.getValue());
                }
            }
            Log.d(TAG, "Successfully loaded " + variables.size() + " variables");
        } catch (Exception e) {
            Log.e(TAG, "Error loading variables", e);
            variables = new HashMap<>();
        }
    }

    /**
     * Saves variables to SharedPreferences.
     */
    private void saveVariables() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            
            // Clear existing variables
            editor.clear();
            
            // Save all variables
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                editor.putString(entry.getKey(), entry.getValue());
            }
            
            editor.apply();
            Log.d(TAG, "Successfully saved " + variables.size() + " variables");
        } catch (Exception e) {
            Log.e(TAG, "Error saving variables", e);
        }
    }
}
