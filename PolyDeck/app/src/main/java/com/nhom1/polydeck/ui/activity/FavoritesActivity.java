package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.ui.adapter.VocabularyAdapter;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_list);

        APIService api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        String userId = sm.getUserData() != null ? sm.getUserData().getMaNguoiDung() : null;
        if (userId == null) { finish(); return; }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar_vocab_list);
        toolbar.setTitle("Từ yêu thích");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        RecyclerView rv = findViewById(R.id.rvVocabulary);
        rv.setLayoutManager(new LinearLayoutManager(this));
        VocabularyAdapter adapter = new VocabularyAdapter(new ArrayList<>());
        rv.setAdapter(adapter);

        api.getUserFavorites(userId).enqueue(new Callback<ApiResponse<List<TuVung>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Response<ApiResponse<List<TuVung>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    adapter.updateData(response.body().getData());
                } else {
                    Toast.makeText(FavoritesActivity.this, "Không tải được danh sách yêu thích", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<TuVung>>> call, @NonNull Throwable t) {
                Toast.makeText(FavoritesActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


