package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.FavoriteRequest;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.VocabViewHolder> {

    private final List<TuVung> vocabList;
    private final Context context;
    private final String userId;
    private final APIService apiService;
    private final Set<String> favoriteIds = new HashSet<>();
    private final boolean isAdmin;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    public VocabularyAdapter(List<TuVung> vocabList, Context context) {
        this.vocabList = vocabList;
        this.context = context;
        SessionManager sm = new SessionManager(context);
        this.userId = sm.getUserData() != null ? sm.getUserData().getId() : null;
        String vaiTro = sm.getVaiTro();
        this.isAdmin = vaiTro != null && vaiTro.equals("admin");
        this.apiService = RetrofitClient.getApiService();
        initTts();
        if (!isAdmin) {
            loadFavorites();
        }
    }
    
    private void initTts() {
        try {
            tts = new TextToSpeech(context, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("VocabularyAdapter", "TTS Language not supported or missing data");
                        ttsReady = false;
                    } else {
                        ttsReady = true;
                        Log.d("VocabularyAdapter", "TTS initialized successfully");
                    }
                } else {
                    Log.e("VocabularyAdapter", "TTS initialization failed - status: " + status);
                    ttsReady = false;
                }
            });
        } catch (Exception e) {
            Log.e("VocabularyAdapter", "Error initializing TTS", e);
            ttsReady = false;
        }
    }

    private void loadFavorites() {
        if (userId == null) return;
        apiService.getUserFavorites(userId).enqueue(new Callback<ApiResponse<List<TuVung>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Response<ApiResponse<List<TuVung>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    favoriteIds.clear();
                    List<TuVung> favs = response.body().getData();
                    if (favs != null) {
                        for (TuVung t : favs) {
                            if (t.getId() != null) {
                                favoriteIds.add(t.getId());
                            }
                        }
                    }
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Throwable t) {
                // Silent fail - favorites will just not be marked
            }
        });
    }

    @NonNull
    @Override
    public VocabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vocabulary, parent, false);
        return new VocabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VocabViewHolder holder, int position) {
        TuVung vocab = vocabList.get(position);
        if (vocab == null) return;

        holder.tvEnglish.setText(vocab.getTuTiengAnh());
        holder.tvPronunciation.setText(vocab.getPhienAm() != null ? vocab.getPhienAm() : "");
        holder.tvVietnamese.setText(vocab.getNghiaTiengViet());
        
        TextView tvExample = holder.itemView.findViewById(R.id.tvVocabExample);
        if (tvExample != null) {
            String ex = vocab.getCauViDu();
            if (ex != null && !ex.trim().isEmpty()) {
                tvExample.setText("\"" + ex + "\"");
                tvExample.setVisibility(View.VISIBLE);
            } else {
                tvExample.setVisibility(View.GONE);
            }
        }

        // Update favorite icon - Ẩn nút tim nếu là admin
        if (isAdmin) {
            holder.btnFavorite.setVisibility(View.GONE);
        } else {
            holder.btnFavorite.setVisibility(View.VISIBLE);
            boolean isFavorite = vocab.getId() != null && favoriteIds.contains(vocab.getId());
            holder.btnFavorite.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_heart_outline);
            if (isFavorite) {
                holder.btnFavorite.setColorFilter(0xFFEF4444); // Red color
            } else {
                holder.btnFavorite.setColorFilter(context.getResources().getColor(R.color.gray_medium, null));
            }
        }

        // Speaker button click
        holder.btnSpeak.setOnClickListener(v -> {
            String word = vocab.getTuTiengAnh();
            if (word == null || word.trim().isEmpty()) {
                Toast.makeText(context, "Không có từ để phát âm", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Kiểm tra TTS có sẵn sàng không
            if (tts == null) {
                Log.d("VocabularyAdapter", "TTS is null, reinitializing...");
                initTts();
                Toast.makeText(context, "Đang khởi tạo phát âm...", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!ttsReady) {
                Toast.makeText(context, "Phát âm chưa sẵn sàng. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                // Dừng phát âm hiện tại nếu có
                tts.stop();
                // Phát âm từ mới
                int result = tts.speak(word.trim(), TextToSpeech.QUEUE_FLUSH, null, "word");
                if (result == TextToSpeech.ERROR) {
                    Log.e("VocabularyAdapter", "TTS speak error");
                    Toast.makeText(context, "Lỗi phát âm", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d("VocabularyAdapter", "Speaking: " + word);
                }
            } catch (Exception e) {
                Log.e("VocabularyAdapter", "Error speaking word", e);
                Toast.makeText(context, "Lỗi khi phát âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Favorite button click - Chỉ xử lý nếu không phải admin
        if (!isAdmin) {
            holder.btnFavorite.setOnClickListener(v -> {
                if (userId == null) {
                    Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                boolean isFavorite = vocab.getId() != null && favoriteIds.contains(vocab.getId());
                if (isFavorite) {
                    // Remove favorite
                    removeFavorite(vocab.getId(), position);
                } else {
                    // Add favorite
                    addFavorite(vocab.getId(), position);
                }
            });
        } else {
            holder.btnFavorite.setOnClickListener(null);
        }
    }

    private void addFavorite(String tuVungId, int position) {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            Log.e("VocabularyAdapter", "addFavorite: userId is null or empty");
            return;
        }
        
        if (tuVungId == null || tuVungId.isEmpty()) {
            Toast.makeText(context, "ID từ vựng không hợp lệ", Toast.LENGTH_SHORT).show();
            Log.e("VocabularyAdapter", "addFavorite: tuVungId is null or empty");
            return;
        }
        
        Log.d("VocabularyAdapter", "addFavorite - userId: " + userId + ", tuVungId: " + tuVungId);
        
        apiService.addFavorite(userId, new FavoriteRequest(tuVungId)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                Log.d("VocabularyAdapter", "addFavorite response - code: " + response.code() + ", isSuccessful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        favoriteIds.add(tuVungId);
                        notifyItemChanged(position);
                        Toast.makeText(context, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                        Log.d("VocabularyAdapter", "addFavorite: Success");
                    } else {
                        // API trả về success = false
                        String message = response.body().getMessage();
                        Log.e("VocabularyAdapter", "addFavorite: API returned success=false, message: " + message);
                        Toast.makeText(context, 
                            message != null && !message.isEmpty() ? message : "Không thể thêm vào yêu thích", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Response không thành công (code != 200)
                    Log.e("VocabularyAdapter", "addFavorite failed - code: " + response.code() + ", message: " + response.message());
                    String errorMsg = "Không thể thêm vào yêu thích (Lỗi " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("VocabularyAdapter", "addFavorite errorBody: " + errorBody);
                            if (errorBody != null && !errorBody.isEmpty()) {
                                // Thử parse JSON để lấy message
                                try {
                                    com.google.gson.Gson gson = new com.google.gson.Gson();
                                    ApiResponse<?> errorResponse = gson.fromJson(errorBody, ApiResponse.class);
                                    if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                                        errorMsg = errorResponse.getMessage();
                                    }
                                } catch (Exception e) {
                                    // Nếu không phải JSON (có thể là HTML), chỉ hiển thị code
                                    errorMsg = "Lỗi: " + response.code();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("VocabularyAdapter", "Error reading errorBody", e);
                        }
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                t.printStackTrace();
                Toast.makeText(context, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFavorite(String tuVungId, int position) {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            Log.e("VocabularyAdapter", "removeFavorite: userId is null or empty");
            return;
        }
        
        if (tuVungId == null || tuVungId.isEmpty()) {
            Toast.makeText(context, "ID từ vựng không hợp lệ", Toast.LENGTH_SHORT).show();
            Log.e("VocabularyAdapter", "removeFavorite: tuVungId is null or empty");
            return;
        }
        
        Log.d("VocabularyAdapter", "removeFavorite - userId: " + userId + ", tuVungId: " + tuVungId);
        
        apiService.removeFavorite(userId, tuVungId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                Log.d("VocabularyAdapter", "removeFavorite response - code: " + response.code() + ", isSuccessful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        favoriteIds.remove(tuVungId);
                        notifyItemChanged(position);
                        Toast.makeText(context, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                        Log.d("VocabularyAdapter", "removeFavorite: Success");
                    } else {
                        String message = response.body().getMessage();
                        Log.e("VocabularyAdapter", "removeFavorite: API returned success=false, message: " + message);
                        Toast.makeText(context, 
                            message != null && !message.isEmpty() ? message : "Không thể xóa khỏi yêu thích", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("VocabularyAdapter", "removeFavorite failed - code: " + response.code() + ", message: " + response.message());
                    String errorMsg = "Không thể xóa khỏi yêu thích (Lỗi " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("VocabularyAdapter", "removeFavorite errorBody: " + errorBody);
                            if (errorBody != null && !errorBody.isEmpty()) {
                                // Thử parse JSON để lấy message
                                try {
                                    com.google.gson.Gson gson = new com.google.gson.Gson();
                                    ApiResponse<?> errorResponse = gson.fromJson(errorBody, ApiResponse.class);
                                    if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                                        errorMsg = errorResponse.getMessage();
                                    }
                                } catch (Exception e) {
                                    // Nếu không phải JSON (có thể là HTML), chỉ hiển thị code
                                    errorMsg = "Lỗi: " + response.code();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("VocabularyAdapter", "Error reading errorBody", e);
                        }
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                Log.e("VocabularyAdapter", "removeFavorite onFailure", t);
                t.printStackTrace();
                Toast.makeText(context, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return vocabList != null ? vocabList.size() : 0;
    }

    // FIX: Added this method to update the adapter's data
    public void updateData(List<TuVung> newList) {
        vocabList.clear();
        if (newList != null) {
            vocabList.addAll(newList);
        }
        notifyDataSetChanged();
    }
    
    public void cleanup() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    static class VocabViewHolder extends RecyclerView.ViewHolder {
        TextView tvEnglish, tvPronunciation, tvVietnamese;
        ImageView btnFavorite, btnSpeak;

        public VocabViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEnglish = itemView.findViewById(R.id.tvVocabEnglish);
            tvPronunciation = itemView.findViewById(R.id.tvVocabPronunciation);
            tvVietnamese = itemView.findViewById(R.id.tvVocabVietnamese);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);
        }
    }
}
