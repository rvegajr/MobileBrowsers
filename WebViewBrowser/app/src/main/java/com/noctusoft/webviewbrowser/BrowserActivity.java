package com.noctusoft.webviewbrowser;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.noctusoft.webviewbrowser.model.BrowsingSession;
import com.noctusoft.webviewbrowser.model.Credentials;
import com.noctusoft.webviewbrowser.model.Favorite;
import com.noctusoft.webviewbrowser.model.HistoryEntry;
import com.noctusoft.webviewbrowser.ui.FavoritesAdapter;
import com.noctusoft.webviewbrowser.ui.HistoryListActivity;
import com.noctusoft.webviewbrowser.ui.VariableManagerActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main activity for the browser app with WebView functionality.
 */
public class BrowserActivity extends AppCompatActivity {

    private static final String TAG = "BrowserActivity";
    private static final String DEFAULT_URL = "https://www.google.com";
    private static final String STATE_URL = "current_url";
    private static final String PREF_LAST_SESSION = "last_browsing_session";
    private static final String PREF_SHOW_WELCOME = "show_welcome_dialog";
    private static final int PAGE_LOAD_TIMEOUT = 30000; // 30 seconds timeout
    private static final int MAX_CLIPBOARD_SIZE = 393216; // ~384KB limit for clipboard
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int SELECTOR_TIMEOUT = 5000; // 5 seconds timeout for element selection

    // UI components
    private WebView webView;
    private EditText addressBar;
    private ProgressBar progressBar;
    private ProgressBar loadingIndicator;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton refreshButton;
    private ImageButton stopButton;
    private ImageButton favoriteButton;
    private ImageButton variablesButton;
    private ImageButton historyButton;
    private ImageButton devToolsButton;
    private ImageButton copyAllButton;
    private ImageButton selectorButton;
    private LinearLayout devToolsView;
    private TextView sourceCodeText;

    // State
    private boolean isDevToolsVisible = false;
    private boolean pageLoaded = false;
    private boolean isSelectorMode = false;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private BrowsingSession currentSession;
    private CredentialsManager credentialsManager;
    private HistoryManager historyManager;
    private VariablesManager variablesManager;
    private FavoritesManager favoritesManager;
    private String currentSelector = "";
    private AlertDialog favoritesDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        // Initialize handlers
        timeoutHandler = new Handler(getMainLooper());

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize managers
        credentialsManager = CredentialsManager.getInstance(this);
        historyManager = HistoryManager.getInstance(this);
        variablesManager = VariablesManager.getInstance(this);
        favoritesManager = FavoritesManager.getInstance(this);

        // Set up UI components
        setupUIComponents();
        setupWebView();
        setupListeners();
        setupElementSelector();

