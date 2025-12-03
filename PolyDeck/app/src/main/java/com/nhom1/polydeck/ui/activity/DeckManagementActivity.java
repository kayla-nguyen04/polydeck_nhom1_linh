package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.adapter.DeckAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeckManagementActivity extends AppCompatActivity {

    private static final String TAG = "DeckManagementActivity";

    private ImageView btnBack;
    private EditText etSearchDeck;
    private RecyclerView rvDecks;
    private ImageView btnAddDeck;
    private TextView tvTotalDecks, tvPublishedDecks;
    private DeckAdapter deckAdapter;
    private APIService apiService;
    private List<BoTu> fullDeckList = new ArrayList<>(); // Store the full list for search restoration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck_management);

        apiService = RetrofitClient.getApiService();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();

        btnAddDeck.setOnClickListener(v -> {
            Intent intent = new Intent(DeckManagementActivity.this, AddDeckActivity.class);
            startActivity(intent);
        });
        
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX: Fetch data every time the activity is resumed to see changes
        fetchDecks();
    }

    private void initViews(){
        btnBack = findViewById(R.id.btnBack);
        etSearchDeck = findViewById(R.id.inputSearch);
        rvDecks = findViewById(R.id.recyclerViewDecks);
        btnAddDeck = findViewById(R.id.btnAddDeck);
        tvTotalDecks = findViewById(R.id.tvTotalDecks);
        tvPublishedDecks = findViewById(R.id.tvPublishedDecks);
    }

    private void setupToolbar(){
        // Toolbar is replaced with custom header, no action needed
    }

    private void setupRecyclerView() {
        rvDecks.setLayoutManager(new LinearLayoutManager(this));
        deckAdapter = new DeckAdapter(this, new ArrayList<>());
        deckAdapter.setOnDeckDeletedListener(() -> {
            // Refresh data when deck is deleted
            fetchDecks();
        });
        rvDecks.setAdapter(deckAdapter);
    }

    private void fetchDecks() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullDeckList.clear();
                    fullDeckList.addAll(response.body());
                    
                    // Log deck data to check icon URLs
                    for (BoTu deck : fullDeckList) {
                        String iconUrl = deck.getLinkAnhIcon();
                        Log.d(TAG, "📦 Deck: " + deck.getTenChuDe() + " | Icon: [" + iconUrl + "] | IsNull: " + (iconUrl == null) + " | IsEmpty: " + (iconUrl != null && iconUrl.isEmpty()));
                    }
                    
                    deckAdapter.updateData(new ArrayList<>(fullDeckList)); // Pass a copy to the adapter
                    updateStats();
                } else {
                    Toast.makeText(DeckManagementActivity.this, "Failed to load decks", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(DeckManagementActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        etSearchDeck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchDecks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchDecks(String query) {
        if (query == null || query.trim().isEmpty()) {
            deckAdapter.updateData(new ArrayList<>(fullDeckList)); // Restore the full list
            return;
        }

        List<BoTu> filteredList = new ArrayList<>();
        for (BoTu deck : fullDeckList) {
            if (deck.getTenChuDe().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(deck);
            }
        }
        deckAdapter.updateData(filteredList);
    }

    private void updateStats() {
        int totalDecks = fullDeckList.size();
        // Assuming all decks are published for now
        int publishedDecks = totalDecks; // You can add logic to check published status
        tvTotalDecks.setText(String.valueOf(totalDecks));
        tvPublishedDecks.setText(String.valueOf(publishedDecks));
    }
}
