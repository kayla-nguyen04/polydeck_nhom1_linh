package com.nhom1.polydeck.ui.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.BoTu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditDeckActivity extends AppCompatActivity {

    private static final String TAG = "EditDeckActivity";
    private static final int PICK_IMAGE_REQUEST = 3;

    private Toolbar toolbar;
    private EditText etEditDeckName;
    private ImageView ivEditIconPreview;
    private TextView btnChangeImage;
    private Button btnSaveChangesDeck;

    private APIService apiService;
    private String deckId;
    private BoTu currentDeck;
    private Uri newImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_deck);

        deckId = getIntent().getStringExtra("DECK_ID");
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "ID bộ từ không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();
        initViews();
        setupToolbar();
        fetchDeckDetails();

        btnSaveChangesDeck.setOnClickListener(v -> saveChanges());
        btnChangeImage.setOnClickListener(v -> openFileChooser());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_edit_deck);
        etEditDeckName = findViewById(R.id.etEditDeckName);
        ivEditIconPreview = findViewById(R.id.ivEditIconPreview);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnSaveChangesDeck = findViewById(R.id.btnSaveChangesDeck);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void fetchDeckDetails() {
        apiService.getChuDeDetail(deckId).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(Call<BoTu> call, Response<BoTu> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDeck = response.body();
                    populateDeckData(currentDeck);
                } else {
                    Toast.makeText(EditDeckActivity.this, "Không thể tải thông tin bộ từ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BoTu> call, Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage());
            }
        });
    }

    private void populateDeckData(BoTu deck) {
        etEditDeckName.setText(deck.getTenChuDe());
        Glide.with(this)
                .load(deck.getLinkAnhIcon())
                .error(R.drawable.ic_default_deck_icon)
                .into(ivEditIconPreview);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

     @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            newImageUri = data.getData();
            ivEditIconPreview.setImageURI(newImageUri);
        }
    }

    private void saveChanges() {
        String newDeckName = etEditDeckName.getText().toString().trim();
        if (newDeckName.isEmpty()) {
            etEditDeckName.setError("Tên không được để trống");
            etEditDeckName.requestFocus();
            return;
        }
        
        // If image is selected, upload it first
        if (newImageUri != null) {
            uploadDeckWithImage(newDeckName);
            return;
        }

        // Otherwise, just update name
        currentDeck.setTenChuDe(newDeckName);

        apiService.updateChuDe(deckId, currentDeck).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(Call<BoTu> call, Response<BoTu> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditDeckActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditDeckActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BoTu> call, Throwable t) {
                 Log.e(TAG, "API Error: " + t.getMessage());
                 Toast.makeText(EditDeckActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadDeckWithImage(String deckName) {
        btnSaveChangesDeck.setText("Đang tải lên...");
        btnSaveChangesDeck.setEnabled(false);

        File file = createTempFileFromUri(newImageUri);
        if (file == null) {
            Toast.makeText(this, "Không thể tạo tệp từ ảnh đã chọn", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(newImageUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        RequestBody tenChuDe = RequestBody.create(MediaType.parse("multipart/form-data"), deckName);

        apiService.updateChuDeWithImage(deckId, body, tenChuDe).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(Call<BoTu> call, Response<BoTu> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(EditDeckActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditDeckActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
                resetButton();
            }

            @Override
            public void onFailure(Call<BoTu> call, Throwable t) {
                Log.e(TAG, "Upload image failed: " + t.getMessage());
                Toast.makeText(EditDeckActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = getFileName(uri);
            File tempFile = File.createTempFile("deck_upload_temp", getFileExtension(fileName), getCacheDir());
            OutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return ".jpg";
    }

    private void resetButton() {
        btnSaveChangesDeck.setText("Lưu thay đổi");
        btnSaveChangesDeck.setEnabled(true);
    }
}
