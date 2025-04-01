package com.noctusoft.webviewbrowser.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.noctusoft.webviewbrowser.R;
import com.noctusoft.webviewbrowser.model.HistoryEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying history entries in a RecyclerView.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryEntry> historyEntries = new ArrayList<>();
    private final HistoryItemListener listener;
    private final SimpleDateFormat dateFormat;

    /**
     * Interface for handling history item interactions.
     */
    public interface HistoryItemListener {
        void onHistoryItemClick(HistoryEntry entry);
        void onHistoryItemLongClick(HistoryEntry entry, View view);
    }

    /**
     * Constructs a new HistoryAdapter.
     *
     * @param listener The listener for item interactions
     */
    public HistoryAdapter(HistoryItemListener listener) {
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryEntry entry = historyEntries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return historyEntries.size();
    }

    /**
     * Updates the adapter with a new list of history entries.
     *
     * @param entries The new list of entries
     */
    public void setHistoryEntries(List<HistoryEntry> entries) {
        this.historyEntries = entries;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for history items.
     */
    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView urlTextView;
        private final TextView timeTextView;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.history_title);
            urlTextView = itemView.findViewById(R.id.history_url);
            timeTextView = itemView.findViewById(R.id.history_time);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryItemClick(historyEntries.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHistoryItemLongClick(historyEntries.get(position), v);
                    return true;
                }
                return false;
            });
        }

        void bind(HistoryEntry entry) {
            titleTextView.setText(entry.getTitle());
            urlTextView.setText(entry.getUrl());
            timeTextView.setText(dateFormat.format(entry.getTimestamp()));
        }
    }
}
