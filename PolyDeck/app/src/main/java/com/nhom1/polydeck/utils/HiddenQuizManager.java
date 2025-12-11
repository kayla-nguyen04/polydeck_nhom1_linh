package com.nhom1.polydeck.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class để quản lý danh sách quiz đã ẩn
 * Sử dụng SharedPreferences để lưu trữ danh sách ID các quiz đã ẩn
 */
public class HiddenQuizManager {
    private static final String PREF_NAME = "PolyDeckHiddenQuizzes";
    private static final String KEY_HIDDEN_QUIZ_IDS = "hidden_quiz_ids";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Gson gson;
    
    public HiddenQuizManager(Context context) {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
        this.gson = new Gson();
    }
    
    /**
     * Ẩn một quiz (thêm vào danh sách ẩn)
     */
    public void hideQuiz(String quizId) {
        if (quizId == null || quizId.isEmpty()) return;
        
        Set<String> hiddenIds = getHiddenQuizIds();
        hiddenIds.add(quizId);
        saveHiddenQuizIds(hiddenIds);
    }
    
    /**
     * Hiển thị lại một quiz (xóa khỏi danh sách ẩn)
     */
    public void unhideQuiz(String quizId) {
        if (quizId == null || quizId.isEmpty()) return;
        
        Set<String> hiddenIds = getHiddenQuizIds();
        hiddenIds.remove(quizId);
        saveHiddenQuizIds(hiddenIds);
    }
    
    /**
     * Kiểm tra xem một quiz có đang bị ẩn không
     */
    public boolean isQuizHidden(String quizId) {
        if (quizId == null || quizId.isEmpty()) return false;
        
        Set<String> hiddenIds = getHiddenQuizIds();
        return hiddenIds.contains(quizId);
    }
    
    /**
     * Lấy danh sách ID các quiz đã ẩn
     */
    public Set<String> getHiddenQuizIds() {
        String json = pref.getString(KEY_HIDDEN_QUIZ_IDS, null);
        if (json == null) {
            return new HashSet<>();
        }
        
        try {
            Type type = new TypeToken<Set<String>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            return new HashSet<>();
        }
    }
    
    /**
     * Lưu danh sách ID các quiz đã ẩn
     */
    private void saveHiddenQuizIds(Set<String> hiddenIds) {
        String json = gson.toJson(hiddenIds);
        editor.putString(KEY_HIDDEN_QUIZ_IDS, json);
        editor.apply();
    }
    
    /**
     * Xóa tất cả quiz khỏi danh sách ẩn (hiển thị lại tất cả)
     */
    public void clearAllHiddenQuizzes() {
        editor.remove(KEY_HIDDEN_QUIZ_IDS);
        editor.apply();
    }
}

