package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private MaterialButton loginButton;
    private TextView registerNow, forgotPassword;
    private LinearLayout googleButton;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
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
        googleButton.setOnClickListener(v -> {
            Toast.makeText(this, "Đăng nhập bằng Google", Toast.LENGTH_SHORT).show();
            // TODO: Implement Google Sign-In
            handleGoogleSignIn();
        });
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
        // TODO: Call API to login
        // For now, just show success message
        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

        // Navigate to main activity
        // Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        // startActivity(intent);
        // finish();
    }

    private void handleGoogleSignIn() {
        // TODO: Implement Google Sign-In
        // 1. Add Google Sign-In dependency
        // 2. Configure Google Sign-In in Firebase
        // 3. Implement sign-in flow
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