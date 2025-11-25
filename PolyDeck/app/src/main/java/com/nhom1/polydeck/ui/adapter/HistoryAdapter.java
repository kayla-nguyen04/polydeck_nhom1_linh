package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.LichSuLamBai;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {
    private final List<LichSuLamBai> items = new ArrayList<>();
    private final SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dfTime = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private final Map<String, String> topicNameMap = new HashMap<>();

    public void setItems(List<LichSuLamBai> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void updateTopicNames(Map<String, String> map) {
        if (map == null) return;
        topicNameMap.putAll(map);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        LichSuLamBai it = items.get(position);
        String topicId = it.maChuDe;
        String topicName = topicId != null ? topicNameMap.get(topicId) : null;
        h.tvTopic.setText("Chủ đề: " + (topicName != null ? topicName : (topicId != null ? topicId : "-")));
        h.tvScore.setText(it.diemSo + "%");

        String dateStr = "-";
        if (it.ngayHoanThanh != null) {
            dateStr = dfDate.format(it.ngayHoanThanh);
        }
        String timeStr = formatSeconds(it.thoiGianLamBai);
        h.tvDetail.setText(it.diemDanhDuoc + " câu đúng • " + timeStr + " • " + dateStr);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatSeconds(int seconds) {
        if (seconds <= 0) return "00:00";
        long s = seconds % 60L;
        long m = (seconds / 60L) % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvScore, tvDetail;
        Holder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tv_topic);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvDetail = itemView.findViewById(R.id.tv_detail);
        }
    }
}



