package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.UserDailyStats;

import java.util.List;

public class DailyStatisticsAdapter extends RecyclerView.Adapter<DailyStatisticsAdapter.ViewHolder> {

    private List<UserDailyStats> statsList;

    public DailyStatisticsAdapter(List<UserDailyStats> statsList) {
        this.statsList = statsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_statistics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserDailyStats stats = statsList.get(position);
        
        holder.tvDay.setText(String.format("Ngày %d", stats.getDay()));
        holder.tvTotalUsers.setText(String.format("%,d", stats.getTotalUsers()));
        holder.tvNewUsers.setText(String.format("%,d", stats.getNewUsers()));
        holder.tvActiveUsers.setText(String.format("%,d", stats.getActiveUsers()));
        
        // Tính % tăng trưởng so với ngày trước
        float growthRate = 0f;
        if (position > 0) {
            UserDailyStats previousStats = statsList.get(position - 1);
            int previousTotal = previousStats.getTotalUsers();
            int currentTotal = stats.getTotalUsers();
            if (previousTotal > 0) {
                growthRate = ((float)(currentTotal - previousTotal) / previousTotal) * 100f;
            } else if (currentTotal > 0) {
                growthRate = 100f;
            }
        }
        
        updateGrowthTextView(holder.tvGrowthRate, growthRate);
    }

    private void updateGrowthTextView(TextView textView, float growthRate) {
        String sign = growthRate >= 0 ? "+" : "";
        String formattedRate = String.format("%s%.1f%%", sign, growthRate);
        textView.setText(formattedRate);
        
        int color = growthRate >= 0 ? 0xFF10B981 : 0xFFEF4444;
        int bgColor = growthRate >= 0 ? 0xFFD1FAE5 : 0xFFFEE2E2;
        textView.setTextColor(color);
        textView.setBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return statsList != null ? statsList.size() : 0;
    }

    public void updateData(List<UserDailyStats> newStatsList) {
        this.statsList = newStatsList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvTotalUsers, tvNewUsers, tvActiveUsers, tvGrowthRate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvTotalUsers = itemView.findViewById(R.id.tvTotalUsers);
            tvNewUsers = itemView.findViewById(R.id.tvNewUsers);
            tvActiveUsers = itemView.findViewById(R.id.tvActiveUsers);
            tvGrowthRate = itemView.findViewById(R.id.tvGrowthRate);
        }
    }
}

