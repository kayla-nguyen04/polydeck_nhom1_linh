package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.ui.adapter.LearningStatusAdapter;
import com.nhom1.polydeck.utils.LearningStatusManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LearningStatusActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";

    private APIService apiService;
    private LearningStatusManager learningStatusManager;
    
    private TextView tvTitle, tvSubtitle, tabUnknown, tabKnown;
    private RecyclerView rvWords;
    private LinearLayout emptyState;
    private ImageButton btnBack;
    
    private LearningStatusAdapter adapter;
    private List<TuVung> allWords = new ArrayList<>();
    private List<TuVung> unknownWords = new ArrayList<>();
    private List<TuVung> knownWords = new ArrayList<>();
    
    private String deckId;
    private String deckName;
    private boolean showingUnknown = true; // true = đang hiển thị chưa nhớ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning_status);

        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        deckName = getIntent().getStringExtra(EXTRA_DECK_NAME);
        if (deckId == null) deckId = "";
        if (deckName == null) deckName = "";

        apiService = RetrofitClient.getApiService();
        learningStatusManager = new LearningStatusManager(this);

        bindViews();
        setupTabs();
        loadWords();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        tabUnknown = findViewById(R.id.tab_unknown);
        tabKnown = findViewById(R.id.tab_known);
        rvWords = findViewById(R.id.rv_words);
        emptyState = findViewById(R.id.empty_state);

        tvTitle.setText("Quản lý từ vựng");
        tvSubtitle.setText(deckName);

        btnBack.setOnClickListener(v -> onBackPressed());

        rvWords.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LearningStatusAdapter(new ArrayList<>(), true);
        adapter.setOnRemoveClickListener(word -> removeWord(word));
        rvWords.setAdapter(adapter);
    }

    private void setupTabs() {
        tabUnknown.setOnClickListener(v -> showUnknownWords());
        tabKnown.setOnClickListener(v -> showKnownWords());
    }

    private void loadWords() {
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allWords.clear();
                    allWords.addAll(response.body());
                    filterWords();
                    updateUI();
                } else {
                    Toast.makeText(LearningStatusActivity.this, "Không tải được từ vựng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                Toast.makeText(LearningStatusActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterWords() {
        Set<String> unknownIds = learningStatusManager.getUnknownWords(deckId);
        Set<String> knownIds = learningStatusManager.getKnownWords(deckId);

        unknownWords.clear();
        knownWords.clear();

        for (TuVung word : allWords) {
            if (word.getId() != null) {
                if (unknownIds.contains(word.getId())) {
                    unknownWords.add(word);
                } else if (knownIds.contains(word.getId())) {
                    knownWords.add(word);
                }
            }
        }
    }

    private void showUnknownWords() {
        showingUnknown = true;
        updateTabStyles();
        adapter = new LearningStatusAdapter(unknownWords, true);
        adapter.setOnRemoveClickListener(word -> removeWord(word));
        rvWords.setAdapter(adapter);
        updateUI();
    }

    private void showKnownWords() {
        showingUnknown = false;
        updateTabStyles();
        adapter = new LearningStatusAdapter(knownWords, false);
        adapter.setOnRemoveClickListener(word -> removeWord(word));
        rvWords.setAdapter(adapter);
        updateUI();
    }

    private void updateTabStyles() {
        if (showingUnknown) {
            tabUnknown.setTextColor(0xFFF44336);
            tabUnknown.setTypeface(null, android.graphics.Typeface.BOLD);
            tabKnown.setTextColor(0xFF9E9E9E);
            tabKnown.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tabUnknown.setTextColor(0xFF9E9E9E);
            tabUnknown.setTypeface(null, android.graphics.Typeface.NORMAL);
            tabKnown.setTextColor(0xFF4CAF50);
            tabKnown.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void updateUI() {
        List<TuVung> currentList = showingUnknown ? unknownWords : knownWords;
        
        if (currentList.isEmpty()) {
            rvWords.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvWords.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }

        // Update subtitle
        int unknownCount = unknownWords.size();
        int knownCount = knownWords.size();
        tvSubtitle.setText(deckName + " • " + unknownCount + " chưa nhớ • " + knownCount + " đã nhớ");
    }

    private void removeWord(TuVung word) {
        if (word.getId() == null) return;

        if (showingUnknown) {
            learningStatusManager.removeFromUnknown(deckId, word.getId());
            filterWords();
            showUnknownWords();
            Toast.makeText(this, "Đã xóa khỏi danh sách chưa nhớ", Toast.LENGTH_SHORT).show();
        } else {
            learningStatusManager.removeFromKnown(deckId, word.getId());
            filterWords();
            showKnownWords();
            Toast.makeText(this, "Đã xóa khỏi danh sách đã nhớ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh khi quay lại
        if (deckId != null && !deckId.isEmpty()) {
            filterWords();
            if (showingUnknown) {
                showUnknownWords();
            } else {
                showKnownWords();
            }
        }
    }
}

