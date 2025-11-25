package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;

public class QuizResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "EXTRA_SCORE";
    public static final String EXTRA_CORRECT = "EXTRA_CORRECT";
    public static final String EXTRA_TOTAL = "EXTRA_TOTAL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        int correct = getIntent().getIntExtra(EXTRA_CORRECT, 0);
        int total = getIntent().getIntExtra(EXTRA_TOTAL, 0);

        TextView tvScore = findViewById(R.id.tv_percent);
        TextView tvAcc = findViewById(R.id.tv_accuracy);
        tvScore.setText(score + "%");
        tvAcc.setText(correct + "/" + total);
    }
}

