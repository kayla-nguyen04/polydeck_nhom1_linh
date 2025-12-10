package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.LichSuLamBai;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.ui.adapter.HistoryAdapter;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizHistoryActivity extends AppCompatActivity {
    private static final String TAG = "QuizHistoryActivity";
    
    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private TextView tvEmpty;
    private APIService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);

        initViews();
        loadHistory();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText("Lịch sử làm bài");

        rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        rvHistory.setAdapter(adapter);

        // Thêm click listener để xem chi tiết
        adapter.setOnItemClickListener(history -> {
            // Mở màn hình chi tiết kết quả quiz
            Intent intent = new Intent(this, QuizHistoryDetailActivity.class);
            intent.putExtra(QuizHistoryDetailActivity.EXTRA_HISTORY_ID, history.getId());
            intent.putExtra(QuizHistoryDetailActivity.EXTRA_TOPIC_NAME, history.getTenChuDe());
            intent.putExtra(QuizHistoryDetailActivity.EXTRA_SCORE, history.getDiemSo());
            intent.putExtra(QuizHistoryDetailActivity.EXTRA_CORRECT, history.getSoCauDung());
            intent.putExtra(QuizHistoryDetailActivity.EXTRA_TOTAL, history.getTongSoCau());
            intent.putExtra(QuizHistoryDetailActivity.EXTRA_DATE, history.getNgayLamBai() != null ? history.getNgayLamBai().getTime() : 0);
            startActivity(intent);
        });

        tvEmpty = findViewById(R.id.tv_empty);
    }

    private void loadHistory() {
        LoginResponse user = sessionManager.getUserData();
        String userId = user != null ? user.getId() : null;

        if (userId == null) {
            showEmptyState();
            return;
        }

        // Lấy danh sách tất cả chủ đề để map tên
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, String> topicNameMap = new HashMap<>();
                    for (BoTu deck : response.body()) {
                        if (deck.getId() != null && deck.getTenChuDe() != null) {
                            topicNameMap.put(deck.getId(), deck.getTenChuDe());
                        }
                    }
                    adapter.updateTopicNames(topicNameMap);
                }
                // Tiếp tục load lịch sử
                loadQuizHistory(userId);
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading topics: ", t);
                // Vẫn load lịch sử dù không có tên chủ đề
                loadQuizHistory(userId);
            }
        });
    }

    private void loadQuizHistory(String userId) {
        apiService.getQuizHistory(userId).enqueue(new Callback<ApiResponse<List<LichSuLamBai>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<LichSuLamBai>>> call,
                                   @NonNull Response<ApiResponse<List<LichSuLamBai>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LichSuLamBai> list = response.body().getData();
                    if (list != null && !list.isEmpty()) {
                        adapter.setItems(list);
                        rvHistory.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        showEmptyState();
                    }
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<LichSuLamBai>>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading quiz history: ", t);
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        rvHistory.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
    }
}

