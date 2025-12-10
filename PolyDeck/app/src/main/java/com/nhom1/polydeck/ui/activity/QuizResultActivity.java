package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "EXTRA_SCORE";
    public static final String EXTRA_CORRECT = "EXTRA_CORRECT";
    public static final String EXTRA_TOTAL = "EXTRA_TOTAL";

    private APIService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);

        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        int correct = getIntent().getIntExtra(EXTRA_CORRECT, 0);
        int total = getIntent().getIntExtra(EXTRA_TOTAL, 0);

        Log.d("QuizResultActivity", "Nhận dữ liệu: score=" + score + " điểm, correct=" + correct + ", total=" + total);

        // Đảm bảo dữ liệu hợp lệ
        if (total <= 0) {
            Log.w("QuizResultActivity", "Total questions = 0, không thể hiển thị kết quả");
            finish();
            return;
        }

        // Tính phần trăm để hiển thị
        int percent = total > 0 ? Math.round(correct * 100f / total) : 0;

        // Hiển thị điểm ở giữa progress circle (điểm tuyệt đối)
        TextView tvPercentage = findViewById(R.id.tvPercentage);
        if (tvPercentage != null) {
            tvPercentage.setText(score + "");
        }

        // Cập nhật progress bar (dùng phần trăm)
        ProgressBar circularProgress = findViewById(R.id.circularProgress);
        if (circularProgress != null) {
            circularProgress.setProgress(percent);
        }

        // Hiển thị số câu đúng/tổng số câu
        TextView tvScore = findViewById(R.id.tvScore);
        if (tvScore != null) {
            tvScore.setText(correct + "/" + total);
        }

        // Hiển thị độ chính xác (phần trăm)
        TextView tvCorrectCount = findViewById(R.id.tvCorrectCount);
        if (tvCorrectCount != null) {
            tvCorrectCount.setText(percent + "%");
        }

        // Tính và hiển thị điểm kinh nghiệm (XP)
        // XP = điểm phần trăm (theo backend: diem_tich_luy += finalScore)
        int xpEarned = score;
        TextView tvXpEarned = findViewById(R.id.tvXpEarned);
        if (tvXpEarned != null) {
            tvXpEarned.setText("+" + xpEarned + " XP");
        }

        // Thêm click listener cho nút "Tiếp tục"
        MaterialButton btnContinue = findViewById(R.id.btnContinue);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); // Đóng activity và quay lại màn hình trước
                }
            });
        }

        // Cập nhật streak khi làm quiz xong
        updateStreak();
        
        // Refresh user data từ server sau khi làm quiz xong
        // Thêm delay để đảm bảo backend đã xử lý xong việc cộng XP và lưu lịch sử
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            refreshUserData();
        }, 1000); // Delay 1 giây để backend xử lý xong
    }

    private void updateStreak() {
        com.nhom1.polydeck.data.model.LoginResponse user = sessionManager.getUserData();
        
        if (user == null || user.getId() == null) {
            Log.w("QuizResultActivity", "Không có user data, không thể cập nhật streak");
            return;
        }

        Log.d("QuizResultActivity", "🔄 Calling updateStreak API for userId: " + user.getId());
        apiService.updateStreak(user.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, 
                                   Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d("QuizResultActivity", "✅ Cập nhật streak thành công - Response: " + response.body().getMessage());
                } else {
                    String errorMsg = "Unknown";
                    if (response.body() != null) {
                        errorMsg = response.body().getMessage();
                    } else if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            errorMsg = "Error body read failed";
                        }
                    }
                    Log.w("QuizResultActivity", "❌ Cập nhật streak thất bại - Code: " + response.code() + ", Message: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e("QuizResultActivity", "❌ Lỗi khi cập nhật streak: ", t);
            }
        });
    }

    private void refreshUserData() {
        refreshUserData(0); // Retry count = 0
    }
    
    private void refreshUserData(int retryCount) {
        com.nhom1.polydeck.data.model.LoginResponse user = sessionManager.getUserData();
        if (user != null && user.getId() != null) {
            Log.d("QuizResultActivity", "Refreshing user data after quiz completion... (retry: " + retryCount + ")");
            apiService.getUserDetail(user.getId()).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User updatedUser = response.body();
                        // Cập nhật session với dữ liệu mới nhất từ server
                        sessionManager.refreshUserData(updatedUser);
                        Log.d("QuizResultActivity", "✅ User data refreshed - Streak: " + updatedUser.getChuoiNgayHoc() + ", XP: " + updatedUser.getXp());
                        
                        // Nếu XP vẫn chưa được cập nhật và chưa retry quá 2 lần, thử lại sau 2 giây
                        int currentXp = updatedUser.getXp();
                        int oldXp = user.getDiemTichLuy();
                        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
                        
                        if (retryCount < 2 && currentXp == oldXp && score > 0) {
                            Log.w("QuizResultActivity", "⚠️ XP chưa được cập nhật, retry sau 2 giây... (old: " + oldXp + ", new: " + currentXp + ", expected: " + (oldXp + score) + ")");
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                refreshUserData(retryCount + 1);
                            }, 2000);
                        }
                    } else {
                        Log.w("QuizResultActivity", "Failed to refresh user data: " + response.code());
                        // Retry nếu chưa quá 2 lần
                        if (retryCount < 2) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                refreshUserData(retryCount + 1);
                            }, 2000);
                        }
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Log.e("QuizResultActivity", "Error refreshing user data: ", t);
                    // Retry nếu chưa quá 2 lần
                    if (retryCount < 2) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            refreshUserData(retryCount + 1);
                        }, 2000);
                    }
                }
            });
        } else {
            Log.w("QuizResultActivity", "Cannot refresh user data: user is null");
        }
    }
}

