package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.QuizBundle;

import java.util.ArrayList;
import java.util.List;

public class QuizOptionAdapter extends RecyclerView.Adapter<QuizOptionAdapter.Holder> {
    public interface OnSelectListener {
        void onSelected(String optionId);
    }

    private final List<QuizBundle.Option> items = new ArrayList<>();
    private String selectedId;
    private OnSelectListener listener;

    public void setData(List<QuizBundle.Option> options, String preselected, OnSelectListener l) {
        items.clear();
        if (options != null) items.addAll(options);
        selectedId = preselected;
        listener = l;
        notifyDataSetChanged();
    }

    public String getSelectedId() {
        return selectedId;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_option, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        QuizBundle.Option opt = items.get(position);
        h.tvPrefix.setText(opt.maLuaChon);
        h.tvText.setText(opt.noiDung);
        boolean sel = opt.maLuaChon != null && opt.maLuaChon.equals(selectedId);
        h.root.setSelected(sel);
        h.root.setOnClickListener(v -> {
            selectedId = opt.maLuaChon;
            notifyDataSetChanged();
            if (listener != null) listener.onSelected(selectedId);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        View root;
        TextView tvPrefix, tvText;
        Holder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            tvPrefix = itemView.findViewById(R.id.tv_option_prefix);
            tvText = itemView.findViewById(R.id.tv_option_text);
        }
    }
}



