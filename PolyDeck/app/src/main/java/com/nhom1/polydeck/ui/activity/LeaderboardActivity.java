package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.adapter.LeaderboardAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ProgressBar progressBar;
    private LeaderboardAdapter adapter;
    private APIService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        api = RetrofitClient.getApiService();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        progressBar = findViewById(R.id.progress_loading);
        rv = findViewById(R.id.rv_leaderboard);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        loadData();
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        api.getAllUsers().enqueue(new Callback<List<User>>() {
            @Override public void onResponse(@NonNull Call<List<User>> call, @NonNull Response<List<User>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<User> users = response.body();
                    Collections.sort(users, Comparator.comparingInt(User::getXp).reversed());
                    adapter.update(users);
                } else {
                    Toast.makeText(LeaderboardActivity.this, "Không tải được bảng xếp hạng", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<User>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LeaderboardActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


