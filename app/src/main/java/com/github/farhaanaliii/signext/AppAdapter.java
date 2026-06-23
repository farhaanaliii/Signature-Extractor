package com.github.farhaanaliii.signext;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfo app);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(AppInfo app);
    }

    private List<AppInfo> displayedApps = new ArrayList<>();
    private final OnAppClickListener clickListener;
    private final OnFavoriteClickListener favoriteClickListener;

    public AppAdapter(OnAppClickListener clickListener, OnFavoriteClickListener favoriteClickListener) {
        this.clickListener = clickListener;
        this.favoriteClickListener = favoriteClickListener;
    }

    public void updateList(List<AppInfo> newList) {
        this.displayedApps = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_item, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo app = displayedApps.get(position);
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.appName.setText(app.getLabel());
        holder.appPackage.setText(app.getPackageName());

        if (app.isFavorite()) {
            holder.starBtn.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.starBtn.setImageResource(R.drawable.ic_star_outline);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onAppClick(app);
            }
        });

        holder.starBtn.setOnClickListener(v -> {
            if (favoriteClickListener != null) {
                favoriteClickListener.onFavoriteClick(app);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayedApps.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final ImageView appIcon;
        final TextView appName;
        final TextView appPackage;
        final ImageButton starBtn;

        AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            appPackage = itemView.findViewById(R.id.appPackage);
            starBtn = itemView.findViewById(R.id.starBtn);
        }
    }
}
