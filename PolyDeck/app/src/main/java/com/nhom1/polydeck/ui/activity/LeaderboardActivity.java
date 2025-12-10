package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.adapter.LeaderboardAdapter;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView rv;
    private LeaderboardAdapter adapter;
    private APIService api;
    private SessionManager sessionManager;
    
    // Top 3 views
    private LinearLayout rank1Container, rank2Container, rank3Container;
    private TextView tvRank1Name, tvRank1Avatar, tvRank1Badge, tvRank1Score;
    private TextView tvRank2Name, tvRank2Avatar, tvRank2Badge, tvRank2Score;
    private TextView tvRank3Name, tvRank3Avatar, tvRank3Badge, tvRank3Score;
    private ImageView ivRank1Avatar, ivRank2Avatar, ivRank3Avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        api = RetrofitClient.getApiService();
        sessionManager = new SessionManager(this);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initTop3Views();
        
        rv = findViewById(R.id.rvLeaderboard);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(new ArrayList<>(), sessionManager);
        rv.setAdapter(adapter);

        loadData();
    }

    private void initTop3Views() {
        rank1Container = findViewById(R.id.rank1Container);
        rank2Container = findViewById(R.id.rank2Container);
        rank3Container = findViewById(R.id.rank3Container);
        
        tvRank1Name = findViewById(R.id.tvRank1Name);
        tvRank1Avatar = findViewById(R.id.tvRank1Avatar);
        tvRank1Badge = findViewById(R.id.tvRank1Badge);
        tvRank1Score = findViewById(R.id.tvRank1Score);
        ivRank1Avatar = findViewById(R.id.ivRank1Avatar);
        
        tvRank2Name = findViewById(R.id.tvRank2Name);
        tvRank2Avatar = findViewById(R.id.tvRank2Avatar);
        tvRank2Badge = findViewById(R.id.tvRank2Badge);
        tvRank2Score = findViewById(R.id.tvRank2Score);
        ivRank2Avatar = findViewById(R.id.ivRank2Avatar);
        
        tvRank3Name = findViewById(R.id.tvRank3Name);
        tvRank3Avatar = findViewById(R.id.tvRank3Avatar);
        tvRank3Badge = findViewById(R.id.tvRank3Badge);
        tvRank3Score = findViewById(R.id.tvRank3Score);
        ivRank3Avatar = findViewById(R.id.ivRank3Avatar);
    }

    private void loadData() {
        api.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override 
            public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> users = response.body();
                    Collections.sort(users, Comparator.comparingInt(User::getXp).reversed());
                    
                    // Display top 3
                    displayTop3(users);
                    
                    // Display rest from rank 4
                    if (users.size() > 3) {
                        List<User> restUsers = users.subList(3, users.size());
                        adapter.update(restUsers);
                    } else {
                        adapter.update(new ArrayList<>());
                    }
                } else {
                    Toast.makeText(LeaderboardActivity.this, "Không tải được bảng xếp hạng", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override 
            public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                Toast.makeText(LeaderboardActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTop3(List<User> users) {
        // Rank 1 (Center)
        if (users.size() > 0) {
            User user1 = users.get(0);
            String name1 = user1.getHoTen() != null ? user1.getHoTen() : "Unknown";
            tvRank1Name.setText(name1);
            tvRank1Avatar.setText(user1.getInitials());
            tvRank1Badge.setText("1");
            tvRank1Score.setText(String.valueOf(user1.getXp()));
            
            // Load avatar
            loadAvatar(ivRank1Avatar, tvRank1Avatar, user1.getLinkAnhDaiDien(), 
                    0xFFFFFFFF, 0xFFFF9800); // White background, Orange text
            
            rank1Container.setVisibility(View.VISIBLE);
        } else {
            rank1Container.setVisibility(View.GONE);
        }
        
        // Rank 2 (Left)
        if (users.size() > 1) {
            User user2 = users.get(1);
            String name2 = user2.getHoTen() != null ? user2.getHoTen() : "Unknown";
            tvRank2Name.setText(name2);
            tvRank2Avatar.setText(user2.getInitials());
            tvRank2Badge.setText("2");
            tvRank2Score.setText(String.valueOf(user2.getXp()));
            
            // Load avatar
            loadAvatar(ivRank2Avatar, tvRank2Avatar, user2.getLinkAnhDaiDien(), 
                    0xFFE0E0E0, 0xFF757575); // Light gray background, Gray text
            
            rank2Container.setVisibility(View.VISIBLE);
        } else {
            rank2Container.setVisibility(View.GONE);
        }
        
        // Rank 3 (Right)
        if (users.size() > 2) {
            User user3 = users.get(2);
            String name3 = user3.getHoTen() != null ? user3.getHoTen() : "Unknown";
            tvRank3Name.setText(name3);
            tvRank3Avatar.setText(user3.getInitials());
            tvRank3Badge.setText("3");
            tvRank3Score.setText(String.valueOf(user3.getXp()));
            
            // Load avatar
            loadAvatar(ivRank3Avatar, tvRank3Avatar, user3.getLinkAnhDaiDien(), 
                    0xFFEF5350, 0xFFFFFFFF); // Red/Pink background, White text
            
            rank3Container.setVisibility(View.VISIBLE);
        } else {
            rank3Container.setVisibility(View.GONE);
        }
    }
    
    private void loadAvatar(ImageView imageView, TextView textView, String avatarUrl, 
                           int defaultBgColor, int defaultTextColor) {
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            // Show ImageView, hide TextView
            imageView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            
            // Load image with Glide
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(defaultBgColor)
                    .error(defaultBgColor)
                    .into(imageView);
        } else {
            // No avatar URL, show TextView with initials
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            textView.setBackgroundColor(defaultBgColor);
            textView.setTextColor(defaultTextColor);
        }
    }
}
