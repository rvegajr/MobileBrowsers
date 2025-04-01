package com.noctusoft.webviewbrowser.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.noctusoft.webviewbrowser.BrowserActivity;
import com.noctusoft.webviewbrowser.HistoryManager;
import com.noctusoft.webviewbrowser.R;
import com.noctusoft.webviewbrowser.model.HistoryEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for displaying browsing history.
 */
public class HistoryListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText searchEditText;
    private HistoryAdapter adapter;
    private HistoryManager historyManager;
    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private List<HistoryEntry> filteredEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_list);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.history_title);
        }

        // Initialize history manager
        historyManager = HistoryManager.getInstance(this);

        // Set up UI components
        recyclerView = findViewById(R.id.history_recycler_view);
        emptyView = findViewById(R.id.empty_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        searchEditText = findViewById(R.id.search_edit_text);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshHistoryList);

        // Set up search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterHistoryEntries(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set up clear all button
        FloatingActionButton clearButton = findViewById(R.id.fab_clear_history);
        clearButton.setOnClickListener(v -> showClearHistoryDialog());

        // Load history entries
        refreshHistoryList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHistoryList();
    }

    /**
     * Refreshes the history list.
     */
    private void refreshHistoryList() {
        // Get all history entries
        historyEntries = historyManager.getEntries();

        // Apply filter if search is active
        filterHistoryEntries(searchEditText.getText().toString());

        // Update UI
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Filters history entries based on search text.
     */
    private void filterHistoryEntries(String searchText) {
        filteredEntries.clear();

        if (searchText.isEmpty()) {
            filteredEntries.addAll(historyEntries);
        } else {
            String lowerCaseSearch = searchText.toLowerCase();
            for (HistoryEntry entry : historyEntries) {
                if (entry.getUrl().toLowerCase().contains(lowerCaseSearch) ||
                        (entry.getTitle() != null && entry.getTitle().toLowerCase().contains(lowerCaseSearch))) {
                    filteredEntries.add(entry);
                }
            }
        }

        // Update adapter
        adapter.notifyDataSetChanged();

        // Show/hide empty view
        if (filteredEntries.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    /**
     * Shows a dialog to confirm clearing history.
     */
    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_clear_history)
                .setMessage(R.string.confirm_clear_history)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    historyManager.clearHistory();
                    refreshHistoryList();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Opens a URL in the BrowserActivity.
     */
    private void openUrl(String url) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(url));
        startActivity(intent);
        finish();
    }

    /**
     * RecyclerView adapter for history entries.
     */
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryEntry entry = filteredEntries.get(position);
            holder.titleTextView.setText(entry.getTitle());
            holder.urlTextView.setText(entry.getUrl());
            holder.timeTextView.setText(dateFormat.format(entry.getTimestamp()));

            // Set click listener
            holder.itemView.setOnClickListener(v -> openUrl(entry.getUrl()));

            // Set long click listener for delete option
            holder.itemView.setOnLongClickListener(v -> {
                showDeleteEntryDialog(entry);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return filteredEntries.size();
        }

        /**
         * Shows a dialog to confirm deleting a history entry.
         */
        private void showDeleteEntryDialog(HistoryEntry entry) {
            new AlertDialog.Builder(HistoryListActivity.this)
                    .setTitle("Delete History Entry")
                    .setMessage("Delete this entry from history?")
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        historyManager.deleteEntry(entry.getUrl());
                        refreshHistoryList();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        /**
         * ViewHolder for history entry items.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView urlTextView;
            TextView timeTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.history_title);
                urlTextView = itemView.findViewById(R.id.history_url);
                timeTextView = itemView.findViewById(R.id.history_time);
            }
        }
    }
}
