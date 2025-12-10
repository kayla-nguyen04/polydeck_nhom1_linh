package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QuizHistoryDetailActivity extends AppCompatActivity {
    public static final String EXTRA_HISTORY_ID = "EXTRA_HISTORY_ID";
    public static final String EXTRA_TOPIC_NAME = "EXTRA_TOPIC_NAME";
    public static final String EXTRA_SCORE = "EXTRA_SCORE";
    public static final String EXTRA_CORRECT = "EXTRA_CORRECT";
    public static final String EXTRA_TOTAL = "EXTRA_TOTAL";
    public static final String EXTRA_DATE = "EXTRA_DATE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history_detail);

        initViews();
        loadData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadData() {
        String topicName = getIntent().getStringExtra(EXTRA_TOPIC_NAME);
        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        int correct = getIntent().getIntExtra(EXTRA_CORRECT, 0);
        int total = getIntent().getIntExtra(EXTRA_TOTAL, 0);
        long dateMillis = getIntent().getLongExtra(EXTRA_DATE, 0);

        TextView tvTitle = findViewById(R.id.tv_title);
        if (tvTitle != null) {
            tvTitle.setText(topicName != null ? topicName : "Chi tiết kết quả");
        }

        TextView tvTopic = findViewById(R.id.tv_topic);
        if (tvTopic != null) {
            tvTopic.setText("Chủ đề: " + (topicName != null ? topicName : "-"));
        }

        TextView tvScore = findViewById(R.id.tv_score);
        if (tvScore != null) {
            // Điểm bây giờ là điểm tuyệt đối (mỗi câu đúng = 10 điểm), không phải phần trăm
            tvScore.setText(score + " điểm");
        }

        TextView tvCorrect = findViewById(R.id.tv_correct);
        if (tvCorrect != null) {
            tvCorrect.setText(correct + "/" + total);
        }

        TextView tvDate = findViewById(R.id.tv_date);
        if (tvDate != null && dateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvDate.setText("Ngày làm: " + sdf.format(new Date(dateMillis)));
        } else if (tvDate != null) {
            tvDate.setText("Ngày làm: -");
        }
    }
}

