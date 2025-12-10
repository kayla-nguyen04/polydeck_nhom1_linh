package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.ReadRequest;
import com.nhom1.polydeck.data.model.ThongBao;
import com.nhom1.polydeck.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOTIFICATION = "EXTRA_NOTIFICATION";

    private APIService api;
    private String userId;
    private ThongBao notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) {
            userId = sm.getUserData().getId();
        }

        // Lấy thông báo từ intent
        notification = (ThongBao) getIntent().getSerializableExtra(EXTRA_NOTIFICATION);
        if (notification == null) {
            finish();
            return;
        }

        displayNotification();
        markAsRead();
    }

    private void displayNotification() {
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvDate = findViewById(R.id.tv_date);
        TextView tvContent = findViewById(R.id.tv_content);

        if (tvTitle != null) {
            tvTitle.setText(notification.getTieuDe() != null ? notification.getTieuDe() : "");
        }

        if (tvDate != null) {
            String dateStr = formatDate(notification.getNgayGui());
            tvDate.setText(dateStr);
        }

        if (tvContent != null) {
            tvContent.setText(notification.getNoiDung() != null ? notification.getNoiDung() : "");
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }

        try {
            // Parse ISO 8601 format: 2025-12-08T10:30:00.000Z
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            
            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // Nếu parse lỗi, trả về nguyên chuỗi
            return dateStr;
        }

        return dateStr;
    }

    private void markAsRead() {
        if (notification.getId() == null || userId == null) {
            return;
        }

        api.markThongBaoRead(notification.getId(), new ReadRequest(userId)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                // Đã đánh dấu đã đọc
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Lỗi khi đánh dấu, không cần xử lý
            }
        });
    }
}

