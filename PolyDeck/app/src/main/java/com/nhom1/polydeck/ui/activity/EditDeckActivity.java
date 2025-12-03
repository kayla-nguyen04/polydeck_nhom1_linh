package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.List;

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
    private final List<BoTu> existingDecks = new ArrayList<>();

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
        
        // Load existing decks to check for duplicates
        loadExistingDecks();

        btnSaveChangesDeck.setOnClickListener(v -> saveChanges());
        btnChangeImage.setOnClickListener(v -> openFileChooser());
    }
    
    private void loadExistingDecks() {
        apiService.getAllChuDe().enqueue(new Callback<List<BoTu>>() {
            @Override
            public void onResponse(@NonNull Call<List<BoTu>> call, @NonNull Response<List<BoTu>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    existingDecks.clear();
                    existingDecks.addAll(response.body());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<List<BoTu>> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to load existing decks: " + t.getMessage());
            }
        });
    }
    
    private boolean isDeckNameDuplicate(String deckName) {
        for (BoTu deck : existingDecks) {
            // Skip the current deck being edited
            if (deck.getId() != null && deck.getId().equals(deckId)) {
                continue;
            }
            if (deck.getTenChuDe() != null && deck.getTenChuDe().equalsIgnoreCase(deckName.trim())) {
                return true;
            }
        }
        return false;
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchDeckDetails() {
        apiService.getChuDeDetail(deckId).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentDeck = response.body();
                    populateDeckData(currentDeck);
                } else {
                    Toast.makeText(EditDeckActivity.this, "Không thể tải thông tin bộ từ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                Log.e(TAG, "API Error: " + t.getMessage());
            }
        });
    }

    private void populateDeckData(BoTu deck) {
        etEditDeckName.setText(deck.getTenChuDe());
        String iconUrl = deck.getLinkAnhIcon();
        Log.d(TAG, "Populating deck data - Icon URL: [" + iconUrl + "]");
        
        if (iconUrl != null && !iconUrl.isEmpty() && !iconUrl.equals("null") && !iconUrl.equalsIgnoreCase("null")) {
            // If URL doesn't start with http, prepend base URL
            String fullUrl = iconUrl;
            if (!iconUrl.startsWith("http://") && !iconUrl.startsWith("https://")) {
                String baseUrl = "http://10.0.2.2:3000";
                if (iconUrl.startsWith("/")) {
                    fullUrl = baseUrl + iconUrl;
                } else {
                    fullUrl = baseUrl + "/" + iconUrl;
                }
            }
            Log.d(TAG, "Loading icon from: " + fullUrl);
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_default_deck_icon)
                    .error(R.drawable.ic_default_deck_icon)
                    .centerCrop()
                    .into(ivEditIconPreview);
        } else {
            Log.w(TAG, "Icon URL is null/empty, using default icon");
            Glide.with(this)
                    .load(R.drawable.ic_default_deck_icon)
                    .into(ivEditIconPreview);
        }
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST); // Deprecated but still needed for compatibility
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
        
        // Check for duplicate deck name (excluding current deck)
        if (isDeckNameDuplicate(newDeckName)) {
            etEditDeckName.setError("Tên bộ từ đã tồn tại");
            etEditDeckName.requestFocus();
            Toast.makeText(this, "Tên bộ từ \"" + newDeckName + "\" đã tồn tại. Vui lòng chọn tên khác.", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (deckId == null || deckId.isEmpty()) {
            Toast.makeText(this, "ID bộ từ không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable button to prevent multiple clicks
        btnSaveChangesDeck.setEnabled(false);
        btnSaveChangesDeck.setText(getString(R.string.processing));
        
        // If image is selected, upload it first
        if (newImageUri != null) {
            uploadDeckWithImage(newDeckName);
            return;
        }

        // Create a new BoTu object with only the fields we want to update
        BoTu updateDeck = new BoTu();
        updateDeck.setId(deckId);
        updateDeck.setTenChuDe(newDeckName);
        // Keep the existing icon link if available
        if (currentDeck != null && currentDeck.getLinkAnhIcon() != null) {
            updateDeck.setLinkAnhIcon(currentDeck.getLinkAnhIcon());
        }

        Log.d(TAG, "Updating deck with ID: " + deckId + ", Name: " + newDeckName + ", Icon: " + updateDeck.getLinkAnhIcon());
        apiService.updateChuDe(deckId, updateDeck).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BoTu result = response.body();
                    Log.d(TAG, "Update response - ID: " + result.getId() + ", Name: " + result.getTenChuDe() + ", Icon: " + result.getLinkAnhIcon());
                    Toast.makeText(EditDeckActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMessage = "Cập nhật thất bại";
                    okhttp3.ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            String errorBodyString = errorBody.string();
                            Log.e(TAG, "Update failed - Error body: " + errorBodyString);
                            // Check if error is about duplicate name
                            if (errorBodyString.toLowerCase().contains("đã tồn tại") || 
                                errorBodyString.toLowerCase().contains("already exists") ||
                                errorBodyString.toLowerCase().contains("duplicate") ||
                                response.code() == 409) {
                                errorMessage = "Tên bộ từ \"" + newDeckName + "\" đã tồn tại. Vui lòng chọn tên khác.";
                                etEditDeckName.setError("Tên đã tồn tại");
                                etEditDeckName.requestFocus();
                            } else {
                                errorMessage = "Cập nhật thất bại: " + response.code();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        } finally {
                            errorBody.close();
                        }
                    }
                    Toast.makeText(EditDeckActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    resetButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                 Log.e(TAG, "API Error: " + t.getMessage(), t);
                 Log.e(TAG, "Request URL: " + call.request().url());
                 Toast.makeText(EditDeckActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                 resetButton();
            }
        });
    }

    private void uploadDeckWithImage(String deckName) {
        btnSaveChangesDeck.setText(getString(R.string.uploading));
        btnSaveChangesDeck.setEnabled(false);

        File file = createTempFileFromUri(newImageUri);
        if (file == null) {
            Toast.makeText(this, "Không thể tạo tệp từ ảnh đã chọn", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        String mimeType = getContentResolver().getType(newImageUri);
        MediaType mediaType = mimeType != null ? MediaType.parse(mimeType) : MediaType.parse("image/*");
        RequestBody requestFile = RequestBody.create(mediaType, file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Tạo RequestBody cho id và ten_chu_de
        RequestBody idRequestBody = RequestBody.create(MediaType.parse("text/plain"), deckId);
        RequestBody tenChuDe = RequestBody.create(MediaType.parse("text/plain"), deckName);

        Log.d(TAG, "Updating deck with image - ID: " + deckId + ", Name: " + deckName);
        apiService.updateChuDeWithImage(idRequestBody, body, tenChuDe).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BoTu updatedDeck = response.body();
                    Log.d(TAG, "Update response - ID: " + updatedDeck.getId() + ", Name: " + updatedDeck.getTenChuDe() + ", Icon: " + updatedDeck.getLinkAnhIcon());
                    
                    // Check if the response has the correct ID (not a new deck)
                    if (updatedDeck.getId() != null && updatedDeck.getId().equals(deckId)) {
                        // Same ID, update was successful
                        if (updatedDeck.getLinkAnhIcon() != null && !updatedDeck.getLinkAnhIcon().isEmpty()) {
                            Log.d(TAG, "Update successful with icon: " + updatedDeck.getLinkAnhIcon());
                            Toast.makeText(EditDeckActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            // Image uploaded but link not returned, fetch again to verify
                            Log.w(TAG, "Image uploaded but link_anh_icon is null in response, fetching deck again...");
                            apiService.getChuDeDetail(deckId).enqueue(new Callback<BoTu>() {
                                @Override
                                public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        BoTu fetched = response.body();
                                        Log.d(TAG, "Fetched deck after upload - Icon: " + fetched.getLinkAnhIcon());
                                        if (fetched.getLinkAnhIcon() == null || fetched.getLinkAnhIcon().isEmpty()) {
                                            Log.e(TAG, "Icon still null after fetching! Server may not have saved it.");
                                        }
                                    }
                                }
                                
                                @Override
                                public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                                    Log.e(TAG, "Failed to fetch deck after upload", t);
                                }
                            });
                            Toast.makeText(EditDeckActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        // Different ID - server created a new deck instead of updating
                        // Workaround: Get the image link from the new deck and update the old deck with it
                        Log.w(TAG, "Server created new deck (ID: " + updatedDeck.getId() + ") instead of updating. Getting image link and updating old deck.");
                        
                        String imageLink = updatedDeck.getLinkAnhIcon();
                        // If image link is not in response, try to fetch the new deck to get it
                        if (imageLink == null || imageLink.isEmpty()) {
                            Log.w(TAG, "Image link not in response, fetching new deck to get link...");
                            apiService.getChuDeDetail(updatedDeck.getId()).enqueue(new Callback<BoTu>() {
                                @Override
                                public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        BoTu fetchedDeck = response.body();
                                        String fetchedLink = fetchedDeck.getLinkAnhIcon();
                                        if (fetchedLink != null && !fetchedLink.isEmpty()) {
                                            updateDeckWithImageLink(deckId, deckName, fetchedLink, updatedDeck.getId());
                                        } else {
                                            Log.e(TAG, "Image link still null after fetching deck");
                                            Toast.makeText(EditDeckActivity.this, "Lỗi: Không thể lấy link ảnh từ server", Toast.LENGTH_LONG).show();
                                            resetButton();
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to fetch new deck");
                                        Toast.makeText(EditDeckActivity.this, "Lỗi: Không thể lấy thông tin deck mới", Toast.LENGTH_LONG).show();
                                        resetButton();
                                    }
                                }
                                
                                @Override
                                public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                                    Log.e(TAG, "Error fetching new deck: " + t.getMessage());
                                    Toast.makeText(EditDeckActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    resetButton();
                                }
                            });
                            return;
                        }
                        
                        // Image link is available, update the original deck
                        updateDeckWithImageLink(deckId, deckName, imageLink, updatedDeck.getId());
                    }
                } else {
                    String errorMessage = "Cập nhật thất bại";
                    okhttp3.ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            String errorBodyString = errorBody.string();
                            Log.e(TAG, "Update with image failed - Error body: " + errorBodyString);
                            // Check if error is about duplicate name
                            if (errorBodyString.toLowerCase().contains("đã tồn tại") ||
                                errorBodyString.toLowerCase().contains("already exists") ||
                                errorBodyString.toLowerCase().contains("duplicate") ||
                                response.code() == 409) {
                                errorMessage = "Tên bộ từ \"" + deckName + "\" đã tồn tại. Vui lòng chọn tên khác.";
                                etEditDeckName.setError("Tên đã tồn tại");
                                etEditDeckName.requestFocus();
                            } else {
                                errorMessage = "Cập nhật thất bại: " + response.code();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        } finally {
                            errorBody.close();
                        }
                    }
                    Toast.makeText(EditDeckActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
                resetButton();
            }

            @Override
            public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                Log.e(TAG, "Upload image failed: " + t.getMessage(), t);
                Log.e(TAG, "Request URL: " + call.request().url());
                Toast.makeText(EditDeckActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void updateDeckWithImageLink(String deckId, String deckName, String imageLink, String newDeckIdToDelete) {
        // Update the original deck with the image link and name
        BoTu updateDeck = new BoTu();
        updateDeck.setId(deckId);
        updateDeck.setTenChuDe(deckName);
        updateDeck.setLinkAnhIcon(imageLink);
        
        // Also set other required fields to ensure update works
        if (currentDeck != null) {
            updateDeck.setSoLuongQuiz(currentDeck.getSoLuongQuiz());
            updateDeck.setSoNguoiDung(currentDeck.getSoNguoiDung());
        }
        
        Log.d(TAG, "Updating deck with image link: " + imageLink);
        Log.d(TAG, "Update payload - ID: " + deckId + ", Name: " + deckName + ", Icon: " + imageLink);
        apiService.updateChuDe(deckId, updateDeck).enqueue(new Callback<BoTu>() {
            @Override
            public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BoTu result = response.body();
                    Log.d(TAG, "Update successful - ID: " + result.getId() + ", Name: " + result.getTenChuDe() + ", Icon: " + result.getLinkAnhIcon());
                    
                    // Verify icon was saved - if not, fetch again
                    if (result.getLinkAnhIcon() == null || result.getLinkAnhIcon().isEmpty()) {
                        Log.w(TAG, "Icon not in update response, fetching deck again...");
                        apiService.getChuDeDetail(deckId).enqueue(new Callback<BoTu>() {
                            @Override
                            public void onResponse(@NonNull Call<BoTu> call, @NonNull Response<BoTu> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    BoTu fetched = response.body();
                                    Log.d(TAG, "Fetched deck after update - Icon: " + fetched.getLinkAnhIcon());
                                }
                            }
                            
                            @Override
                            public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                                Log.e(TAG, "Failed to fetch deck after update", t);
                            }
                        });
                    }
                    
                    // Delete the accidentally created deck
                    if (newDeckIdToDelete != null && !newDeckIdToDelete.isEmpty()) {
                        apiService.deleteChuDe(newDeckIdToDelete).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                                Log.d(TAG, "Deleted accidentally created deck: " + newDeckIdToDelete);
                            }
                            
                            @Override
                            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                                Log.w(TAG, "Failed to delete accidentally created deck: " + newDeckIdToDelete);
                            }
                        });
                    }
                    
                    Toast.makeText(EditDeckActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Log.e(TAG, "Update failed - Code: " + response.code());
                    Toast.makeText(EditDeckActivity.this, "Cập nhật tên thành công nhưng ảnh có thể chưa được lưu", Toast.LENGTH_LONG).show();
                    resetButton();
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<BoTu> call, @NonNull Throwable t) {
                Log.e(TAG, "Failed to update deck with image link: " + t.getMessage());
                Toast.makeText(EditDeckActivity.this, "Lỗi khi cập nhật: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "Error creating temp file from URI", e);
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
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
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                if (cut != -1) {
                    result = path.substring(cut + 1);
                } else {
                    result = path;
                }
            }
        }
        return result != null ? result : "image.jpg";
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return ".jpg";
    }

    private void resetButton() {
        btnSaveChangesDeck.setText(getString(R.string.save_changes));
        btnSaveChangesDeck.setEnabled(true);
    }
}
