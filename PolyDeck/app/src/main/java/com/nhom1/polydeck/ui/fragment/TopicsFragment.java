package com.nhom1.polydeck.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TopicsFragment extends Fragment {

    private UserDeckAdapter adapter;
    private APIService apiService;
    private EditText edtSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = RetrofitClient.getApiService();

        edtSearch = view.findViewById(R.id.edt_search_topic);
        RecyclerView rvDecks = view.findViewById(R.id.rv_decks);
        rvDecks.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new UserDeckAdapter(requireContext());
        rvDecks.setAdapter(adapter);

        loadAll();
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim();
                if (q.isEmpty()) {
                    loadAll();
                } else {
                    search(q);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh deck list when returning to this fragment (e.g., after deleting a deck)
        // If there's a search query, reload all to show updated list
        if (edtSearch != null && edtSearch.getText() != null) {
            String query = edtSearch.getText().toString().trim();
            if (query.isEmpty()) {
                loadAll();
            } else {
                // Reload all first, then reapply search if needed
                loadAll();
            }
        } else {
            loadAll();
        }
    }

    private void loadAll() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) { }
        });
    }

    private void search(String q) {
        apiService.searchChuDe(q).enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setItems(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) { }
        });
    }
}



