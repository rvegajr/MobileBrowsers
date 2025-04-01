package com.noctusoft.webviewbrowser.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.noctusoft.webviewbrowser.R;

/**
 * Activity for displaying the HTML source code of a webpage.
 */
public class SourceCodeActivity extends AppCompatActivity {

    public static final String EXTRA_SOURCE_CODE = "extra_source_code";
    public static final String EXTRA_PAGE_TITLE = "extra_page_title";

    private String sourceCode;
    private String pageTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_source_code);

        // Get intent extras
        if (getIntent().hasExtra(EXTRA_SOURCE_CODE)) {
            sourceCode = getIntent().getStringExtra(EXTRA_SOURCE_CODE);
        } else {
            sourceCode = "";
        }

        if (getIntent().hasExtra(EXTRA_PAGE_TITLE)) {
            pageTitle = getIntent().getStringExtra(EXTRA_PAGE_TITLE);
        } else {
            pageTitle = getString(R.string.source_code_title);
        }

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.source_code_title) + " - " + pageTitle);
        }

        // Set up source code text view
        TextView sourceCodeTextView = findViewById(R.id.source_code_text_view);
        sourceCodeTextView.setText(sourceCode);

        // Set up copy button
        FloatingActionButton copyButton = findViewById(R.id.fab_copy_source);
        copyButton.setOnClickListener(v -> copySourceCodeToClipboard());
    }

    /**
     * Copies the source code to the clipboard.
     */
    private void copySourceCodeToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("HTML Source", sourceCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Source code copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
