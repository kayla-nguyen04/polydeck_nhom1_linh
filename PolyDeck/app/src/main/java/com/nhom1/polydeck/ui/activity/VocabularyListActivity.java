package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.TuVung;
import com.nhom1.polydeck.ui.adapter.VocabularyAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VocabularyListActivity extends AppCompatActivity {

    public static final String EXTRA_DECK_ID = "EXTRA_DECK_ID";
    public static final String EXTRA_DECK_NAME = "EXTRA_DECK_NAME";
    private static final String TAG = "VocabListActivity";
    private static final int REQUEST_CODE_ADD_VOCAB = 2001;

    private Toolbar toolbar;
    private RecyclerView rvVocabulary;
    private VocabularyAdapter adapter;
    private APIService apiService;
    private EditText edtSearchVocab;
    private FloatingActionButton fabAddVocabulary;
    private String deckId;

    private final List<TuVung> original = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vocabulary_list);

        deckId = getIntent().getStringExtra(EXTRA_DECK_ID);
        String deckName = getIntent().getStringExtra(EXTRA_DECK_NAME);

        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "Lỗi: ID bộ từ không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();

        initViews();
        setupToolbar(deckName);
        setupRecyclerView();
        setupSearch();
        setupFab();
        fetchVocabulary(deckId);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_vocab_list);
        rvVocabulary = findViewById(R.id.rvVocabulary);
        edtSearchVocab = findViewById(R.id.edtSearchVocab);
        fabAddVocabulary = findViewById(R.id.fabAddVocabulary);
    }

    private void setupToolbar(String deckName) {
        toolbar.setTitle("Từ của bộ: " + (deckName != null ? deckName : ""));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        rvVocabulary.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VocabularyAdapter(new ArrayList<>(), this);
        rvVocabulary.setAdapter(adapter);
    }

    private void setupSearch() {
        edtSearchVocab.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String q = s.toString().trim().toLowerCase(Locale.getDefault());
                List<TuVung> filtered = new ArrayList<>();
                for (TuVung t : original) {
                    String en = t.getTuTiengAnh() != null ? t.getTuTiengAnh().toLowerCase(Locale.getDefault()) : "";
                    String vi = t.getNghiaTiengViet() != null ? t.getNghiaTiengViet().toLowerCase(Locale.getDefault()) : "";
                    if (en.contains(q) || vi.contains(q)) filtered.add(t);
                }
                adapter.updateData(filtered);
            }
        });
    }

    private void setupFab() {
        fabAddVocabulary.setOnClickListener(v -> {
            Intent intent = new Intent(VocabularyListActivity.this, AddVocabularyActivity.class);
            intent.putExtra("DECK_ID", deckId);
            startActivityForResult(intent, REQUEST_CODE_ADD_VOCAB);
        });
    }

    private void fetchVocabulary(String deckId) {
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    original.clear();
                    original.addAll(response.body());
                    adapter.updateData(original);
                } else {
                    Toast.makeText(VocabularyListActivity.this, "Không thể tải danh sách từ vựng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage());
                Toast.makeText(VocabularyListActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh vocabulary list when returning from AddVocabularyActivity
        if (deckId != null && !deckId.isEmpty()) {
            fetchVocabulary(deckId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Refresh vocabulary list when returning from AddVocabularyActivity (after adding a new word)
        if (requestCode == REQUEST_CODE_ADD_VOCAB) {
            if (deckId != null && !deckId.isEmpty()) {
                fetchVocabulary(deckId);
            }
            // Set result to notify parent activity (DeckManagementActivity) that vocabulary changed
            setResult(RESULT_OK);
        }
    }

    @Override
    public void onBackPressed() {
        // Set result to notify DeckManagementActivity that vocabulary might have changed
        setResult(RESULT_OK);
        super.onBackPressed();
    }
}
