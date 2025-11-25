package com.nhom1.polydeck.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.ui.adapter.UserDeckAdapter;
import com.nhom1.polydeck.ui.activity.NotificationsActivity;
import com.nhom1.polydeck.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private UserDeckAdapter adapter;
    private APIService apiService;
    private TextView tvUsername;
    private TextView tvGreeting;
    private TextView tvStreak;
    private TextView tvXp;
    private TextView tvContinueTitle;
    private TextView tvContinueSubtitle;
    private ProgressBar progressContinue;
    private ImageView ivContinueIcon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = RetrofitClient.getApiService();

        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvUsername = view.findViewById(R.id.tv_username);
        tvStreak = view.findViewById(R.id.tv_streak_value);
        tvXp = view.findViewById(R.id.tv_xp_value);
        tvContinueTitle = view.findViewById(R.id.tv_continue_title);
        tvContinueSubtitle = view.findViewById(R.id.tv_continue_subtitle);
        progressContinue = view.findViewById(R.id.progress_continue);
        ivContinueIcon = view.findViewById(R.id.iv_continue_icon);

        // Bind user info from Session
        SessionManager sessionManager = new SessionManager(requireContext());
        LoginResponse user = sessionManager.getUserData();
        tvGreeting.setText("Xin chào,");
        if (user != null) {
            tvUsername.setText(user.getHoTen());
            tvStreak.setText(user.getChuoiNgayHoc() + " ngày");
            tvXp.setText(String.valueOf(user.getDiemTichLuy()));
        } else {
            tvUsername.setText("Người học");
            tvStreak.setText("0 ngày");
            tvXp.setText("0");
        }

        View btnNotif = view.findViewById(R.id.btn_notifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), NotificationsActivity.class)));
        }

        TextView tvViewAll = view.findViewById(R.id.tv_view_all);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                if (getActivity() != null) {
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_topics);
                    }
                }
            });
        }

        RecyclerView rvDecks = view.findViewById(R.id.rv_decks_home);
        rvDecks.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new UserDeckAdapter(requireContext());
        rvDecks.setAdapter(adapter);

        loadDecks();
    }

    private void loadDecks() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BoTu> items = response.body();
                    adapter.setItems(items);

                    // Bind continue learning card with first topic as placeholder
                    if (items != null && !items.isEmpty()) {
                        BoTu first = items.get(0);
                        if (tvContinueTitle != null) {
                            tvContinueTitle.setText(first.getTenChuDe());
                        }
                        if (tvContinueSubtitle != null) {
                            int total = Math.max(first.getSoLuongQuiz(), 0);
                            // Placeholder learned count = 0 until progress API available
                            tvContinueSubtitle.setText("Đã học 0/" + total + " từ");
                        }
                        if (progressContinue != null) {
                            progressContinue.setProgress(0);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
            }
        });
    }
}

