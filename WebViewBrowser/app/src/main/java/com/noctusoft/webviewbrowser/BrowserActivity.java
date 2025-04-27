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
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

import com.noctusoft.webviewbrowser.model.BrowsingSession;
import com.noctusoft.webviewbrowser.model.Credentials;
import com.noctusoft.webviewbrowser.model.Favorite;
import com.noctusoft.webviewbrowser.model.HistoryEntry;
import com.noctusoft.webviewbrowser.ui.FavoritesAdapter;
import com.noctusoft.webviewbrowser.ui.HistoryListActivity;
import com.noctusoft.webviewbrowser.ui.VariableManagerActivity;

import com.noctusoft.webviewbrowser.utils.JsonUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

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
    private static final int DEV_TOOLS_TAB_SOURCE = 0;
    private static final int DEV_TOOLS_TAB_CONSOLE = 1;

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
    private View devToolsView;
    private TextView sourceCodeText;
    private RecyclerView consoleLogRecyclerView;
    private ConsoleLogAdapter consoleLogAdapter;
    private List<ConsoleLogEntry> consoleLogEntries;
    private boolean isConsoleVisible = false;
    private Button consoleToggleButton;
    private RadioGroup segmentedControl;
    private ScrollView sourceContainer;

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
        
        // Initialize console logs collection
        consoleLogEntries = new ArrayList<>();

        // Set up UI components
        setupUIComponents();
        setupWebView();
        setupListeners();
        setupElementSelector();

        // Note: Console logger is now injected in onPageFinished to ensure proper timing

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

        // Set initial visibility
        progressBar.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
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

        // Allow mixed content (HTTP in HTTPS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Add JavaScript interface for communication between WebView and app
        webView.removeJavascriptInterface("Android"); // Remove any existing interface first
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        
        // Log that the interface was added
        Log.d(TAG, "Added JavaScript interface 'Android' to WebView");

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
        
        @JavascriptInterface
        public void consoleLog(String type, String message) {
            Log.d(TAG, "Console " + type + ": " + message);
            
            // Create an entry directly
            final ConsoleLogEntry entry = new ConsoleLogEntry(type, message, new Date());
            
            runOnUiThread(() -> {
                try {
                    // Ensure we have a list
                    if (consoleLogEntries == null) {
                        consoleLogEntries = new ArrayList<>();
                    }
                    
                    // Add the entry to the list
                    consoleLogEntries.add(entry);
                    
                    // Initialize adapter if needed and view is available
                    if (consoleLogAdapter == null && consoleLogRecyclerView != null) {
                        consoleLogAdapter = new ConsoleLogAdapter(consoleLogEntries);
                        consoleLogRecyclerView.setLayoutManager(new LinearLayoutManager(BrowserActivity.this));
                        consoleLogRecyclerView.setAdapter(consoleLogAdapter);
                        Log.d(TAG, "Created new console adapter");
                    }
                    
                    // Update adapter if available
                    if (consoleLogAdapter != null) {
                        consoleLogAdapter.notifyDataSetChanged();
                        
                        // Scroll to show latest message if visible
                        if (consoleLogRecyclerView != null && 
                            isDevToolsVisible && 
                            segmentedControl != null && 
                            segmentedControl.getCheckedRadioButtonId() == R.id.tab_console) {
                            
                            consoleLogRecyclerView.post(() -> {
                                consoleLogRecyclerView.scrollToPosition(consoleLogEntries.size() - 1);
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling console log: " + e.getMessage(), e);
                }
            });
        }
        
        @JavascriptInterface
        public void onPageFullyLoaded() {
            Log.d(TAG, "Page fully loaded callback from JavaScript");
            runOnUiThread(() -> {
                if (!isFinishing()) {
                    pageLoaded = true;
                    showLoading(false);
                    
                    // Make sure console logger is injected after page is fully loaded
                    injectConsoleLogger();
                    
                    if (timeoutHandler != null && timeoutRunnable != null) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                    }
                }
            });
        }
        
        // Additional methods to help with diagnostics
        @JavascriptInterface
        public boolean isInterfaceAvailable() {
            // Simple method for JavaScript to test if the interface is working
            Log.d(TAG, "isInterfaceAvailable() called from JavaScript");
            return true;
        }
        
        @JavascriptInterface
        public String getInterfaceVersion() {
            return "1.0";
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
     * Toggle the developer tools panel visibility
     */
    private void toggleDevTools() {
        if (devToolsView == null) {
            // Inflate the dev tools layout
            devToolsView = getLayoutInflater().inflate(R.layout.layout_dev_tools, null);
            // Initialize components
            initDevToolsComponents();
        }
        
        // Get the parent view container
        FrameLayout webViewContainer = findViewById(R.id.web_view_container);
        if (webViewContainer == null) {
            Log.e(TAG, "Cannot toggle developer tools: web view container not found");
            return;
        }
        
        // If dev tools are currently shown, hide them
        if (isDevToolsVisible) {
            // Check if devToolsView has a parent, then remove it
            ViewGroup parent = (ViewGroup) devToolsView.getParent();
            if (parent != null) {
                // Animate hiding
                devToolsView.animate()
                    .translationY(devToolsView.getHeight())
                    .alpha(0.0f)
                    .setDuration(250)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            parent.removeView(devToolsView);
                            isDevToolsVisible = false;
                        }
                    });
            } else {
                isDevToolsVisible = false;
            }
        } else {
            // First make sure devToolsView doesn't have a parent
            ViewGroup parent = (ViewGroup) devToolsView.getParent();
            if (parent != null) {
                parent.removeView(devToolsView);
            }
            
            // Get device screen height
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenHeight = displayMetrics.heightPixels;
            
            // Calculate dev tools height (about 40% of screen height)
            int devToolsHeight = (int) (screenHeight * 0.4);
            ViewGroup.LayoutParams params = devToolsView.getLayoutParams();
            if (params == null) {
                params = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        devToolsHeight);
            } else {
                params.height = devToolsHeight;
            }
            devToolsView.setLayoutParams(params);
            
            // Set initial translation for animation
            devToolsView.setTranslationY(devToolsHeight);
            devToolsView.setAlpha(0.0f);
            
            // Add dev tools to container and show with animation
            webViewContainer.addView(devToolsView);
            devToolsView.animate()
                .translationY(0)
                .alpha(1.0f)
                .setDuration(250)
                .setListener(null);
            
            isDevToolsVisible = true;
            
            // Load source code immediately regardless of which tab is selected
            loadSourceCode();
            
            // Make sure console logger is injected
            injectConsoleLogger();
        }
    }
    
    /**
     * Initialize developer tools UI components
     */
    private void initDevToolsComponents() {
        if (devToolsView == null) return;
        
        // Get references to views
        sourceCodeText = devToolsView.findViewById(R.id.source_code_text);
        consoleLogRecyclerView = devToolsView.findViewById(R.id.console_log_recycler_view);
        sourceContainer = devToolsView.findViewById(R.id.source_container);
        segmentedControl = devToolsView.findViewById(R.id.dev_tools_tabs);
        
        // Set up toolbar with close button
        Toolbar toolbar = devToolsView.findViewById(R.id.dev_tools_toolbar);
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
        toolbar.setNavigationOnClickListener(v -> toggleDevTools());
        
        // Set up toolbar action buttons
        Button clearButton = devToolsView.findViewById(R.id.btn_clear_logs);
        Button copyButton = devToolsView.findViewById(R.id.btn_copy_content);
        Button testLogButton = devToolsView.findViewById(R.id.btn_test_log);
        
        clearButton.setOnClickListener(v -> clearContent());
        copyButton.setOnClickListener(v -> copyContent());
        testLogButton.setOnClickListener(v -> testConsoleLog());
        
        // Set up console log recycler view
        if (consoleLogEntries == null) {
            consoleLogEntries = new ArrayList<>();
            Log.d(TAG, "Created new consoleLogEntries list");
        }
        
        // Force recreation of adapter for each initialization
        consoleLogAdapter = new ConsoleLogAdapter(consoleLogEntries);
        consoleLogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        consoleLogRecyclerView.setAdapter(consoleLogAdapter);
        
        // Add sample log to verify adapter setup
        if (consoleLogEntries.isEmpty()) {
            ConsoleLogEntry entry = new ConsoleLogEntry("info", "DevTools initialized - waiting for console logs", new Date());
            consoleLogEntries.add(entry);
            consoleLogAdapter.notifyDataSetChanged();
        }
        
        Log.d(TAG, "Console adapter setup complete with " + consoleLogEntries.size() + " entries");
        
        // Get references to tab buttons for styling
        RadioButton sourceTab = devToolsView.findViewById(R.id.tab_source);
        RadioButton consoleTab = devToolsView.findViewById(R.id.tab_console);
        
        // Set up tab selection listener
        segmentedControl.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.tab_source) {
                // Show source, hide console
                sourceContainer.setVisibility(View.VISIBLE);
                consoleLogRecyclerView.setVisibility(View.GONE);
                
                // Update tab appearance
                sourceTab.setBackgroundColor(Color.WHITE);
                sourceTab.setTextColor(Color.parseColor("#444444"));
                sourceTab.setTypeface(null, Typeface.BOLD);
                
                consoleTab.setBackgroundColor(Color.parseColor("#DDDDDD"));
                consoleTab.setTextColor(Color.parseColor("#444444"));
                consoleTab.setTypeface(null, Typeface.NORMAL);
                
                // Update source code if it's not already loaded
                if (sourceCodeText.getText().toString().isEmpty()) {
                    loadSourceCode();
                }
            } else if (checkedId == R.id.tab_console) {
                // Show console, hide source
                sourceContainer.setVisibility(View.GONE);
                consoleLogRecyclerView.setVisibility(View.VISIBLE);
                
                // Update tab appearance
                consoleTab.setBackgroundColor(Color.WHITE);
                consoleTab.setTextColor(Color.parseColor("#444444"));
                consoleTab.setTypeface(null, Typeface.BOLD);
                
                sourceTab.setBackgroundColor(Color.parseColor("#DDDDDD"));
                sourceTab.setTextColor(Color.parseColor("#444444"));
                sourceTab.setTypeface(null, Typeface.NORMAL);
                
                // Inject console logger and verify it's working
                injectConsoleLogger();
                
                // Verify list and adapter are working
                if (consoleLogAdapter != null) {
                    consoleLogAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Refreshed console log adapter with " + consoleLogEntries.size() + " entries");
                    
                    // Scroll to latest log if there are entries
                    if (!consoleLogEntries.isEmpty()) {
                        consoleLogRecyclerView.scrollToPosition(consoleLogEntries.size() - 1);
                    }
                }
            }
        });
        
        // Make sure initial tab styling is applied
        if (segmentedControl.getCheckedRadioButtonId() == R.id.tab_source) {
            sourceTab.setBackgroundColor(Color.WHITE);
            sourceTab.setTextColor(Color.parseColor("#444444"));
            sourceTab.setTypeface(null, Typeface.BOLD);
            
            consoleTab.setBackgroundColor(Color.parseColor("#DDDDDD"));
            consoleTab.setTextColor(Color.parseColor("#444444"));
            consoleTab.setTypeface(null, Typeface.NORMAL);
        } else {
            consoleTab.setBackgroundColor(Color.WHITE);
            consoleTab.setTextColor(Color.parseColor("#444444"));
            consoleTab.setTypeface(null, Typeface.BOLD);
            
            sourceTab.setBackgroundColor(Color.parseColor("#DDDDDD"));
            sourceTab.setTextColor(Color.parseColor("#444444"));
            sourceTab.setTypeface(null, Typeface.NORMAL);
        }
    }
    
    /**
     * Load source code for the current page
     */
    private void loadSourceCode() {
        if (webView == null || sourceCodeText == null) {
            return;
        }
        
        // Show loading indicator in source code view
        sourceCodeText.setText("Loading source code...");
        
        // Get source code of the current page
        webView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();",
                html -> {
                    if (html == null || html.equals("null")) {
                        sourceCodeText.setText("No source code available");
                        return;
                    }
                    
                    // Parse and format the HTML (this might be a large string, so do on background thread)
                    Executors.newSingleThreadExecutor().execute(() -> {
                        // Decode the JSON string
                        String decodedHtml = JsonUtils.unescapeJavaScript(html);
                        
                        // Format the HTML nicely
                        final String formattedHtml = formatHtml(decodedHtml);
                        
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            sourceCodeText.setText(formattedHtml);
                            // Set monospace font and other formatting
                            sourceCodeText.setTypeface(Typeface.MONOSPACE);
                            // Add padding and line numbers if needed
                            sourceCodeText.setPadding(16, 16, 16, 16);
                        });
                    });
                });
    }
    
    /**
     * Format HTML with proper indentation using Jsoup
     * @param html Raw HTML to format
     * @return Formatted HTML
     */
    private String formatHtml(String html) {
        try {
            Document doc = Jsoup.parse(html);
            doc.outputSettings().indentAmount(4).prettyPrint(true);
            return doc.outerHtml();
        } catch (Exception e) {
            Log.e(TAG, "Error formatting HTML: " + e.getMessage());
            return html;
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

        if (id == R.id.action_dev_tools) {
            // Show developer tools with tabbed interface
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
        
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(TAG, "Console message: " + consoleMessage.message() +
                  " -- From line " + consoleMessage.lineNumber() +
                  " of " + consoleMessage.sourceId());
            
            // Already handled by our custom console.log override in injectConsoleLogger
            return super.onConsoleMessage(consoleMessage);
        }
    }

    /**
     * Custom WebViewClient class to handle page navigation events.
     */
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Handle URL loading within the app
            loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // Show loading indicators
            pageLoaded = false;
            showLoading(true);

            // Set a timeout for page load
            if (timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }

            timeoutRunnable = () -> {
                if (!pageLoaded) {
                    showLoading(false);
                    Toast.makeText(BrowserActivity.this, "Page load timed out", Toast.LENGTH_SHORT).show();
                }
            };
            timeoutHandler.postDelayed(timeoutRunnable, PAGE_LOAD_TIMEOUT);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            // Inject JavaScript to check if page is fully loaded
            view.evaluateJavascript(
                    "(function() {" +
                            "   try {" +
                            "       var checkReadyState = function() {" +
                            "           if (document.readyState === 'complete') {" +
                            "               window.Android.onPageFullyLoaded();" +
                            "           } else {" +
                            "               setTimeout(checkReadyState, 100);" +
                            "           }" +
                            "       };" +
                            "       checkReadyState();" +
                            "       return document.readyState === 'complete';" +
                            "} catch(e) { " +
                            "   try {" +
                            "       return document.getElementsByTagName('html')[0].outerHTML;" +
                            "   } catch(e2) {" +
                            "       return document.body.innerHTML;" +
                            "   }" +
                            "} })();",
                    value -> {
                        if (Boolean.parseBoolean(value)) {
                            onPageFullyLoaded();
                        }
                    }
            );
            
            // Inject the console logger after the page is loaded - this ensures the JS interface is ready
            injectConsoleLogger();

            // Update UI elements
            updateNavigationButtons();
            updateUrlBar(url);

            // Save to history
            if (historyManager != null) {
                historyManager.addEntry(url, view.getTitle(), null);
            }
        }
    }

    /**
     * Called when the page is fully loaded.
     */
    private void onPageFullyLoaded() {
        if (!isFinishing() && !pageLoaded) {
            pageLoaded = true;
            showLoading(false);
            
            if (timeoutHandler != null && timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
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
     * Show or hide loading indicators
     * @param show True to show loading indicators, false to hide
     */
    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            loadingIndicator.setVisibility(View.GONE);
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

    // Test console logging with various types of logs
    private void testConsoleLog() {
        if (webView == null) {
            Toast.makeText(this, "WebView is not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show console tab if not already showing
        if (segmentedControl != null && segmentedControl.getCheckedRadioButtonId() != R.id.tab_console) {
            showDevTools(DEV_TOOLS_TAB_CONSOLE);
        }
        
        // First make sure the console logger is properly initialized
        injectConsoleLogger();
        
        // Wait a moment for the console logger to initialize
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Add a direct test entry - this bypasses JavaScript entirely
            ConsoleLogEntry directEntry = new ConsoleLogEntry("info", "Direct test entry - added from Java code", new Date());
            consoleLogEntries.add(directEntry);
            
            if (consoleLogAdapter != null) {
                consoleLogAdapter.notifyDataSetChanged();
                if (consoleLogRecyclerView != null) {
                    consoleLogRecyclerView.scrollToPosition(consoleLogEntries.size() - 1);
                }
            }
            
            // Now try through JavaScript
            Log.d(TAG, "Testing console log via JavaScript");
            
            // Inject JavaScript to generate various log types
            final String testScript = 
                "if (typeof Android !== 'undefined' && typeof Android.consoleLog === 'function') {" +
                "    Android.consoleLog('log', 'Direct Android interface test (bypassing console)');" +
                "}" +
                "console.log('Standard log message');" +
                "console.debug('Debug message - dark gray');" +
                "console.info('Info message - blue');" +
                "console.warn('Warning message - orange');" +
                "console.error('Error message - red');" +
                "console.log('Object example:', { id: 1, name: 'Test', nested: { value: true } });" +
                "console.log('Array example:', [1, 2, 3, 'test', { key: 'value' }]);" +
                "try { throw new Error('Test exception'); } catch(e) { console.error('Caught exception:', e); }" +
                "console.log('Log with multiple arguments:', 42, true, null, undefined);";
            
            webView.evaluateJavascript(testScript, result -> {
                Log.d(TAG, "Test script executed with result: " + result);
                Toast.makeText(BrowserActivity.this, "Test logs generated", Toast.LENGTH_SHORT).show();
            });
        }, 300);
    }
    
    /**
     * Shows the developer tools panel with the specified tab selected
     * @param mode The tab to select (DEV_TOOLS_TAB_SOURCE or DEV_TOOLS_TAB_CONSOLE)
     */
    private void showDevTools(int mode) {
        // First make sure dev tools are visible
        if (!isDevToolsVisible) {
            toggleDevTools();
        }
        
        // Select the appropriate tab
        if (segmentedControl != null) {
            if (mode == DEV_TOOLS_TAB_SOURCE) {
                segmentedControl.check(R.id.tab_source);
            } else if (mode == DEV_TOOLS_TAB_CONSOLE) {
                segmentedControl.check(R.id.tab_console);
            }
        }
    }
    
    /**
     * Injects JavaScript to capture console logs
     */
    private void injectConsoleLogger() {
        if (webView == null) {
            Log.e(TAG, "Cannot inject console logger: WebView is null");
            return;
        }
        
        Log.d(TAG, "Injecting console logger JavaScript");
        
        // Simpler, more reliable console logging script
        final String consoleScript = 
            "(function() {\n" +
            "    try {\n" +
            "        // Simple check if we're already initialized\n" +
            "        if (window.androidConsoleHooked === true) {\n" +
            "            console.log('Android console already hooked');\n" +
            "            return true;\n" +
            "        }\n" +
            "\n" +
            "        // First verify the Android bridge exists\n" +
            "        if (typeof Android === 'undefined') {\n" +
            "            console.error('Android interface not found');\n" +
            "            return false;\n" +
            "        }\n" +
            "\n" +
            "        // Direct test of interface\n" +
            "        try {\n" +
            "            Android.consoleLog('init', 'Testing Android interface...');\n" +
            "        } catch (e) {\n" +
            "            console.error('Android.consoleLog test failed: ' + e.message);\n" +
            "            return false;\n" +
            "        }\n" +
            "\n" +
            "        // Store original methods\n" +
            "        var originalLog = console.log;\n" +
            "        var originalError = console.error;\n" +
            "        var originalWarn = console.warn;\n" +
            "        var originalInfo = console.info;\n" +
            "        var originalDebug = console.debug;\n" +
            "\n" +
            "        // Simple object stringifier\n" +
            "        function stringify(obj) {\n" +
            "            if (obj === null) return 'null';\n" +
            "            if (obj === undefined) return 'undefined';\n" +
            "            if (typeof obj === 'string') return obj;\n" +
            "            if (typeof obj !== 'object') return String(obj);\n" +
            "            try {\n" +
            "                return JSON.stringify(obj);\n" +
            "            } catch (e) {\n" +
            "                return '[Object]';\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        // Override console methods\n" +
            "        console.log = function() {\n" +
            "            originalLog.apply(console, arguments);\n" +
            "            try {\n" +
            "                var message = Array.prototype.map.call(arguments, stringify).join(' ');\n" +
            "                Android.consoleLog('log', message);\n" +
            "            } catch (e) {\n" +
            "                originalError.call(console, 'Error in console.log override: ' + e.message);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        console.error = function() {\n" +
            "            originalError.apply(console, arguments);\n" +
            "            try {\n" +
            "                var message = Array.prototype.map.call(arguments, stringify).join(' ');\n" +
            "                Android.consoleLog('error', message);\n" +
            "            } catch (e) {\n" +
            "                originalError.call(console, 'Error in console.error override: ' + e.message);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        console.warn = function() {\n" +
            "            originalWarn.apply(console, arguments);\n" +
            "            try {\n" +
            "                var message = Array.prototype.map.call(arguments, stringify).join(' ');\n" +
            "                Android.consoleLog('warn', message);\n" +
            "            } catch (e) {\n" +
            "                originalError.call(console, 'Error in console.warn override: ' + e.message);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        console.info = function() {\n" +
            "            originalInfo.apply(console, arguments);\n" +
            "            try {\n" +
            "                var message = Array.prototype.map.call(arguments, stringify).join(' ');\n" +
            "                Android.consoleLog('info', message);\n" +
            "            } catch (e) {\n" +
            "                originalError.call(console, 'Error in console.info override: ' + e.message);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        console.debug = function() {\n" +
            "            originalDebug.apply(console, arguments);\n" +
            "            try {\n" +
            "                var message = Array.prototype.map.call(arguments, stringify).join(' ');\n" +
            "                Android.consoleLog('debug', message);\n" +
            "            } catch (e) {\n" +
            "                originalError.call(console, 'Error in console.debug override: ' + e.message);\n" +
            "            }\n" +
            "        };\n" +
            "\n" +
            "        // Mark as initialized\n" +
            "        window.androidConsoleHooked = true;\n" +
            "        console.log('Android console logging initialized successfully');\n" +
            "        return true;\n" +
            "    } catch (e) {\n" +
            "        if (typeof console !== 'undefined' && console.error) {\n" +
            "            console.error('Failed to initialize Android console: ' + e.message);\n" +
            "        }\n" +
            "        return false;\n" +
            "    }\n" +
            "})();";
        
        // Inject the script with error handling
        try {
            webView.evaluateJavascript(consoleScript, result -> {
                Log.d(TAG, "Console logger initialization result: " + result);
                if (!"true".equals(result)) {
                    Log.e(TAG, "Failed to initialize console logger - result: " + result);
                    // Still add items to the console log list to indicate the issue
                    ConsoleLogEntry errorEntry = new ConsoleLogEntry(
                            "error", 
                            "Failed to initialize console logger. Please reload the page.", 
                            new Date());
                    
                    runOnUiThread(() -> {
                        if (consoleLogEntries != null) {
                            consoleLogEntries.add(errorEntry);
                            if (consoleLogAdapter != null) {
                                consoleLogAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                } else {
                    // Test with a simple message
                    ConsoleLogEntry successEntry = new ConsoleLogEntry(
                            "info", 
                            "Console logger initialized successfully", 
                            new Date());
                    
                    runOnUiThread(() -> {
                        if (consoleLogEntries != null) {
                            consoleLogEntries.add(successEntry);
                            if (consoleLogAdapter != null) {
                                consoleLogAdapter.notifyDataSetChanged();
                                if (consoleLogRecyclerView != null) {
                                    consoleLogRecyclerView.scrollToPosition(consoleLogEntries.size() - 1);
                                }
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception injecting console logger: " + e.getMessage(), e);
        }
    }

    /**
     * Model class for console log entries
     */
    class ConsoleLogEntry {
        private String type;
        private String message;
        private Date timestamp;
        
        public ConsoleLogEntry(String type, String message, Date timestamp) {
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getType() {
            return type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Date getTimestamp() {
            return timestamp;
        }
        
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
            return sdf.format(timestamp);
        }
    }

    /**
     * Adapter for displaying console logs in a RecyclerView
     */
    class ConsoleLogAdapter extends RecyclerView.Adapter<ConsoleLogAdapter.LogViewHolder> {
        private List<ConsoleLogEntry> logEntries;
        
        public ConsoleLogAdapter(List<ConsoleLogEntry> logEntries) {
            this.logEntries = logEntries;
        }
        
        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            
            // Create main layout for log entry
            LinearLayout logLayout = new LinearLayout(context);
            logLayout.setOrientation(LinearLayout.VERTICAL);
            logLayout.setPadding(16, 8, 16, 8);
            
            // Add divider at the bottom
            View divider = new View(context);
            divider.setBackgroundColor(Color.LTGRAY);
            
            // Create timestamp text view (smaller, at top)
            TextView timestampText = new TextView(context);
            timestampText.setTextSize(10);
            timestampText.setTypeface(Typeface.MONOSPACE);
            timestampText.setTextColor(Color.GRAY);
            
            // Create message text view (larger, below timestamp)
            TextView logText = new TextView(context);
            logText.setTextSize(14);
            logText.setTypeface(Typeface.MONOSPACE);
            
            // Add views to layout
            logLayout.addView(timestampText);
            logLayout.addView(logText);
            
            // Create layout parameters
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    1); // 1px height
            dividerParams.setMargins(0, 8, 0, 0);
            
            logLayout.addView(divider, dividerParams);
            
            // Set layout parameters for the main layout
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            logLayout.setLayoutParams(layoutParams);
            
            return new LogViewHolder(logLayout, logText, timestampText);
        }
        
        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            ConsoleLogEntry entry = logEntries.get(position);
            holder.logText.setText(entry.getMessage());
            holder.timestampText.setText(entry.getFormattedTime() + " [" + entry.getType() + "]");
            
            // Set color based on log type
            switch (entry.getType().toLowerCase()) {
                case "error":
                    holder.logText.setTextColor(Color.RED);
                    break;
                case "warn":
                    holder.logText.setTextColor(Color.rgb(255, 165, 0)); // Orange
                    break;
                case "info":
                    holder.logText.setTextColor(Color.BLUE);
                    break;
                case "debug":
                    holder.logText.setTextColor(Color.DKGRAY);
                    break;
                default:
                    holder.logText.setTextColor(Color.BLACK);
                    break;
            }
        }
        
        @Override
        public int getItemCount() {
            return logEntries.size();
        }
        
        class LogViewHolder extends RecyclerView.ViewHolder {
            TextView logText;
            TextView timestampText;
            
            public LogViewHolder(View itemView, TextView logText, TextView timestampText) {
                super(itemView);
                this.logText = logText;
                this.timestampText = timestampText;
            }
        }
    }
    
    /**
     * Direct test of the console log functionality
     */
    private void testConsoleLogDirectly() {
        if (webView == null) return;
        
        Log.d(TAG, "Testing console log directly");
        String testScript = "console.log('Direct test message from testConsoleLogDirectly()');";
        webView.evaluateJavascript(testScript, null);
    }
    
    /**
     * Clear the current content (source or console logs) based on active tab
     */
    private void clearContent() {
        // Determine which tab is active and clear appropriate content
        if (segmentedControl != null) {
            if (segmentedControl.getCheckedRadioButtonId() == R.id.tab_source) {
                // Nothing to clear in source view - it's read-only
                Toast.makeText(this, "Source view is read-only", Toast.LENGTH_SHORT).show();
            } else {
                // Clear console logs
                consoleLogEntries.clear();
                if (consoleLogAdapter != null) {
                    consoleLogAdapter.notifyDataSetChanged();
                }
                Toast.makeText(this, "Console logs cleared", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Copy the current content (source or console logs) to clipboard based on active tab
     */
    private void copyContent() {
        // Determine which tab is active and copy appropriate content
        if (segmentedControl != null) {
            if (segmentedControl.getCheckedRadioButtonId() == R.id.tab_source) {
                copySourceToClipboard();
            } else {
                copyConsoleLogsToClipboard();
            }
        }
    }
    
    /**
     * Copy source code to clipboard
     */
    private void copySourceToClipboard() {
        if (sourceCodeText != null && sourceCodeText.getText() != null) {
            String sourceCode = sourceCodeText.getText().toString();
            if (sourceCode.isEmpty()) {
                Toast.makeText(this, "No source code to copy", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("HTML Source", sourceCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Source code copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Copy console logs to clipboard
     */
    private void copyConsoleLogsToClipboard() {
        if (consoleLogEntries == null || consoleLogEntries.isEmpty()) {
            Toast.makeText(this, "No console logs to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringBuilder logs = new StringBuilder();
        for (ConsoleLogEntry entry : consoleLogEntries) {
            logs.append(entry.getFormattedTime())
                .append(" [").append(entry.getType()).append("] ")
                .append(entry.getMessage())
                .append("\n");
        }
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Console Logs", logs.toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Console logs copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
