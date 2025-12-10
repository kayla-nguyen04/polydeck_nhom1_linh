package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.YeuCauHoTro;
import com.nhom1.polydeck.ui.adapter.SupportRequestAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupportRequestManagementActivity extends AppCompatActivity {

    private static final String TAG = "SupportRequestMgmt";
    
    private ImageView btnBack;
    private RecyclerView rvSupportRequests;
    private LinearLayout llEmptyState;
    private SupportRequestAdapter adapter;
    private APIService apiService;
    private List<YeuCauHoTro> supportRequests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_request_management);

        apiService = RetrofitClient.getApiService();
        
        initViews();
        setupRecyclerView();
        loadSupportRequests();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvSupportRequests = findViewById(R.id.rvSupportRequests);
        llEmptyState = findViewById(R.id.tvEmptyState); // This is actually a LinearLayout
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rvSupportRequests.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SupportRequestAdapter(supportRequests, this);
        adapter.setOnDeleteListener((request, position) -> deleteSupportRequest(request, position));
        adapter.setOnItemClickListener((request, position) -> showSupportRequestDetail(request));
        rvSupportRequests.setAdapter(adapter);
    }

    private void loadSupportRequests() {
        Log.d(TAG, "Loading support requests...");
        apiService.getAllSupportRequests().enqueue(new Callback<List<YeuCauHoTro>>() {
            @Override
            public void onResponse(@NonNull Call<List<YeuCauHoTro>> call, @NonNull Response<List<YeuCauHoTro>> response) {
                Log.d(TAG, "Response code: " + response.code() + ", isSuccessful: " + response.isSuccessful());
                
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<YeuCauHoTro> body = response.body();
                        Log.d(TAG, "Successfully parsed " + body.size() + " support requests");
                        
                        // Log first item for debugging
                        if (!body.isEmpty()) {
                            YeuCauHoTro first = body.get(0);
                            Log.d(TAG, "First item - ID: " + first.getId() + 
                                ", maNguoiDung: " + first.getMaNguoiDung() + 
                                ", tenNguoiGui: " + first.getTenNguoiGui() +
                                ", noiDung length: " + (first.getNoiDung() != null ? first.getNoiDung().length() : 0));
                        }
                        
                        supportRequests.clear();
                        supportRequests.addAll(body);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing support requests: " + e.getMessage(), e);
                        e.printStackTrace();
                        Toast.makeText(SupportRequestManagementActivity.this, "Lỗi parse dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        updateEmptyState();
                    }
                } else {
                    String errorMsg = "Không thể tải danh sách phản hồi (Code: " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error response body: " + errorBody);
                            errorMsg += "\n" + errorBody;
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    Toast.makeText(SupportRequestManagementActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<YeuCauHoTro>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading support requests: " + t.getMessage(), t);
                t.printStackTrace();
                String errorMsg = t.getMessage();
                if (errorMsg != null && (errorMsg.contains("Expected a string but was") || errorMsg.contains("com.google.gson"))) {
                    errorMsg = "Lỗi định dạng dữ liệu từ server. Vui lòng kiểm tra logcat để biết chi tiết.";
                    Log.e(TAG, "Gson parsing error detected. Check HttpLoggingInterceptor output for raw response.");
                }
                Toast.makeText(SupportRequestManagementActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                updateEmptyState();
            }
        });
    }

    private void deleteSupportRequest(YeuCauHoTro request, int position) {
        if (request.getId() == null || request.getId().isEmpty()) {
            Toast.makeText(this, "ID phản hồi không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.deleteSupportRequest(request.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    supportRequests.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, supportRequests.size());
                    Toast.makeText(SupportRequestManagementActivity.this, "Đã xóa phản hồi", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                } else {
                    Toast.makeText(SupportRequestManagementActivity.this, "Xóa phản hồi thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Error deleting support request: " + t.getMessage());
                Toast.makeText(SupportRequestManagementActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyState() {
        if (supportRequests == null || supportRequests.isEmpty()) {
            if (llEmptyState != null) llEmptyState.setVisibility(View.VISIBLE);
            if (rvSupportRequests != null) rvSupportRequests.setVisibility(View.GONE);
        } else {
            if (llEmptyState != null) llEmptyState.setVisibility(View.GONE);
            if (rvSupportRequests != null) rvSupportRequests.setVisibility(View.VISIBLE);
        }
    }

    private void showSupportRequestDetail(YeuCauHoTro request) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Chi tiết phản hồi");
        
        StringBuilder message = new StringBuilder();
        message.append("Người gửi: ").append(request.getTenNguoiGui() != null ? request.getTenNguoiGui() : "Không có tên").append("\n\n");
        message.append("Email: ").append(request.getEmailNguoiGui() != null ? request.getEmailNguoiGui() : "").append("\n\n");
        
        if (request.getNgayGui() != null && !request.getNgayGui().isEmpty()) {
            try {
                java.util.Date date = request.getNgayGuiAsDate();
                if (date != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    message.append("Ngày gửi: ").append(dateFormat.format(date)).append("\n\n");
                } else {
                    message.append("Ngày gửi: ").append(request.getNgayGui()).append("\n\n");
                }
            } catch (Exception e) {
                message.append("Ngày gửi: ").append(request.getNgayGui()).append("\n\n");
            }
        }
        
        message.append("Nội dung:\n").append(request.getNoiDung() != null ? request.getNoiDung() : "");
        
        builder.setMessage(message.toString());
        builder.setPositiveButton("Đóng", null);
        builder.setNeutralButton("Xóa", (dialog, which) -> {
            int position = supportRequests.indexOf(request);
            if (position >= 0) {
                deleteSupportRequest(request, position);
            }
        });
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSupportRequests();
    }
}

