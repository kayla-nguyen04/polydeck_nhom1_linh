package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.DeckProgress;
import com.nhom1.polydeck.data.model.QuizBundle;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.utils.SessionManager;
import com.nhom1.polydeck.ui.adapter.VocabularyAdapter;
import com.nhom1.polydeck.utils.LearningStatusManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";

    private APIService apiService;
    private VocabularyAdapter vocabAdapter;
    private TextView tvProgressPercent, tvCounts, tvUnknownCount;
    private ProgressBar progressXp;
    private LearningStatusManager learningStatusManager;
    private EditText edtSearchVocab;

    private String deckId;
    private final List<TuVung> originalVocabList = new ArrayList<>();
    private int cachedTotalWords = 0; // Lưu tổng số từ để refresh progress mà không cần load lại từ vựng
    private Handler refreshHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_detail);

        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        String deckName = getIntent().getStringExtra(EXTRA_DECK_NAME);
        if (deckId == null) deckId = "";
        if (deckName == null) deckName = "";

        apiService = RetrofitClient.getApiService();
        learningStatusManager = new LearningStatusManager(this);

        TextView tvTitle = findViewById(R.id.tv_title);
        tvCounts = findViewById(R.id.tv_counts);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        tvUnknownCount = findViewById(R.id.tv_unknown_count);
        progressXp = findViewById(R.id.progress_xp);
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvTitle.setText(deckName);
        btnBack.setOnClickListener(v -> onBackPressed());

        View btnFlashcard = findViewById(R.id.btn_flashcard);
        View btnQuiz = findViewById(R.id.btn_quiz);
        TextView btnManageStatus = findViewById(R.id.btn_manage_status);
        edtSearchVocab = findViewById(R.id.edtSearchVocab);
        RecyclerView rv = findViewById(R.id.rv_preview_vocab);
        rv.setLayoutManager(new LinearLayoutManager(this));
        vocabAdapter = new VocabularyAdapter(new ArrayList<>(), this);
        rv.setAdapter(vocabAdapter);

        String finalDeckId = deckId;
        String finalDeckName = deckName;
        btnFlashcard.setOnClickListener(v -> {
            showFlashcardModeDialog(finalDeckId, finalDeckName);
        });

        btnQuiz.setOnClickListener(v -> {
            showQuizOptionsDialog(finalDeckId, finalDeckName);
        });

        btnManageStatus.setOnClickListener(v -> {
            Intent i = new Intent(this, LearningStatusActivity.class);
            i.putExtra(LearningStatusActivity.EXTRA_DECK_ID, finalDeckId);
            i.putExtra(LearningStatusActivity.EXTRA_DECK_NAME, finalDeckName);
            startActivity(i);
        });

        // Add click listener to "Danh sách từ" title to view full list
        TextView tvDanhSachTu = findViewById(R.id.tv_danh_sach_tu);
        if (tvDanhSachTu != null) {
            tvDanhSachTu.setOnClickListener(v -> {
                Intent i = new Intent(this, VocabularyListActivity.class);
                i.putExtra(VocabularyListActivity.EXTRA_DECK_ID, finalDeckId);
                i.putExtra(VocabularyListActivity.EXTRA_DECK_NAME, finalDeckName);
                startActivity(i);
            });
            tvDanhSachTu.setClickable(true);
            tvDanhSachTu.setFocusable(true);
        }

        // Setup search functionality
        setupSearch();

        loadPreview(deckId);
        updateUnknownCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh vocabulary list and progress when returning from FlashcardActivity, QuizActivity, etc.
        if (deckId != null && !deckId.isEmpty()) {
            android.util.Log.d("TopicDetailActivity", "onResume - Refreshing data for deckId: " + deckId);
            // Refresh progress ngay lập tức với số từ đã cache (nếu có)
            if (cachedTotalWords > 0) {
                loadDeckProgress(deckId, cachedTotalWords);
                // Refresh lại sau 1 giây để đảm bảo server đã cập nhật xong
                refreshHandler.postDelayed(() -> {
                    if (cachedTotalWords > 0) {
                        android.util.Log.d("TopicDetailActivity", "Delayed refresh progress after 1s");
                        loadDeckProgress(deckId, cachedTotalWords);
                    }
                }, 1000);
            }
            // Load lại từ vựng và cập nhật progress
            loadPreview(deckId);
            updateUnknownCount();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Hủy các delayed refresh khi activity bị pause
        refreshHandler.removeCallbacksAndMessages(null);
    }
    
    private void updateUnknownCount() {
        if (deckId == null || deckId.isEmpty() || tvUnknownCount == null) return;
        int unknownCount = learningStatusManager.getUnknownCount(deckId);
        if (unknownCount > 0) {
            tvUnknownCount.setText(unknownCount + " từ chưa nhớ");
            tvUnknownCount.setVisibility(View.VISIBLE);
        } else {
            tvUnknownCount.setVisibility(View.GONE);
        }
    }

    private void setupSearch() {
        if (edtSearchVocab == null) return;
        
        edtSearchVocab.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase(Locale.getDefault());
                filterVocabulary(query);
            }
        });
    }

    private void filterVocabulary(String query) {
        if (query.isEmpty()) {
            // Hiển thị tất cả nếu không có từ khóa tìm kiếm
            vocabAdapter.updateData(originalVocabList);
        } else {
            // Lọc từ vựng theo từ khóa
            List<TuVung> filtered = new ArrayList<>();
            for (TuVung vocab : originalVocabList) {
                String english = vocab.getTuTiengAnh() != null ? vocab.getTuTiengAnh().toLowerCase(Locale.getDefault()) : "";
                String vietnamese = vocab.getNghiaTiengViet() != null ? vocab.getNghiaTiengViet().toLowerCase(Locale.getDefault()) : "";
                String pronunciation = vocab.getPhienAm() != null ? vocab.getPhienAm().toLowerCase(Locale.getDefault()) : "";
                String example = vocab.getCauViDu() != null ? vocab.getCauViDu().toLowerCase(Locale.getDefault()) : "";
                
                if (english.contains(query) || vietnamese.contains(query) || 
                    pronunciation.contains(query) || example.contains(query)) {
                    filtered.add(vocab);
                }
            }
            vocabAdapter.updateData(filtered);
        }
    }

    private void loadPreview(String deckId) {
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TuVung> all = response.body();
                    // Lưu danh sách gốc để dùng cho tìm kiếm
                    originalVocabList.clear();
                    originalVocabList.addAll(all);
                    
                    // Lưu tổng số từ để dùng cho refresh progress
                    cachedTotalWords = all.size();
                    
                    // Hiển thị tất cả từ vựng thay vì chỉ 5 từ đầu
                    vocabAdapter.updateData(all);

                    // Lấy tiến độ học tập từ API
                    loadDeckProgress(deckId, all.size());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) { }
        });
    }

    private void loadDeckProgress(String deckId, int totalWords) {
        SessionManager sessionManager = new SessionManager(this);
        com.nhom1.polydeck.data.model.LoginResponse user = sessionManager.getUserData();
        String userId = user != null ? user.getId() : null;

        android.util.Log.d("TopicDetailActivity", "loadDeckProgress - deckId: " + deckId + ", userId: " + userId + ", totalWords: " + totalWords);

        if (userId == null) {
            // Chưa đăng nhập: chỉ hiển thị tổng số từ
            if (tvCounts != null) {
                tvCounts.setText(totalWords + " từ • 0 đã học");
            }
            if (tvProgressPercent != null) tvProgressPercent.setText("0%");
            if (progressXp != null) progressXp.setProgress(0);
            return;
        }

        // Gọi API để lấy tiến độ học tập thực tế
        apiService.getDeckProgress(deckId, userId).enqueue(new Callback<ApiResponse<DeckProgress>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<DeckProgress>> call, @NonNull Response<ApiResponse<DeckProgress>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    DeckProgress dp = response.body().getData();
                    if (dp != null) {
                        int total = Math.max(dp.getTotalWords(), totalWords); // Dùng total từ API hoặc từ vocab list
                        int learned = Math.max(dp.getLearnedWords(), 0);
                        // Giới hạn learned không vượt quá total (backend có thể đếm số lần học thay vì số từ duy nhất)
                        learned = Math.min(learned, total);
                        
                        android.util.Log.d("TopicDetailActivity", "✅ Got deck progress - total: " + total + ", learned: " + learned);
                        
                        if (tvCounts != null) {
                            tvCounts.setText(total + " từ • " + learned + " đã học");
                            android.util.Log.d("TopicDetailActivity", "Updated tvCounts: " + total + " từ • " + learned + " đã học");
                        }
                        int percent = total > 0 ? Math.min(100, (int) (learned * 100f / total)) : 0;
                        if (tvProgressPercent != null) {
                            tvProgressPercent.setText(percent + "%");
                            android.util.Log.d("TopicDetailActivity", "Updated tvProgressPercent: " + percent + "%");
                        }
                        if (progressXp != null) {
                            progressXp.setProgress(percent);
                            android.util.Log.d("TopicDetailActivity", "Updated progressXp: " + percent + "%");
                        }
                    } else {
                        android.util.Log.w("TopicDetailActivity", "DeckProgress data is null");
                    }
                } else {
                    android.util.Log.w("TopicDetailActivity", "Failed to get deck progress - Code: " + response.code() + ", Message: " + (response.body() != null ? response.body().getMessage() : "Unknown"));
                    // Fallback nếu API lỗi
                    if (tvCounts != null) {
                        tvCounts.setText(totalWords + " từ • 0 đã học");
                    }
                    if (tvProgressPercent != null) tvProgressPercent.setText("0%");
                    if (progressXp != null) progressXp.setProgress(0);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<DeckProgress>> call, @NonNull Throwable t) {
                android.util.Log.e("TopicDetailActivity", "Error getting deck progress: ", t);
                // Fallback nếu API lỗi
                if (tvCounts != null) {
                    tvCounts.setText(totalWords + " từ • 0 đã học");
                }
                if (tvProgressPercent != null) tvProgressPercent.setText("0%");
                if (progressXp != null) progressXp.setProgress(0);
            }
        });
    }

    private void showFlashcardModeDialog(String deckId, String deckName) {
        // Kiểm tra xem có từ vựng không trước khi mở dialog
        if (originalVocabList.isEmpty()) {
            Toast.makeText(this, "Chưa có từ vựng cho chủ đề này", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int unknownCount = learningStatusManager.getUnknownCount(deckId);
        LearningStatusManager.FlashcardProgress savedProgress = learningStatusManager.getFlashcardProgress(deckId);
        
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_flashcard_mode, null);
        
        CardView cardContinue = dialogView.findViewById(R.id.card_continue);
        CardView cardAll = dialogView.findViewById(R.id.card_all);
        CardView cardReview = dialogView.findViewById(R.id.card_review);
        TextView tvContinueTitle = dialogView.findViewById(R.id.tv_continue_title);
        TextView tvContinueSubtitle = dialogView.findViewById(R.id.tv_continue_subtitle);
        TextView tvReviewTitle = dialogView.findViewById(R.id.tv_review_title);
        TextView tvReviewSubtitle = dialogView.findViewById(R.id.tv_review_subtitle);
        
        // Hiển thị card "Học tiếp" nếu có tiến độ đã lưu
        if (savedProgress != null && savedProgress.index > 0) {
            cardContinue.setVisibility(View.VISIBLE);
            tvContinueTitle.setText("Học tiếp");
            tvContinueSubtitle.setText("Tiếp tục từ vị trí " + (savedProgress.index + 1));
        } else {
            cardContinue.setVisibility(View.GONE);
        }
        
        // Hiển thị card review nếu có từ chưa nhớ
        if (unknownCount > 0) {
            cardReview.setVisibility(View.VISIBLE);
            tvReviewTitle.setText("Học lại từ chưa nhớ (" + unknownCount + " từ)");
            tvReviewSubtitle.setText("Chỉ học lại các từ bạn chưa nhớ");
        } else {
            cardReview.setVisibility(View.GONE);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        
        // Click vào "Học tiếp"
        cardContinue.setOnClickListener(v -> {
            if (savedProgress != null) {
                Intent i = new Intent(this, FlashcardActivity.class);
                i.putExtra(FlashcardActivity.EXTRA_DECK_ID, deckId);
                i.putExtra(FlashcardActivity.EXTRA_DECK_NAME, deckName);
                i.putExtra(FlashcardActivity.EXTRA_REVIEW_UNKNOWN_ONLY, savedProgress.reviewUnknownOnly);
                i.putExtra(FlashcardActivity.EXTRA_RESUME_INDEX, savedProgress.index);
                startActivity(i);
                dialog.dismiss();
            }
        });
        
        // Click vào "Học tất cả"
        cardAll.setOnClickListener(v -> {
            // Xóa tiến độ đã lưu khi chọn học lại từ đầu
            learningStatusManager.clearFlashcardProgress(deckId);
            Intent i = new Intent(this, FlashcardActivity.class);
            i.putExtra(FlashcardActivity.EXTRA_DECK_ID, deckId);
            i.putExtra(FlashcardActivity.EXTRA_DECK_NAME, deckName);
            i.putExtra(FlashcardActivity.EXTRA_REVIEW_UNKNOWN_ONLY, false);
            startActivity(i);
            dialog.dismiss();
        });
        
        // Click vào "Học lại từ chưa nhớ"
        cardReview.setOnClickListener(v -> {
            if (unknownCount > 0) {
                // Xóa tiến độ đã lưu khi chọn học lại từ chưa nhớ
                learningStatusManager.clearFlashcardProgress(deckId);
                Intent i = new Intent(this, FlashcardActivity.class);
                i.putExtra(FlashcardActivity.EXTRA_DECK_ID, deckId);
                i.putExtra(FlashcardActivity.EXTRA_DECK_NAME, deckName);
                i.putExtra(FlashcardActivity.EXTRA_REVIEW_UNKNOWN_ONLY, true);
                startActivity(i);
                dialog.dismiss();
            }
        });
        
        dialog.show();
        
        // Làm tròn góc cho dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void showQuizOptionsDialog(String deckId, String deckName) {
        // Load số câu hỏi có sẵn trước
        apiService.getQuizByTopic(deckId).enqueue(new Callback<ApiResponse<QuizBundle>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<QuizBundle>> call, 
                                   @NonNull Response<ApiResponse<QuizBundle>> response) {
                int totalQuestions = 0;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    QuizBundle bundle = response.body().getData();
                    if (bundle != null && bundle.questions != null) {
                        totalQuestions = bundle.questions.size();
                    }
                }
                
                // Nếu không có quiz, thông báo ngay và không mở dialog
                if (totalQuestions == 0) {
                    Toast.makeText(TopicDetailActivity.this, "Chưa có quiz cho chủ đề này", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                showQuizOptionsDialogWithCount(deckId, deckName, totalQuestions);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<QuizBundle>> call, @NonNull Throwable t) {
                // Nếu không load được, thông báo lỗi
                Toast.makeText(TopicDetailActivity.this, "Không thể tải quiz. Vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQuizOptionsDialogWithCount(String deckId, String deckName, int totalQuestions) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quiz_options, null);
        
        CardView card5 = dialogView.findViewById(R.id.card_5);
        CardView card10 = dialogView.findViewById(R.id.card_10);
        CardView card15 = dialogView.findViewById(R.id.card_15);
        CardView card20 = dialogView.findViewById(R.id.card_20);
        CardView cardAll = dialogView.findViewById(R.id.card_all);
        com.google.android.material.textfield.TextInputEditText etCustom = dialogView.findViewById(R.id.etCustomQuestions);
        com.google.android.material.button.MaterialButton btnStartCustom = dialogView.findViewById(R.id.btnStartCustom);
        TextView tvMaxQuestions = dialogView.findViewById(R.id.tvMaxQuestions);
        
        // Hiển thị số câu hỏi có sẵn
        if (tvMaxQuestions != null) {
            if (totalQuestions > 0) {
                tvMaxQuestions.setText("Có " + totalQuestions + " câu hỏi có sẵn");
                tvMaxQuestions.setVisibility(View.VISIBLE);
            } else {
                tvMaxQuestions.setVisibility(View.GONE);
            }
        }
        
        // Disable và làm mờ các option vượt quá số câu có sẵn
        if (totalQuestions > 0) {
            if (totalQuestions < 5) {
                card5.setEnabled(false);
                card5.setAlpha(0.5f);
            }
            if (totalQuestions < 10) {
                card10.setEnabled(false);
                card10.setAlpha(0.5f);
            }
            if (totalQuestions < 15) {
                card15.setEnabled(false);
                card15.setAlpha(0.5f);
            }
            if (totalQuestions < 20) {
                card20.setEnabled(false);
                card20.setAlpha(0.5f);
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(true);
        
        AlertDialog dialog = builder.create();
        
        card5.setOnClickListener(v -> {
            if (totalQuestions > 0 && totalQuestions < 5) {
                Toast.makeText(TopicDetailActivity.this, "Chỉ có " + totalQuestions + " câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }
            startQuiz(deckId, deckName, 5);
            dialog.dismiss();
        });
        
        card10.setOnClickListener(v -> {
            if (totalQuestions > 0 && totalQuestions < 10) {
                Toast.makeText(TopicDetailActivity.this, "Chỉ có " + totalQuestions + " câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }
            startQuiz(deckId, deckName, 10);
            dialog.dismiss();
        });
        
        card15.setOnClickListener(v -> {
            if (totalQuestions > 0 && totalQuestions < 15) {
                Toast.makeText(TopicDetailActivity.this, "Chỉ có " + totalQuestions + " câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }
            startQuiz(deckId, deckName, 15);
            dialog.dismiss();
        });
        
        card20.setOnClickListener(v -> {
            if (totalQuestions > 0 && totalQuestions < 20) {
                Toast.makeText(TopicDetailActivity.this, "Chỉ có " + totalQuestions + " câu hỏi", Toast.LENGTH_SHORT).show();
                return;
            }
            startQuiz(deckId, deckName, 20);
            dialog.dismiss();
        });
        
        cardAll.setOnClickListener(v -> {
            startQuiz(deckId, deckName, -1); // -1 means all
            dialog.dismiss();
        });
        
        btnStartCustom.setOnClickListener(v -> {
            String customText = etCustom.getText() != null ? etCustom.getText().toString().trim() : "";
            if (customText.isEmpty()) {
                etCustom.setError("Vui lòng nhập số câu hỏi");
                etCustom.requestFocus();
                return;
            }
            try {
                int customNum = Integer.parseInt(customText);
                if (customNum <= 0) {
                    etCustom.setError("Số câu hỏi phải lớn hơn 0");
                    etCustom.requestFocus();
                    return;
                }
                // Kiểm tra nếu vượt quá số câu có sẵn
                if (totalQuestions > 0 && customNum > totalQuestions) {
                    etCustom.setError("Chỉ có " + totalQuestions + " câu hỏi. Vui lòng nhập số nhỏ hơn hoặc bằng " + totalQuestions);
                    etCustom.requestFocus();
                    return;
                }
                startQuiz(deckId, deckName, customNum);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                etCustom.setError("Vui lòng nhập số hợp lệ");
                etCustom.requestFocus();
            }
        });
        
        dialog.show();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void startQuiz(String deckId, String deckName, int numQuestions) {
        Intent i = new Intent(this, QuizActivity.class);
        i.putExtra(QuizActivity.EXTRA_DECK_ID, deckId);
        i.putExtra(QuizActivity.EXTRA_DECK_NAME, deckName);
        i.putExtra(QuizActivity.EXTRA_NUM_QUESTIONS, numQuestions);
        startActivity(i);
    }
}




