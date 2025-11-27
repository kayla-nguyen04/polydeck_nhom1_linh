package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail;
    private TextView tvInitials;
    private Button btnSave;

    private APIService api;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        LoginResponse user = sm.getUserData();
        if (user != null) {
            userId = user.getMaNguoiDung();
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        tvInitials = findViewById(R.id.tv_initials);
        etName = findViewById(R.id.et_fullname);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);

        if (user != null) {
            etName.setText(user.getHoTen());
            etEmail.setText(user.getEmail());
            tvInitials.setText(makeInitials(user.getHoTen()));
        }

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        if (userId == null) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        User u = new User();
        u.setId(userId);
        u.setHoTen(etName.getText().toString().trim());
        u.setEmail(etEmail.getText().toString().trim());
        api.updateUser(userId, u).enqueue(new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Không thể cập nhật", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String makeInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "NA";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0,1) + parts[parts.length-1].substring(0,1)).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }
}



