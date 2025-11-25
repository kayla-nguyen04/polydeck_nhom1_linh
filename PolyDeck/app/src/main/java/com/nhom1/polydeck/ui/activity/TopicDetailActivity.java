package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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
        tvTitle.setText(deckName);

        Button btnFlashcard = findViewById(R.id.btn_flashcard);
        Button btnQuiz = findViewById(R.id.btn_quiz);
        RecyclerView rv = findViewById(R.id.rv_preview_vocab);
        rv.setLayoutManager(new LinearLayoutManager(this));
        vocabAdapter = new VocabularyAdapter(new ArrayList<>());
        rv.setAdapter(vocabAdapter);

        String finalDeckId = deckId;
        String finalDeckName = deckName;
        btnFlashcard.setOnClickListener(v -> {
            Intent i = new Intent(this, VocabularyListActivity.class);
            i.putExtra(VocabularyListActivity.EXTRA_DECK_ID, finalDeckId);
            i.putExtra(VocabularyListActivity.EXTRA_DECK_NAME, finalDeckName);
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
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) { }
        });
    }
}



