package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.TuVung;

import java.util.List;

public class LearningStatusAdapter extends RecyclerView.Adapter<LearningStatusAdapter.ViewHolder> {

    private List<TuVung> wordList;
    private boolean isUnknownList; // true = chưa nhớ, false = đã nhớ
    private OnRemoveClickListener onRemoveClickListener;

    public interface OnRemoveClickListener {
        void onRemove(TuVung word);
    }

    public LearningStatusAdapter(List<TuVung> wordList, boolean isUnknownList) {
        this.wordList = wordList;
        this.isUnknownList = isUnknownList;
    }

    public void setOnRemoveClickListener(OnRemoveClickListener listener) {
        this.onRemoveClickListener = listener;
    }

    public void updateData(List<TuVung> newList) {
        this.wordList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_learning_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TuVung word = wordList.get(position);
        
        holder.tvWord.setText(word.getTuTiengAnh() != null ? word.getTuTiengAnh() : "");
        holder.tvPronunciation.setText(word.getPhienAm() != null ? word.getPhienAm() : "");
        holder.tvMeaning.setText(word.getNghiaTiengViet() != null ? word.getNghiaTiengViet() : "");
        
        // Status badge
        if (isUnknownList) {
            holder.tvStatus.setText("Chưa nhớ");
            holder.tvStatus.setTextColor(0xFFF44336); // Red
            holder.tvStatus.setBackgroundResource(R.drawable.vocab_chip_learned_bg);
        } else {
            holder.tvStatus.setText("Đã nhớ");
            holder.tvStatus.setTextColor(0xFF047857); // Green
            holder.tvStatus.setBackgroundResource(R.drawable.vocab_chip_learned_bg);
        }
        
        // Example sentence
        if (word.getCauViDu() != null && !word.getCauViDu().trim().isEmpty()) {
            holder.tvExample.setText("\"" + word.getCauViDu() + "\"");
            holder.tvExample.setVisibility(View.VISIBLE);
        } else {
            holder.tvExample.setVisibility(View.GONE);
        }
        
        // Remove button
        holder.btnAction.setOnClickListener(v -> {
            if (onRemoveClickListener != null) {
                onRemoveClickListener.onRemove(word);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wordList != null ? wordList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord, tvPronunciation, tvMeaning, tvStatus, tvExample;
        ImageButton btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tv_word);
            tvPronunciation = itemView.findViewById(R.id.tv_pronunciation);
            tvMeaning = itemView.findViewById(R.id.tv_meaning);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvExample = itemView.findViewById(R.id.tv_example);
            btnAction = itemView.findViewById(R.id.btn_action);
        }
    }
}

