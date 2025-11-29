package com.nhom1.polydeck.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.TuVung;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddVocabularyActivity extends AppCompatActivity {

    private static final String TAG = "AddVocabularyActivity";
    private static final int PICK_EXCEL_FILE_REQUEST = 2;

    private ImageView btnBack;
    private TextView btnImportExcel;
    private EditText etEnglishWord, etPronunciation, etVietnameseMeaning, etExample, etAudioURL;
    private Button btnAddVocabulary;
    private ImageView btnSaveAndExit;
    private ProgressBar importProgressBar;

    private APIService apiService;
    private String deckId;
    private List<TuVung> existingVocab = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vocabulary);

        apiService = RetrofitClient.getApiService();

        deckId = getIntent().getStringExtra("DECK_ID");
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID bộ từ.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupListeners();
        
        // Load existing vocabulary to check for duplicates
        loadExistingVocabulary();
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
            if (vocab.getTuTiengAnh() != null && vocab.getTuTiengAnh().equalsIgnoreCase(englishWord.trim())) {
                return true;
            }
        }
        return false;
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnImportExcel = findViewById(R.id.btnImportExcel);
        etEnglishWord = findViewById(R.id.inputWord);
        etPronunciation = findViewById(R.id.inputIPA);
        etVietnameseMeaning = findViewById(R.id.inputMeaning);
        etExample = findViewById(R.id.inputExample);
        etAudioURL = findViewById(R.id.inputAudioURL);
        btnAddVocabulary = findViewById(R.id.btnAddWord);
        btnSaveAndExit = findViewById(R.id.btnSave);
        // You need to add a ProgressBar to your activity_add_vocabulary.xml
        // importProgressBar = findViewById(R.id.importProgressBar);
    }

    private void setupToolbar() {
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        btnAddVocabulary.setOnClickListener(v -> addVocabularyManually(false));
        btnImportExcel.setOnClickListener(v -> openFileChooser());
        btnSaveAndExit.setOnClickListener(v -> addVocabularyManually(true));
    }

    private void addVocabularyManually(boolean shouldExit) {
        String englishWord = etEnglishWord.getText().toString().trim();
        String pronunciation = etPronunciation.getText().toString().trim();
        String vietnameseMeaning = etVietnameseMeaning.getText().toString().trim();
        String example = etExample.getText().toString().trim();
        String audioURL = etAudioURL.getText().toString().trim();

        if (englishWord.isEmpty() || vietnameseMeaning.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập từ vựng và nghĩa", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check for duplicate vocabulary
        if (isVocabularyDuplicate(englishWord)) {
            etEnglishWord.setError("Từ vựng đã tồn tại");
            etEnglishWord.requestFocus();
            Toast.makeText(this, "Từ vựng \"" + englishWord + "\" đã tồn tại trong bộ từ này.", Toast.LENGTH_LONG).show();
            return;
        }

        TuVung vocab = new TuVung();
        vocab.setTuTiengAnh(englishWord);
        vocab.setPhienAm(pronunciation);
        vocab.setNghiaTiengViet(vietnameseMeaning);
        vocab.setCauViDu(example.isEmpty() ? null : example);
        vocab.setAmThanh(audioURL.isEmpty() ? null : audioURL);

        apiService.addTuVungToChuDe(deckId, vocab).enqueue(new Callback<TuVung>() {
            @Override
            public void onResponse(@NonNull Call<TuVung> call, @NonNull Response<TuVung> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddVocabularyActivity.this, "Đã thêm từ vựng thành công", Toast.LENGTH_SHORT).show();
                    // Add to existing list to prevent duplicates
                    if (response.body() != null) {
                        existingVocab.add(response.body());
                    }
                    // Clear fields
                    etEnglishWord.setText("");
                    etPronunciation.setText("");
                    etVietnameseMeaning.setText("");
                    etExample.setText("");
                    etAudioURL.setText("");
                    
                    if (shouldExit) {
                        finish();
                    }
                } else {
                    String errorMessage = "Thêm từ vựng thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Add vocabulary failed - Error body: " + errorBody);
                            // Check if error is about duplicate
                            if (errorBody.toLowerCase().contains("đã tồn tại") || 
                                errorBody.toLowerCase().contains("already exists") ||
                                errorBody.toLowerCase().contains("duplicate") ||
                                response.code() == 409) {
                                errorMessage = "Từ vựng \"" + englishWord + "\" đã tồn tại trong bộ từ này.";
                                etEnglishWord.setError("Từ vựng đã tồn tại");
                                etEnglishWord.requestFocus();
                            } else {
                                errorMessage = "Thêm từ vựng thất bại: " + response.code();
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Toast.makeText(AddVocabularyActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TuVung> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(AddVocabularyActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // .xlsx
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn tệp Excel"), PICK_EXCEL_FILE_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Vui lòng cài đặt một trình quản lý tệp.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_EXCEL_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            readExcelFile(uri);
        }
    }

    private void readExcelFile(Uri uri) {
        // showLoading(true);
        new Thread(() -> {
            List<TuVung> vocabList = new ArrayList<>();
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

                XSSFSheet sheet = workbook.getSheetAt(0);
                Iterator<Row> rowIterator = sheet.iterator();

                // Skip header row if it exists (check if first row looks like header)
                if (rowIterator.hasNext()) {
                    Row firstRow = rowIterator.next();
                    Cell firstCell = firstRow.getCell(0);
                    // If first cell contains common header words, skip it
                    if (firstCell != null) {
                        String firstCellValue = getCellValueAsString(firstCell).toLowerCase();
                        if (firstCellValue.contains("từ") || firstCellValue.contains("word") || 
                            firstCellValue.contains("english") || firstCellValue.contains("tiếng anh") ||
                            firstCellValue.contains("nghĩa") || firstCellValue.contains("meaning")) {
                            // This is a header row, skip it
                        } else {
                            // This is data, process it
                            rowIterator = sheet.iterator(); // Reset iterator
                            processRow(firstRow, vocabList);
                        }
                    }
                }

                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    processRow(row, vocabList);
                }

                if (!vocabList.isEmpty()) {
                    runOnUiThread(() -> uploadVocabList(vocabList));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Không tìm thấy dữ liệu hợp lệ trong tệp Excel.", Toast.LENGTH_LONG).show();
                        // showLoading(false);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Error reading Excel file", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Lỗi khi đọc tệp Excel", Toast.LENGTH_SHORT).show();
                    // showLoading(false);
                });
            }
        }).start();
    }

    private void processRow(Row row, List<TuVung> vocabList) {
        Cell cell1 = row.getCell(0); // English word
        Cell cell2 = row.getCell(1); // Vietnamese meaning
        Cell cell3 = row.getCell(2); // Pronunciation (optional)
        Cell cell4 = row.getCell(3); // Example sentence (optional)
        Cell cell5 = row.getCell(4); // Audio URL (optional)

        if (cell1 != null && cell2 != null) {
            String english = getCellValueAsString(cell1);
            String vietnamese = getCellValueAsString(cell2);
            String pronunciation = (cell3 != null) ? getCellValueAsString(cell3) : "";
            String example = (cell4 != null) ? getCellValueAsString(cell4) : "";
            String audioURL = (cell5 != null) ? getCellValueAsString(cell5) : "";

            if (!english.trim().isEmpty() && !vietnamese.trim().isEmpty()) {
                TuVung vocab = new TuVung();
                vocab.setTuTiengAnh(english.trim());
                vocab.setNghiaTiengViet(vietnamese.trim());
                vocab.setPhienAm(pronunciation.trim().isEmpty() ? null : pronunciation.trim());
                vocab.setCauViDu(example.trim().isEmpty() ? null : example.trim());
                vocab.setAmThanh(audioURL.trim().isEmpty() ? null : audioURL.trim());
                vocabList.add(vocab);
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    // Handle numeric values (convert to string)
                    double numericValue = cell.getNumericCellValue();
                    // Check if it's a whole number
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return "";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading cell value: " + e.getMessage());
            return "";
        }
    }

    private void uploadVocabList(List<TuVung> vocabList) {
        apiService.importVocab(deckId, vocabList).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(AddVocabularyActivity.this, "Đã import thành công " + vocabList.size() + " từ vựng!", Toast.LENGTH_LONG).show();
                    finish(); // Close activity after successful import
                } else {
                    Toast.makeText(AddVocabularyActivity.this, "Import thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // showLoading(false);
                Log.e(TAG, "API call failed: " + t.getMessage());
                Toast.makeText(AddVocabularyActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
