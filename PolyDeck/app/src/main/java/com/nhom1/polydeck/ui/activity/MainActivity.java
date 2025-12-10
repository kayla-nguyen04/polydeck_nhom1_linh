package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.fragment.HomeFragment;
import com.nhom1.polydeck.ui.fragment.ProfileFragment;
import com.nhom1.polydeck.ui.fragment.StatsFragment;
import com.nhom1.polydeck.ui.fragment.TopicsFragment;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private APIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService();
        
        // Kiểm tra user có còn tồn tại trong database không
        verifyUserAndProceed();
    }

    private void verifyUserAndProceed() {
        LoginResponse userData = sessionManager.getUserData();
        if (userData == null || userData.getId() == null) {
            // Không có thông tin user, chuyển đến đăng nhập
            sessionManager.logout();
            goToLogin();
            return;
        }

        // Kiểm tra user có còn tồn tại trong database không
        apiService.getUserDetail(userData.getId()).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User userFromDB = response.body();
                    String vaiTroInDB = userFromDB.getVaiTro();
                    String vaiTroInSession = sessionManager.getVaiTro();
                    
                    // Kiểm tra vai trò có thay đổi không
                    if (vaiTroInDB != null && vaiTroInSession != null && !vaiTroInDB.equals(vaiTroInSession)) {
                        // Vai trò đã thay đổi trong database - logout để user đăng nhập lại
                        Log.w("MainActivity", "Vai trò đã thay đổi trong database (từ '" + vaiTroInSession + "' sang '" + vaiTroInDB + "'), tự động logout");
                        sessionManager.logout();
                        goToLogin();
                        return;
                    }
                    
                    // User còn tồn tại và vai trò không thay đổi - tiếp tục load UI
                    setupUI();
                } else {
                    // User không còn tồn tại - logout và chuyển đến đăng nhập
                    Log.w("MainActivity", "User không còn tồn tại trong database, tự động logout");
                    sessionManager.logout();
                    goToLogin();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Lỗi mạng - vẫn cho phép vào app (có thể là lỗi tạm thời)
                Log.w("MainActivity", "Lỗi khi kiểm tra user: " + t.getMessage() + ", vẫn cho phép vào app");
                setupUI();
            }
        });
    }

    private void setupUI() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                return insets;
            });
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                lp.bottomMargin = sb.bottom; // lift the bar above system nav
                v.setLayoutParams(lp);
                return insets;
            });

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    switchFragment(new HomeFragment());
                    return true;
                } else if (id == R.id.nav_topics) {
                    switchFragment(new TopicsFragment());
                    return true;
                } else if (id == R.id.nav_stats) {
                    switchFragment(new StatsFragment());
                    return true;
                } else if (id == R.id.nav_profile) {
                    switchFragment(new ProfileFragment());
                    return true;
                }
                return false;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}