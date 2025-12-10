package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.YeuCauHoTro;
import com.nhom1.polydeck.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportActivity extends AppCompatActivity {

    private EditText etName, etEmail, etMessage;
    private Button btnSend;
    private APIService apiService;
    private SessionManager sessionManager;
    
    // Email hỗ trợ - có thể thay đổi theo email của bạn
    private static final String SUPPORT_EMAIL = "polydeck.support@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        apiService = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);
        
        // Luôn lấy thông tin từ user đang đăng nhập trong app
        if (sessionManager.getUserData() != null) {
            etName.setText(sessionManager.getUserData().getHoTen());
            etEmail.setText(sessionManager.getUserData().getEmail());
            // Disable email field để tránh chỉnh sửa, đảm bảo dùng đúng email đăng nhập
            etEmail.setEnabled(false);
            etEmail.setFocusable(false);
        } else {
            // Nếu chưa đăng nhập, cho phép nhập thủ công
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSend.setOnClickListener(v -> sendSupportRequest());
    }

    private void sendSupportRequest() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String msg = etMessage.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (msg.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lưu vào database trước
        saveToDatabase(name, email, msg);

        // Sau đó mở email client
        openEmailClient(name, email, msg);
    }

    private void saveToDatabase(String name, String email, String message) {
        YeuCauHoTro request = new YeuCauHoTro();
        request.setTenNguoiGui(name);
        request.setEmailNguoiGui(email);
        request.setNoiDung(message);
        
        // Lấy userId nếu đã đăng nhập
        if (sessionManager.getUserData() != null && sessionManager.getUserData().getId() != null) {
            request.setMaNguoiDung(sessionManager.getUserData().getId());
        }

        apiService.createSupportRequest(request).enqueue(new Callback<ApiResponse<YeuCauHoTro>>() {
            @Override
            public void onResponse(Call<ApiResponse<YeuCauHoTro>> call, Response<ApiResponse<YeuCauHoTro>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Đã lưu thành công vào database
                } else {
                    // Lỗi nhưng vẫn tiếp tục mở email client
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<YeuCauHoTro>> call, Throwable t) {
                // Lỗi network nhưng vẫn tiếp tục mở email client
            }
        });
    }

    private void openEmailClient(String name, String email, String msg) {
        // Tạo email với subject và body
        String subject = "Yêu cầu hỗ trợ từ " + name;
        String body = "Xin chào,\n\n" +
                "Tên: " + name + "\n" +
                "Email: " + email + "\n\n" +
                "Nội dung:\n" + msg + "\n\n" +
                "Trân trọng,\n" + name;

        // Dùng ACTION_SEND để đảm bảo nội dung được điền sẵn
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822"); // Chỉ hiển thị email clients
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        
        // Thử mở với ACTION_SEND trước, nếu không được thì dùng ACTION_SENDTO
        try {
            startActivity(Intent.createChooser(intent, "Gửi email bằng..."));
            // Clear form sau khi mở email client
            etMessage.setText("");
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback: thử dùng ACTION_SENDTO
            try {
                Intent fallbackIntent = new Intent(Intent.ACTION_SENDTO);
                fallbackIntent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
                fallbackIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                fallbackIntent.putExtra(Intent.EXTRA_TEXT, body);
                startActivity(fallbackIntent);
                etMessage.setText("");
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Không tìm thấy ứng dụng email. Vui lòng cài đặt ứng dụng email.", Toast.LENGTH_LONG).show();
            }
        }
    }
}



