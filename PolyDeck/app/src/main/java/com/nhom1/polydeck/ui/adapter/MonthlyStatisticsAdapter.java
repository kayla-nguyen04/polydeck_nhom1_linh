package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.UserMonthlyStats;

import java.util.List;

public class MonthlyStatisticsAdapter extends RecyclerView.Adapter<MonthlyStatisticsAdapter.ViewHolder> {

    private List<UserMonthlyStats> statsList;
    private List<UserMonthlyStats> previousStatsList;
    private String[] monthNames = {"Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                                   "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"};

    public MonthlyStatisticsAdapter(List<UserMonthlyStats> statsList) {
        this.statsList = statsList;
    }

    public void setPreviousStats(List<UserMonthlyStats> previousStatsList) {
        this.previousStatsList = previousStatsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_monthly_statistics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserMonthlyStats stats = statsList.get(position);
        
        int month = stats.getMonth();
        if (month >= 1 && month <= 12) {
            holder.tvMonth.setText(monthNames[month - 1]);
        } else {
            holder.tvMonth.setText("Tháng " + month);
        }
        
        holder.tvTotalUsers.setText(String.format("%,d", stats.getTotalUsers()));
        holder.tvNewUsers.setText(String.format("%,d", stats.getNewUsers()));
        holder.tvActiveUsers.setText(String.format("%,d", stats.getActiveUsers()));
        
        // Tính % tăng trưởng so với tháng trước
        float growthRate = 0f;
        if (position > 0) {
            UserMonthlyStats previousStats = statsList.get(position - 1);
            int previousTotal = previousStats.getTotalUsers();
            int currentTotal = stats.getTotalUsers();
            if (previousTotal > 0) {
                growthRate = ((float)(currentTotal - previousTotal) / previousTotal) * 100f;
            } else if (currentTotal > 0) {
                growthRate = 100f;
            }
        } else if (previousStatsList != null && !previousStatsList.isEmpty()) {
            // So sánh với tháng 12 năm trước
            UserMonthlyStats lastYearDec = null;
            for (UserMonthlyStats s : previousStatsList) {
                if (s.getMonth() == 12) {
                    lastYearDec = s;
                    break;
                }
            }
            if (lastYearDec != null) {
                int previousTotal = lastYearDec.getTotalUsers();
                int currentTotal = stats.getTotalUsers();
                if (previousTotal > 0) {
                    growthRate = ((float)(currentTotal - previousTotal) / previousTotal) * 100f;
                } else if (currentTotal > 0) {
                    growthRate = 100f;
                }
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

    public void updateData(List<UserMonthlyStats> newStatsList) {
        this.statsList = newStatsList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvTotalUsers, tvNewUsers, tvActiveUsers, tvGrowthRate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTotalUsers = itemView.findViewById(R.id.tvTotalUsers);
            tvNewUsers = itemView.findViewById(R.id.tvNewUsers);
            tvActiveUsers = itemView.findViewById(R.id.tvActiveUsers);
            tvGrowthRate = itemView.findViewById(R.id.tvGrowthRate);
        }
    }
}


