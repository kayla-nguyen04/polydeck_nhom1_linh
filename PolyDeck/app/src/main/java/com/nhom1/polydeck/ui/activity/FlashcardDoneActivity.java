package com.nhom1.polydeck.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardDoneActivity extends AppCompatActivity {

    private static final String K_DECK_ID = "K_DECK_ID";
    private static final String K_DECK_NAME = "K_DECK_NAME";
    private static final String K_KNOWN = "K_KNOWN";
    private static final String K_TOTAL = "K_TOTAL";

    public static void start(Context ctx, String deckId, String deckName, int known, int unknown, int total) {
        Intent i = new Intent(ctx, FlashcardDoneActivity.class);
        i.putExtra(K_DECK_ID, deckId);
        i.putExtra(K_DECK_NAME, deckName);
        i.putExtra(K_KNOWN, known);
        i.putExtra(K_TOTAL, total);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_done);

        String deckId = getIntent().getStringExtra(K_DECK_ID);
        String deckName = getIntent().getStringExtra(K_DECK_NAME);
        int known = getIntent().getIntExtra(K_KNOWN, 0);
        int total = getIntent().getIntExtra(K_TOTAL, 0);

        Log.d("FlashcardDoneActivity", "Dữ liệu: deckId=" + deckId + ", deckName=" + deckName + ", known=" + known + ", total=" + total);

        // Streak đã được tăng ngay khi bắt đầu học trong FlashcardActivity
        // Chỉ refresh user data để hiển thị thông tin mới nhất
        refreshUserData();

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvSubtitle = findViewById(R.id.tv_subtitle);
        
        if (tvTitle != null) {
        tvTitle.setText("Đã học xong!");
        }
        
        if (tvSubtitle != null) {
            // Hiển thị đúng số từ đã học
            String deckNameText = (deckName != null && !deckName.isEmpty()) ? deckName : "chủ đề này";
            String subtitleText = "Bạn đã hoàn thành " + total + " từ vựng của chủ đề " + deckNameText + ".";
            tvSubtitle.setText(subtitleText);
        }

        MaterialButton btnQuiz = findViewById(R.id.btn_quiz_now);
        MaterialButton btnBack = findViewById(R.id.btn_back_list);

        if (btnQuiz != null) {
        btnQuiz.setOnClickListener(v -> {
                // Kiểm tra xem có quiz không trước khi mở
                if (deckId != null && !deckId.isEmpty()) {
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra(QuizActivity.EXTRA_DECK_ID, deckId);
            i.putExtra(QuizActivity.EXTRA_DECK_NAME, deckName);
            i.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, -1); // -1 = all questions
            startActivity(i);
                }
                finish();
            });
        }
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Set result để HomeFragment biết cần refresh
                setResult(RESULT_OK);
            finish();
        });
        }
    }

    private void refreshUserData() {
        SessionManager sessionManager = new SessionManager(this);
        com.nhom1.polydeck.data.model.LoginResponse user = sessionManager.getUserData();
        
        if (user == null || user.getId() == null) {
            return;
        }

        APIService apiService = RetrofitClient.getApiService();
        apiService.getUserDetail(user.getId()).enqueue(new Callback<com.nhom1.polydeck.data.model.User>() {
            @Override
            public void onResponse(Call<com.nhom1.polydeck.data.model.User> call, 
                                   Response<com.nhom1.polydeck.data.model.User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Cập nhật session với dữ liệu mới nhất từ server
                    sessionManager.refreshUserData(response.body());
                    Log.d("FlashcardDoneActivity", "✅ Đã refresh user data với streak mới");
                }
            }

            @Override
            public void onFailure(Call<com.nhom1.polydeck.data.model.User> call, Throwable t) {
                Log.e("FlashcardDoneActivity", "Lỗi khi refresh user data: ", t);
            }
        });
    }
}



