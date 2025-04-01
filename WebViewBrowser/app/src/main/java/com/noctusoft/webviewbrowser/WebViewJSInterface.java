package com.noctusoft.webviewbrowser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.noctusoft.webviewbrowser.model.Credentials;

/**
 * JavaScript interface for communication between the WebView and the Android app.
 */
public class WebViewJSInterface {

    private final Context context;
    private final CredentialsManager credentialsManager;
    private final VariablesManager variablesManager;
    private final BrowserCallback callback;
    private final Handler mainHandler;

    /**
     * Interface for browser interactions.
     */
    public interface BrowserCallback {
        void onCredentialsDetected(String username, String password, String domain);
        void onLogMessage(String message, String level);
        void onInsertVariable(String variableName);
    }

    /**
     * Creates a new WebViewJSInterface.
     *
     * @param context The application context
     * @param callback The browser callback
     */
    public WebViewJSInterface(Context context, BrowserCallback callback) {
        this.context = context;
        this.credentialsManager = CredentialsManager.getInstance(context);
        this.variablesManager = VariablesManager.getInstance(context);
        this.callback = callback;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Shows a toast message on the UI thread.
     *
     * @param message The message to show
     */
    @JavascriptInterface
    public void showToast(final String message) {
        mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Logs a JavaScript console message.
     *
     * @param message The log message
     * @param level The log level (info, warn, error)
     */
    @JavascriptInterface
    public void consoleLog(final String message, final String level) {
        if (callback != null) {
            mainHandler.post(() -> callback.onLogMessage(message, level));
        }
    }

    /**
     * Detects and saves credentials from form submission.
     *
     * @param username The username
     * @param password The password
     * @param domain The domain
     */
    @JavascriptInterface
    public void detectCredentials(final String username, final String password, final String domain) {
        if (callback != null) {
            mainHandler.post(() -> callback.onCredentialsDetected(username, password, domain));
        }
    }

    /**
     * Gets a variable value by name.
     *
     * @param name The variable name
     * @return The variable value or an empty string if not found
     */
    @JavascriptInterface
    public String getVariable(String name) {
        return variablesManager.getValue(name);
    }

    /**
     * Gets all available variable names as a JSON array string.
     *
     * @return JSON array string of variable names
     */
    @JavascriptInterface
    public String getVariableNames() {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        
        for (String name : variablesManager.getAllVariableNames()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(name).append("\"");
            first = false;
        }
        
        json.append("]");
        return json.toString();
    }

    /**
     * Requests insertion of a variable into the current form.
     *
     * @param variableName The variable name to insert
     */
    @JavascriptInterface
    public void insertVariable(final String variableName) {
        if (callback != null) {
            mainHandler.post(() -> callback.onInsertVariable(variableName));
        }
    }

    /**
     * Injects the JavaScript needed for form detection and variable integration.
     *
     * @return The JavaScript to inject
     */
    public static String getInjectionScript() {
        return "javascript: (function() {\n" +
               "    // Form submission detection\n" +
               "    document.addEventListener('submit', function(event) {\n" +
               "        var forms = document.getElementsByTagName('form');\n" +
               "        for (var i = 0; i < forms.length; i++) {\n" +
               "            var form = forms[i];\n" +
               "            var usernameField = form.querySelector('input[type=\"text\"], input[type=\"email\"]');\n" +
               "            var passwordField = form.querySelector('input[type=\"password\"]');\n" +
               "            \n" +
               "            if (usernameField && passwordField) {\n" +
               "                var username = usernameField.value;\n" +
               "                var password = passwordField.value;\n" +
               "                var domain = window.location.hostname;\n" +
               "                \n" +
               "                if (username && password && domain) {\n" +
               "                    AndroidInterface.detectCredentials(username, password, domain);\n" +
               "                }\n" +
               "            }\n" +
               "        }\n" +
               "    });\n" +
               "    \n" +
               "    // Console logging\n" +
               "    var originalConsole = console;\n" +
               "    console = {\n" +
               "        log: function(msg) {\n" +
               "            originalConsole.log(msg);\n" +
               "            AndroidInterface.consoleLog(msg, 'info');\n" +
               "        },\n" +
               "        warn: function(msg) {\n" +
               "            originalConsole.warn(msg);\n" +
               "            AndroidInterface.consoleLog(msg, 'warn');\n" +
               "        },\n" +
               "        error: function(msg) {\n" +
               "            originalConsole.error(msg);\n" +
               "            AndroidInterface.consoleLog(msg, 'error');\n" +
               "        }\n" +
               "    };\n" +
               "    \n" +
               "    // Variable insertion helper\n" +
               "    window.insertVariable = function(name) {\n" +
               "        var activeElement = document.activeElement;\n" +
               "        if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA')) {\n" +
               "            var value = AndroidInterface.getVariable(name);\n" +
               "            var start = activeElement.selectionStart;\n" +
               "            var end = activeElement.selectionEnd;\n" +
               "            var text = activeElement.value;\n" +
               "            activeElement.value = text.slice(0, start) + value + text.slice(end);\n" +
               "            activeElement.selectionStart = activeElement.selectionEnd = start + value.length;\n" +
               "        }\n" +
               "    };\n" +
               "})();";
    }
}
