package com.nhom1.polydeck.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import android.widget.Toast;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.LichSuLamBai;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.adapter.HistoryAdapter;
import com.nhom1.polydeck.utils.LearningStatusManager;
import com.nhom1.polydeck.utils.SessionManager;
import com.nhom1.polydeck.data.model.LoginResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {
    private static final String TAG = "StatsFragment";
    private HistoryAdapter adapter;
    private TextView tvStreak, tvXp, tvWords, tvAccuracy;
    private TextView tvWeeklyWords, tvWeeklyQuizzes, tvWeeklyDays, tvWeeklyXp;
    private boolean isVisibleToUser = false;
    private boolean hasLoadedStats = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvStreak = view.findViewById(R.id.tv_streak);
        tvXp = view.findViewById(R.id.tv_xp);
        tvWords = view.findViewById(R.id.tv_words);
        tvAccuracy = view.findViewById(R.id.tv_accuracy);
        
        // Weekly stats
        tvWeeklyWords = view.findViewById(R.id.tv_weekly_words);
        tvWeeklyQuizzes = view.findViewById(R.id.tv_weekly_quizzes);
        tvWeeklyDays = view.findViewById(R.id.tv_weekly_days);
        tvWeeklyXp = view.findViewById(R.id.tv_weekly_xp);

        RecyclerView rv = view.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter();
        rv.setAdapter(adapter);

        // Xử lý click "Xem tất cả" để mở màn hình xem tất cả lịch sử
        TextView tvViewAll = view.findViewById(R.id.tv_history_view_all);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.nhom1.polydeck.ui.activity.QuizHistoryActivity.class);
                startActivity(intent);
            });
        }
        
        // Thêm click listener vào adapter để xem chi tiết
        adapter.setOnItemClickListener(history -> {
            Intent intent = new Intent(requireContext(), com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.class);
            intent.putExtra(com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.EXTRA_HISTORY_ID, history.getId());
            intent.putExtra(com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.EXTRA_TOPIC_NAME, history.getTenChuDe());
            intent.putExtra(com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.EXTRA_SCORE, history.getDiemSo());
            intent.putExtra(com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.EXTRA_CORRECT, history.getSoCauDung());
            intent.putExtra(com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.EXTRA_TOTAL, history.getTongSoCau());
            intent.putExtra(com.nhom1.polydeck.ui.activity.QuizHistoryDetailActivity.EXTRA_DATE, history.getNgayLamBai() != null ? history.getNgayLamBai().getTime() : 0);
            startActivity(intent);
        });

        loadStats();
        hasLoadedStats = true;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Luôn refresh stats khi quay lại fragment để đảm bảo dữ liệu mới nhất
        // (sau khi làm quiz, học từ mới, etc.)
        // Chỉ refresh nếu fragment đã được tạo và đã load lần đầu
        if (hasLoadedStats && isAdded() && getView() != null) {
            // Thêm delay để đảm bảo backend đã lưu dữ liệu (sau khi làm quiz)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && getView() != null) {
                    loadStats();
                }
            }, 500); // Delay 500ms để backend xử lý xong
        }
    }
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;
        // Refresh stats khi fragment được hiển thị
        if (isVisibleToUser && isResumed()) {
            loadStats();
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // Refresh khi fragment được hiển thị lại (không bị ẩn) và đã load lần đầu
        if (!hidden && isResumed() && hasLoadedStats) {
            // Thêm delay để đảm bảo backend đã lưu dữ liệu
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && getView() != null && !isHidden()) {
                    loadStats();
                }
            }, 500);
        }
    }
    
    private void loadStats() {
        Log.d(TAG, "loadStats: Starting to load statistics");
        APIService api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(requireContext());
        LearningStatusManager learningStatusManager = new LearningStatusManager(requireContext());
        
        // Lấy thông tin user để có userId
        LoginResponse user = sm.getUserData();
        String userId = user != null ? user.getId() : null;
        Log.d(TAG, "loadStats: userId = " + userId);
        
        // Hiển thị dữ liệu từ session ngay lập tức (không cần đợi API)
        if (user != null) {
            int userStreak = user.getChuoiNgayHoc();
            int userXp = user.getDiemTichLuy();
            Log.d(TAG, "loadStats: Displaying session data - Streak: " + userStreak + ", XP: " + userXp);
            if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
            if (tvXp != null) tvXp.setText(String.valueOf(userXp));
        } else {
            Log.w(TAG, "loadStats: No user data in session, setting default values");
            if (tvStreak != null) tvStreak.setText("0 ngày");
            if (tvXp != null) tvXp.setText("0");
        }
        
        // Set default values cho các stats khác (sẽ được cập nhật sau khi load quiz history)
        if (tvWords != null) tvWords.setText("0");
        if (tvAccuracy != null) tvAccuracy.setText("0%");
        if (tvWeeklyWords != null) tvWeeklyWords.setText("0");
        if (tvWeeklyQuizzes != null) tvWeeklyQuizzes.setText("0");
        if (tvWeeklyDays != null) tvWeeklyDays.setText("0/7");
        if (tvWeeklyXp != null) tvWeeklyXp.setText("0");
        
        if (userId == null) {
            Log.w(TAG, "loadStats: userId is null, cannot load from server");
            return;
        }

        // Refresh user data từ server để có thông tin mới nhất về streak và XP
        Log.d(TAG, "loadStats: Calling getUserDetail for userId: " + userId);
        api.getUserDetail(userId).enqueue(new Callback<com.nhom1.polydeck.data.model.User>() {
            @Override
            public void onResponse(Call<com.nhom1.polydeck.data.model.User> call, Response<com.nhom1.polydeck.data.model.User> response) {
                if (!isAdded()) {
                    Log.w(TAG, "getUserDetail onResponse: Fragment not added, returning");
                    return;
                }
                if (response.isSuccessful() && response.body() != null) {
                    com.nhom1.polydeck.data.model.User userData = response.body();
                    // Cập nhật Streak và XP từ server
                    int userStreak = userData.getChuoiNgayHoc();
                    int userXp = userData.getXp();
                    Log.d(TAG, "getUserDetail onResponse: Success - Streak: " + userStreak + ", XP: " + userXp);
                    
                    // Cập nhật session với dữ liệu mới nhất
                    SessionManager sm = new SessionManager(requireContext());
                    sm.refreshUserData(userData);
                    
                    if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
                    if (tvXp != null) tvXp.setText(String.valueOf(userXp));
                } else {
                    Log.w(TAG, "getUserDetail onResponse: Failed - Code: " + response.code() + ", Body: " + (response.body() != null ? "not null" : "null"));
                    // Fallback: dùng data từ session nếu không lấy được từ server
                    int userStreak = user != null ? user.getChuoiNgayHoc() : 0;
                    int userXp = user != null ? user.getDiemTichLuy() : 0;
                    Log.d(TAG, "getUserDetail onResponse: Using fallback - Streak: " + userStreak + ", XP: " + userXp);
                    if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
                    if (tvXp != null) tvXp.setText(String.valueOf(userXp));
                }
            }

            @Override
            public void onFailure(Call<com.nhom1.polydeck.data.model.User> call, Throwable t) {
                Log.e(TAG, "getUserDetail onFailure: " + t.getMessage(), t);
                // Fallback: dùng data từ session nếu lỗi
                int userStreak = user != null ? user.getChuoiNgayHoc() : 0;
                int userXp = user != null ? user.getDiemTichLuy() : 0;
                if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
                if (tvXp != null) tvXp.setText(String.valueOf(userXp));
            }
        });

        Log.d(TAG, "loadStats: Calling getQuizHistory for userId: " + userId);
        loadQuizHistory(userId, 0); // Retry count = 0
    }
    
    private void loadQuizHistory(String userId, int retryCount) {
        APIService api = RetrofitClient.getApiService();
        final LearningStatusManager learningStatusManager = new LearningStatusManager(requireContext());
        api.getQuizHistory(userId).enqueue(new Callback<ApiResponse<List<LichSuLamBai>>>() {
            @Override public void onResponse(Call<ApiResponse<List<LichSuLamBai>>> call, Response<ApiResponse<List<LichSuLamBai>>> response) {
                if (!isAdded()) {
                    Log.w(TAG, "getQuizHistory onResponse: Fragment not added, returning");
                    return;
                }
                Log.d(TAG, "getQuizHistory onResponse: Code: " + response.code() + ", Success: " + (response.body() != null ? response.body().isSuccess() : "null body"));
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LichSuLamBai> list = response.body().getData();
                    Log.d(TAG, "getQuizHistory onResponse: Got " + (list != null ? list.size() : 0) + " history items");
                    if (list == null) {
                        Log.w(TAG, "getQuizHistory onResponse: List is null, using empty list");
                        list = new ArrayList<>();
                    }
                    adapter.setItems(list);

                    // Tính độ chính xác từ quiz (không tính từ đã học từ quiz)
                    int totalCorrect = 0;
                    int totalQuestions = 0;
                    
                    if (list != null && !list.isEmpty()) {
                        for (LichSuLamBai h : list) {
                            // Log từng item để debug
                            Log.d(TAG, "Quiz history item - SoCauDung: " + h.getSoCauDung() + ", TongSoCau: " + h.getTongSoCau() + ", NgayLamBai: " + h.getNgayLamBai());
                            
                            // Tính độ chính xác: tổng câu đúng / tổng câu hỏi
                            totalCorrect += Math.max(0, h.getSoCauDung());
                            totalQuestions += Math.max(1, h.getTongSoCau()); // Tránh chia 0
                        }
                    }
                    Log.d(TAG, "getQuizHistory onResponse: totalCorrect=" + totalCorrect + ", totalQuestions=" + totalQuestions);
                    
                    // Tính độ chính xác trung bình: (tổng câu đúng / tổng câu hỏi) * 100
                    int avgAcc = totalQuestions > 0 ? Math.round((totalCorrect * 100f) / totalQuestions) : 0;
                    
                    Log.d(TAG, "Calculated accuracy: " + avgAcc + "% (from " + totalCorrect + "/" + totalQuestions + ")");
                    
                    if (tvAccuracy != null) {
                        tvAccuracy.setText(avgAcc + "%");
                        Log.d(TAG, "Updated tvAccuracy to: " + avgAcc + "%");
                    } else {
                        Log.w(TAG, "tvAccuracy is null!");
                    }
                    
                    // Tạo biến final để sử dụng trong inner class
                    final List<LichSuLamBai> finalList = list;
                    final LearningStatusManager finalLearningStatusManager = learningStatusManager;
                    
                    // Lấy tất cả deck để tính từ đã học từ flashcard (KHÔNG tính từ quiz)
                    api.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
                        @Override
                        public void onResponse(Call<List<BoTu>> call, Response<List<BoTu>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null) {
                                // Lấy số từ đã học từ server cho mỗi deck (DeckProgress.learnedWords)
                                List<BoTu> decks = response.body();
                                if (decks.isEmpty()) {
                                    if (tvWords != null) tvWords.setText("0");
                                    calculateWeeklyStats(finalList, finalLearningStatusManager, decks);
                                    return;
                                }

                                AtomicInteger remaining = new AtomicInteger(decks.size());
                                AtomicInteger learnedFromDecks = new AtomicInteger(0);

                                for (BoTu deck : decks) {
                                    if (deck.getId() == null) {
                                        if (remaining.decrementAndGet() == 0) {
                                            // Chỉ tính từ flashcard, không tính từ quiz
                                            int totalWordsLearned = learnedFromDecks.get();
                                            if (tvWords != null) tvWords.setText(String.valueOf(totalWordsLearned));
                                            calculateWeeklyStats(finalList, finalLearningStatusManager, decks);
                                        }
                                        continue;
                                    }
                                    api.getDeckProgress(deck.getId(), userId).enqueue(new Callback<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>>() {
                                        @Override
                                        public void onResponse(Call<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> call, Response<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> res) {
                                            if (res.isSuccessful() && res.body() != null && res.body().isSuccess() && res.body().getData() != null) {
                                                com.nhom1.polydeck.data.model.DeckProgress dp = res.body().getData();
                                                int learned = Math.max(0, dp.getLearnedWords());
                                                int total = Math.max(0, dp.getTotalWords());
                                                // Giới hạn learned không vượt quá total (backend có thể đếm số lần học thay vì số từ duy nhất)
                                                learned = Math.min(learned, total);
                                                learnedFromDecks.addAndGet(learned);
                                            }
                                            if (remaining.decrementAndGet() == 0) {
                                                // Chỉ tính từ flashcard, không tính từ quiz
                                                int totalWordsLearned = learnedFromDecks.get();
                                                if (tvWords != null) tvWords.setText(String.valueOf(totalWordsLearned));
                                                calculateWeeklyStats(finalList, finalLearningStatusManager, decks);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> call, Throwable t) {
                                            if (remaining.decrementAndGet() == 0) {
                                                // Chỉ tính từ flashcard, không tính từ quiz
                                                int totalWordsLearned = learnedFromDecks.get();
                                                if (tvWords != null) tvWords.setText(String.valueOf(totalWordsLearned));
                                                calculateWeeklyStats(finalList, finalLearningStatusManager, decks);
                                            }
                                        }
                                    });
                                }

                                // Calculate weekly stats với tất cả deck
                            } else {
                                // Nếu không lấy được deck, set 0
                                if (tvWords != null) tvWords.setText("0");
                                calculateWeeklyStats(finalList, finalLearningStatusManager, new ArrayList<>());
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<List<BoTu>> call, Throwable t) {
                            // Nếu lỗi, set 0
                            if (tvWords != null) tvWords.setText("0");
                            calculateWeeklyStats(finalList, finalLearningStatusManager, new ArrayList<>());
                        }
                    });

                    Set<String> ids = new HashSet<>();
                    for (LichSuLamBai h : list) {
                        if (h.getMaChuDe() != null) ids.add(h.getMaChuDe());
                    }
                    Map<String, String> nameMap = new HashMap<>();
                    for (String id : ids) {
                        api.getChuDeDetail(id).enqueue(new Callback<BoTu>() {
                            @Override public void onResponse(Call<BoTu> call, Response<BoTu> res) {
                                if (!isAdded()) return;
                                if (res.isSuccessful() && res.body() != null) {
                                    nameMap.put(id, res.body().getTenChuDe());
                                    adapter.updateTopicNames(nameMap);
                                }
                            }
                            @Override public void onFailure(Call<BoTu> call, Throwable t) { }
                        });
                    }
                } else {
                    // Response không thành công hoặc không có dữ liệu
                    Log.w(TAG, "getQuizHistory onResponse: Response not successful or no data - Code: " + response.code());
                    
                    // Retry nếu chưa quá 2 lần và là lỗi server (có thể backend chưa kịp lưu)
                    if (retryCount < 2 && response.code() >= 500) {
                        Log.w(TAG, "Retrying getQuizHistory after 2 seconds... (retry: " + (retryCount + 1) + ")");
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isAdded() && getView() != null) {
                                loadQuizHistory(userId, retryCount + 1);
                            }
                        }, 2000);
                        return;
                    }
                    
                    // Dữ liệu streak và XP đã được hiển thị từ session ở đầu hàm
                    // Set default values cho các stats khác (từ đã học, độ chính xác)
                    if (tvWords != null) tvWords.setText("0");
                    if (tvAccuracy != null) tvAccuracy.setText("0%");
                    adapter.setItems(new ArrayList<>());
                    calculateWeeklyStats(new ArrayList<>(), learningStatusManager, new ArrayList<>());
                    // Chỉ hiển thị toast nếu là lỗi server (5xx), không hiển thị nếu là 404 (chưa có dữ liệu)
                    if (response.code() >= 500) {
                        Toast.makeText(requireContext(), "Không tải được lịch sử: " + response.code(), Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 404) {
                        Log.d(TAG, "getQuizHistory onResponse: 404 - No quiz history found (user hasn't done any quizzes yet)");
                    }
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<LichSuLamBai>>> call, Throwable t) {
                if (!isAdded()) {
                    Log.w(TAG, "getQuizHistory onFailure: Fragment not added, returning");
                    return;
                }
                Log.e(TAG, "getQuizHistory onFailure: " + t.getMessage(), t);
                
                // Retry nếu chưa quá 2 lần
                if (retryCount < 2) {
                    Log.w(TAG, "Retrying getQuizHistory after 2 seconds... (retry: " + (retryCount + 1) + ")");
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded() && getView() != null) {
                            loadQuizHistory(userId, retryCount + 1);
                        }
                    }, 2000);
                    return;
                }
                
                // Dữ liệu streak và XP đã được hiển thị từ session ở đầu hàm
                // Set default values cho các stats khác
                if (tvWords != null) tvWords.setText("0");
                if (tvAccuracy != null) tvAccuracy.setText("0%");
                adapter.setItems(new ArrayList<>());
                calculateWeeklyStats(new ArrayList<>(), learningStatusManager, new ArrayList<>());
                // Chỉ hiển thị toast nếu là lỗi kết nối nghiêm trọng (không phải lỗi kết nối thông thường)
                if (t.getMessage() != null && !t.getMessage().contains("Failed to connect") && !t.getMessage().contains("Unable to resolve host")) {
                    Toast.makeText(requireContext(), "Lỗi lịch sử: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void calculateWeeklyStats(List<LichSuLamBai> allHistory, LearningStatusManager learningStatusManager, List<BoTu> allDecks) {
        // Get date 7 days ago (start of week)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date weekAgo = calendar.getTime();
        
        // Get today's date (end of day)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        today.set(Calendar.MILLISECOND, 999);
        Date todayEnd = today.getTime();
        
        int weeklyQuizzes = 0;
        int weeklyXp = 0;
        Set<String> weeklyDays = new HashSet<>();
        Set<String> weeklyDeckIds = new HashSet<>();
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
        
        for (LichSuLamBai h : allHistory) {
            if (h.getNgayLamBai() != null) {
                Date quizDate = h.getNgayLamBai();
                // Check if quiz is within the last 7 days (including today)
                if ((quizDate.after(weekAgo) || quizDate.equals(weekAgo)) && 
                    (quizDate.before(todayEnd) || quizDate.equals(todayEnd))) {
                    weeklyQuizzes++;
                    // XP = điểm số từ quiz
                    weeklyXp += Math.max(0, h.getDiemSo());
                    weeklyDays.add(df.format(quizDate));
                    if (h.getMaChuDe() != null) weeklyDeckIds.add(h.getMaChuDe());
                }
            }
        }
        
        // Tính từ đã học từ flashcard trong tuần này (KHÔNG tính từ quiz)
        // Lấy từ đã học từ server (DeckProgress) cho các deck có quiz trong tuần
        // Lưu ý: Vì không có timestamp cho từng từ, ta tính tổng số từ đã học từ các deck có quiz trong tuần
        calculateWeeklyFlashcardWords(weeklyDeckIds, 0);
        int weeklyDaysCount = weeklyDays.size();
        
        // Tạm thời set 0, sẽ được cập nhật khi load xong flashcard
        if (tvWeeklyWords != null) tvWeeklyWords.setText("0");
        if (tvWeeklyQuizzes != null) tvWeeklyQuizzes.setText(String.valueOf(weeklyQuizzes));
        if (tvWeeklyDays != null) tvWeeklyDays.setText(weeklyDaysCount + "/7");
        if (tvWeeklyXp != null) tvWeeklyXp.setText(String.valueOf(weeklyXp));
    }
    
    private void calculateWeeklyFlashcardWords(Set<String> weeklyDeckIds, int weeklyWordsFromQuiz) {
        if (weeklyDeckIds == null || weeklyDeckIds.isEmpty()) {
            // Không có deck nào có quiz trong tuần, set 0
            if (tvWeeklyWords != null) {
                tvWeeklyWords.setText("0");
            }
            return;
        }
        
        APIService api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(requireContext());
        LoginResponse user = sm.getUserData();
        String userId = user != null ? user.getId() : null;
        
        if (userId == null) {
            // Không có userId, set 0
            if (tvWeeklyWords != null) {
                tvWeeklyWords.setText("0");
            }
            return;
        }
        
        AtomicInteger remainingDecks = new AtomicInteger(weeklyDeckIds.size());
        AtomicInteger totalFlashcardWords = new AtomicInteger(0);
        
        for (String deckId : weeklyDeckIds) {
            api.getDeckProgress(deckId, userId).enqueue(new Callback<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>>() {
                @Override
                public void onResponse(Call<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> call, 
                                       Response<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                        com.nhom1.polydeck.data.model.DeckProgress dp = response.body().getData();
                        int learned = Math.max(0, dp.getLearnedWords());
                        int total = Math.max(0, dp.getTotalWords());
                        // Giới hạn learned không vượt quá total (backend có thể đếm số lần học thay vì số từ duy nhất)
                        learned = Math.min(learned, total);
                        totalFlashcardWords.addAndGet(learned);
                    }
                    if (remainingDecks.decrementAndGet() == 0) {
                        // Chỉ tính từ flashcard, không tính từ quiz
                        int totalWeeklyWords = totalFlashcardWords.get();
                        if (tvWeeklyWords != null) {
                            tvWeeklyWords.setText(String.valueOf(totalWeeklyWords));
                        }
                        Log.d(TAG, "Weekly words - Flashcard only: " + totalFlashcardWords.get());
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> call, Throwable t) {
                    if (!isAdded()) return;
                    if (remainingDecks.decrementAndGet() == 0) {
                        // Chỉ tính từ flashcard, không tính từ quiz
                        int totalWeeklyWords = totalFlashcardWords.get();
                        if (tvWeeklyWords != null) {
                            tvWeeklyWords.setText(String.valueOf(totalWeeklyWords));
                        }
                    }
                }
            });
        }
    }
}


