package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.ui.adapter.FavoriteWordAdapter;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private FavoriteWordAdapter adapter;
    private TextToSpeech tts;
    private String userId;
    private TextView tvTotalWords;
    private RecyclerView recyclerViewFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        APIService api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        userId = sm.getUserData() != null ? sm.getUserData().getId() : null;
        if (userId == null) { finish(); return; }

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        // Total words
        tvTotalWords = findViewById(R.id.tvTotalWords);

        // RecyclerView
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);
        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoriteWordAdapter(new ArrayList<>(), userId, this);
        recyclerViewFavorites.setAdapter(adapter);

        // Set listener để cập nhật tổng số từ khi xóa
        adapter.setOnFavoriteRemovedListener(position -> {
            updateTotalWords();
        });

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, this);

        // Load favorites
        loadFavoritesWithToast();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh favorites list when returning to this activity (e.g., after deleting a deck)
        if (userId != null) {
            loadFavorites();
        }
    }

    private void loadFavoritesWithToast() {
        APIService api = RetrofitClient.getApiService();
        api.getUserFavorites(userId).enqueue(new Callback<ApiResponse<List<TuVung>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Response<ApiResponse<List<TuVung>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        List<TuVung> data = response.body().getData();
                        if (data != null) {
                            adapter.updateData(data);
                            adapter.setTextToSpeech(tts);
                            updateTotalWords();
                            if (data.isEmpty()) {
                                Toast.makeText(FavoritesActivity.this, "Danh sách yêu thích trống", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            adapter.updateData(new ArrayList<>());
                            updateTotalWords();
                            Toast.makeText(FavoritesActivity.this, "Danh sách yêu thích trống", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String message = response.body().getMessage();
                        Toast.makeText(FavoritesActivity.this, 
                            message != null ? message : "Không tải được danh sách yêu thích", 
                            Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FavoritesActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Throwable t) {
                t.printStackTrace();
                Toast.makeText(FavoritesActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFavorites() {
        APIService api = RetrofitClient.getApiService();
        api.getUserFavorites(userId).enqueue(new Callback<ApiResponse<List<TuVung>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Response<ApiResponse<List<TuVung>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        List<TuVung> data = response.body().getData();
                        if (data != null) {
                            adapter.updateData(data);
                            adapter.setTextToSpeech(tts);
                            updateTotalWords();
                        } else {
                            adapter.updateData(new ArrayList<>());
                            updateTotalWords();
                        }
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Throwable t) {
                // Silent fail on refresh
            }
        });
    }

    private void updateTotalWords() {
        if (tvTotalWords != null && adapter != null) {
            tvTotalWords.setText(String.valueOf(adapter.getItemCount()));
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language not supported
            }
            if (adapter != null) {
                adapter.setTextToSpeech(tts);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}

