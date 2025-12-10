package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "PolyDeckSettings";
    private static final String KEY_SOUND = "sound_enabled";
    private SharedPreferences prefs;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        sessionManager = new SessionManager(this);

        setupViews();
    }

    private void setupViews() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Switches
        SwitchCompat switchSound = findViewById(R.id.switchSound);

        // Load saved preferences
        switchSound.setChecked(prefs.getBoolean(KEY_SOUND, true));

        // Switch listeners
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply();
            Toast.makeText(this, isChecked ? "Âm thanh đã bật" : "Âm thanh đã tắt", 
                    Toast.LENGTH_SHORT).show();
        });

        // Language item
        View itemLanguage = findViewById(R.id.itemLanguage);
        itemLanguage.setOnClickListener(v -> {
            showLanguageDialog();
        });

        // Change password item
        View itemChangePassword = findViewById(R.id.itemChangePassword);
        itemChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Logout button
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void showLanguageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chọn ngôn ngữ")
                .setItems(new String[]{"Tiếng Việt", "English"}, (dialog, which) -> {
                    String language = which == 0 ? "Tiếng Việt" : "English";
                    Toast.makeText(this, "Đã chọn: " + language + "\nTính năng đang được phát triển", 
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    performLogout();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        // Clear session
        sessionManager.logout();
        
        // Navigate to login screen
        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
}