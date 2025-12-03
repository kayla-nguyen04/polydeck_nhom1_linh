package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
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
import com.nhom1.polydeck.data.model.TuVung;
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
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra(QuizActivity.EXTRA_DECK_ID, finalDeckId);
            i.putExtra(QuizActivity.EXTRA_DECK_NAME, finalDeckName);
            startActivity(i);
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
        // Refresh vocabulary list when returning from AddVocabularyActivity
        if (deckId != null && !deckId.isEmpty()) {
            loadPreview(deckId);
            updateUnknownCount();
        }
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
                    
                    // Hiển thị tất cả từ vựng thay vì chỉ 5 từ đầu
                    vocabAdapter.updateData(all);

                    // Update counts/progress roughly based on loaded data (placeholder logic)
                    int total = all.size();
                    int learned = Math.max(0, Math.min(total, total / 2));
                    if (tvCounts != null) {
                        tvCounts.setText(total + " từ • " + learned + " đã học");
                    }
                    int percent = total == 0 ? 0 : Math.round(learned * 100f / total);
                    if (tvProgressPercent != null) tvProgressPercent.setText(percent + "%");
                    if (progressXp != null) progressXp.setProgress(percent);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) { }
        });
    }

    private void showFlashcardModeDialog(String deckId, String deckName) {
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
}




