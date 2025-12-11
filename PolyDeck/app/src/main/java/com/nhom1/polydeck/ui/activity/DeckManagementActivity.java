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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;
import com.nhom1.polydeck.ui.adapter.DeckAdapter;
import com.nhom1.polydeck.utils.HiddenDeckManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeckManagementActivity extends AppCompatActivity {

    private static final String TAG = "DeckManagementActivity";
    private static final int REQUEST_CODE_ADD_DECK = 1001;
    private static final int REQUEST_CODE_VOCAB_LIST = 2002;

    private ImageView btnBack;
    private EditText etSearchDeck;
    private RecyclerView rvDecks;
    private RecyclerView rvHiddenDecks;
    private android.view.View sectionHiddenDecks;
    private ImageView btnAddDeck;
    private TextView tvTotalDecks, tvPublishedDecks;
    private DeckAdapter deckAdapter;
    private DeckAdapter hiddenDeckAdapter;
    private APIService apiService;
    private List<BoTu> fullDeckList = new ArrayList<>(); // Store the full list for search restoration
    private HiddenDeckManager hiddenDeckManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck_management);

        apiService = RetrofitClient.getApiService();
        hiddenDeckManager = new HiddenDeckManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();

        btnAddDeck.setOnClickListener(v -> {
            Intent intent = new Intent(DeckManagementActivity.this, AddDeckActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_DECK);
        });
        
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX: Fetch data every time the activity is resumed to see changes
        fetchDecks();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Refresh deck list when returning from AddDeckActivity (after creating a new deck)
        if (requestCode == REQUEST_CODE_ADD_DECK) {
            fetchDecks();
            // Set result to notify AdminDashboardActivity to refresh stats
            setResult(RESULT_OK);
        } else if (requestCode == REQUEST_CODE_VOCAB_LIST) {
            // Vocabulary was added/changed, refresh deck list to update vocab counts
            fetchDecks();
            // Set result to notify AdminDashboardActivity to refresh stats
            setResult(RESULT_OK);
        }
    }

    @Override
    public void onBackPressed() {
        // Set result when going back to refresh stats in AdminDashboardActivity
        // Luôn set result để đảm bảo AdminDashboardActivity refresh stats
        Log.d(TAG, "onBackPressed: Setting result OK and finishing");
        setResult(RESULT_OK);
        finish();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Đảm bảo set result trước khi pause để AdminDashboardActivity có thể nhận được
        Log.d(TAG, "onPause: Setting result OK");
        setResult(RESULT_OK);
    }

    private void initViews(){
        btnBack = findViewById(R.id.btnBack);
        etSearchDeck = findViewById(R.id.inputSearch);
        rvDecks = findViewById(R.id.recyclerViewDecks);
        rvHiddenDecks = findViewById(R.id.recyclerViewHiddenDecks);
        sectionHiddenDecks = findViewById(R.id.sectionHiddenDecks);
        btnAddDeck = findViewById(R.id.btnAddDeck);
        tvTotalDecks = findViewById(R.id.tvTotalDecks);
        tvPublishedDecks = findViewById(R.id.tvPublishedDecks);
        
        // Xử lý window insets cho NestedScrollView
        android.view.View nestedScrollView = findViewById(R.id.nestedScrollView);
        if (nestedScrollView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(nestedScrollView, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 
                        Math.max(systemBars.bottom, 16)); // Tối thiểu 16dp
            return insets;
        });
        }
    }

    private void setupToolbar(){
        // Toolbar is replaced with custom header, no action needed
    }

    private void setupRecyclerView() {
        // Adapter cho bộ từ đang hiển thị
        rvDecks.setLayoutManager(new LinearLayoutManager(this));
        deckAdapter = new DeckAdapter(this, new ArrayList<>());
        deckAdapter.setOnDeckDeletedListener(() -> {
            // Refresh data when deck is hidden
            fetchDecks();
            // Set result to notify AdminDashboardActivity to refresh stats
            setResult(RESULT_OK);
        });
        rvDecks.setAdapter(deckAdapter);
        
        // Adapter cho bộ từ đã ẩn
        rvHiddenDecks.setLayoutManager(new LinearLayoutManager(this));
        hiddenDeckAdapter = new DeckAdapter(this, new ArrayList<>(), true); // true = mode hiển thị lại
        hiddenDeckAdapter.setOnDeckDeletedListener(() -> {
            // Refresh data when deck is unhidden
            fetchDecks();
            setResult(RESULT_OK);
        });
        rvHiddenDecks.setAdapter(hiddenDeckAdapter);
    }

    private void fetchDecks() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullDeckList.clear();
                    fullDeckList.addAll(response.body());
                    
                    // Lọc bỏ các bộ từ đã ẩn
                    List<BoTu> visibleDecks = filterHiddenDecks(fullDeckList);
                    // Lấy các bộ từ đã ẩn
                    List<BoTu> hiddenDecks = getHiddenDecks(fullDeckList);
                    
                    // Log deck data to check icon URLs
                    for (BoTu deck : visibleDecks) {
                        String iconUrl = deck.getLinkAnhIcon();
                        Log.d(TAG, "📦 Deck: " + deck.getTenChuDe() + " | Icon: [" + iconUrl + "] | IsNull: " + (iconUrl == null) + " | IsEmpty: " + (iconUrl != null && iconUrl.isEmpty()));
                    }
                    
                    deckAdapter.updateData(new ArrayList<>(visibleDecks)); // Pass a copy to the adapter
                    hiddenDeckAdapter.updateData(new ArrayList<>(hiddenDecks)); // Update hidden decks list
                    
                    // Hiển thị/ẩn section bộ từ đã ẩn
                    if (hiddenDecks.isEmpty()) {
                        sectionHiddenDecks.setVisibility(android.view.View.GONE);
                    } else {
                        sectionHiddenDecks.setVisibility(android.view.View.VISIBLE);
                    }
                    
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
    
    /**
     * Lọc bỏ các bộ từ đã ẩn khỏi danh sách
     */
    private List<BoTu> filterHiddenDecks(List<BoTu> allDecks) {
        List<BoTu> visibleDecks = new ArrayList<>();
        for (BoTu deck : allDecks) {
            if (deck.getId() != null && !hiddenDeckManager.isDeckHidden(deck.getId())) {
                visibleDecks.add(deck);
            }
        }
        return visibleDecks;
    }
    
    /**
     * Lấy danh sách các bộ từ đã ẩn
     */
    private List<BoTu> getHiddenDecks(List<BoTu> allDecks) {
        List<BoTu> hiddenDecks = new ArrayList<>();
        for (BoTu deck : allDecks) {
            if (deck.getId() != null && hiddenDeckManager.isDeckHidden(deck.getId())) {
                hiddenDecks.add(deck);
            }
        }
        return hiddenDecks;
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
        // Lọc bỏ các bộ từ đã ẩn trước
        List<BoTu> visibleDecks = filterHiddenDecks(fullDeckList);
        
        if (query == null || query.trim().isEmpty()) {
            deckAdapter.updateData(new ArrayList<>(visibleDecks)); // Restore the visible list
            return;
        }

        List<BoTu> filteredList = new ArrayList<>();
        for (BoTu deck : visibleDecks) {
            if (deck.getTenChuDe().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(deck);
            }
        }
        deckAdapter.updateData(filteredList);
    }

    private void updateStats() {
        // Tổng = tất cả bộ từ (bao gồm cả ẩn)
        int totalDecks = fullDeckList.size();
        
        // Đã xuất bản = chỉ bộ từ đang hiển thị (không ẩn)
        List<BoTu> visibleDecks = filterHiddenDecks(fullDeckList);
        // Assuming all visible decks are published for now
        int publishedDecks = visibleDecks.size();
        
        tvTotalDecks.setText(String.valueOf(totalDecks));
        tvPublishedDecks.setText(String.valueOf(publishedDecks));
    }
}
