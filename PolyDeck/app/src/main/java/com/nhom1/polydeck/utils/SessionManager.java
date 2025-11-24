package com.nhom1.polydeck.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.nhom1.polydeck.data.model.LoginResponse;

public class SessionManager {
    private static final String PREF_NAME = "PolyDeckSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_DATA = "userData";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password"; // Lưu để auto login (optional)
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Gson gson;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
    }

    // Lưu thông tin user sau khi đăng nhập
    public void saveUserSession(LoginResponse user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_DATA, gson.toJson(user));
        editor.putString(KEY_EMAIL, user.getEmail());
        editor.apply();
    }

    // Lấy thông tin user
    public LoginResponse getUserData() {
        String userJson = pref.getString(KEY_USER_DATA, null);
        if (userJson != null) {
            return gson.fromJson(userJson, LoginResponse.class);
        }
        return null;
    }

    // Kiểm tra đã đăng nhập chưa
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Lấy email
    public String getEmail() {
        return pref.getString(KEY_EMAIL, null);
    }

    // Lấy vai trò
    public String getVaiTro() {
        LoginResponse user = getUserData();
        if (user != null) {
            return user.getVaiTro();
        }
        return null;
    }

    // Xóa session (đăng xuất)
    public void logout() {
        editor.clear();
        editor.apply();
    }

    // Lưu mật khẩu (optional - chỉ dùng nếu muốn auto login)
    public void savePassword(String password) {
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }

    // Lấy mật khẩu (optional)
    public String getPassword() {
        return pref.getString(KEY_PASSWORD, null);
    }
}




