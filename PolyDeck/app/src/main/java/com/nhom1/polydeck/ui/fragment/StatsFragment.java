package com.nhom1.polydeck.ui.fragment;

import android.os.Bundle;
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
    private HistoryAdapter adapter;
    private TextView tvStreak, tvXp, tvWords, tvAccuracy;
    private TextView tvWeeklyWords, tvWeeklyQuizzes, tvWeeklyDays, tvWeeklyXp;
    private boolean isVisibleToUser = false;

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

        loadStats();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh stats khi quay lại fragment
        if (isVisibleToUser) {
            loadStats();
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
    
    private void loadStats() {
        APIService api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(requireContext());
        LearningStatusManager learningStatusManager = new LearningStatusManager(requireContext());
        
        // Lấy thông tin user để có userId
        LoginResponse user = sm.getUserData();
        String userId = user != null ? user.getId() : null;
        if (userId == null) {
            // Set default values nếu không có user
            if (tvStreak != null) tvStreak.setText("0 ngày");
            if (tvXp != null) tvXp.setText("0");
            if (tvWords != null) tvWords.setText("0");
            if (tvAccuracy != null) tvAccuracy.setText("0%");
            if (tvWeeklyWords != null) tvWeeklyWords.setText("0");
            if (tvWeeklyQuizzes != null) tvWeeklyQuizzes.setText("0");
            if (tvWeeklyDays != null) tvWeeklyDays.setText("0/7");
            if (tvWeeklyXp != null) tvWeeklyXp.setText("0");
            return;
        }

        // Refresh user data từ server để có thông tin mới nhất về streak và XP
        api.getUserDetail(userId).enqueue(new Callback<com.nhom1.polydeck.data.model.User>() {
            @Override
            public void onResponse(Call<com.nhom1.polydeck.data.model.User> call, Response<com.nhom1.polydeck.data.model.User> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    com.nhom1.polydeck.data.model.User userData = response.body();
                    // Cập nhật Streak và XP từ server
                    int userStreak = userData.getChuoiNgayHoc();
                    int userXp = userData.getXp();
                    
                    if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
                    if (tvXp != null) tvXp.setText(String.valueOf(userXp));
                } else {
                    // Fallback: dùng data từ session nếu không lấy được từ server
                    int userStreak = user != null ? user.getChuoiNgayHoc() : 0;
                    int userXp = user != null ? user.getDiemTichLuy() : 0;
                    if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
                    if (tvXp != null) tvXp.setText(String.valueOf(userXp));
                }
            }

            @Override
            public void onFailure(Call<com.nhom1.polydeck.data.model.User> call, Throwable t) {
                // Fallback: dùng data từ session nếu lỗi
                int userStreak = user != null ? user.getChuoiNgayHoc() : 0;
                int userXp = user != null ? user.getDiemTichLuy() : 0;
                if (tvStreak != null) tvStreak.setText(userStreak + " ngày");
                if (tvXp != null) tvXp.setText(String.valueOf(userXp));
            }
        });

        api.getQuizHistory(userId).enqueue(new Callback<ApiResponse<List<LichSuLamBai>>>() {
            @Override public void onResponse(Call<ApiResponse<List<LichSuLamBai>>> call, Response<ApiResponse<List<LichSuLamBai>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LichSuLamBai> list = response.body().getData();
                    adapter.setItems(list);

                    // Tính từ đã học từ quiz
                    int wordsFromQuiz = 0;
                    int totalCorrect = 0;
                    int totalQuestions = 0;
                    
                    for (LichSuLamBai h : list) {
                        // Tổng từ đã học từ quiz = tổng số câu đúng
                        wordsFromQuiz += Math.max(0, h.getSoCauDung());
                        // Tính độ chính xác: tổng câu đúng / tổng câu hỏi
                        totalCorrect += Math.max(0, h.getSoCauDung());
                        totalQuestions += Math.max(1, h.getTongSoCau()); // Tránh chia 0
                    }
                    
                    // Tính độ chính xác trung bình: (tổng câu đúng / tổng câu hỏi) * 100
                    int avgAcc = totalQuestions > 0 ? Math.round((totalCorrect * 100f) / totalQuestions) : 0;
                    
                    if (tvAccuracy != null) tvAccuracy.setText(avgAcc + "%");
                    
                    // Tạo biến final để sử dụng trong inner class
                    final int finalWordsFromQuiz = wordsFromQuiz;
                    final List<LichSuLamBai> finalList = list;
                    
                    // Lấy tất cả deck để tính từ đã học từ flashcard
                    api.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
                        @Override
                        public void onResponse(Call<List<BoTu>> call, Response<List<BoTu>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null) {
                                // Lấy số từ đã học từ server cho mỗi deck (DeckProgress.learnedWords)
                                List<BoTu> decks = response.body();
                                if (decks.isEmpty()) {
                                    if (tvWords != null) tvWords.setText(String.valueOf(finalWordsFromQuiz));
                                    calculateWeeklyStats(finalList, learningStatusManager, decks);
                                    return;
                                }

                                AtomicInteger remaining = new AtomicInteger(decks.size());
                                AtomicInteger learnedFromDecks = new AtomicInteger(0);

                                for (BoTu deck : decks) {
                                    if (deck.getId() == null) {
                                        if (remaining.decrementAndGet() == 0) {
                                            int totalWordsLearned = finalWordsFromQuiz + learnedFromDecks.get();
                                            if (tvWords != null) tvWords.setText(String.valueOf(totalWordsLearned));
                                            calculateWeeklyStats(finalList, learningStatusManager, decks);
                                        }
                                        continue;
                                    }
                                    api.getDeckProgress(deck.getId(), userId).enqueue(new Callback<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>>() {
                                        @Override
                                        public void onResponse(Call<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> call, Response<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> res) {
                                            if (res.isSuccessful() && res.body() != null && res.body().isSuccess() && res.body().getData() != null) {
                                                learnedFromDecks.addAndGet(Math.max(0, res.body().getData().getLearnedWords()));
                                            }
                                            if (remaining.decrementAndGet() == 0) {
                                                int totalWordsLearned = finalWordsFromQuiz + learnedFromDecks.get();
                                                if (tvWords != null) tvWords.setText(String.valueOf(totalWordsLearned));
                                                calculateWeeklyStats(finalList, learningStatusManager, decks);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ApiResponse<com.nhom1.polydeck.data.model.DeckProgress>> call, Throwable t) {
                                            if (remaining.decrementAndGet() == 0) {
                                                int totalWordsLearned = finalWordsFromQuiz + learnedFromDecks.get();
                                                if (tvWords != null) tvWords.setText(String.valueOf(totalWordsLearned));
                                                calculateWeeklyStats(finalList, learningStatusManager, decks);
                                            }
                                        }
                                    });
                                }

                                // Calculate weekly stats với tất cả deck
                            } else {
                                // Nếu không lấy được deck, chỉ tính từ quiz
                                if (tvWords != null) tvWords.setText(String.valueOf(finalWordsFromQuiz));
                                calculateWeeklyStats(finalList, learningStatusManager, new ArrayList<>());
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<List<BoTu>> call, Throwable t) {
                            // Nếu lỗi, chỉ tính từ quiz
                            if (tvWords != null) tvWords.setText(String.valueOf(finalWordsFromQuiz));
                            calculateWeeklyStats(finalList, learningStatusManager, new ArrayList<>());
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
                    Toast.makeText(requireContext(), "Không tải được lịch sử: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<LichSuLamBai>>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Lỗi lịch sử: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
        
        int weeklyWordsFromQuiz = 0;
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
                    // Số từ đã học từ quiz = số câu đúng
                    weeklyWordsFromQuiz += Math.max(0, h.getSoCauDung());
                    // XP = điểm số từ quiz
                    weeklyXp += Math.max(0, h.getDiemSo());
                    weeklyDays.add(df.format(quizDate));
                    if (h.getMaChuDe() != null) weeklyDeckIds.add(h.getMaChuDe());
                }
            }
        }
        
        // Tính từ đã học từ flashcard trong tuần này (từ các deck có quiz trong tuần)
        int weeklyWordsFromFlashcard = 0;
        for (String deckId : weeklyDeckIds) {
            weeklyWordsFromFlashcard += learningStatusManager.getKnownCount(deckId);
        }
        
        int totalWeeklyWords = weeklyWordsFromQuiz + weeklyWordsFromFlashcard;
        int weeklyDaysCount = weeklyDays.size();
        
        if (tvWeeklyWords != null) tvWeeklyWords.setText(String.valueOf(totalWeeklyWords));
        if (tvWeeklyQuizzes != null) tvWeeklyQuizzes.setText(String.valueOf(weeklyQuizzes));
        if (tvWeeklyDays != null) tvWeeklyDays.setText(weeklyDaysCount + "/7");
        if (tvWeeklyXp != null) tvWeeklyXp.setText(String.valueOf(weeklyXp));
    }
}

