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
    private final Map<String, String> topicNameMap = new HashMap<>();
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(LichSuLamBai history);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

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
        
        // FIX: Use getter methods to access private fields
        String topicId = it.getMaChuDe();
        String topicName = it.getTenChuDe();
        if (topicName == null && topicId != null) {
            topicName = topicNameMap.get(topicId);
        }
        h.tvTopic.setText("Chủ đề: " + (topicName != null ? topicName : (topicId != null ? topicId : "-")));
        // Điểm bây giờ là điểm tuyệt đối (mỗi câu đúng = 10 điểm), không phải phần trăm
        h.tvScore.setText(it.getDiemSo() + " điểm");

        String dateStr = "-";
        if (it.getNgayLamBai() != null) {
            dateStr = dfDate.format(it.getNgayLamBai());
        }
        
        // FIX: The field is soCauDung, not diemDanhDuoc
        String detailText = it.getSoCauDung() + "/" + it.getTongSoCau() + " câu đúng • " + dateStr;
        h.tvDetail.setText(detailText);
        
        // Thêm click listener
        h.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(it);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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
