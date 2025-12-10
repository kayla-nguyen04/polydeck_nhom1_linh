package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ThongBao;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendNotificationActivity extends AppCompatActivity {

    private static final String TAG = "SendNotification";

    private Toolbar toolbar;
    private TextInputEditText etNotificationTitle, etNotificationContent;
    private Button btnSendNotification;
    private APIService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_notification);

        apiService = RetrofitClient.getApiService();

        // Initialize Views
        etNotificationTitle = findViewById(R.id.etNotificationTitle);
        etNotificationContent = findViewById(R.id.etNotificationContent);
        btnSendNotification = findViewById(R.id.btnSendNotification);
        
        // Setup Back Button
        android.widget.ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Xử lý window insets cho NestedScrollView
        android.view.View nestedScrollView = findViewById(R.id.nestedScrollView);
        if (nestedScrollView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(nestedScrollView, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 
                            Math.max(systemBars.bottom, 16)); // Tối thiểu 16dp
                return insets;
            });
        }

        // Set Listener
        btnSendNotification.setOnClickListener(v -> sendNotification());
    }

    private void sendNotification() {
        String title = etNotificationTitle.getText().toString().trim();
        String content = etNotificationContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Tiêu đề và nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendNotification.setEnabled(false);
        btnSendNotification.setText("Đang gửi...");

        ThongBao notification = new ThongBao(title, content);

        apiService.createSystemNotification(notification).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SendNotificationActivity.this, "Gửi thông báo thành công!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after sending
                } else {
                    Toast.makeText(SendNotificationActivity.this, "Gửi thất bại, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
                resetButton();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(SendNotificationActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnSendNotification.setEnabled(true);
        btnSendNotification.setText("Gửi");
    }
}