        // Check permissions once at startup
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        }

        // Show welcome dialog if needed, regardless of permissions
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if (prefs.getBoolean(PREF_SHOW_WELCOME, true)) {
            showWelcomeDialog();
        }

        // Handle intent or restore state
        handleIntentOrRestoreState(savedInstanceState);
    }

    /**
     * Sets up UI components.
     */
    private void setupUIComponents() {
        // Find views
        webView = findViewById(R.id.web_view);
        addressBar = findViewById(R.id.address_bar);
        progressBar = findViewById(R.id.progress_bar);
        loadingIndicator = findViewById(R.id.loading_indicator);
        backButton = findViewById(R.id.btn_back);
        forwardButton = findViewById(R.id.btn_forward);
        refreshButton = findViewById(R.id.btn_refresh);
        stopButton = findViewById(R.id.btn_stop);
        favoriteButton = findViewById(R.id.btn_favorite);
        variablesButton = findViewById(R.id.btn_variables);
        historyButton = findViewById(R.id.btn_history);
        devToolsButton = findViewById(R.id.btn_dev_tools);
        copyAllButton = findViewById(R.id.btn_copy_all);
        selectorButton = findViewById(R.id.selector_button);
        devToolsView = findViewById(R.id.dev_tools_view);
        sourceCodeText = findViewById(R.id.source_code_text);

        // Set initial visibility
        progressBar.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        devToolsView.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
        copyAllButton.setVisibility(View.GONE);
    }

    /**
     * Sets up the WebView component.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        // Configure WebView settings
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Add JavaScript interface for communication between WebView and app
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Set WebViewClient to handle page navigation
        webView.setWebViewClient(new CustomWebViewClient());

        // Set WebChromeClient to handle JavaScript dialogs and progress updates
        webView.setWebChromeClient(new BrowserChromeClient());

        // Enable context menu
        registerForContextMenu(webView);
    }

    /**
     * Sets up event listeners.
     */
    private void setupListeners() {
        // Address bar action listener (for GO button on keyboard)
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                loadUrl(addressBar.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Navigation buttons
        backButton.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });

        forwardButton.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        refreshButton.setOnClickListener(v -> webView.reload());

        favoriteButton.setOnClickListener(v -> toggleFavorite());

        historyButton.setOnClickListener(v -> openHistoryList());

        variablesButton.setOnClickListener(v -> openVariableManager());

        devToolsButton.setOnClickListener(v -> toggleDevTools());

        copyAllButton.setOnClickListener(v -> copyAllContent());

        // Go button
        ImageButton goButton = findViewById(R.id.btn_go);
        goButton.setOnClickListener(v -> {
            loadUrl(addressBar.getText().toString());
            hideKeyboard();
        });

        // Close button for dev tools
        Button closeButton = findViewById(R.id.btn_close_dev_tools);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> toggleDevTools());
        }
    }

    private void setupElementSelector() {
        selectorButton = findViewById(R.id.selector_button);
        selectorButton.setOnClickListener(v -> showSelectorDialog());
    }

    private void showSelectorDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selector, null);
        EditText selectorInput = dialogView.findViewById(R.id.selector_input);
        selectorInput.setText(currentSelector);

        new AlertDialog.Builder(this)
            .setTitle("Element Selector")
            .setView(dialogView)
            .setPositiveButton("Apply", (dialog, which) -> {
                String selector = selectorInput.getText().toString().trim();
                if (!selector.isEmpty()) {
                    currentSelector = selector;
                    findAndHighlightElements(selector);
                }
            })
            .setNeutralButton("Test", (dialog, which) -> {
                String selector = selectorInput.getText().toString().trim();
                if (!selector.isEmpty()) {
                    testSelector(selector);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void findAndHighlightElements(String selector) {
        String js = 
            "javascript:(function() {" +
            "   try {" +
            "       var elements = document.querySelectorAll('" + selector.replace("'", "\\'") + "');" +
            "       if (elements.length === 0) {" +
            "           Android.onElementsFound(false, 'No elements found');" +
            "           return;" +
            "       }" +
            "       var results = [];" +
            "       elements.forEach(function(el) {" +
            "           el.style.outline = '2px solid red';" +
            "           results.push(el.outerHTML);" +
            "       });" +
            "       setTimeout(function() {" +
            "           elements.forEach(function(el) {" +
            "               el.style.outline = '';" +
            "           });" +
            "       }, " + SELECTOR_TIMEOUT + ");" +
            "       Android.onElementsFound(true, JSON.stringify(results));" +
            "   } catch(e) {" +
            "       Android.onElementsFound(false, e.toString());" +
            "   }" +
            "})();";
        
        webView.evaluateJavascript(js, null);
    }

    private void testSelector(String selector) {
        String js = 
            "javascript:(function() {" +
            "   try {" +
            "       var elements = document.querySelectorAll('" + selector.replace("'", "\\'") + "');" +
            "       Android.onElementsFound(true, 'Found ' + elements.length + ' elements');" +
            "   } catch(e) {" +
            "       Android.onElementsFound(false, e.toString());" +
            "   }" +
            "})();";
        
        webView.evaluateJavascript(js, null);
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onElementsFound(boolean success, String result) {
            runOnUiThread(() -> {
                if (success) {
                    if (result.startsWith("[")) {
                        // We got HTML content
                        try {
                            JSONObject json = new JSONObject();
                            json.put("elements", new JSONArray(result));
                            copyToClipboardInChunks(json.toString(2));
                        } catch (Exception e) {
                            Toast.makeText(BrowserActivity.this, "Error processing elements: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // We got a test result
                        Toast.makeText(BrowserActivity.this, result, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BrowserActivity.this, "Error: " + result, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Handles the intent or restores the saved state.
     */
    private void handleIntentOrRestoreState(Bundle savedInstanceState) {
        // Get URL from intent
        Intent intent = getIntent();
        String url = null;

        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            url = intent.getDataString();
        }

        // If no URL in intent, try to get it from saved state
        if (url == null && savedInstanceState != null) {
            url = savedInstanceState.getString(STATE_URL);
        }

        // If still no URL, try to restore last session or use default
        if (url == null) {
            url = loadSavedSession();
        }

        // Load the URL
        if (url != null && !url.isEmpty()) {
            loadUrl(url);
        } else {
            loadUrl(DEFAULT_URL);
        }
    }

    /**
     * Loads a URL in the WebView.
     */
    private void loadUrl(String urlString) {
        // Add http:// prefix if missing
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            urlString = "https://" + urlString;
        }

        // Update address bar
        addressBar.setText(urlString);

        // Load the URL
        webView.loadUrl(urlString);
    }

    /**
     * Saves the current browsing session.
     */
    private void saveCurrentSession() {
        if (currentSession != null) {
            SharedPreferences prefs = getSharedPreferences(PREF_LAST_SESSION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("url", currentSession.getUrl());
            editor.putString("title", currentSession.getTitle());
            editor.putLong("timestamp", currentSession.getLastVisited().getTime());
            editor.apply();

            Log.d(TAG, "Saved session: " + currentSession.getUrl());
        }
    }

    /**
     * Loads the last saved browsing session.
     */
    private String loadSavedSession() {
        SharedPreferences prefs = getSharedPreferences(PREF_LAST_SESSION, Context.MODE_PRIVATE);
        String url = prefs.getString("url", null);
        String title = prefs.getString("title", "");
        long timestamp = prefs.getLong("timestamp", System.currentTimeMillis());

        if (url != null) {
            currentSession = new BrowsingSession(url, title, new Date(timestamp));
            Log.d(TAG, "Loaded session: " + url);
            return url;
        }

        return null;
    }

    /**
     * Opens the history list activity.
     */
    private void openHistoryList() {
        Intent intent = new Intent(this, HistoryListActivity.class);
        startActivity(intent);
    }

    /**
     * Opens the variable manager activity.
     */
    private void openVariableManager() {
        Intent intent = new Intent(this, VariableManagerActivity.class);
        startActivity(intent);
    }

    /**
     * Shows a dialog to manage credentials.
     */
    private void showCredentialsDialog() {
        // TODO: Implement credentials dialog
        Toast.makeText(this, "Credentials feature coming soon", Toast.LENGTH_SHORT).show();
    }

    /**
     * Toggles the visibility of the developer tools panel.
     */
    private void toggleDevTools() {
        boolean isVisible = devToolsView.getVisibility() == View.VISIBLE;
        devToolsView.setVisibility(isVisible ? View.GONE : View.VISIBLE);

        if (!isVisible) {
            // Load the HTML source
            webView.evaluateJavascript(
                    "(function() { try {" +
                            "   var content = '';" +
                            "   if (document.documentElement) {" +
                            "       content = document.documentElement.outerHTML;" +
                            "   } else if (document.body) {" +
                            "       content = document.body.outerHTML;" +
                            "   } else {" +
                            "       content = document.getElementsByTagName('*')[0].outerHTML;" +
                            "   }" +
                            "   return content || document.documentElement.innerHTML;" +
                            "} catch(e) { " +
                            "   try {" +
                            "       return document.getElementsByTagName('html')[0].outerHTML;" +
                            "   } catch(e2) {" +
                            "       return document.body.innerHTML;" +
                            "   }" +
                            "} })();",
                    html -> {
                        // Format the HTML (remove escape sequences)
                        String formattedHtml = formatHtml(html);
                        sourceCodeText.setText(formattedHtml);
                    }
            );

            // Set up close button
            Button closeButton = findViewById(R.id.btn_close_dev_tools);
            closeButton.setOnClickListener(v -> toggleDevTools());

            // Set up copy button
            Button copyButton = findViewById(R.id.btn_copy_source);
            copyButton.setOnClickListener(v -> copySourceToClipboard());
        }
    }

    /**
     * Copies the HTML source code to the clipboard
     */
    private void copySourceToClipboard() {
        CharSequence sourceCode = sourceCodeText.getText();
        if (sourceCode != null && sourceCode.length() > 0) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("HTML Source", sourceCode);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Source code copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to access clipboard", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No source code to copy", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        if (!isFinishing()) {
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (show) {
                        copyAllButton.setVisibility(View.GONE);
                        stopButton.setVisibility(View.VISIBLE);
                        refreshButton.setVisibility(View.GONE);
                    } else {
                        copyAllButton.setVisibility(View.VISIBLE);
                        stopButton.setVisibility(View.GONE);
                        refreshButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    /**
     * WebViewClient to handle page navigation and loading events.
     */
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            pageLoaded = false;
            BrowserActivity.this.showLoading(true);

            // Set a timeout for page load
            if (timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }
            timeoutRunnable = () -> {
                if (!pageLoaded) {
                    BrowserActivity.this.showLoading(false);
                    Toast.makeText(BrowserActivity.this, "Page load timed out", Toast.LENGTH_SHORT).show();
                }
            };
            timeoutHandler.postDelayed(timeoutRunnable, PAGE_LOAD_TIMEOUT);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // Inject JavaScript to check if the page is fully loaded
            view.evaluateJavascript(
                    "(function() {" +
                            "   var checkReadyState = function() {" +
                            "       if (document.readyState === 'complete') {" +
                            "           Android.onPageFullyLoaded();" +
                            "       } else {" +
                            "           setTimeout(checkReadyState, 100);" +
                            "       }" +
                            "   };" +
                            "   checkReadyState();" +
                            "   return document.readyState === 'complete';" +
                            "})();",
                    value -> {
                        if (Boolean.parseBoolean(value)) {
                            onPageFullyLoaded();
                        }
                    }
            );

            updateNavigationButtons();
            updateUrlBar(url);
            updateFavoriteButton();

            // Save to history
            if (historyManager != null) {
                historyManager.addEntry(url, view.getTitle(), null);
            }
        }
    }

    private void onPageFullyLoaded() {
        if (!isFinishing() && !pageLoaded) {  // Only process this once per page load
            pageLoaded = true;
            showLoading(false);
            if (timeoutHandler != null && timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }
        }
    }

    /**
     * Copies the HTML content of the page to the clipboard.
     */
    private void copyAllContent() {
        if (!pageLoaded) {
            Toast.makeText(this, "Please wait for the page to finish loading", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Copying Content");
        progressDialog.setMessage("Processing page content...");
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Simulate progress updates
        Handler progressHandler = new Handler(getMainLooper());
        AtomicInteger progress = new AtomicInteger(0);
        Runnable progressUpdate = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && progress.get() < 90) {  // Leave room for final processing
                    progress.addAndGet(5);
                    progressDialog.setProgress(progress.get());
                    progressHandler.postDelayed(this, 100);
                }
            }
        };
        progressHandler.post(progressUpdate);

        // Get content on background thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (isFinishing()) {
                progressDialog.dismiss();
                executor.shutdown();
                return;
            }

            webView.post(() -> {
                if (isFinishing()) {
                    progressDialog.dismiss();
                    executor.shutdown();
                    return;
                }

                webView.evaluateJavascript(
                        "(function() { try {" +
                                "   var content = '';" +
                                "   if (document.documentElement) {" +
                                "       content = document.documentElement.outerHTML;" +
                                "   } else if (document.body) {" +
                                "       content = document.body.outerHTML;" +
                                "   } else {" +
                                "       content = document.getElementsByTagName('*')[0].outerHTML;" +
                                "   }" +
                                "   return content || document.documentElement.innerHTML;" +
                                "} catch(e) { " +
                                "   try {" +
                                "       return document.getElementsByTagName('html')[0].outerHTML;" +
                                "   } catch(e2) {" +
                                "       return document.body.innerHTML;" +
                                "   }" +
                                "} })();",
                        value -> {
                            if (isFinishing()) {
                                progressDialog.dismiss();
                                executor.shutdown();
                                return;
                            }

                            if (value != null && !value.equals("null")) {
                                // Process on background thread
                                executor.execute(() -> {
                                    if (isFinishing()) {
                                        progressDialog.dismiss();
                                        executor.shutdown();
                                        return;
                                    }

                                    String html = formatHtml(value);
                                    if (html == null || html.trim().isEmpty()) {
                                        runOnUiThread(() -> {
                                            if (!isFinishing()) {
                                                progressDialog.dismiss();
                                                Toast.makeText(BrowserActivity.this, "Error: Could not access page content", Toast.LENGTH_SHORT).show();
                                            }
                                            executor.shutdown();
                                        });
                                        return;
                                    }

                                    // Update UI on main thread
                                    runOnUiThread(() -> {
                                        try {
                                            if (!isFinishing()) {
                                                progressDialog.setProgress(95);
                                                copyToClipboardInChunks(html);
                                                progressDialog.setProgress(100);
                                                progressHandler.removeCallbacks(progressUpdate);
                                                progressDialog.dismiss();
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error copying content", e);
                                            if (!isFinishing()) {
                                                progressDialog.dismiss();
                                                Toast.makeText(BrowserActivity.this, "Error copying content: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        } finally {
                                            executor.shutdown();
                                        }
                                    });
                                });
                            } else {
                                runOnUiThread(() -> {
                                    if (!isFinishing()) {
                                        progressDialog.dismiss();
                                        Toast.makeText(BrowserActivity.this, "Error: Could not access page content", Toast.LENGTH_SHORT).show();
                                    }
                                    executor.shutdown();
                                });
                            }
                        });
            });
        });
    }

    private String formatHtml(String html) {
        try {
            // Remove escaped quotes from the JavaScript string
            html = html.substring(1, html.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\u003C", "<")   // Decode \u003C to <
                    .replace("\\u003E", ">")   // Decode \u003E to >
                    .replace("\\u0026", "&")   // Decode \u0026 to &
                    .replace("\\u0027", "'")   // Decode \u0027 to '
                    .replace("\\u0022", "\""); // Decode \u0022 to "

            // Basic HTML formatting (you can enhance this further)
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inTag = false;
            boolean inScriptOrStyle = false;

            for (int i = 0; i < html.length(); i++) {
                char c = html.charAt(i);

                if (c == '<') {
                    inTag = true;
                    String peek = i + 4 <= html.length() ? html.substring(i, i + 4).toLowerCase() : "";
                    if (peek.startsWith("</")) {
                        indent = Math.max(0, indent - 1);
                    }
                    formatted.append('\n').append("  ".repeat(indent));
                }

                formatted.append(c);

                if (c == '>') {
                    inTag = false;
                    String tag = formatted.substring(formatted.lastIndexOf("<") + 1, formatted.length() - 1).trim();
                    if (!tag.startsWith("/") && !tag.endsWith("/") && !tag.startsWith("!--")) {
                        indent++;
                    }
                }
            }

            return formatted.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error formatting HTML", e);
            return html;  // Return unformatted HTML if formatting fails
        }
    }

    private void copyToClipboardInChunks(String content) {
        if (content == null || content.isEmpty()) {
            Toast.makeText(this, "No content to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (content.length() <= MAX_CLIPBOARD_SIZE) {
                // Content is small enough, copy directly
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("HTML Content", content);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Content copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                // Check for permissions first
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    isActivelySavingContent = true;
                    requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }, PERMISSION_REQUEST_CODE);
                    return;
                }

                // Content is too large, save to Downloads directory
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File outputFile = new File(downloadsDir, "webpage_" + timestamp + ".html");
                
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(content);
                }

                // Create content URI using FileProvider
                Uri contentUri = FileProvider.getUriForFile(this, 
                    getApplicationContext().getPackageName() + ".provider", 
                    outputFile);

                // Share the file
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/html");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Toast.makeText(this, "Content saved to: " + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                startActivity(Intent.createChooser(shareIntent, "Share HTML Content"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling content", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            isActivelySavingContent = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Only show settings dialog if we're actively trying to save content
                if (isActivelySavingContent) {
                    new AlertDialog.Builder(this)
                        .setTitle("Storage Permission Required")
                        .setMessage("This app needs storage permission to save large web content. " +
                                  "You can grant this permission in Settings > Apps > WebView Browser > Permissions.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", null)
                        .show();
                }
            }
        }
    }

    private boolean isActivelySavingContent = false;

    /**
     * Hides the soft keyboard.
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save current URL
        if (currentSession != null) {
            outState.putString(STATE_URL, currentSession.getUrl());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentSession();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_view_source) {
            toggleDevTools();
            return true;
        } else if (id == R.id.action_share) {
            shareCurrentPage();
            return true;
        } else if (id == R.id.action_favorites) {
            showFavoritesList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Create a context menu for long press on WebView
        WebView.HitTestResult result = webView.getHitTestResult();

        if (result.getType() == WebView.HitTestResult.ANCHOR_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            // Menu for links
            menu.setHeaderTitle(result.getExtra());
            menu.add(Menu.NONE, 1, Menu.NONE, "Open in New Window");
            menu.add(Menu.NONE, 2, Menu.NONE, "Copy Link");
            menu.add(Menu.NONE, 3, Menu.NONE, "Share Link");
        } else if (result.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            // Menu for images
            menu.add(Menu.NONE, 4, Menu.NONE, "View Image");
            menu.add(Menu.NONE, 5, Menu.NONE, "Save Image");
            menu.add(Menu.NONE, 6, Menu.NONE, "Share Image");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        WebView.HitTestResult result = webView.getHitTestResult();
        String url = result.getExtra();

        switch (item.getItemId()) {
            case 1: // Open in New Window
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                return true;
            case 2: // Copy Link
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("URL", url);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
                }
                return true;
            case 3: // Share Link
            case 6: // Share Image
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                return true;
            case 4: // View Image
                webView.loadUrl(url);
                return true;
            case 5: // Save Image
                // TODO: Implement image saving
                Toast.makeText(this, "Image saving feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Shares the current page.
     */
    private void shareCurrentPage() {
        if (currentSession != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentSession.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, currentSession.getUrl());
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    }

    /**
     * Toggles the current page as a favorite.
     */
    private void toggleFavorite() {
        String url = webView.getUrl();
        String title = webView.getTitle();

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Cannot add empty URL to favorites", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title == null || title.isEmpty()) {
            title = url;
        }

        boolean isFavorite = favoritesManager.isFavorite(url);

        if (isFavorite) {
            favoritesManager.removeFavorite(url);
            Toast.makeText(this, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show();
            favoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
        } else {
            favoritesManager.addFavorite(title, url);
            Toast.makeText(this, R.string.added_to_favorites, Toast.LENGTH_SHORT).show();
            favoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
        }
    }

    /**
     * Shows the favorites list in a dialog.
     */
    private void showFavoritesList() {
        List<Favorite> favorites = favoritesManager.getAllFavorites();

        if (favorites.isEmpty()) {
            Toast.makeText(this, R.string.empty_favorites, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog with RecyclerView
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.favorites_title);

        View view = getLayoutInflater().inflate(R.layout.dialog_favorites, null);
        RecyclerView recyclerView = view.findViewById(R.id.favorites_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create the adapter outside the dialog setup
        FavoritesAdapter favAdapter = createFavoritesAdapter(favorites);
        recyclerView.setAdapter(favAdapter);

        builder.setView(view);
        builder.setNegativeButton(R.string.cancel, null);

        // Add option to clear all favorites
        builder.setNeutralButton(R.string.delete, (dialog, which) -> {
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
            confirmBuilder.setTitle(R.string.favorites_title);
            confirmBuilder.setMessage(R.string.confirm_clear_favorites);
            confirmBuilder.setPositiveButton(R.string.yes, (dialogInterface, whichButton) -> {
                // Clear all favorites
                List<Favorite> allFavorites = favoritesManager.getAllFavorites();
                for (Favorite favorite : allFavorites) {
                    favoritesManager.removeFavorite(favorite.getUrl());
                }
                Toast.makeText(this, R.string.empty_favorites, Toast.LENGTH_SHORT).show();
                updateFavoriteButton();
                dialogInterface.dismiss();
            });
            confirmBuilder.setNegativeButton(R.string.no, null);
            confirmBuilder.show();
        });

        favoritesDialog = builder.create();
        favoritesDialog.show();
    }

    /**
     * Creates a new favorites adapter with the appropriate click listeners
     * @param favorites List of favorites to display
     * @return The configured adapter
     */
    private FavoritesAdapter createFavoritesAdapter(List<Favorite> favorites) {
        return new FavoritesAdapter(this, favorites, new FavoritesAdapter.OnFavoriteClickListener() {
            @Override
            public void onFavoriteClick(Favorite favorite) {
                // Load the URL when a favorite is clicked
                loadUrl(favorite.getUrl());
                // Dismiss the dialog
                dismissFavoritesDialog();
            }

            @Override
            public void onFavoriteDeleteClick(Favorite favorite, int position) {
                // Remove the favorite
                favoritesManager.removeFavorite(favorite.getUrl());

                // Get the adapter from the RecyclerView
                if (favoritesDialog != null) {
                    View dialogView = favoritesDialog.findViewById(R.id.favorites_recycler_view);
                    if (dialogView instanceof RecyclerView) {
                        RecyclerView recyclerView = (RecyclerView) dialogView;
                        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                        if (adapter instanceof FavoritesAdapter) {
                            ((FavoritesAdapter) adapter).removeItem(position);
                        }
                    }
                }

                // Show a toast
                Toast.makeText(BrowserActivity.this, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show();

                // If no favorites left, dismiss the dialog
                List<Favorite> remainingFavorites = favoritesManager.getAllFavorites();
                if (remainingFavorites.isEmpty()) {
                    dismissFavoritesDialog();
                    Toast.makeText(BrowserActivity.this, R.string.empty_favorites, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Helper method to safely dismiss the favorites dialog
     */
    private void dismissFavoritesDialog() {
        if (favoritesDialog != null && favoritesDialog.isShowing()) {
            favoritesDialog.dismiss();
        }
    }

    /**
     * Updates the favorite button icon based on current URL.
     */
    private void updateFavoriteButton() {
        String url = webView.getUrl();
        if (url != null && !url.isEmpty() && favoritesManager.isFavorite(url)) {
            favoriteButton.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            favoriteButton.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    /**
     * Handles form field detection from JavaScript.
     *
     * @param fieldType The type of the form field (e.g., "text", "password")
     * @param fieldName The name attribute of the form field
     */
    private void onFormField(String fieldType, String fieldName) {
        if (!isFinishing()) {
            if ("password".equalsIgnoreCase(fieldType)) {
                // Check for saved credentials
                List<Credentials> savedCredentials = credentialsManager.getCredentialsForDomain(webView.getUrl());
                if (!savedCredentials.isEmpty()) {
                    // Show saved credentials dialog
                    showCredentialsDialog();
                }
            }
        }
    }

    /**
     * WebChromeClient to handle JavaScript dialogs and progress updates.
     */
    private class BrowserChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);

            // Hide loading indicator when progress is complete
            if (newProgress == 100) {
                loadingIndicator.setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // Update current session
            if (currentSession != null) {
                currentSession = new BrowsingSession(
                        currentSession.getUrl(),
                        title,
                        currentSession.getLastVisited()
                );
            }
        }
    }

    /**
     * Updates the state of navigation buttons.
     */
    private void updateNavigationButtons() {
        backButton.setEnabled(webView.canGoBack());
        forwardButton.setEnabled(webView.canGoForward());
    }

    /**
     * Injects JavaScript to detect form fields.
     */
    private void injectFormDetectionScript() {
        String javascript = "javascript:" +
                "document.querySelectorAll('input').forEach(function(input) {" +
                "    input.addEventListener('focus', function() {" +
                "        Android.onFormField(this.type, this.name);" +
                "    });" +
                "});";

        webView.evaluateJavascript(javascript, null);
    }

    /**
     * Updates the URL bar with the current URL.
     *
     * @param url The URL to display in the address bar.
     */
    private void updateUrlBar(String url) {
        if (url != null) {
            addressBar.setText(url);
            currentSession = new BrowsingSession(url, webView.getTitle(), new Date());
        }
    }

    /**
     * Toggles the loading state of the browser.
     *
     * @param isLoading True if the page is loading, false otherwise.
     */
    private void toggleLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            loadingIndicator.setVisibility(View.GONE);
            // Update navigation buttons when loading is complete
            updateNavigationButtons();
        }
    }

    @Override
    protected void onDestroy() {
        // Clean up handlers and runnables
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }

        // Clean up WebView
        if (webView != null) {
            webView.stopLoading();
            webView.clearCache(true);
            webView.clearHistory();
            webView.destroy();
        }

        super.onDestroy();
    }

    private void showWelcomeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_welcome, null);
        CheckBox dontShowAgain = dialogView.findViewById(R.id.checkbox_dont_show);
        TextView messageView = dialogView.findViewById(R.id.welcome_message);

        String welcomeMessage = 
            "Welcome to WebView Browser!\n\n" +
            "This app includes a special feature for handling large web content:\n\n" +
            "1. Small content is copied directly to clipboard\n" +
            "2. Large content is saved to your Downloads folder\n\n" +
            "To access saved files on your computer:\n\n" +
            "Using ADB:\n" +
            "adb pull /storage/emulated/0/Download/webpage_*.html ~/Downloads/\n\n" +
            "Or mount a directory:\n" +
            "adb push ~/Downloads/ /storage/emulated/0/Download/\n\n" +
            "Files are saved with timestamps for easy identification.";

        messageView.setText(welcomeMessage);

        new AlertDialog.Builder(this)
            .setTitle("Welcome")
            .setView(dialogView)
            .setPositiveButton("Got it!", (dialog, which) -> {
                if (dontShowAgain.isChecked()) {
                    SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                    editor.putBoolean(PREF_SHOW_WELCOME, false);
                    editor.apply();
                }
            })
            .show();
    }
}
