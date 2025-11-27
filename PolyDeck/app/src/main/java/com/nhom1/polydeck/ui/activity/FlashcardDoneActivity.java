package com.nhom1.polydeck.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;

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

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvSubtitle = findViewById(R.id.tv_subtitle);
        tvTitle.setText("Đã học xong!");
        tvSubtitle.setText("Bạn đã hoàn thành " + total + " từ vựng của chủ đề " + (deckName != null ? deckName : "") + ".");

        Button btnQuiz = findViewById(R.id.btn_quiz_now);
        Button btnBack = findViewById(R.id.btn_back_list);

        btnQuiz.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity.class);
            i.putExtra(QuizActivity.EXTRA_DECK_ID, deckId);
            i.putExtra(QuizActivity.EXTRA_DECK_NAME, deckName);
            startActivity(i);
            finish();
        });
        btnBack.setOnClickListener(v -> finish());
    }
}



