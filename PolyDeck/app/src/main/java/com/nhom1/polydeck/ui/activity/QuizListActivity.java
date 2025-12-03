package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.adapter.QuizAdminAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizListActivity extends AppCompatActivity {

    private static final String TAG = "QuizListActivity";

    private ImageView btnBack, btnAddQuiz;
    private EditText etSearchQuiz;
    private RecyclerView rvQuizList;
    private TextView tvTotalQuizzes, tvPublishedQuizzes;
    private QuizAdminAdapter adapter;
    private APIService apiService;
    private List<BaiQuiz> fullQuizList = new ArrayList<>(); // Store the full list for search restoration
    private Map<String, BoTu> deckMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_list);

        apiService = RetrofitClient.getApiService();

        initViews();
        setupRecyclerView();
        setupSearch();

        btnBack.setOnClickListener(v -> onBackPressed());
        btnAddQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(QuizListActivity.this, CreateQuizActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Fetch data every time the activity is resumed to see changes
        fetchData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnAddQuiz = findViewById(R.id.btnAddQuiz);
        etSearchQuiz = findViewById(R.id.inputSearch);
        rvQuizList = findViewById(R.id.rvQuizList);
        tvTotalQuizzes = findViewById(R.id.tvTotalQuizzes);
        tvPublishedQuizzes = findViewById(R.id.tvPublishedQuizzes);
    }

    private void setupRecyclerView() {
        rvQuizList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizAdminAdapter(this, new ArrayList<>(), new HashMap<>());
        rvQuizList.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearchQuiz.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuizzes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchQuizzes(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.updateData(new ArrayList<>(fullQuizList), deckMap); // Restore the full list
            return;
        }

        List<BaiQuiz> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.getDefault());
        
        for (BaiQuiz quiz : fullQuizList) {
            BoTu deck = deckMap.get(quiz.getMaChuDe());
            String deckName = (deck != null) ? deck.getTenChuDe() : "";
            
            // Search by deck name
            if (deckName.toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                filteredList.add(quiz);
            }
        }
        adapter.updateData(filteredList, deckMap);
    }

    private void fetchData() {
        // We need to fetch both all quizzes and all decks (to map deck ID to deck name)
        Call<List<BoTu>> decksCall = apiService.getAllChuDe();

        // Fetch all decks first
        decksCall.enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    deckMap.clear();
                    deckMap = response.body().stream()
                            .collect(Collectors.toMap(BoTu::getId, deck -> deck));

                    // Now fetch all quizzes
                    Call<List<BaiQuiz>> quizzesCall = apiService.getAllQuizzes();
                    quizzesCall.enqueue(new Callback<List<BaiQuiz>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<BaiQuiz>> call, @NonNull Response<List<BaiQuiz>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                fullQuizList.clear();
                                fullQuizList.addAll(response.body());
                                
                                if (fullQuizList.isEmpty()) {
                                    Toast.makeText(QuizListActivity.this, "Chưa có quiz nào", Toast.LENGTH_SHORT).show();
                                }
                                
                                adapter.updateData(new ArrayList<>(fullQuizList), deckMap);
                                updateStats();
                            } else {
                                Toast.makeText(QuizListActivity.this, "Không thể tải danh sách quiz", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<BaiQuiz>> call, @NonNull Throwable t) {
                            Log.e(TAG, "API call failed: " + t.getMessage());
                            Toast.makeText(QuizListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(QuizListActivity.this, "Không thể tải danh sách bộ từ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(QuizListActivity.this, "Lỗi kết nối khi tải bộ từ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        int totalQuizzes = fullQuizList.size();
        // Assuming all quizzes are published for now
        int publishedQuizzes = totalQuizzes; // You can add logic to check published status
        tvTotalQuizzes.setText(String.valueOf(totalQuizzes));
        tvPublishedQuizzes.setText(String.valueOf(publishedQuizzes));
    }
}
