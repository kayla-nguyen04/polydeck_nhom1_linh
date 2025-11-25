package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.activity.TopicDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class UserDeckAdapter extends RecyclerView.Adapter<UserDeckAdapter.ViewHolder> {

    private final Context context;
    private final List<BoTu> items = new ArrayList<>();

    public UserDeckAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<BoTu> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BoTu deck = items.get(position);
        holder.tvName.setText(deck.getTenChuDe());
        Glide.with(context)
                .load(deck.getLinkAnhIcon())
                .placeholder(R.drawable.ic_default_deck_icon)
                .error(R.drawable.ic_default_deck_icon)
                .into(holder.ivIcon);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TopicDetailActivity.class);
            intent.putExtra(TopicDetailActivity.EXTRA_DECK_ID, deck.getId());
            intent.putExtra(TopicDetailActivity.EXTRA_DECK_NAME, deck.getTenChuDe());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivDeckIcon);
            tvName = itemView.findViewById(R.id.tvDeckName);
        }
    }
}

