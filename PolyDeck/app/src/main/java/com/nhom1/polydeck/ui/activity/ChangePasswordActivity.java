package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.ChangePasswordRequest;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText inputOldPassword, inputNewPassword, inputConfirmNewPassword;
    private MaterialButton btnSaveChanges;
    private ImageView btnBack;
    private APIService apiService;
    private SessionManager sessionManager;

    private boolean oldPasswordVisible = false;
    private boolean newPasswordVisible = false;
    private boolean confirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);

        initViews();
        setupPasswordVisibilityToggles();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        inputOldPassword = findViewById(R.id.inputOldPassword);
        inputNewPassword = findViewById(R.id.inputNewPassword);
        inputConfirmNewPassword = findViewById(R.id.inputConfirmNewPassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        
        // Đảm bảo drawable được set bounds
        inputOldPassword.post(() -> {
            android.graphics.drawable.Drawable drawable = inputOldPassword.getCompoundDrawables()[2];
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
        });
        inputNewPassword.post(() -> {
            android.graphics.drawable.Drawable drawable = inputNewPassword.getCompoundDrawables()[2];
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
        });
        inputConfirmNewPassword.post(() -> {
            android.graphics.drawable.Drawable drawable = inputConfirmNewPassword.getCompoundDrawables()[2];
            if (drawable != null) {
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
        });
    }

    private void setupPasswordVisibilityToggles() {
        final int DRAWABLE_END = 2;
        
        // Toggle visibility cho mật khẩu cũ
        inputOldPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                android.graphics.drawable.Drawable[] drawables = inputOldPassword.getCompoundDrawables();
                if (drawables[DRAWABLE_END] != null) {
                    int drawableWidth = drawables[DRAWABLE_END].getBounds().width();
                    int clickX = (int) event.getX();
                    int editTextWidth = inputOldPassword.getWidth();
                    
                    if (clickX >= (editTextWidth - drawableWidth - inputOldPassword.getPaddingEnd())) {
                        togglePasswordVisibility(inputOldPassword, 0);
                        return true;
                    }
                }
            }
            return false;
        });

        // Toggle visibility cho mật khẩu mới
        inputNewPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                android.graphics.drawable.Drawable[] drawables = inputNewPassword.getCompoundDrawables();
                if (drawables[DRAWABLE_END] != null) {
                    int drawableWidth = drawables[DRAWABLE_END].getBounds().width();
                    int clickX = (int) event.getX();
                    int editTextWidth = inputNewPassword.getWidth();
                    
                    if (clickX >= (editTextWidth - drawableWidth - inputNewPassword.getPaddingEnd())) {
                        togglePasswordVisibility(inputNewPassword, 1);
                        return true;
                    }
                }
            }
            return false;
        });

        // Toggle visibility cho xác nhận mật khẩu mới
        inputConfirmNewPassword.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                android.graphics.drawable.Drawable[] drawables = inputConfirmNewPassword.getCompoundDrawables();
                if (drawables[DRAWABLE_END] != null) {
                    int drawableWidth = drawables[DRAWABLE_END].getBounds().width();
                    int clickX = (int) event.getX();
                    int editTextWidth = inputConfirmNewPassword.getWidth();
                    
                    if (clickX >= (editTextWidth - drawableWidth - inputConfirmNewPassword.getPaddingEnd())) {
                        togglePasswordVisibility(inputConfirmNewPassword, 2);
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility(EditText editText, int fieldIndex) {
        boolean isVisible;
        switch (fieldIndex) {
            case 0:
                isVisible = oldPasswordVisible;
                oldPasswordVisible = !oldPasswordVisible;
                break;
            case 1:
                isVisible = newPasswordVisible;
                newPasswordVisible = !newPasswordVisible;
                break;
            case 2:
                isVisible = confirmPasswordVisible;
                confirmPasswordVisible = !confirmPasswordVisible;
                break;
            default:
                return;
        }

        if (isVisible) {
            // Ẩn mật khẩu
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
        } else {
            // Hiện mật khẩu
            editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_on, 0);
        }
        editText.setSelection(editText.getText().length());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        String oldPassword = inputOldPassword.getText().toString().trim();
        String newPassword = inputNewPassword.getText().toString().trim();
        String confirmPassword = inputConfirmNewPassword.getText().toString().trim();

        // Validate input
        if (oldPassword.isEmpty()) {
            inputOldPassword.setError("Vui lòng nhập mật khẩu cũ");
            inputOldPassword.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            inputNewPassword.setError("Vui lòng nhập mật khẩu mới");
            inputNewPassword.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            inputNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
            inputNewPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            inputConfirmNewPassword.setError("Vui lòng xác nhận mật khẩu mới");
            inputConfirmNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            inputConfirmNewPassword.setError("Mật khẩu xác nhận không khớp");
            inputConfirmNewPassword.requestFocus();
            return;
        }

        if (oldPassword.equals(newPassword)) {
            inputNewPassword.setError("Mật khẩu mới phải khác mật khẩu cũ");
            inputNewPassword.requestFocus();
            return;
        }

        // Lấy email từ session
        String email = sessionManager.getEmail();
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Gọi API
        performChangePassword(email, oldPassword, newPassword);
    }

    private void performChangePassword(String email, String oldPassword, String newPassword) {
        showLoading(true);

        ChangePasswordRequest request = new ChangePasswordRequest(email, oldPassword, newPassword);

        Call<ApiResponse<Void>> call = apiService.changePassword(request);
        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        Toast.makeText(ChangePasswordActivity.this, 
                                apiResponse.getMessage() != null ? apiResponse.getMessage() : "Đổi mật khẩu thành công", 
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String message = apiResponse.getMessage();
                        if (message == null || message.isEmpty()) {
                            message = "Đổi mật khẩu thất bại";
                        }
                        Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = "Đổi mật khẩu thất bại";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.d("ChangePassword", "Error body: " + errorBody);
                            
                            // Parse JSON error response
                            if (errorBody.contains("Mật khẩu cũ không đúng") || errorBody.contains("mat_khau_cu")) {
                                errorMessage = "Mật khẩu cũ không đúng";
                                inputOldPassword.setError("Mật khẩu cũ không đúng");
                                inputOldPassword.requestFocus();
                            } else if (errorBody.contains("Email không tồn tại") || errorBody.contains("email")) {
                                errorMessage = "Email không tồn tại";
                            } else if (errorBody.contains("Mật khẩu mới phải khác")) {
                                errorMessage = "Mật khẩu mới phải khác mật khẩu cũ";
                                inputNewPassword.setError("Mật khẩu mới phải khác mật khẩu cũ");
                                inputNewPassword.requestFocus();
                            } else {
                                // Try to extract message from JSON
                                try {
                                    com.google.gson.JsonObject jsonObject = new com.google.gson.Gson().fromJson(errorBody, com.google.gson.JsonObject.class);
                                    if (jsonObject.has("message")) {
                                        errorMessage = jsonObject.get("message").getAsString();
                                    }
                                } catch (Exception e) {
                                    // Keep default message
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ChangePassword", "Error parsing error body", e);
                        }
                    }
                    Toast.makeText(ChangePasswordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                showLoading(false);
                android.util.Log.e("ChangePassword", "API call failed", t);
                String errorMsg = "Lỗi kết nối. Vui lòng thử lại.";
                if (t.getMessage() != null) {
                    errorMsg += "\n" + t.getMessage();
                }
                Toast.makeText(ChangePasswordActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        btnSaveChanges.setEnabled(!show);
        btnSaveChanges.setText(show ? "Đang xử lý..." : "Lưu thay đổi");
    }
}