package com.noctusoft.webviewbrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.noctusoft.webviewbrowser.model.BrowsingSession;
import com.noctusoft.webviewbrowser.model.Credentials;
import com.noctusoft.webviewbrowser.ui.HistoryListActivity;
import com.noctusoft.webviewbrowser.ui.VariableManagerActivity;

import java.io.ByteArrayOutputStream;
import java.util.Date;

/**
 * Main activity for the browser app with WebView functionality.
 */
public class BrowserActivity extends AppCompatActivity {

    private static final String TAG = "BrowserActivity";
    private static final String DEFAULT_URL = "https://www.google.com";
    private static final String STATE_URL = "current_url";
    private static final String PREF_LAST_SESSION = "last_browsing_session";

    // UI components
    private WebView webView;
    private EditText addressBar;
    private ProgressBar progressBar;
    private ProgressBar loadingIndicator;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton refreshButton;
    private ImageButton credentialsButton;
    private ImageButton variablesButton;
    private ImageButton historyButton;
    private ImageButton devToolsButton;
    private LinearLayout devToolsView;
    private TextView sourceCodeText;

    // State
    private boolean isDevToolsVisible = false;
    private BrowsingSession currentSession;
    private CredentialsManager credentialsManager;
    private HistoryManager historyManager;
    private VariablesManager variablesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

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

        // Set up UI components
        setupUIComponents();
        setupWebView();
        setupListeners();

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
        credentialsButton = findViewById(R.id.btn_credentials);
        variablesButton = findViewById(R.id.btn_variables);
        historyButton = findViewById(R.id.btn_history);
        devToolsButton = findViewById(R.id.btn_dev_tools);
        devToolsView = findViewById(R.id.dev_tools_view);
        sourceCodeText = findViewById(R.id.source_code_text);

        // Set initial visibility
        progressBar.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        devToolsView.setVisibility(View.GONE);
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
        webView.setWebViewClient(new BrowserWebViewClient());
        
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
            if (actionId == EditorInfo.IME_ACTION_GO) {
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

        // Other buttons
        historyButton.setOnClickListener(v -> openHistoryList());
        devToolsButton.setOnClickListener(v -> toggleDevTools());
        credentialsButton.setOnClickListener(v -> showCredentialsDialog());
        variablesButton.setOnClickListener(v -> openVariableManager());

        // Go button
        ImageButton goButton = findViewById(R.id.btn_go);
        goButton.setOnClickListener(v -> {
            loadUrl(addressBar.getText().toString());
            hideKeyboard();
        });

        // Menu button
        ImageButton menuButton = findViewById(R.id.btn_menu);
        menuButton.setOnClickListener(v -> openOptionsMenu());
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
        isDevToolsVisible = !isDevToolsVisible;
        
        if (isDevToolsVisible) {
            devToolsView.setVisibility(View.VISIBLE);
            // Load the HTML source
            webView.evaluateJavascript(
                    "(function() { return document.documentElement.outerHTML; })();",
                    html -> {
                        // Format the HTML (remove escape sequences)
                        String formattedHtml = formatHtml(html);
                        sourceCodeText.setText(formattedHtml);
                    });
        } else {
            devToolsView.setVisibility(View.GONE);
        }
    }

    /**
     * Formats HTML for display.
     */
    private String formatHtml(String html) {
        if (html == null) return "";
        
        // Remove quotes added by evaluateJavascript
        if (html.startsWith("\"") && html.endsWith("\"")) {
            html = html.substring(1, html.length() - 1);
        }
        
        // Unescape sequences
        html = html.replace("\\\"", "\"");
        html = html.replace("\\n", "\n");
        html = html.replace("\\t", "\t");
        html = html.replace("\\\\", "\\");
        
        return html;
    }

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
                result.getType() == WebView.HitTestResult.SRC_IMAGE_TYPE) {
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
     * JavaScript interface for communication between WebView and Android code.
     */
    private class WebAppInterface {
        
        @JavascriptInterface
        public void onFormField(String fieldType, String fieldName) {
            Log.d(TAG, "Form field detected: " + fieldType + " - " + fieldName);
            
            // Check for username/password fields
            if ("password".equalsIgnoreCase(fieldType)) {
                // TODO: Implement auto-fill functionality
            }
        }
    }

    /**
     * WebViewClient to handle page navigation events.
     */
    private class BrowserWebViewClient extends WebViewClient {
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // Handle all URLs within the WebView
            return false;
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            
            // Update UI
            progressBar.setVisibility(View.VISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            
            // Update address bar
            addressBar.setText(url);
            
            // Update navigation buttons
            updateNavigationButtons();
            
            Log.d(TAG, "Page load started: " + url);
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            
            // Update UI
            progressBar.setVisibility(View.GONE);
            loadingIndicator.setVisibility(View.GONE);
            
            // Get page title
            String title = view.getTitle();
            
            // Create browsing session
            currentSession = new BrowsingSession(url, title);
            
            // Add to history
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (view.getFavicon() != null) {
                view.getFavicon().compress(Bitmap.CompressFormat.PNG, 100, baos);
            }
            historyManager.addEntry(url, title, baos.toByteArray());
            
            // Update navigation buttons
            updateNavigationButtons();
            
            // Inject JavaScript to detect form fields
            injectFormDetectionScript();
            
            Log.d(TAG, "Page load finished: " + url + " - " + title);
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
}
