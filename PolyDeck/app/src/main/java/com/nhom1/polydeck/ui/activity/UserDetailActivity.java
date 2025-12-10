package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserDetailActivity extends AppCompatActivity {

    private static final String TAG = "UserDetailActivity";
    public static final String EXTRA_USER_ID = "EXTRA_USER_ID";

    private Toolbar toolbar;
    private CircleImageView ivDetailAvatar;
    private TextView tvDetailInitials;
    private TextView tvDetailUserName;
    private EditText etDetailFullName, etDetailEmail, etDetailLevel, etDetailXp, etDetailJoinDate;
    private Button btnSaveChanges;

    private APIService apiService;
    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();
        initViews();
        setupToolbar();
        fetchUserDetails();

        btnSaveChanges.setOnClickListener(v -> saveUserChanges());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_user_detail);
        ivDetailAvatar = findViewById(R.id.ivDetailAvatar);
        tvDetailInitials = findViewById(R.id.tvDetailInitials);
        tvDetailUserName = findViewById(R.id.tvDetailUserName);
        etDetailFullName = findViewById(R.id.etDetailFullName);
        etDetailEmail = findViewById(R.id.etDetailEmail);
        etDetailLevel = findViewById(R.id.etDetailLevel);
        etDetailXp = findViewById(R.id.etDetailXp);
        etDetailJoinDate = findViewById(R.id.etDetailJoinDate);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void fetchUserDetails() {
        apiService.getUserDetail(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    populateUserData(currentUser);
                } else {
                    Toast.makeText(UserDetailActivity.this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage());
                Toast.makeText(UserDetailActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUserData(User user) {
        // Hiển thị ảnh đại diện hoặc initials
        String avatarUrl = user.getLinkAnhDaiDien();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            // Có ảnh - hiển thị ảnh
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .error(R.drawable.circle_purple)
                    .into(ivDetailAvatar);
            ivDetailAvatar.setVisibility(android.view.View.VISIBLE);
            tvDetailInitials.setVisibility(android.view.View.GONE);
        } else {
            // Không có ảnh - hiển thị initials
            String initials = makeInitials(user.getHoTen());
            tvDetailInitials.setText(initials);
            ivDetailAvatar.setVisibility(android.view.View.GONE);
            tvDetailInitials.setVisibility(android.view.View.VISIBLE);
        }

        tvDetailUserName.setText(user.getHoTen());
        etDetailFullName.setText(user.getHoTen());
        etDetailEmail.setText(user.getEmail());
        etDetailLevel.setText(String.valueOf(user.getLevel()));
        etDetailXp.setText(String.valueOf(user.getXp()));
        etDetailJoinDate.setText(user.getNgayThamGia());
    }

    private String makeInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "?";
        }
        String[] words = fullName.trim().split("\\s+");
        if (words.length >= 2) {
            return (words[0].charAt(0) + "" + words[words.length - 1].charAt(0)).toUpperCase();
        }
        if (fullName.length() > 0) {
            return fullName.substring(0, 1).toUpperCase();
        }
        return "?";
    }

    private void saveUserChanges() {
        if (currentUser == null) return;

        // Validate input
        String newFullName = etDetailFullName.getText().toString().trim();
        String newEmail = etDetailEmail.getText().toString().trim();
        String levelStr = etDetailLevel.getText().toString().trim();
        String xpStr = etDetailXp.getText().toString().trim();

        // Validation
        if (newFullName.isEmpty()) {
            etDetailFullName.setError("Họ và tên không được để trống");
            etDetailFullName.requestFocus();
            return;
        }

        if (newEmail.isEmpty()) {
            etDetailEmail.setError("Email không được để trống");
            etDetailEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etDetailEmail.setError("Email không hợp lệ");
            etDetailEmail.requestFocus();
            return;
        }

        int newLevel;
        int newXp;
        try {
            newLevel = Integer.parseInt(levelStr);
            newXp = Integer.parseInt(xpStr);
            if (newLevel < 1) {
                etDetailLevel.setError("Level phải >= 1");
                etDetailLevel.requestFocus();
                return;
            }
            if (newXp < 0) {
                etDetailXp.setError("XP phải >= 0");
                etDetailXp.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Level và XP phải là số", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem có thay đổi gì không
        boolean hasChanges = false;
        boolean emailChanged = false;
        StringBuilder changesLog = new StringBuilder();

        if (!newFullName.equals(currentUser.getHoTen())) {
            hasChanges = true;
            changesLog.append("• Tên: ").append(currentUser.getHoTen()).append(" → ").append(newFullName).append("\n");
        }
        if (!newEmail.equals(currentUser.getEmail())) {
            hasChanges = true;
            emailChanged = true;
            changesLog.append("• Email: ").append(currentUser.getEmail()).append(" → ").append(newEmail).append("\n");
        }
        if (newLevel != currentUser.getLevel()) {
            hasChanges = true;
            changesLog.append("• Level: ").append(currentUser.getLevel()).append(" → ").append(newLevel).append("\n");
        }
        if (newXp != currentUser.getXp()) {
            hasChanges = true;
            changesLog.append("• XP: ").append(currentUser.getXp()).append(" → ").append(newXp).append("\n");
        }

        if (!hasChanges) {
            Toast.makeText(this, "Không có thay đổi nào", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cảnh báo khi sửa email
        if (emailChanged) {
            showEmailChangeWarning(newFullName, newEmail, newLevel, newXp, changesLog.toString());
        } else {
            // Không sửa email - xác nhận trực tiếp
            showConfirmDialog(newFullName, newEmail, newLevel, newXp, changesLog.toString());
        }
    }

    private void showEmailChangeWarning(String newFullName, String newEmail, int newLevel, int newXp, String changesLog) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Cảnh báo")
                .setMessage("Bạn đang thay đổi email của người dùng.\n\n" +
                           "Thay đổi email có thể ảnh hưởng đến:\n" +
                           "• Khả năng đăng nhập của người dùng\n" +
                           "• Xác thực email\n" +
                           "• Bảo mật tài khoản\n\n" +
                           "Bạn có chắc chắn muốn tiếp tục?")
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    showConfirmDialog(newFullName, newEmail, newLevel, newXp, changesLog);
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showConfirmDialog(String newFullName, String newEmail, int newLevel, int newXp, String changesLog) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thay đổi")
                .setMessage("Bạn có chắc chắn muốn lưu các thay đổi sau?\n\n" + changesLog + "\n" +
                           "Người dùng sẽ được thông báo về các thay đổi này.")
                .setPositiveButton("Lưu", (dialog, which) -> {
                    performSave(newFullName, newEmail, newLevel, newXp, changesLog);
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void performSave(String newFullName, String newEmail, int newLevel, int newXp, String changesLog) {
        // Log thay đổi
        Log.i(TAG, "Admin đang cập nhật thông tin user ID: " + userId);
        Log.i(TAG, "Thay đổi:\n" + changesLog);

        // Update user object
        currentUser.setHoTen(newFullName);
        currentUser.setEmail(newEmail);
        currentUser.setLevel(newLevel);
        currentUser.setXp(newXp);

        // Gọi API
        apiService.updateUser(userId, currentUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "✅ Đã cập nhật thông tin user thành công");
                    Toast.makeText(UserDetailActivity.this, 
                        "✅ Đã cập nhật thông tin người dùng thành công", 
                        Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the list
                } else {
                    Log.e(TAG, "❌ Cập nhật thất bại - Response code: " + response.code());
                    Toast.makeText(UserDetailActivity.this, 
                        "Cập nhật thất bại. Vui lòng thử lại.", 
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "❌ API Error: " + t.getMessage());
                Toast.makeText(UserDetailActivity.this, 
                    "Lỗi mạng: " + t.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
