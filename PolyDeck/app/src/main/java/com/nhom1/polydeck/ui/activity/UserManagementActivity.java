package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.adapter.UserAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManagementActivity extends AppCompatActivity {

    private static final String TAG = "UserManagementActivity";

    private Toolbar toolbar;
    private EditText etSearchUser;
    private RecyclerView rvUsers;
    private TextView tvTotalUsers, tvBannedUsers;
    private ImageView btnRefresh;
    private UserAdapter userAdapter;
    private APIService apiService;
    private List<User> fullUserList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        fetchUsers(); // Fetch data on create
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX: Fetch data every time the activity is resumed to see changes
        fetchUsers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearchUser = findViewById(R.id.etSearchUser);
        rvUsers = findViewById(R.id.rvUsers);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvBannedUsers = findViewById(R.id.tvBannedUsers);
        btnRefresh = findViewById(R.id.btnRefresh);
        
        btnRefresh.setOnClickListener(v -> fetchUsers());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this, new ArrayList<>());
        userAdapter.setOnUserStatusChangedListener(() -> {
            // Refresh data when user status is changed (block/unblock)
            fetchUsers();
        });
        rvUsers.setAdapter(userAdapter);
    }

    private void fetchUsers() {
        apiService.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullUserList.clear();
                    fullUserList.addAll(response.body());
                    userAdapter.updateData(new ArrayList<>(fullUserList));
                    updateStats();
                } else {
                    Toast.makeText(UserManagementActivity.this, "Failed to load users", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(UserManagementActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            userAdapter.updateData(new ArrayList<>(fullUserList));
            return;
        }
        List<User> filteredList = new ArrayList<>();
        for (User user : fullUserList) {
            if (user.getHoTen().toLowerCase().contains(query.toLowerCase()) || user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }
        userAdapter.updateData(filteredList);
    }

    private void updateStats() {
        long activeCount = fullUserList.stream().filter(u -> "active".equalsIgnoreCase(u.getTrangThai())).count();
        long bannedCount = fullUserList.stream().filter(u -> "banned".equalsIgnoreCase(u.getTrangThai())).count();
        tvTotalUsers.setText(String.format("%d Hoạt động", activeCount));
        tvBannedUsers.setText(String.format("%d Bị khóa", bannedCount));
    }
}
