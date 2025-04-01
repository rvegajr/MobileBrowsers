package com.noctusoft.webviewbrowser.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.noctusoft.webviewbrowser.R;
import com.noctusoft.webviewbrowser.model.Favorite;

import java.util.List;

/**
 * Adapter for displaying favorite websites in a RecyclerView.
 */
public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private final List<Favorite> favorites;
    private final OnFavoriteClickListener listener;
    private final Context context;

    /**
     * Interface for handling click events on favorites.
     */
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Favorite favorite);
        void onFavoriteDeleteClick(Favorite favorite, int position);
    }

    /**
     * Constructor for the adapter.
     *
     * @param context   The context
     * @param favorites List of favorites to display
     * @param listener  Click listener for favorites
     */
    public FavoritesAdapter(Context context, List<Favorite> favorites, OnFavoriteClickListener listener) {
        this.context = context;
        this.favorites = favorites;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Favorite favorite = favorites.get(position);
        
        // Set title and URL
        holder.titleTextView.setText(favorite.getTitle());
        holder.urlTextView.setText(favorite.getUrl());
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(favorite);
            }
        });
        
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteDeleteClick(favorite, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    /**
     * Remove a favorite at the specified position.
     *
     * @param position Position to remove
     */
    public void removeItem(int position) {
        if (position >= 0 && position < favorites.size()) {
            favorites.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, favorites.size());
        }
    }

    /**
     * ViewHolder for favorite items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final TextView urlTextView;
        final ImageView faviconView;
        final ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.title);
            urlTextView = itemView.findViewById(R.id.url);
            faviconView = itemView.findViewById(R.id.favicon);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}
