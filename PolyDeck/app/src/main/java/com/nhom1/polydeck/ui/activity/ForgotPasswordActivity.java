package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nhom1.polydeck.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText inputEmail;
    private MaterialButton sendLinkButton;
    private TextView backToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        inputEmail = findViewById(R.id.inputEmailForgot);
        sendLinkButton = findViewById(R.id.sendLinkButton);
        backToLogin = findViewById(R.id.backToLogin);
    }

    private void setupClickListeners() {
        // Gửi link reset password
        sendLinkButton.setOnClickListener(v -> handleSendResetLink());

        // Quay lại màn hình đăng nhập
        backToLogin.setOnClickListener(v -> finish());
    }

    private void handleSendResetLink() {
        String email = inputEmail.getText().toString().trim();

        // Validate email
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

        // TODO: Call API to send reset password link
        sendResetLink(email);
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendResetLink(String email) {
        // TODO: Implement API call to send reset password email
        Toast.makeText(this, "Liên kết đặt lại mật khẩu đã được gửi đến " + email, Toast.LENGTH_LONG).show();

        // Optional: Go back to login after successful send
        // finish();
    }
}