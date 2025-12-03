package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.AdminStats;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvTotalDecks, tvActiveUsers, tvTotalWords;
    private TextView tvUserGrowth, tvDeckGrowth, tvActiveGrowth, tvWordGrowth;

    private CardView cardUsers, cardDecks, cardQuiz, cardNotification, cardSupport;

    // Logout button
    private ImageView btnLogout;

    private APIService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupAPI();
        setupSessionManager();
        loadStats();
        setupClickListeners();
    }

    private void initViews() {
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalDecks = findViewById(R.id.tvTotalDecks);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvTotalWords = findViewById(R.id.tvTotalWords);

        tvUserGrowth = findViewById(R.id.tvUserGrowth);
        tvDeckGrowth = findViewById(R.id.tvDeckGrowth);
        tvActiveGrowth = findViewById(R.id.tvActiveGrowth);
        tvWordGrowth = findViewById(R.id.tvWordGrowth);

        cardUsers = findViewById(R.id.cardUsers);
        cardDecks = findViewById(R.id.cardDecks);
        cardQuiz = findViewById(R.id.cardQuiz);
        cardNotification = findViewById(R.id.cardNotification);
        cardSupport = findViewById(R.id.cardSupport);

        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupAPI() {
        apiService = RetrofitClient.getApiService();
    }

    private void setupSessionManager() {
        sessionManager = new SessionManager(this);
    }

    private void loadStats() {
        apiService.getAdminStats().enqueue(new Callback<AdminStats>() {
            @Override
            public void onResponse(Call<AdminStats> call, Response<AdminStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AdminStats stats = response.body();
                    updateStatsUI(stats);
                } else {
                    Toast.makeText(AdminDashboardActivity.this,
                            "Không thể tải thống kê", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AdminStats> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this,
                        "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatsUI(AdminStats stats) {
        tvTotalUsers.setText(String.format("%,d", stats.getTongNguoiDung()));
        tvTotalDecks.setText(String.format("%,d", stats.getTongBoTu()));
        tvActiveUsers.setText(String.format("%,d", stats.getNguoiHoatDong()));
        tvTotalWords.setText(String.format("%,d", stats.getTongTuVung()));

        tvUserGrowth.setText(stats.getTyLeNguoiDung());
        tvDeckGrowth.setText(stats.getTyLeBoTu());
        tvActiveGrowth.setText(stats.getTyLeHoatDong());
        tvWordGrowth.setText(stats.getTyLeTuVung());
    }

    private void setupClickListeners() {
        cardUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
            startActivity(intent);
        });

        cardDecks.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, DeckManagementActivity.class);
            startActivity(intent);
        });

        cardQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, QuizManagementActivity.class);
            startActivity(intent);
        });

        cardNotification.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, SendNotificationActivity.class);
            startActivity(intent);
        });

        // Support requests removed - users now contact via Gmail
        cardSupport.setVisibility(View.GONE);

        btnLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performLogout() {
        // Xóa session
        sessionManager.logout();
        
        // Chuyển về màn hình đăng nhập
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}