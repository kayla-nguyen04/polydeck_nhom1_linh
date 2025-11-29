package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteWordAdapter extends RecyclerView.Adapter<FavoriteWordAdapter.FavoriteViewHolder> {

    private final List<TuVung> favoriteList;
    private final String userId;
    private final APIService apiService;
    private final Context context;
    private TextToSpeech tts;
    private OnFavoriteRemovedListener onFavoriteRemovedListener;

    public interface OnFavoriteRemovedListener {
        void onFavoriteRemoved(int position);
    }

    public FavoriteWordAdapter(List<TuVung> favoriteList, String userId, Context context) {
        this.favoriteList = favoriteList;
        this.userId = userId;
        this.apiService = RetrofitClient.getApiService();
        this.context = context;
    }

    public void setOnFavoriteRemovedListener(OnFavoriteRemovedListener listener) {
        this.onFavoriteRemovedListener = listener;
    }

    public void setTextToSpeech(TextToSpeech tts) {
        this.tts = tts;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_word, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        TuVung vocab = favoriteList.get(position);
        if (vocab == null) return;

        holder.tvWord.setText(vocab.getTuTiengAnh());
        holder.tvPronunciation.setText(vocab.getPhienAm() != null ? vocab.getPhienAm() : "");
        holder.tvMeaning.setText(vocab.getNghiaTiengViet());
        
        // Example
        String example = vocab.getCauViDu();
        if (example != null && !example.trim().isEmpty()) {
            holder.tvExample.setText("\"" + example + "\"");
            holder.tvExample.setVisibility(View.VISIBLE);
        } else {
            holder.tvExample.setVisibility(View.GONE);
        }

        // Category - có thể lấy từ deck hoặc để mặc định
        holder.tvCategory.setText("Basic English");

        // Icon book với màu khác nhau dựa trên position
        int colorIndex = position % 3;
        int backgroundResId;
        switch (colorIndex) {
            case 0:
                backgroundResId = R.drawable.bg_icon_book; // Blue
                break;
            case 1:
                backgroundResId = R.drawable.bg_icon_book_green; // Green
                break;
            default:
                backgroundResId = R.drawable.bg_icon_book_orange; // Orange
                break;
        }
        holder.iconBook.setBackgroundResource(backgroundResId);

        // Speaker button
        holder.btnSpeak.setOnClickListener(v -> {
            if (tts != null && vocab.getTuTiengAnh() != null) {
                tts.speak(vocab.getTuTiengAnh(), TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        // Favorite button - đã là favorite nên có thể bỏ qua hoặc dùng để xóa
        holder.btnFavorite.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                removeFavorite(vocab.getId(), pos);
            }
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                removeFavorite(vocab.getId(), pos);
            }
        });
    }

    private void removeFavorite(String tuVungId, int position) {
        if (userId == null || tuVungId == null) return;

        apiService.removeFavorite(userId, tuVungId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    if (position >= 0 && position < favoriteList.size()) {
                        favoriteList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, favoriteList.size());
                        if (onFavoriteRemovedListener != null) {
                            onFavoriteRemovedListener.onFavoriteRemoved(position);
                        }
                    }
                } else {
                    if (context != null) {
                        Toast.makeText(context, "Không thể xóa từ yêu thích", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                if (context != null) {
                    Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList != null ? favoriteList.size() : 0;
    }

    public void updateData(List<TuVung> newList) {
        favoriteList.clear();
        if (newList != null) {
            favoriteList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        TextView tvWord, tvPronunciation, tvMeaning, tvExample, tvCategory;
        ImageView btnSpeak, btnFavorite, btnDelete, iconBook;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWord = itemView.findViewById(R.id.tvWord);
            tvPronunciation = itemView.findViewById(R.id.tvPronunciation);
            tvMeaning = itemView.findViewById(R.id.tvMeaning);
            tvExample = itemView.findViewById(R.id.tvExample);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            iconBook = itemView.findViewById(R.id.iconBook);
        }
    }
}

