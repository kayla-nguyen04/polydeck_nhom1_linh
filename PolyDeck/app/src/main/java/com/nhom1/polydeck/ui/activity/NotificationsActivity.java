package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.ReadRequest;
import com.nhom1.polydeck.data.model.ThongBao;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private APIService api;
    private ArrayAdapter<String> adapter;
    private final List<ThongBao> data = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        if (sm.getUserData() != null) userId = sm.getUserData().getMaNguoiDung();

        ListView listView = findViewById(R.id.lv_notifications);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> markAsRead(data.get(position)));

        loadNotifications();
    }

    private void loadNotifications() {
        api.getThongBao(userId).enqueue(new Callback<ApiResponse<List<ThongBao>>>() {
            @Override public void onResponse(Call<ApiResponse<List<ThongBao>>> call, Response<ApiResponse<List<ThongBao>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    data.clear();
                    data.addAll(response.body().getData());
                    titles.clear();
                    for (ThongBao tb : data) titles.add(tb.getTieuDe());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<ThongBao>>> call, Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAsRead(ThongBao tb) {
        if (tb.getId() == null || userId == null) return;
        api.markThongBaoRead(tb.getId(), new ReadRequest(userId)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                // Sau khi đánh dấu, có thể đổi màu item hoặc reload
                Toast.makeText(NotificationsActivity.this, "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) { }
        });
    }
}

