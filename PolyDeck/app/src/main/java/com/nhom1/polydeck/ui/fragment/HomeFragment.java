package com.nhom1.polydeck.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.adapter.UserDeckAdapter;
import com.nhom1.polydeck.ui.activity.NotificationsActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private UserDeckAdapter adapter;
    private APIService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = RetrofitClient.getApiService();

        TextView tvGreeting = view.findViewById(R.id.tv_greeting);
        tvGreeting.setText("Xin chào, người học!");
        View btnNotif = view.findViewById(R.id.btn_notifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> startActivity(new android.content.Intent(requireContext(), NotificationsActivity.class)));
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
                    adapter.setItems(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
            }
        });
    }
}

