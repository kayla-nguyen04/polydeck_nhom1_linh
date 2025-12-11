package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BaiQuiz;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.adapter.QuizAdminAdapter;
import com.nhom1.polydeck.utils.HiddenQuizManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizManagementActivity extends AppCompatActivity {

    private static final String TAG = "QuizManagementActivity";

    private ImageView btnBack, btnAddQuiz;
    private EditText etSearchQuiz;
    private RecyclerView rvQuizList;
    private RecyclerView rvHiddenQuizzes;
    private View sectionHiddenQuizzes;
    private TextView tvTotalQuizzes, tvPublishedQuizzes;
    private QuizAdminAdapter adapter;
    private QuizAdminAdapter hiddenQuizAdapter;
    private APIService apiService;
    private List<BaiQuiz> fullQuizList = new ArrayList<>(); // Store the full list for search restoration
    private Map<String, BoTu> deckMap = new HashMap<>();
    private HiddenQuizManager hiddenQuizManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_management);

        apiService = RetrofitClient.getApiService();
        hiddenQuizManager = new HiddenQuizManager(this);

        initViews();
        setupRecyclerView();
        setupSearch();

        btnBack.setOnClickListener(v -> onBackPressed());
        btnAddQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(QuizManagementActivity.this, CreateQuizActivity.class);
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
        rvQuizList = findViewById(R.id.recyclerViewQuizzes);
        rvHiddenQuizzes = findViewById(R.id.recyclerViewHiddenQuizzes);
        sectionHiddenQuizzes = findViewById(R.id.sectionHiddenQuizzes);
        tvTotalQuizzes = findViewById(R.id.tvTotalQuizzes);
        tvPublishedQuizzes = findViewById(R.id.tvPublishedQuizzes);
        
        // Xử lý window insets cho NestedScrollView
        View nestedScrollView = findViewById(R.id.nestedScrollView);
        if (nestedScrollView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(nestedScrollView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 
                            Math.max(systemBars.bottom, 16)); // Tối thiểu 16dp
                return insets;
            });
        }
    }

    private void setupRecyclerView() {
        // Adapter cho quiz đang hiển thị
        rvQuizList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuizAdminAdapter(this, new ArrayList<>(), new HashMap<>());
        adapter.setOnQuizHiddenListener(() -> {
            // Refresh data when quiz is hidden
            fetchData();
        });
        rvQuizList.setAdapter(adapter);
        
        // Adapter cho quiz đã ẩn
        rvHiddenQuizzes.setLayoutManager(new LinearLayoutManager(this));
        hiddenQuizAdapter = new QuizAdminAdapter(this, new ArrayList<>(), new HashMap<>());
        hiddenQuizAdapter.setUnhideMode(true);
        hiddenQuizAdapter.setOnQuizHiddenListener(() -> {
            // Refresh data when quiz is unhidden
            fetchData();
        });
        rvHiddenQuizzes.setAdapter(hiddenQuizAdapter);
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
                                
                                // Lọc bỏ các quiz đã ẩn
                                Set<String> hiddenQuizIds = hiddenQuizManager.getHiddenQuizIds();
                                List<BaiQuiz> visibleQuizzes = new ArrayList<>();
                                List<BaiQuiz> hiddenQuizzes = new ArrayList<>();
                                
                                for (BaiQuiz quiz : fullQuizList) {
                                    if (hiddenQuizIds.contains(quiz.getId())) {
                                        hiddenQuizzes.add(quiz);
                                    } else {
                                        visibleQuizzes.add(quiz);
                                    }
                                }
                                
                                if (visibleQuizzes.isEmpty() && hiddenQuizzes.isEmpty()) {
                                    Toast.makeText(QuizManagementActivity.this, "Chưa có quiz nào", Toast.LENGTH_SHORT).show();
                                }
                                
                                adapter.setUnhideMode(false);
                                adapter.updateData(new ArrayList<>(visibleQuizzes), deckMap);
                                
                                hiddenQuizAdapter.setUnhideMode(true);
                                hiddenQuizAdapter.updateData(new ArrayList<>(hiddenQuizzes), deckMap);
                                
                                // Hiển thị/ẩn section quiz đã ẩn
                                if (hiddenQuizzes.isEmpty()) {
                                    sectionHiddenQuizzes.setVisibility(View.GONE);
                                } else {
                                    sectionHiddenQuizzes.setVisibility(View.VISIBLE);
                                }
                                
                                updateStats();
                            } else {
                                Toast.makeText(QuizManagementActivity.this, "Không thể tải danh sách quiz", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<BaiQuiz>> call, @NonNull Throwable t) {
                            Log.e(TAG, "API call failed: " + t.getMessage());
                            Toast.makeText(QuizManagementActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(QuizManagementActivity.this, "Không thể tải danh sách bộ từ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(QuizManagementActivity.this, "Lỗi kết nối khi tải bộ từ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats() {
        // Tổng = tất cả quiz (bao gồm cả ẩn)
        int totalQuizzes = fullQuizList.size();
        
        // Đã xuất bản = chỉ quiz đang hiển thị (không ẩn)
        Set<String> hiddenQuizIds = hiddenQuizManager.getHiddenQuizIds();
        int visibleQuizzes = 0;
        for (BaiQuiz quiz : fullQuizList) {
            if (!hiddenQuizIds.contains(quiz.getId())) {
                visibleQuizzes++;
            }
        }
        // Assuming all visible quizzes are published for now
        int publishedQuizzes = visibleQuizzes;
        
        tvTotalQuizzes.setText(String.valueOf(totalQuizzes));
        tvPublishedQuizzes.setText(String.valueOf(publishedQuizzes));
    }
}
