package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.GoogleLoginRequest;
import com.nhom1.polydeck.data.model.LoginRequest;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.utils.SessionManager;
import com.google.gson.Gson;

import androidx.annotation.NonNull;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private MaterialButton loginButton;
    private TextView registerNow, forgotPassword;
    private LinearLayout googleButton;
    private ProgressBar progressBar;
    private APIService apiService;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupAPI();
        setupGoogleSignIn();
        setupPasswordToggle();
        setupClickListeners();
    }

    private void initViews() {
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        loginButton = findViewById(R.id.loginButton);
        registerNow = findViewById(R.id.registerNow);
        forgotPassword = findViewById(R.id.forgotPasswordView);
        googleButton = findViewById(R.id.googleButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupAPI() {
        apiService = RetrofitClient.getApiService();
    }

    private void setupGoogleSignIn() {
        // TODO: Thay YOUR_WEB_CLIENT_ID bằng Google Client ID từ Google Cloud Console
        // Lấy từ: https://console.cloud.google.com/apis/credentials
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("362338924386-2ivjv5vd5r57g9er53cco26bs5r906it.apps.googleusercontent.com") // Web Client ID, không phải Android Client ID
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupPasswordToggle() {
        inputPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_END = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (inputPassword.getRight() -
                        inputPassword.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {

                    togglePasswordVisibility();
                    return true;
                }
            }
            return false;
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ẩn mật khẩu
            inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            inputPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0);
            isPasswordVisible = false;
        } else {
            // Hiện mật khẩu
            inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            inputPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_on, 0);
            isPasswordVisible = true;
        }
        // Giữ con trỏ ở cuối text
        inputPassword.setSelection(inputPassword.getText().length());
    }

    private void setupClickListeners() {
        // Đăng nhập
        loginButton.setOnClickListener(v -> handleLogin());

        // Chuyển đến màn hình đăng ký
        registerNow.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Quên mật khẩu

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });


        // Đăng nhập bằng Google
        googleButton.setOnClickListener(v -> handleGoogleSignIn());
    }

    private void handleLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            inputEmail.setError("Vui lòng nhập email");
            inputEmail.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            inputEmail.setError("Email không hợp lệ");
            inputEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            inputPassword.setError("Vui lòng nhập mật khẩu");
            inputPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            inputPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            inputPassword.requestFocus();
            return;
        }

        // TODO: Implement actual login logic with API
        performLogin(email, password);
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void performLogin(String email, String password) {
        showLoading(true);

        LoginRequest request = new LoginRequest(email, password);

        Call<ApiResponse<LoginResponse>> call = apiService.login(request);
        call.enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Response<ApiResponse<LoginResponse>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        LoginResponse loginData = apiResponse.getData();
                        if (loginData != null) {
                            // Lưu thông tin đăng nhập
                            SessionManager sessionManager = new SessionManager(LoginActivity.this);
                            sessionManager.saveUserSession(loginData);
                            
                            Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            
                            // Chuyển màn hình dựa vào vai trò
                            Intent intent;
                            String vaiTro = loginData.getVaiTro();
                            if (vaiTro != null && vaiTro.equals("admin")) {
                                // Admin → Admin Dashboard
                                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                            } else {
                                // Student/User → Main Activity
                                intent = new Intent(LoginActivity.this, MainActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Dữ liệu đăng nhập không hợp lệ", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String message = apiResponse.getMessage();
                        if (message == null || message.trim().isEmpty()) {
                            message = "Đăng nhập thất bại";
                        }
                        if (isCredentialErrorMessage(message)) {
                            showCredentialError(message);
                        } else {
                            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    handleLoginErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Throwable t) {
                showLoading(false);
                String errorMsg = "Lỗi kết nối";
                if (t.getMessage() != null) {
                    if (t.getMessage().contains("Failed to connect") || t.getMessage().contains("Unable to resolve host")) {
                        errorMsg = "Không thể kết nối đến server. Vui lòng kiểm tra:\n- Server đã chạy chưa?\n- IP/URL đúng chưa?";
                    } else {
                        errorMsg = "Lỗi: " + t.getMessage();
                    }
                }
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleLoginErrorResponse(Response<ApiResponse<LoginResponse>> response) {
        String errorMessage = "Đăng nhập thất bại";
        boolean credentialError = response.code() == 401;

        if (response.errorBody() != null) {
            try {
                String errorBody = response.errorBody().string();
                Gson gson = new Gson();
                ApiResponse<?> errorResponse = gson.fromJson(errorBody, ApiResponse.class);
                if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                    errorMessage = errorResponse.getMessage();
                }
                if (errorBody.contains("Email hoặc mật khẩu không đúng")) {
                    credentialError = true;
                }
            } catch (Exception e) {
                errorMessage = "Lỗi: " + response.code();
            }
        } else {
            errorMessage = "Lỗi: " + response.code();
        }

        if (isCredentialErrorMessage(errorMessage) || credentialError) {
            showCredentialError("Email hoặc mật khẩu không đúng");
        } else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCredentialErrorMessage(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("email hoặc mật khẩu") || lower.contains("không đúng");
    }

    private void showCredentialError(String message) {
        inputEmail.setError(message);
        inputPassword.setError(message);
        inputPassword.requestFocus();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        loginButton.setEnabled(!show);
    }

    private void handleGoogleSignIn() {
        if (googleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In chưa được cấu hình. Vui lòng thêm Web Client ID.", Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            if (account == null) {
                Toast.makeText(this, "Không thể lấy thông tin tài khoản Google", Toast.LENGTH_LONG).show();
                return;
            }

            String idToken = account.getIdToken();
            if (idToken == null || idToken.isEmpty()) {
                Toast.makeText(this, "Không thể lấy ID token từ Google", Toast.LENGTH_LONG).show();
                return;
            }

            performGoogleLogin(idToken);
        } catch (ApiException e) {
            String errorMessage = "Đăng nhập Google thất bại";
            
            switch (e.getStatusCode()) {
                case 10: // DEVELOPER_ERROR
                    errorMessage = "Lỗi cấu hình: Vui lòng kiểm tra SHA-1 fingerprint và Web Client ID";
                    break;
                case 12500: // SIGN_IN_CANCELLED
                    errorMessage = "Đăng nhập bị hủy";
                    break;
                case 7: // NETWORK_ERROR
                    errorMessage = "Lỗi kết nối mạng. Vui lòng thử lại";
                    break;
                case 8: // INTERNAL_ERROR
                    errorMessage = "Lỗi nội bộ. Vui lòng thử lại";
                    break;
                default:
                    errorMessage = "Lỗi: " + e.getStatusCode() + " - " + e.getMessage();
                    break;
            }
            
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void performGoogleLogin(String idToken) {
        showLoading(true);

        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        Call<ApiResponse<LoginResponse>> call = apiService.googleLogin(request);
        call.enqueue(new Callback<ApiResponse<LoginResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Response<ApiResponse<LoginResponse>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess()) {
                        LoginResponse loginData = apiResponse.getData();
                        if (loginData != null) {
                            // Lưu thông tin đăng nhập
                            SessionManager sessionManager = new SessionManager(LoginActivity.this);
                            sessionManager.saveUserSession(loginData);
                            
                            Toast.makeText(LoginActivity.this, apiResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            
                            // Chuyển màn hình dựa vào vai trò
                            Intent intent;
                            String vaiTro = loginData.getVaiTro();
                            if (vaiTro != null && vaiTro.equals("admin")) {
                                intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                            } else {
                                intent = new Intent(LoginActivity.this, MainActivity.class);
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Dữ liệu đăng nhập không hợp lệ", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String message = apiResponse.getMessage();
                        if (message == null || message.isEmpty()) {
                            message = "Đăng nhập thất bại";
                        }
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Xử lý lỗi từ server
                    String errorMessage = "Đăng nhập Google thất bại";
                    
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            
                            // Thử parse JSON để lấy message
                            try {
                                Gson gson = new Gson();
                                ApiResponse<?> errorResponse = gson.fromJson(errorBody, ApiResponse.class);
                                if (errorResponse != null) {
                                    String message = errorResponse.getMessage();
                                    if (message != null && !message.isEmpty()) {
                                        errorMessage = message;
                                        
                                        // Kiểm tra nếu có error detail
                                        if (errorBody.contains("\"error\"")) {
                                            try {
                                                // Parse để lấy error detail
                                                java.util.Map<String, Object> errorMap = gson.fromJson(errorBody, java.util.Map.class);
                                                if (errorMap != null && errorMap.containsKey("error")) {
                                                    String errorDetail = errorMap.get("error").toString();
                                                    // Nếu là lỗi validation về mat_khau_hash
                                                    if (errorDetail.contains("mat_khau_hash") && errorDetail.contains("required")) {
                                                        errorMessage = "Lỗi server: Schema database yêu cầu mật khẩu.\nVui lòng liên hệ admin để sửa.";
                                                    } else {
                                                        errorMessage += "\n" + errorDetail;
                                                    }
                                                }
                                            } catch (Exception e) {
                                                // Ignore parse error
                                            }
                                        }
                                    } else {
                                        errorMessage = "Lỗi server: " + response.code();
                                    }
                                } else {
                                    errorMessage = "Lỗi server: " + response.code();
                                }
                            } catch (Exception e) {
                                // Nếu không parse được JSON, hiển thị raw message
                                if (errorBody.length() > 150) {
                                    errorBody = errorBody.substring(0, 150) + "...";
                                }
                                errorMessage = "Lỗi server (Mã: " + response.code() + ")\n" + errorBody;
                            }
                        } catch (IOException e) {
                            errorMessage = "Lỗi server: " + response.code();
                        }
                    } else {
                        errorMessage = "Lỗi server: " + response.code();
                    }
                    
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LoginResponse>> call, @NonNull Throwable t) {
                showLoading(false);
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                if (t.getMessage() != null && t.getMessage().contains("Unable to resolve host")) {
                    errorMsg = "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.";
                }
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear sensitive data
        if (inputPassword != null) {
            inputPassword.setText("");
        }
    }
}