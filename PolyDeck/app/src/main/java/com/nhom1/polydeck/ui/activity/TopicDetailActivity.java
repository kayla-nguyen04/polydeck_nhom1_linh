package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.ui.adapter.VocabularyAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicDetailActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";

    private APIService apiService;
    private VocabularyAdapter vocabAdapter;
    private TextView tvProgressPercent, tvCounts;
    private ProgressBar progressXp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_detail);

        String deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        String deckName = getIntent().getStringExtra(EXTRA_DECK_NAME);
        if (deckId == null) deckId = "";
        if (deckName == null) deckName = "";

        apiService = RetrofitClient.getApiService();

        TextView tvTitle = findViewById(R.id.tv_title);
        tvCounts = findViewById(R.id.tv_counts);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        progressXp = findViewById(R.id.progress_xp);
        ImageButton btnBack = findViewById(R.id.btn_back);
        tvTitle.setText(deckName);
        btnBack.setOnClickListener(v -> onBackPressed());

        View btnFlashcard = findViewById(R.id.btn_flashcard);
        View btnQuiz = findViewById(R.id.btn_quiz);
        RecyclerView rv = findViewById(R.id.rv_preview_vocab);
        rv.setLayoutManager(new LinearLayoutManager(this));
        vocabAdapter = new VocabularyAdapter(new ArrayList<>());
        rv.setAdapter(vocabAdapter);

        String finalDeckId = deckId;
        String finalDeckName = deckName;
        btnFlashcard.setOnClickListener(v -> {
            Intent i = new Intent(this, FlashcardActivity.class);
            i.putExtra(FlashcardActivity.EXTRA_DECK_ID, finalDeckId);
            i.putExtra(FlashcardActivity.EXTRA_DECK_NAME, finalDeckName);
            startActivity(i);
        });

        btnQuiz.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra(QuizActivity.EXTRA_DECK_ID, finalDeckId);
            i.putExtra(QuizActivity.EXTRA_DECK_NAME, finalDeckName);
            startActivity(i);
        });

        loadPreview(finalDeckId);
    }

    private void loadPreview(String deckId) {
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TuVung> all = response.body();
                    List<TuVung> preview = all.size() > 5 ? all.subList(0, 5) : all;
                    vocabAdapter.updateData(preview);

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
}




