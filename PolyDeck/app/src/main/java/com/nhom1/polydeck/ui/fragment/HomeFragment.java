package com.nhom1.polydeck.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.adapter.UserDeckAdapter;
import com.nhom1.polydeck.ui.activity.NotificationsActivity;
import com.nhom1.polydeck.utils.SessionManager;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.DeckProgress;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.bumptech.glide.Glide;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private UserDeckAdapter adapter;
    private APIService apiService;
    private TextView tvUsername;
    private TextView tvGreeting;
    private TextView tvStreak;
    private TextView tvXp;
    private TextView tvContinueTitle;
    private TextView tvContinueSubtitle;
    private ProgressBar progressContinue;
    private ImageView ivContinueIcon;
    private ImageView ivAvatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = RetrofitClient.getApiService();

        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvUsername = view.findViewById(R.id.tv_username);
        tvStreak = view.findViewById(R.id.tv_streak_value);
        tvXp = view.findViewById(R.id.tv_xp_value);
        tvContinueTitle = view.findViewById(R.id.tv_continue_title);
        tvContinueSubtitle = view.findViewById(R.id.tv_continue_subtitle);
        progressContinue = view.findViewById(R.id.progress_continue);
        ivContinueIcon = view.findViewById(R.id.iv_continue_icon);
        ivAvatar = view.findViewById(R.id.iv_avatar);

        loadUserData();

        View btnNotif = view.findViewById(R.id.btn_notifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), NotificationsActivity.class)));
        }

        TextView tvViewAll = view.findViewById(R.id.tv_view_all);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_topics);
                    }
                }
            });
        }

        RecyclerView rvDecks = view.findViewById(R.id.rv_decks_home);
        rvDecks.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new UserDeckAdapter(requireContext());
        rvDecks.setAdapter(adapter);

        loadDecks();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning from EditProfileActivity, QuizResultActivity, etc.
        loadUserData();
        // Refresh deck list when returning to this fragment (e.g., after deleting a deck)
        loadDecks();
        // Force refresh continue learning card
        refreshContinueLearningCard();
    }
    
    private void refreshContinueLearningCard() {
        // Refresh continue learning card với deck đầu tiên
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    List<BoTu> items = response.body();
                    if (items != null && !items.isEmpty()) {
                        BoTu first = items.get(0);
                        if (tvContinueTitle != null) {
                            tvContinueTitle.setText(first.getTenChuDe());
                        }
                        
                        SessionManager sm = new SessionManager(requireContext());
                        LoginResponse user = sm.getUserData();
                        String userId = user != null ? user.getId() : null;
                        
                        if (userId != null) {
                            // Lấy số từ thực tế từ danh sách từ vựng trước
                            apiService.getTuVungByBoTu(first.getId()).enqueue(new Callback<List<TuVung>>() {
                                @Override
                                public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                                    if (!isAdded()) return;
                                    final int actualTotalWords;
                                    if (response.isSuccessful() && response.body() != null) {
                                        actualTotalWords = response.body().size();
                                    } else {
                                        actualTotalWords = 0;
                                    }
                                    
                                    // Sau đó lấy tiến độ học tập từ API
                                    apiService.getDeckProgress(first.getId(), userId)
                                            .enqueue(new Callback<ApiResponse<DeckProgress>>() {
                                                @Override
                                                public void onResponse(@NonNull Call<ApiResponse<DeckProgress>> call,
                                                                       @NonNull Response<ApiResponse<DeckProgress>> response) {
                                                    if (!isAdded()) return;
                                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                                        DeckProgress dp = response.body().getData();
                                                        if (dp != null) {
                                                            // Dùng total từ API hoặc từ vocab list (lấy giá trị lớn hơn)
                                                            int total = Math.max(dp.getTotalWords(), actualTotalWords);
                                                            int learned = Math.max(dp.getLearnedWords(), 0);
                                                            // Giới hạn learned không vượt quá total (backend có thể đếm số lần học thay vì số từ duy nhất)
                                                            learned = Math.min(learned, total);
                                                            android.util.Log.d("HomeFragment", "Refresh continue learning - Learned: " + learned + "/" + total);
                                                            if (tvContinueSubtitle != null) {
                                                                tvContinueSubtitle.setText("Đã học " + learned + "/" + total + " từ");
                                                            }
                                                            if (progressContinue != null) {
                                                                int percent = total > 0 ? Math.min(100, (int) (learned * 100f / total)) : 0;
                                                                progressContinue.setProgress(percent);
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                @Override
                                                public void onFailure(@NonNull Call<ApiResponse<DeckProgress>> call, @NonNull Throwable t) {
                                                    android.util.Log.e("HomeFragment", "Error refreshing continue learning: ", t);
                                                }
                                            });
                                }
                                
                                @Override
                                public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                                    // Nếu không lấy được danh sách từ vựng, vẫn thử lấy progress từ API
                                    apiService.getDeckProgress(first.getId(), userId)
                                            .enqueue(new Callback<ApiResponse<DeckProgress>>() {
                                                @Override
                                                public void onResponse(@NonNull Call<ApiResponse<DeckProgress>> call,
                                                                       @NonNull Response<ApiResponse<DeckProgress>> response) {
                                                    if (!isAdded()) return;
                                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                                        DeckProgress dp = response.body().getData();
                                                        if (dp != null) {
                                                            int total = Math.max(dp.getTotalWords(), 0);
                                                            int learned = Math.max(dp.getLearnedWords(), 0);
                                                            learned = Math.min(learned, total);
                                                            if (tvContinueSubtitle != null) {
                                                                tvContinueSubtitle.setText("Đã học " + learned + "/" + total + " từ");
                                                            }
                                                            if (progressContinue != null) {
                                                                int percent = total > 0 ? Math.min(100, (int) (learned * 100f / total)) : 0;
                                                                progressContinue.setProgress(percent);
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                                @Override
                                                public void onFailure(@NonNull Call<ApiResponse<DeckProgress>> call, @NonNull Throwable t) {
                                                    android.util.Log.e("HomeFragment", "Error refreshing continue learning: ", t);
                                                }
                                            });
                                }
                            });
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                android.util.Log.e("HomeFragment", "Error getting decks for continue learning: ", t);
            }
        });
    }

    private void loadUserData() {
        // Bind user info from Session trước (hiển thị ngay)
        SessionManager sessionManager = new SessionManager(requireContext());
        LoginResponse user = sessionManager.getUserData();
        tvGreeting.setText("Xin chào,");
        if (user != null) {
            tvUsername.setText(user.getHoTen());
            int currentStreak = user.getChuoiNgayHoc();
            int currentXp = user.getDiemTichLuy();
            android.util.Log.d("HomeFragment", "Load user data from session - Streak: " + currentStreak + ", XP: " + currentXp);
            if (tvStreak != null) tvStreak.setText(currentStreak + " ngày");
            if (tvXp != null) tvXp.setText(String.valueOf(currentXp));
            
            // Load avatar
            if (ivAvatar != null) {
                String avatarUrl = user.getLinkAnhDaiDien();
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .error(R.drawable.ic_avatar_placeholder)
                            .circleCrop()
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            }
            
            // Refresh từ server để có dữ liệu mới nhất
            if (user.getId() != null) {
                android.util.Log.d("HomeFragment", "Calling getUserDetail API for userId: " + user.getId());
                apiService.getUserDetail(user.getId()).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null) {
                            User updatedUser = response.body();
                            int newStreak = updatedUser.getChuoiNgayHoc();
                            int newXp = updatedUser.getXp();
                            android.util.Log.d("HomeFragment", "✅ Got updated data from server - Streak: " + newStreak + ", XP: " + newXp);
                            // Cập nhật session
                            sessionManager.refreshUserData(updatedUser);
                            // Cập nhật UI
                            if (tvStreak != null) {
                                tvStreak.setText(newStreak + " ngày");
                                android.util.Log.d("HomeFragment", "Updated streak UI to: " + newStreak);
                            }
                            if (tvXp != null) {
                                tvXp.setText(String.valueOf(newXp));
                                android.util.Log.d("HomeFragment", "Updated XP UI to: " + newXp);
                            }
                            
                            // Update avatar from server
                            if (ivAvatar != null) {
                                String avatarUrl = updatedUser.getLinkAnhDaiDien();
                                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                    Glide.with(HomeFragment.this)
                                            .load(avatarUrl)
                                            .placeholder(R.drawable.ic_avatar_placeholder)
                                            .error(R.drawable.ic_avatar_placeholder)
                                            .circleCrop()
                                            .into(ivAvatar);
                                } else {
                                    ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                                }
                            }
                        } else {
                            android.util.Log.w("HomeFragment", "Failed to get user detail: " + (response != null ? response.code() : "null response"));
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        android.util.Log.e("HomeFragment", "Error getting user detail: ", t);
                        // Giữ dữ liệu từ session nếu lỗi
                    }
                });
            }
        } else {
            tvUsername.setText("Người học");
            tvStreak.setText("0 ngày");
            tvXp.setText("0");
        }
    }

    private void loadDecks() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BoTu> items = response.body();
                    adapter.setItems(items);

                    // Bind continue learning card with first topic as placeholder
                    if (items != null && !items.isEmpty()) {
                        BoTu first = items.get(0);
                        if (tvContinueTitle != null) {
                            tvContinueTitle.setText(first.getTenChuDe());
                        }

                        // Gọi API lấy tiến độ học cho chủ đề đầu tiên
                        SessionManager sm = new SessionManager(requireContext());
                        LoginResponse user = sm.getUserData();
                        String userId = user != null ? user.getId() : null;

                        if (userId == null) {
                            // Chưa đăng nhập: chỉ hiển thị tổng số từ
                            if (tvContinueSubtitle != null) {
                                int total = Math.max(first.getSoLuongQuiz(), 0);
                                tvContinueSubtitle.setText("Đã học 0/" + total + " từ");
                            }
                            if (progressContinue != null) {
                                progressContinue.setProgress(0);
                            }
                        } else {
                            // Lấy số từ thực tế từ danh sách từ vựng trước
                            apiService.getTuVungByBoTu(first.getId()).enqueue(new Callback<List<TuVung>>() {
                                @Override
                                public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                                    if (!isAdded()) return;
                                    final int actualTotalWords;
                                    if (response.isSuccessful() && response.body() != null) {
                                        actualTotalWords = response.body().size();
                                    } else {
                                        actualTotalWords = 0;
                                    }
                                    
                                    // Sau đó lấy tiến độ học tập từ API
                                    apiService.getDeckProgress(first.getId(), userId)
                                            .enqueue(new Callback<ApiResponse<DeckProgress>>() {
                                                @Override
                                                public void onResponse(@NonNull Call<ApiResponse<DeckProgress>> call,
                                                                       @NonNull Response<ApiResponse<DeckProgress>> response) {
                                                    if (!isAdded()) return;
                                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                                        DeckProgress dp = response.body().getData();
                                                        if (dp != null) {
                                                            // Dùng total từ API hoặc từ vocab list (lấy giá trị lớn hơn)
                                                            int total = Math.max(dp.getTotalWords(), actualTotalWords);
                                                            int learned = Math.max(dp.getLearnedWords(), 0);
                                                            // Giới hạn learned không vượt quá total (backend có thể đếm số lần học thay vì số từ duy nhất)
                                                            learned = Math.min(learned, total);
                                                            if (tvContinueSubtitle != null) {
                                                                tvContinueSubtitle.setText("Đã học " + learned + "/" + total + " từ");
                                                            }
                                                            if (progressContinue != null) {
                                                                int percent = total > 0 ? Math.min(100, (int) (learned * 100f / total)) : 0;
                                                                progressContinue.setProgress(percent);
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure(@NonNull Call<ApiResponse<DeckProgress>> call, @NonNull Throwable t) {
                                                    // Giữ placeholder nếu lỗi
                                                }
                                            });
                                }
                                
                                @Override
                                public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                                    // Nếu không lấy được danh sách từ vựng, vẫn thử lấy progress từ API
                                    apiService.getDeckProgress(first.getId(), userId)
                                            .enqueue(new Callback<ApiResponse<DeckProgress>>() {
                                                @Override
                                                public void onResponse(@NonNull Call<ApiResponse<DeckProgress>> call,
                                                                       @NonNull Response<ApiResponse<DeckProgress>> response) {
                                                    if (!isAdded()) return;
                                                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                                        DeckProgress dp = response.body().getData();
                                                        if (dp != null) {
                                                            int total = Math.max(dp.getTotalWords(), 0);
                                                            int learned = Math.max(dp.getLearnedWords(), 0);
                                                            learned = Math.min(learned, total);
                                                            if (tvContinueSubtitle != null) {
                                                                tvContinueSubtitle.setText("Đã học " + learned + "/" + total + " từ");
                                                            }
                                                            if (progressContinue != null) {
                                                                int percent = total > 0 ? Math.min(100, (int) (learned * 100f / total)) : 0;
                                                                progressContinue.setProgress(percent);
                                                            }
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure(@NonNull Call<ApiResponse<DeckProgress>> call, @NonNull Throwable t) {
                                                    // Giữ placeholder nếu lỗi
                                                }
                                            });
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
            }
        });
    }
}

