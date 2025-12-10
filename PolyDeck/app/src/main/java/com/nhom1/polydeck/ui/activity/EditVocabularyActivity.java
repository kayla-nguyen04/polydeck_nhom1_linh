package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.TuVung;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditVocabularyActivity extends AppCompatActivity {

    private static final String TAG = "EditVocabularyActivity";

    private ImageView btnBack;
    private EditText etEnglishWord, etPronunciation, etVietnameseMeaning, etExample;
    private Button btnSaveVocabulary;

    private APIService apiService;
    private String vocabId;
    private String deckId;
    private TuVung currentVocab;
    private List<TuVung> existingVocab = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vocabulary);

        apiService = RetrofitClient.getApiService();

        vocabId = getIntent().getStringExtra("VOCAB_ID");
        deckId = getIntent().getStringExtra("DECK_ID");

        if (vocabId == null || vocabId.isEmpty() || deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Thiếu thông tin từ vựng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupListeners();
        
        // Load existing vocabulary to check for duplicates
        loadExistingVocabulary();
        
        // Load vocabulary details
        loadVocabularyDetails();
    }
    
    private void loadVocabularyDetails() {
        // Load vocabulary from deck
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (TuVung vocab : response.body()) {
                        if (vocab.getId() != null && vocab.getId().equals(vocabId)) {
                            currentVocab = vocab;
                            populateVocabularyData(vocab);
                            break;
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load vocabulary details: " + t.getMessage());
                Toast.makeText(EditVocabularyActivity.this, "Không thể tải thông tin từ vựng", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadExistingVocabulary() {
        apiService.getTuVungByBoTu(deckId).enqueue(new Callback<List<TuVung>>() {
            @Override
            public void onResponse(@NonNull Call<List<TuVung>> call, @NonNull Response<List<TuVung>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    existingVocab.clear();
                    existingVocab.addAll(response.body());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<TuVung>> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load existing vocabulary: " + t.getMessage());
            }
        });
    }
    
    private boolean isVocabularyDuplicate(String englishWord) {
        for (TuVung vocab : existingVocab) {
            // Skip the current vocabulary being edited
            if (vocab.getId() != null && vocab.getId().equals(vocabId)) {
                continue;
            }
            if (vocab.getTuTiengAnh() != null && vocab.getTuTiengAnh().equalsIgnoreCase(englishWord.trim())) {
                return true;
            }
        }
        return false;
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etEnglishWord = findViewById(R.id.inputWord);
        etPronunciation = findViewById(R.id.inputIPA);
        etVietnameseMeaning = findViewById(R.id.inputMeaning);
        etExample = findViewById(R.id.inputExample);
        btnSaveVocabulary = findViewById(R.id.btnAddWord);
        
        // Change button text for edit mode
        btnSaveVocabulary.setText("Lưu thay đổi");
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnSaveVocabulary.setOnClickListener(v -> saveVocabulary());
    }
    
    private void populateVocabularyData(TuVung vocab) {
        if (vocab == null) return;
        
        etEnglishWord.setText(vocab.getTuTiengAnh() != null ? vocab.getTuTiengAnh() : "");
        etPronunciation.setText(vocab.getPhienAm() != null ? vocab.getPhienAm() : "");
        etVietnameseMeaning.setText(vocab.getNghiaTiengViet() != null ? vocab.getNghiaTiengViet() : "");
        etExample.setText(vocab.getCauViDu() != null ? vocab.getCauViDu() : "");
    }

    private void saveVocabulary() {
        String englishWord = etEnglishWord.getText().toString().trim();
        String pronunciation = etPronunciation.getText().toString().trim();
        String vietnameseMeaning = etVietnameseMeaning.getText().toString().trim();
        String example = etExample.getText().toString().trim();

        if (englishWord.isEmpty() || vietnameseMeaning.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ vựng và nghĩa", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check for duplicate vocabulary (excluding current vocab)
        if (isVocabularyDuplicate(englishWord)) {
            etEnglishWord.setError("Từ vựng đã tồn tại");
            etEnglishWord.requestFocus();
            Toast.makeText(this, "Từ vựng \"" + englishWord + "\" đã tồn tại trong bộ từ này.", Toast.LENGTH_LONG).show();
            return;
        }

        TuVung vocab = new TuVung();
        vocab.setTuTiengAnh(englishWord);
        vocab.setPhienAm(pronunciation.isEmpty() ? null : pronunciation);
        vocab.setNghiaTiengViet(vietnameseMeaning);
        vocab.setCauViDu(example.isEmpty() ? null : example);

        btnSaveVocabulary.setEnabled(false);
        btnSaveVocabulary.setText("Đang lưu...");

        apiService.updateTuVung(deckId, vocabId, vocab).enqueue(new Callback<TuVung>() {
            @Override
            public void onResponse(@NonNull Call<TuVung> call, @NonNull Response<TuVung> response) {
                btnSaveVocabulary.setEnabled(true);
                btnSaveVocabulary.setText("Lưu thay đổi");
                
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(EditVocabularyActivity.this, "Đã cập nhật từ vựng thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String errorMessage = "Cập nhật từ vựng thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Update vocabulary failed - Error body: " + errorBody);
                            if (errorBody.toLowerCase().contains("đã tồn tại") || 
                                errorBody.toLowerCase().contains("already exists") ||
                                errorBody.toLowerCase().contains("duplicate") ||
                                response.code() == 409) {
                                errorMessage = "Từ vựng \"" + englishWord + "\" đã tồn tại trong bộ từ này.";
                                etEnglishWord.setError("Từ vựng đã tồn tại");
                                etEnglishWord.requestFocus();
                            } else {
                                errorMessage = "Cập nhật từ vựng thất bại: " + response.code();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(EditVocabularyActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuVung> call, @NonNull Throwable t) {
                btnSaveVocabulary.setEnabled(true);
                btnSaveVocabulary.setText("Lưu thay đổi");
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(EditVocabularyActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

