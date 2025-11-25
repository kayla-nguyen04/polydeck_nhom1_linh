package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.QuizBundle;
import com.nhom1.polydeck.data.model.QuizResult;
import com.nhom1.polydeck.data.model.SubmitQuizRequest;
import com.nhom1.polydeck.ui.adapter.QuizOptionAdapter;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends AppCompatActivity {
    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";

    private APIService api;
    private QuizBundle quizBundle;
    private String userId;

    private TextView tvQuestion, tvProgress, tvTimer;
    private RecyclerView rvOptions;
    private QuizOptionAdapter optionAdapter;
    private Button btnPrev, btnNext, btnSubmit;

    private int currentIndex = 0;
    private final Map<String, String> picks = new HashMap<>(); // ma_cau_hoi -> ma_lua_chon

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) userId = sm.getUserData().getMaNguoiDung();

        tvQuestion = findViewById(R.id.tv_question);
        tvProgress = findViewById(R.id.tv_progress);
        tvTimer = findViewById(R.id.tv_timer);
        rvOptions = findViewById(R.id.rv_options);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnSubmit = findViewById(R.id.btn_submit_quiz);

        rvOptions.setLayoutManager(new LinearLayoutManager(this));
        optionAdapter = new QuizOptionAdapter();
        rvOptions.setAdapter(optionAdapter);

        String deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        loadQuiz(deckId);

        btnPrev.setOnClickListener(v -> move(-1));
        btnNext.setOnClickListener(v -> move(1));
        btnSubmit.setOnClickListener(v -> submitQuiz());
    }

    private void loadQuiz(String deckId) {
        api.getQuizByTopic(deckId).enqueue(new Callback<ApiResponse<QuizBundle>>() {
            @Override public void onResponse(Call<ApiResponse<QuizBundle>> call, Response<ApiResponse<QuizBundle>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    quizBundle = response.body().getData();
                    currentIndex = 0;
                    showQuestion();
                } else {
                    Toast.makeText(QuizActivity.this, "Chưa có quiz cho chủ đề này", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<QuizBundle>> call, Throwable t) {
                Toast.makeText(QuizActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuestion() {
        if (quizBundle == null || quizBundle.questions == null || quizBundle.questions.isEmpty()) return;
        if (currentIndex < 0) currentIndex = 0;
        if (currentIndex >= quizBundle.questions.size()) currentIndex = quizBundle.questions.size() - 1;

        QuizBundle.Question q = quizBundle.questions.get(currentIndex);
        tvQuestion.setText(q.noiDung);
        tvProgress.setText("Câu " + (currentIndex + 1) + "/" + quizBundle.questions.size());

        String pre = picks.get(q.maCauHoi);
        optionAdapter.setData(q.options, pre, chosen -> picks.put(q.maCauHoi, chosen));

        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < quizBundle.questions.size() - 1);
    }

    private void move(int delta) {
        currentIndex += delta;
        showQuestion();
    }

    private void submitQuiz() {
        if (quizBundle == null || quizBundle.questions == null || quizBundle.questions.isEmpty()) {
            Toast.makeText(this, "Không có câu hỏi", Toast.LENGTH_SHORT).show();
            return;
        }
        SubmitQuizRequest req = new SubmitQuizRequest();
        req.ma_nguoi_dung = userId;
        req.ma_quiz = quizBundle.quiz.maQuiz;
        req.ma_chu_de = quizBundle.quiz.maChuDe;
        req.thoi_gian_lam_bai = 0;
        req.answers = new ArrayList<>();
        for (QuizBundle.Question q : quizBundle.questions) {
            String answer = picks.get(q.maCauHoi);
            if (answer != null) {
                req.answers.add(new SubmitQuizRequest.Answer(q.maCauHoi, answer));
            }
        }

        api.submitQuiz(req).enqueue(new Callback<ApiResponse<QuizResult>>() {
            @Override public void onResponse(Call<ApiResponse<QuizResult>> call, Response<ApiResponse<QuizResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    QuizResult r = response.body().getData();
                    Intent i = new Intent(QuizActivity.this, QuizResultActivity.class);
                    i.putExtra(QuizResultActivity.EXTRA_SCORE, r.scorePercent);
                    i.putExtra(QuizResultActivity.EXTRA_CORRECT, r.correct);
                    i.putExtra(QuizResultActivity.EXTRA_TOTAL, r.total);
                    startActivity(i);
                } else {
                    Toast.makeText(QuizActivity.this, "Nộp bài thất bại", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<QuizResult>> call, Throwable t) {
                Toast.makeText(QuizActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

