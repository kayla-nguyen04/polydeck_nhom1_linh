package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000;
    private APIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        apiService = RetrofitClient.getApiService();

        ImageView imgLogo = findViewById(R.id.imgLogo);

        imgLogo.setScaleX(0.5f);
        imgLogo.setScaleY(0.5f);

        imgLogo.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator())
                .start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(this);
            
            // Kiểm tra đã đăng nhập chưa
            if (sessionManager.isLoggedIn()) {
                // Kiểm tra user có còn tồn tại trong database không
                LoginResponse userData = sessionManager.getUserData();
                if (userData != null && userData.getId() != null) {
                    verifyUserExists(userData.getId(), sessionManager);
                } else {
                    // Không có thông tin user, chuyển đến đăng nhập
                    sessionManager.logout();
                    goToLogin();
                }
            } else {
                // Chưa đăng nhập - chuyển đến màn hình đăng nhập
                goToLogin();
            }
        }, SPLASH_DELAY);
    }

    private void verifyUserExists(String userId, SessionManager sessionManager) {
        apiService.getUserDetail(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User userFromDB = response.body();
                    String vaiTroInDB = userFromDB.getVaiTro();
                    String vaiTroInSession = sessionManager.getVaiTro();
                    
                    // Kiểm tra vai trò có thay đổi không
                    if (vaiTroInDB != null && vaiTroInSession != null && !vaiTroInDB.equals(vaiTroInSession)) {
                        // Vai trò đã thay đổi trong database - logout để user đăng nhập lại
                        Log.w("WelcomeActivity", "Vai trò đã thay đổi trong database (từ '" + vaiTroInSession + "' sang '" + vaiTroInDB + "'), tự động logout");
                        sessionManager.logout();
                        goToLogin();
                        return;
                    }
                    
                    // User còn tồn tại và vai trò không thay đổi - cho phép vào app
                    String vaiTro = sessionManager.getVaiTro();
                    Intent intent;
                    if (vaiTro != null && vaiTro.equals("admin")) {
                        intent = new Intent(WelcomeActivity.this, AdminDashboardActivity.class);
                    } else {
                        intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    // User không còn tồn tại trong database - logout và chuyển đến đăng nhập
                    Log.w("WelcomeActivity", "User không còn tồn tại trong database, tự động logout");
                    sessionManager.logout();
                    goToLogin();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Lỗi mạng - vẫn cho phép vào app (có thể là lỗi tạm thời)
                Log.w("WelcomeActivity", "Lỗi khi kiểm tra user: " + t.getMessage() + ", vẫn cho phép vào app");
                String vaiTro = sessionManager.getVaiTro();
                Intent intent;
                if (vaiTro != null && vaiTro.equals("admin")) {
                    intent = new Intent(WelcomeActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(WelcomeActivity.this, MainActivity.class);
                }
                startActivity(intent);
                finish();
            }
        });
    }

    private void goToLogin() {
        Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
