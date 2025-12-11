package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.ui.activity.EditDeckActivity;
import com.nhom1.polydeck.ui.activity.VocabularyListActivity;
import com.nhom1.polydeck.utils.LearningStatusManager;
import com.nhom1.polydeck.utils.HiddenDeckManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {

    private List<BoTu> deckList;
    private Context context;
    private APIService apiService;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnDeckDeletedListener onDeckDeletedListener; // Callback for when deck is hidden/unhidden
    private Map<String, Integer> vocabCountCache = new HashMap<>(); // Cache số từ vựng
    private HiddenDeckManager hiddenDeckManager;
    private boolean isUnhideMode; // true = mode hiển thị lại, false = mode ẩn

    public interface OnDeckDeletedListener {
        void onDeckDeleted();
    }

    public DeckAdapter(Context context, List<BoTu> deckList) {
        this(context, deckList, false);
    }
    
    public DeckAdapter(Context context, List<BoTu> deckList, boolean isUnhideMode) {
        this.context = context;
        this.deckList = deckList;
        this.apiService = RetrofitClient.getApiService();
        this.hiddenDeckManager = new HiddenDeckManager(context);
        this.isUnhideMode = isUnhideMode;
    }
    
    public void setOnDeckDeletedListener(OnDeckDeletedListener listener) {
        this.onDeckDeletedListener = listener;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        BoTu deck = deckList.get(position);
        if (deck == null) return;

        holder.tvDeckName.setText(deck.getTenChuDe());

        // Lấy số từ vựng từ cache hoặc load từ API
        String deckId = deck.getId();
        if (deckId != null) {
            if (vocabCountCache.containsKey(deckId)) {
                // Đã có trong cache
                int vocabCount = vocabCountCache.get(deckId);
                String stats = String.format(Locale.getDefault(), "%d từ • %s",
                        vocabCount,
                        deck.getNgayTao() != null ? sdf.format(deck.getNgayTao()) : "N/A");
                holder.tvDeckInfo.setText(stats);
            } else {
                // Chưa có, hiển thị "Đang tải..." và load từ API
                holder.tvDeckInfo.setText("Đang tải... • " + (deck.getNgayTao() != null ? sdf.format(deck.getNgayTao()) : "N/A"));
                loadVocabCount(deckId, holder);
            }
        } else {
            // Không có ID, hiển thị 0
            String stats = String.format(Locale.getDefault(), "0 từ • %s",
                    deck.getNgayTao() != null ? sdf.format(deck.getNgayTao()) : "N/A");
            holder.tvDeckInfo.setText(stats);
        }

        // Load deck icon
        String iconUrl = deck.getLinkAnhIcon();
        android.util.Log.d("DeckAdapter", "Deck: " + deck.getTenChuDe() + " | Icon URL from server: [" + iconUrl + "]");
        
        String fullUrl = buildImageUrl(iconUrl);
        
        if (fullUrl != null) {
            android.util.Log.d("DeckAdapter", "Loading image from: " + fullUrl);
            
            // Create final variable for use in inner class
            final String finalUrl = fullUrl;
            
            Glide.with(context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_default_deck_icon)
                    .error(R.drawable.ic_default_deck_icon)
                    .centerCrop()
                    .skipMemoryCache(false)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("DeckAdapter", "❌ FAILED to load: " + finalUrl);
                            if (e != null) {
                                android.util.Log.e("DeckAdapter", "Exception: " + e.getMessage());
                                if (e.getRootCauses() != null && !e.getRootCauses().isEmpty()) {
                                    android.util.Log.e("DeckAdapter", "Root cause: " + e.getRootCauses().get(0).getMessage());
                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("DeckAdapter", "✅ SUCCESS loading: " + finalUrl);
                            return false;
                        }
                    })
                    .into(holder.ivDeckIcon);
        } else {
            android.util.Log.w("DeckAdapter", "⚠️ Icon URL is NULL/EMPTY for: " + deck.getTenChuDe());
            Glide.with(context)
                    .load(R.drawable.ic_default_deck_icon)
                    .into(holder.ivDeckIcon);
        }

        // View button - navigate to vocabulary list
        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, VocabularyListActivity.class);
            intent.putExtra(VocabularyListActivity.EXTRA_DECK_ID, deck.getId());
            intent.putExtra(VocabularyListActivity.EXTRA_DECK_NAME, deck.getTenChuDe());
            // Use startActivityForResult if context is an Activity to track vocabulary changes
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivityForResult(intent, 2002);
            } else {
                context.startActivity(intent);
            }
        });

        // Edit button
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditDeckActivity.class);
            intent.putExtra("DECK_ID", deck.getId());
            context.startActivity(intent);
        });

        // Hide/Unhide button
        if (isUnhideMode) {
            // Mode hiển thị lại: đổi text và màu nút
            holder.btnDelete.setText("👁️ Hiển thị lại");
            holder.btnDelete.setTextColor(android.graphics.Color.parseColor("#059669")); // Green
            holder.btnDelete.setBackgroundResource(R.drawable.bg_button_unhide);
            holder.btnDelete.setOnClickListener(v -> showUnhideConfirmationDialog(deck, position));
        } else {
            // Mode ẩn: giữ nguyên
            holder.btnDelete.setText("👁️‍🗨️ Ẩn");
            holder.btnDelete.setTextColor(android.graphics.Color.parseColor("#D97706")); // Orange
            holder.btnDelete.setBackgroundResource(R.drawable.bg_button_hide);
            holder.btnDelete.setOnClickListener(v -> showHideConfirmationDialog(deck, position));
        }
    }

    private void showHideConfirmationDialog(BoTu deck, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận ẩn")
                .setMessage("Bạn có chắc chắn muốn ẩn bộ từ '" + deck.getTenChuDe() + "'?\n\nBộ từ sẽ không hiển thị trong danh sách quản lý nhưng dữ liệu vẫn được giữ nguyên. Người dùng đang học vẫn có thể tiếp tục học.")
                .setPositiveButton("Ẩn", (dialog, which) -> hideDeck(deck, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void hideDeck(BoTu deck, int position) {
        String deckId = deck.getId();
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(context, "ID bộ từ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("DeckAdapter", "Hiding deck: " + deck.getTenChuDe() + " (ID: " + deckId + ")");
        
        // Lưu vào danh sách ẩn
        hiddenDeckManager.hideDeck(deckId);
        
        // Hiển thị thông báo
        Toast.makeText(context, "Đã ẩn bộ từ", Toast.LENGTH_SHORT).show();
        
        // Cập nhật UI - xóa khỏi danh sách hiển thị
        updateUIAfterHiding(position);
    }

    private void updateUIAfterHiding(int position) {
        // Xóa khỏi danh sách hiển thị
        deckList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, deckList.size());
        
        // Notify activity to refresh data (update stats and fullDeckList)
        if (onDeckDeletedListener != null) {
            onDeckDeletedListener.onDeckDeleted();
        }
    }
    
    private void showUnhideConfirmationDialog(BoTu deck, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận hiển thị lại")
                .setMessage("Bạn có chắc chắn muốn hiển thị lại bộ từ '" + deck.getTenChuDe() + "'?\n\nBộ từ sẽ xuất hiện lại trong danh sách quản lý và người dùng có thể thấy.")
                .setPositiveButton("Hiển thị lại", (dialog, which) -> unhideDeck(deck, position))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void unhideDeck(BoTu deck, int position) {
        String deckId = deck.getId();
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(context, "ID bộ từ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        android.util.Log.d("DeckAdapter", "Unhiding deck: " + deck.getTenChuDe() + " (ID: " + deckId + ")");
        
        // Xóa khỏi danh sách ẩn
        hiddenDeckManager.showDeck(deckId);
        
        // Hiển thị thông báo
        Toast.makeText(context, "Đã hiển thị lại bộ từ", Toast.LENGTH_SHORT).show();
        
        // Cập nhật UI - xóa khỏi danh sách ẩn
        updateUIAfterUnhiding(position);
    }

    private void updateUIAfterUnhiding(int position) {
        // Xóa khỏi danh sách ẩn
        deckList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, deckList.size());
        
        // Notify activity to refresh data
        if (onDeckDeletedListener != null) {
            onDeckDeletedListener.onDeckDeleted();
        }
    }

    @Override
    public int getItemCount() {
        return deckList != null ? deckList.size() : 0;
    }

    public void updateData(List<BoTu> newList) {
        this.deckList = newList;
        // Xóa cache khi cập nhật danh sách mới
        vocabCountCache.clear();
        notifyDataSetChanged();
    }
    
    private void loadVocabCount(String deckId, DeckViewHolder holder) {
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(Call<List<TuVung>> call, Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int vocabCount = response.body().size();
                    // Lưu vào cache
                    vocabCountCache.put(deckId, vocabCount);
                    
                    // Cập nhật lại view holder nếu vẫn ở cùng vị trí
                    int position = holder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < deckList.size()) {
                        BoTu deck = deckList.get(position);
                        if (deck != null && deck.getId().equals(deckId)) {
                            String stats = String.format(Locale.getDefault(), "%d từ • %s",
                                    vocabCount,
                                    deck.getNgayTao() != null ? sdf.format(deck.getNgayTao()) : "N/A");
                            holder.tvDeckInfo.setText(stats);
                        }
                    }
                } else {
                    // Lỗi, hiển thị 0
                    vocabCountCache.put(deckId, 0);
                    int position = holder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && position < deckList.size()) {
                        BoTu deck = deckList.get(position);
                        if (deck != null && deck.getId().equals(deckId)) {
                            String stats = String.format(Locale.getDefault(), "0 từ • %s",
                                    deck.getNgayTao() != null ? sdf.format(deck.getNgayTao()) : "N/A");
                            holder.tvDeckInfo.setText(stats);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<TuVung>> call, Throwable t) {
                // Lỗi, hiển thị 0
                vocabCountCache.put(deckId, 0);
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < deckList.size()) {
                    BoTu deck = deckList.get(position);
                    if (deck != null && deck.getId().equals(deckId)) {
                        String stats = String.format(Locale.getDefault(), "0 từ • %s",
                                deck.getNgayTao() != null ? sdf.format(deck.getNgayTao()) : "N/A");
                        holder.tvDeckInfo.setText(stats);
                    }
                }
            }
        });
    }
    
    private String buildImageUrl(String iconUrl) {
        if (iconUrl == null || iconUrl.isEmpty() || iconUrl.equals("null") || iconUrl.equalsIgnoreCase("null")) {
            return null;
        }
        
        // If already full URL, return as is
        if (iconUrl.startsWith("http://") || iconUrl.startsWith("https://")) {
            return iconUrl;
        }
        
        // Build full URL
        String baseUrl = "http://10.0.2.2:3000";
        if (iconUrl.startsWith("/")) {
            return baseUrl + iconUrl;
        } else {
            return baseUrl + "/" + iconUrl;
        }
    }

    static class DeckViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDeckIcon;
        TextView tvDeckName, tvDeckInfo;
        TextView btnView, btnEdit, btnDelete;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDeckIcon = itemView.findViewById(R.id.deckIcon);
            tvDeckName = itemView.findViewById(R.id.tvDeckName);
            tvDeckInfo = itemView.findViewById(R.id.tvDeckInfo);
            btnView = itemView.findViewById(R.id.btnView);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
