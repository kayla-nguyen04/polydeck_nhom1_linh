package com.nhom1.polydeck.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.ApiResponse;
import com.nhom1.polydeck.data.model.LichSuLamBai;
import com.nhom1.polydeck.ui.adapter.HistoryAdapter;
import com.nhom1.polydeck.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatsFragment extends Fragment {
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvStreak = view.findViewById(R.id.tv_streak);
        TextView tvXp = view.findViewById(R.id.tv_xp);

        RecyclerView rv = view.findViewById(R.id.rv_history);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter();
        rv.setAdapter(adapter);

        APIService api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(requireContext());
        String userId = sm.getUserData() != null ? sm.getUserData().getMaNguoiDung() : null;
        if (userId == null) return;

        api.getQuizHistory(userId).enqueue(new Callback<ApiResponse<List<LichSuLamBai>>>() {
            @Override public void onResponse(Call<ApiResponse<List<LichSuLamBai>>> call, Response<ApiResponse<List<LichSuLamBai>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LichSuLamBai> list = response.body().getData();
                    adapter.setItems(list);

                    int xp = 0;
                    int streak = 0; // số ngày có bài
                    java.util.HashSet<String> days = new java.util.HashSet<>();
                    java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    for (LichSuLamBai h : list) {
                        xp += Math.max(50, h.diemSo);
                        if (h.ngayHoanThanh != null) days.add(df.format(h.ngayHoanThanh));
                    }
                    streak = days.size();
                    tvStreak.setText(streak + " ngày");
                    tvXp.setText(String.valueOf(xp));
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<LichSuLamBai>>> call, Throwable t) { }
        });
    }
}

