package com.nhom1.polydeck.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.utils.SessionManager;

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

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "EditProfileActivity";

    private EditText etName, etEmail;
    private TextView tvInitials, tvChangeAvatar;
    private ImageView ivAvatar;
    private Button btnSave;

    private APIService api;
    private String userId;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        api = RetrofitClient.getApiService();
        SessionManager sm = new SessionManager(this);
        LoginResponse user = sm.getUserData();
        if (user != null) {
            userId = user.getId();
        }

        tvInitials = findViewById(R.id.tv_initials);
        ivAvatar = findViewById(R.id.iv_avatar);
        tvChangeAvatar = findViewById(R.id.tv_change_avatar);
        etName = findViewById(R.id.et_fullname);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);

        if (user != null) {
            etName.setText(user.getHoTen());
            etEmail.setText(user.getEmail());
            tvInitials.setText(makeInitials(user.getHoTen()));
            
            // Load avatar if exists
            if (user.getLinkAnhDaiDien() != null && !user.getLinkAnhDaiDien().isEmpty()) {
                Glide.with(this)
                        .load(user.getLinkAnhDaiDien())
                        .error(R.drawable.bg_header_rounded)
                        .into(ivAvatar);
                ivAvatar.setVisibility(android.view.View.VISIBLE);
                tvInitials.setVisibility(android.view.View.GONE);
            } else {
                ivAvatar.setVisibility(android.view.View.GONE);
                tvInitials.setVisibility(android.view.View.VISIBLE);
            }
        }

        tvChangeAvatar.setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivAvatar.setImageURI(imageUri);
            ivAvatar.setVisibility(android.view.View.VISIBLE);
            tvInitials.setVisibility(android.view.View.GONE);
        }
    }

    private void saveChanges() {
        if (userId == null) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // If image is selected, upload it first
        if (imageUri != null) {
            uploadAvatar();
            return;
        }

        // Otherwise, just update name
        User u = new User();
        u.setId(userId);
        u.setHoTen(etName.getText().toString().trim());
        u.setEmail(etEmail.getText().toString().trim());
        api.updateUser(userId, u).enqueue(new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    // Update session with new data
                    SessionManager sm = new SessionManager(EditProfileActivity.this);
                    LoginResponse loginData = sm.getUserData();
                    if (loginData != null) {
                        loginData.setHoTen(etName.getText().toString().trim());
                        loginData.setEmail(etEmail.getText().toString().trim());
                        sm.saveUserSession(loginData);
                    }
                    Toast.makeText(EditProfileActivity.this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Không thể cập nhật", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAvatar() {
        btnSave.setText("Đang tải lên...");
        btnSave.setEnabled(false);

        File file = createTempFileFromUri(imageUri);
        if (file == null) {
            Toast.makeText(this, "Không thể tạo tệp từ ảnh đã chọn", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        String mimeType = getContentResolver().getType(imageUri);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        api.uploadUserAvatar(userId, body).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Update session with new avatar URL
                    User updatedUser = response.body();
                    SessionManager sm = new SessionManager(EditProfileActivity.this);
                    LoginResponse loginData = sm.getUserData();
                    if (loginData != null && updatedUser.getLinkAnhDaiDien() != null) {
                        loginData.setLinkAnhDaiDien(updatedUser.getLinkAnhDaiDien());
                        sm.saveUserSession(loginData);
                    }
                    // After uploading avatar, update name
                    updateUserName();
                } else {
                    String errorMsg = "Không thể tải ảnh lên";
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    Toast.makeText(EditProfileActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    resetButton();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Log.e(TAG, "Upload avatar failed: " + t.getMessage());
                Toast.makeText(EditProfileActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void updateUserName() {
        User u = new User();
        u.setId(userId);
        u.setHoTen(etName.getText().toString().trim());
        u.setEmail(etEmail.getText().toString().trim());
        api.updateUser(userId, u).enqueue(new Callback<User>() {
            @Override public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful()) {
                    // Update session with new data
                    SessionManager sm = new SessionManager(EditProfileActivity.this);
                    LoginResponse loginData = sm.getUserData();
                    if (loginData != null) {
                        loginData.setHoTen(etName.getText().toString().trim());
                        loginData.setEmail(etEmail.getText().toString().trim());
                        sm.saveUserSession(loginData);
                    }
                    Toast.makeText(EditProfileActivity.this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Đã tải ảnh nhưng không thể cập nhật tên", Toast.LENGTH_SHORT).show();
                }
                resetButton();
            }
            @Override public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Đã tải ảnh nhưng lỗi cập nhật tên", Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = getFileName(uri);
            File tempFile = File.createTempFile("avatar_temp", getFileExtension(fileName), getCacheDir());
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
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
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
        btnSave.setText("Lưu thay đổi");
        btnSave.setEnabled(true);
    }

    private String makeInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "NA";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0,1) + parts[parts.length-1].substring(0,1)).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }
}



