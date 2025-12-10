package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.QuizBundle;
import com.nhom1.polydeck.data.model.QuizResult;
import com.nhom1.polydeck.data.model.SubmitQuizRequest;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends AppCompatActivity {
    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";
    public static final String EXTRA_NUM_QUESTIONS = "EXTRA_NUM_QUESTIONS";

    private APIService api;
    private QuizBundle quizBundle;
    private String userId;

    private TextView tvQuestion, tvProgress, tvProgressPercent;
    private ProgressBar progressBar;
    private LinearLayout optionA, optionB, optionC, optionD;
    private TextView tvOptionA, tvOptionB, tvOptionC, tvOptionD;
    private MaterialButton btnNext;
    private LinearLayout feedbackContainer;
    private ImageView ivFeedbackIcon;
    private TextView tvFeedback;

    private int currentIndex = 0;
    private int correctCount = 0; // Số câu đúng đã trả lời
    private int answeredCount = 0; // Số câu đã trả lời (để tính phần trăm chính xác)
    private final Map<String, String> picks = new HashMap<>(); // ma_cau_hoi -> ma_lua_chon
    private boolean isAnswerChecked = false; // Đã kiểm tra đáp án chưa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) userId = sm.getUserData().getId();

        tvQuestion = findViewById(R.id.tvQuestion);
        tvProgress = findViewById(R.id.tvProgress);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        btnNext = findViewById(R.id.btnNext);
        
        optionA = findViewById(R.id.optionA);
        optionB = findViewById(R.id.optionB);
        optionC = findViewById(R.id.optionC);
        optionD = findViewById(R.id.optionD);
        
        tvOptionA = findViewById(R.id.tvOptionA);
        tvOptionB = findViewById(R.id.tvOptionB);
        tvOptionC = findViewById(R.id.tvOptionC);
        tvOptionD = findViewById(R.id.tvOptionD);
        
        feedbackContainer = findViewById(R.id.feedbackContainer);
        ivFeedbackIcon = findViewById(R.id.ivFeedbackIcon);
        tvFeedback = findViewById(R.id.tvFeedback);

        // Set click listeners for options
        optionA.setOnClickListener(v -> selectOption(0));
        optionB.setOnClickListener(v -> selectOption(1));
        optionC.setOnClickListener(v -> selectOption(2));
        optionD.setOnClickListener(v -> selectOption(3));

        btnNext.setOnClickListener(v -> {
            if (isAnswerChecked) {
                if (currentIndex < quizBundle.questions.size() - 1) {
                    move(1);
                } else {
                    submitQuiz();
                }
            }
        });

        String deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "Không có chủ đề được chọn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        int numQuestions = getIntent().getIntExtra(EXTRA_NUM_QUESTIONS, -1); // -1 = all questions
        loadQuiz(deckId, numQuestions);
    }

    private void selectOption(int index) {
        if (quizBundle == null || quizBundle.questions == null || currentIndex >= quizBundle.questions.size()) {
            return;
        }
        
        // Không cho chọn lại nếu đã kiểm tra đáp án
        if (isAnswerChecked) {
            return;
        }
        
        QuizBundle.Question q = quizBundle.questions.get(currentIndex);
        if (q.options == null || index >= q.options.size()) {
            return;
        }
        
        String selectedMaLuaChon = q.options.get(index).noiDung;
        picks.put(q.maCauHoi, q.options.get(index).maLuaChon);
        
        // Kiểm tra đáp án đúng
        boolean isCorrect = checkAnswer(q, selectedMaLuaChon);
        
        // Tăng số câu đúng nếu trả lời đúng
        if (isCorrect) {
            correctCount++;
        }
        
        // Tăng số câu đã trả lời
        answeredCount++;
        
        // Cập nhật phần trăm và progress bar
        updateProgress();
        
        // Highlight đáp án
        highlightAnswer(index, isCorrect, q);
        
        // Hiển thị feedback
        showFeedback(isCorrect, q);
        
        // Disable các option để không cho chọn lại
        disableOptions();
        
        // Enable nút tiếp theo
        isAnswerChecked = true;
        btnNext.setEnabled(true);
        
        // Update button text if last question
        if (currentIndex == quizBundle.questions.size() - 1) {
            btnNext.setText("Kết quả");
        } else {
            btnNext.setText("Câu tiếp theo");
        }
    }

    private boolean checkAnswer(QuizBundle.Question q, String selectedText) {
        if (q.dapAnDung == null || q.dapAnDung.isEmpty()) {
            return false;
        }
        
        // So sánh với đáp án đúng (có thể là text hoặc ma_lua_chon)
        String dapAnDung = q.dapAnDung.trim();
        String selected = selectedText.trim();
        
        // So sánh trực tiếp text
        if (dapAnDung.equalsIgnoreCase(selected)) {
            return true;
        }
        
        // So sánh với ma_lua_chon nếu dapAnDung là ma_lua_chon
        if (q.options != null) {
            for (QuizBundle.Option opt : q.options) {
                if (opt.maLuaChon != null && opt.maLuaChon.equals(dapAnDung)) {
                    return opt.noiDung != null && opt.noiDung.trim().equalsIgnoreCase(selected);
                }
            }
        }
        
        return false;
    }

    private void highlightAnswer(int selectedIndex, boolean isCorrect, QuizBundle.Question q) {
        // Tìm đáp án đúng
        int correctIndex = -1;
        String correctText = null;
        
        if (q.dapAnDung != null && q.options != null) {
            String dapAnDung = q.dapAnDung.trim();
            
            // Tìm đáp án đúng trong options
            for (int i = 0; i < q.options.size(); i++) {
                QuizBundle.Option opt = q.options.get(i);
                if (opt.noiDung != null && opt.noiDung.trim().equalsIgnoreCase(dapAnDung)) {
                    correctIndex = i;
                    correctText = opt.noiDung;
                    break;
                }
                if (opt.maLuaChon != null && opt.maLuaChon.equals(dapAnDung)) {
                    correctIndex = i;
                    correctText = opt.noiDung;
                    break;
                }
            }
        }
        
        // Reset tất cả options
        clearOptionSelection();
        
        // Highlight đáp án đúng (màu xanh)
        if (correctIndex >= 0) {
            switch (correctIndex) {
                case 0: optionA.setBackgroundResource(R.drawable.bg_option_correct); break;
                case 1: optionB.setBackgroundResource(R.drawable.bg_option_correct); break;
                case 2: optionC.setBackgroundResource(R.drawable.bg_option_correct); break;
                case 3: optionD.setBackgroundResource(R.drawable.bg_option_correct); break;
            }
        }
        
        // Highlight đáp án sai nếu chọn sai (màu đỏ)
        if (!isCorrect && selectedIndex != correctIndex) {
            switch (selectedIndex) {
                case 0: optionA.setBackgroundResource(R.drawable.bg_option_wrong); break;
                case 1: optionB.setBackgroundResource(R.drawable.bg_option_wrong); break;
                case 2: optionC.setBackgroundResource(R.drawable.bg_option_wrong); break;
                case 3: optionD.setBackgroundResource(R.drawable.bg_option_wrong); break;
            }
        }
    }

    private void showFeedback(boolean isCorrect, QuizBundle.Question q) {
        if (feedbackContainer == null) return;
        
        feedbackContainer.setVisibility(View.VISIBLE);
        
        if (isCorrect) {
            // Đúng
            feedbackContainer.setBackgroundResource(R.drawable.bg_feedback_correct);
            if (ivFeedbackIcon != null) {
                ivFeedbackIcon.setImageResource(R.drawable.ic_check_circle);
                ivFeedbackIcon.setColorFilter(0xFF10B981); // Green color
            }
            if (tvFeedback != null) {
                tvFeedback.setText("Chính xác!");
                tvFeedback.setTextColor(0xFF10B981); // Green color
            }
        } else {
            // Sai
            feedbackContainer.setBackgroundResource(R.drawable.bg_feedback_wrong);
            if (ivFeedbackIcon != null) {
                ivFeedbackIcon.setImageResource(R.drawable.ic_error);
                ivFeedbackIcon.setColorFilter(getResources().getColor(android.R.color.holo_red_dark, null));
            }
            if (tvFeedback != null) {
                // Tìm đáp án đúng để hiển thị
                String correctAnswer = q.dapAnDung;
                if (q.options != null) {
                    for (QuizBundle.Option opt : q.options) {
                        if (opt.noiDung != null && opt.noiDung.trim().equalsIgnoreCase(q.dapAnDung.trim())) {
                            correctAnswer = opt.noiDung;
                            break;
                        }
                        if (opt.maLuaChon != null && opt.maLuaChon.equals(q.dapAnDung)) {
                            correctAnswer = opt.noiDung;
                            break;
                        }
                    }
                }
                tvFeedback.setText("Không chính xác. Đáp án đúng là: " + (correctAnswer != null ? correctAnswer : q.dapAnDung));
                tvFeedback.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
            }
        }
    }

    private void disableOptions() {
        optionA.setClickable(false);
        optionB.setClickable(false);
        optionC.setClickable(false);
        optionD.setClickable(false);
    }

    private void enableOptions() {
        optionA.setClickable(true);
        optionB.setClickable(true);
        optionC.setClickable(true);
        optionD.setClickable(true);
    }

    private void clearOptionSelection() {
        optionA.setBackgroundResource(R.drawable.bg_option_default);
        optionB.setBackgroundResource(R.drawable.bg_option_default);
        optionC.setBackgroundResource(R.drawable.bg_option_default);
        optionD.setBackgroundResource(R.drawable.bg_option_default);
    }

    private void loadQuiz(String deckId, int numQuestions) {
        api.getQuizByTopic(deckId).enqueue(new Callback<ApiResponse<QuizBundle>>() {
            @Override public void onResponse(Call<ApiResponse<QuizBundle>> call, Response<ApiResponse<QuizBundle>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    quizBundle = response.body().getData();
                    if (quizBundle == null || quizBundle.questions == null || quizBundle.questions.isEmpty()) {
                        Toast.makeText(QuizActivity.this, "Chưa có quiz cho chủ đề này", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    
                    // Random/shuffle câu hỏi như flashcard
                    List<QuizBundle.Question> allQuestions = new ArrayList<>(quizBundle.questions);
                    Collections.shuffle(allQuestions);
                    
                    // Chỉ lấy số câu đã chọn (hoặc tất cả nếu numQuestions = -1)
                    if (numQuestions > 0 && numQuestions < allQuestions.size()) {
                        quizBundle.questions = allQuestions.subList(0, numQuestions);
                    } else {
                        quizBundle.questions = allQuestions;
                    }
                    
                    currentIndex = 0;
                    correctCount = 0; // Reset số câu đúng khi bắt đầu quiz mới
                    answeredCount = 0; // Reset số câu đã trả lời
                    showQuestion();
                } else {
                    Toast.makeText(QuizActivity.this, "Chưa có quiz cho chủ đề này", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            @Override public void onFailure(Call<ApiResponse<QuizBundle>> call, Throwable t) {
                Log.e("QuizActivity", "Error loading quiz: ", t);
                Toast.makeText(QuizActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                finish();
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
        
        // Cập nhật phần trăm dựa trên số câu đã trả lời đúng
        updateProgress();

        // Reset state
        isAnswerChecked = false;
        clearOptionSelection();
        enableOptions();
        feedbackContainer.setVisibility(View.GONE);
        btnNext.setEnabled(false);

        // Display options
        if (q.options != null) {
            if (q.options.size() >= 1) {
                tvOptionA.setText(q.options.get(0).noiDung);
                optionA.setVisibility(View.VISIBLE);
            }
            if (q.options.size() >= 2) {
                tvOptionB.setText(q.options.get(1).noiDung);
                optionB.setVisibility(View.VISIBLE);
            }
            if (q.options.size() >= 3) {
                tvOptionC.setText(q.options.get(2).noiDung);
                optionC.setVisibility(View.VISIBLE);
            } else {
                optionC.setVisibility(View.GONE);
            }
            if (q.options.size() >= 4) {
                tvOptionD.setText(q.options.get(3).noiDung);
                optionD.setVisibility(View.VISIBLE);
            } else {
                optionD.setVisibility(View.GONE);
            }
        }

        // Update button text
        if (currentIndex == quizBundle.questions.size() - 1) {
            btnNext.setText("Kết quả");
        } else {
            btnNext.setText("Câu tiếp theo");
        }
    }

    private void move(int delta) {
        currentIndex += delta;
        showQuestion();
    }
    
    private void updateProgress() {
        if (quizBundle == null || quizBundle.questions == null || quizBundle.questions.isEmpty()) {
            return;
        }
        
        int totalQuestions = quizBundle.questions.size();
        // Tính phần trăm tiến độ: số câu đã trả lời / tổng số câu
        int percentage = 0;
        if (totalQuestions > 0) {
            percentage = Math.round((answeredCount * 100f) / totalQuestions);
        }
        
        // Cập nhật TextView phần trăm
        if (tvProgressPercent != null) {
            tvProgressPercent.setText(percentage + "%");
        }
        
        // Cập nhật progress bar
        if (progressBar != null) {
            progressBar.setProgress(percentage);
        }
        
        Log.d("QuizActivity", "Progress updated - Answered: " + answeredCount + "/" + totalQuestions + " = " + percentage + "%");
    }

    private void submitQuiz() {
        if (quizBundle == null || quizBundle.questions == null || quizBundle.questions.isEmpty()) {
            Toast.makeText(this, "Không có câu hỏi", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tính điểm từ client trước
        int totalQuestions = quizBundle.questions.size();
        int correctCount = 0;
        
        for (QuizBundle.Question q : quizBundle.questions) {
            String userAnswer = picks.get(q.maCauHoi);
            if (userAnswer != null && q.dapAnDung != null && q.options != null) {
                // Tìm đáp án người dùng chọn
                QuizBundle.Option selectedOption = null;
                for (QuizBundle.Option opt : q.options) {
                    if (opt.maLuaChon != null && opt.maLuaChon.equals(userAnswer)) {
                        selectedOption = opt;
                        break;
                    }
                }
                
                if (selectedOption != null) {
                    // So sánh với đáp án đúng
                    String selectedText = selectedOption.noiDung != null ? selectedOption.noiDung.trim() : "";
                    String correctAnswer = q.dapAnDung.trim();
                    
                    // Kiểm tra đúng
                    if (selectedText.equalsIgnoreCase(correctAnswer)) {
                        correctCount++;
                    } else {
                        // Kiểm tra xem có phải ma_lua_chon không
                        for (QuizBundle.Option opt : q.options) {
                            if (opt.noiDung != null && opt.noiDung.trim().equalsIgnoreCase(correctAnswer)) {
                                if (opt.maLuaChon != null && opt.maLuaChon.equals(userAnswer)) {
                                    correctCount++;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Tính điểm: mỗi câu đúng = 10 điểm (điểm tuyệt đối, không phải phần trăm)
        // Ví dụ: 5 câu đúng = 50 điểm, 10 câu đúng = 100 điểm
        int scorePoints = correctCount * 10;
        // Giới hạn tối đa 100 điểm
        scorePoints = Math.min(scorePoints, 100);
        
        // Vẫn tính phần trăm để hiển thị
        int scorePercent = totalQuestions > 0 ? Math.round(correctCount * 100f / totalQuestions) : 0;
        
        Log.d("QuizActivity", "Tính điểm từ client - Correct: " + correctCount + "/" + totalQuestions + " = " + scorePoints + " điểm (" + scorePercent + "%)");
        
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
        
        // Gửi kèm số câu đúng, tổng số câu và điểm số để backend lưu trực tiếp
        req.so_cau_dung = correctCount;
        req.tong_so_cau = totalQuestions;
        req.diem_so = scorePoints; // Dùng điểm tuyệt đối thay vì phần trăm

        Log.d("QuizActivity", "Submitting quiz - ma_nguoi_dung: " + req.ma_nguoi_dung + ", ma_quiz: " + req.ma_quiz + ", answers: " + (req.answers != null ? req.answers.size() : 0));
        
        // Dùng điểm tính được từ client (đảm bảo chính xác)
        final int finalScore = scorePoints; // Điểm tuyệt đối (mỗi câu đúng = 10 điểm)
        final int finalCorrect = correctCount;
        final int finalTotal = totalQuestions;
        
        Log.d("QuizActivity", "📤 Submitting quiz - Correct: " + finalCorrect + "/" + finalTotal + ", Score: " + finalScore + " điểm, Answers: " + (req.answers != null ? req.answers.size() : 0));
        Log.d("QuizActivity", "📤 Request data - so_cau_dung: " + req.so_cau_dung + ", tong_so_cau: " + req.tong_so_cau + ", diem_so: " + req.diem_so);
        
        // Log JSON request để debug
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String jsonRequest = gson.toJson(req);
            Log.d("QuizActivity", "📤 JSON Request: " + jsonRequest);
        } catch (Exception e) {
            Log.e("QuizActivity", "Error serializing request to JSON", e);
        }
        
        api.submitQuiz(req).enqueue(new Callback<ApiResponse<QuizResult>>() {
            @Override public void onResponse(Call<ApiResponse<QuizResult>> call, Response<ApiResponse<QuizResult>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    QuizResult r = response.body().getData();
                    if (r != null) {
                        Log.d("QuizActivity", "✅ Quiz submitted successfully - API Score: " + r.scorePercent + "%, Correct: " + r.correct + "/" + r.total + ", Client Score: " + finalScore + "%, Client Correct: " + finalCorrect + "/" + finalTotal);
                        
                        // Kiểm tra xem backend có trả về đúng không
                        if (r.correct != finalCorrect || r.total != finalTotal) {
                            Log.w("QuizActivity", "⚠️ Backend trả về dữ liệu không khớp! Backend: " + r.correct + "/" + r.total + ", Client: " + finalCorrect + "/" + finalTotal);
                        }
                    } else {
                        Log.w("QuizActivity", "⚠️ QuizResult data is null");
                    }
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : "Unknown error";
                    Log.w("QuizActivity", "⚠️ Quiz submit response not successful - Code: " + response.code() + ", Message: " + errorMsg);
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("QuizActivity", "Error body: " + errorBody);
                        } catch (Exception e) {
                            Log.e("QuizActivity", "Cannot read error body", e);
                        }
                    }
                }
                
                // Luôn dùng điểm tính từ client để đảm bảo chính xác
                Intent i = new Intent(QuizActivity.this, QuizResultActivity.class);
                i.putExtra(QuizResultActivity.EXTRA_SCORE, finalScore);
                i.putExtra(QuizResultActivity.EXTRA_CORRECT, finalCorrect);
                i.putExtra(QuizResultActivity.EXTRA_TOTAL, finalTotal);
                startActivity(i);
                finish(); // Đóng QuizActivity sau khi chuyển sang kết quả
            }
            @Override public void onFailure(Call<ApiResponse<QuizResult>> call, Throwable t) {
                Log.e("QuizActivity", "❌ Network error when submitting quiz: ", t);
                // Ngay cả khi API lỗi, vẫn hiển thị kết quả với điểm tính được
                Intent i = new Intent(QuizActivity.this, QuizResultActivity.class);
                i.putExtra(QuizResultActivity.EXTRA_SCORE, finalScore);
                i.putExtra(QuizResultActivity.EXTRA_CORRECT, finalCorrect);
                i.putExtra(QuizResultActivity.EXTRA_TOTAL, finalTotal);
                startActivity(i);
                finish();
            }
        });
    }
}
