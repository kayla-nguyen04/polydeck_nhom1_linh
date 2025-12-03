package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.Question;
import com.nhom1.polydeck.ui.adapter.ViewQuestionAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewQuizActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvQuizTitle, tvQuizInfo;
    private RecyclerView rvQuestions;
    private APIService apiService;
    private ViewQuestionAdapter adapter;
    private String quizId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_quiz);

        quizId = getIntent().getStringExtra("QUIZ_ID");
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();

        initViews();
        setupRecyclerView();
        loadQuiz();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvQuizTitle = findViewById(R.id.tvQuizTitle);
        tvQuizInfo = findViewById(R.id.tvQuizInfo);
        rvQuestions = findViewById(R.id.rvQuestions);

        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ViewQuestionAdapter(new ArrayList<>());
        rvQuestions.setAdapter(adapter);
    }

    private void loadQuiz() {
        // Fetch quiz directly by ID from database
        apiService.getQuizById(quizId).enqueue(new Callback<BaiQuiz>() {
            @Override
            public void onResponse(@NonNull Call<BaiQuiz> call, @NonNull Response<BaiQuiz> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayQuiz(response.body());
                } else {
                    Toast.makeText(ViewQuizActivity.this, "Không thể tải quiz", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaiQuiz> call, @NonNull Throwable t) {
                Toast.makeText(ViewQuizActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayQuiz(BaiQuiz quiz) {
        // Fetch deck name
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                String deckName = "Unknown Deck";
                if (response.isSuccessful() && response.body() != null) {
                    for (BoTu deck : response.body()) {
                        if (deck.getId().equals(quiz.getMaChuDe())) {
                            deckName = deck.getTenChuDe();
                            break;
                        }
                    }
                }

                // Set quiz title and info
                tvQuizTitle.setText("Quiz: " + deckName);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String info = String.format(Locale.getDefault(), "%d câu • Thuộc Bộ: %s • %s",
                        quiz.getQuestions() != null ? quiz.getQuestions().size() : 0,
                        deckName,
                        quiz.getCreatedAt() != null ? sdf.format(quiz.getCreatedAt()) : "N/A");
                tvQuizInfo.setText(info);

                // Display questions
                if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
                    adapter.updateData(quiz.getQuestions());
                } else {
                    Toast.makeText(ViewQuizActivity.this, "Quiz chưa có câu hỏi nào", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                // Still display quiz even if deck name fetch fails
                tvQuizTitle.setText("Quiz");
                if (quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
                    adapter.updateData(quiz.getQuestions());
                }
            }
        });
    }
}

