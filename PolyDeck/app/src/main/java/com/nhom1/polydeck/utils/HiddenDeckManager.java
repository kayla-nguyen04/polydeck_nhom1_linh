package com.nhom1.polydeck.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class để quản lý danh sách bộ từ đã ẩn
 * Sử dụng SharedPreferences để lưu trữ danh sách ID các bộ từ đã ẩn
 */
public class HiddenDeckManager {
    private static final String PREF_NAME = "PolyDeckHiddenDecks";
    private static final String KEY_HIDDEN_DECK_IDS = "hidden_deck_ids";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Gson gson;
    
    public HiddenDeckManager(Context context) {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.editor = pref.edit();
        this.gson = new Gson();
    }
    
    /**
     * Ẩn một bộ từ (thêm vào danh sách ẩn)
     */
    public void hideDeck(String deckId) {
        if (deckId == null || deckId.isEmpty()) return;
        
        Set<String> hiddenIds = getHiddenDeckIds();
        hiddenIds.add(deckId);
        saveHiddenDeckIds(hiddenIds);
    }
    
    /**
     * Hiển thị lại một bộ từ (xóa khỏi danh sách ẩn)
     */
    public void showDeck(String deckId) {
        if (deckId == null || deckId.isEmpty()) return;
        
        Set<String> hiddenIds = getHiddenDeckIds();
        hiddenIds.remove(deckId);
        saveHiddenDeckIds(hiddenIds);
    }
    
    /**
     * Kiểm tra xem một bộ từ có đang bị ẩn không
     */
    public boolean isDeckHidden(String deckId) {
        if (deckId == null || deckId.isEmpty()) return false;
        
        Set<String> hiddenIds = getHiddenDeckIds();
        return hiddenIds.contains(deckId);
    }
    
    /**
     * Lấy danh sách ID các bộ từ đã ẩn
     */
    public Set<String> getHiddenDeckIds() {
        String json = pref.getString(KEY_HIDDEN_DECK_IDS, null);
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
     * Lưu danh sách ID các bộ từ đã ẩn
     */
    private void saveHiddenDeckIds(Set<String> hiddenIds) {
        String json = gson.toJson(hiddenIds);
        editor.putString(KEY_HIDDEN_DECK_IDS, json);
        editor.apply();
    }
    
    /**
     * Xóa tất cả bộ từ khỏi danh sách ẩn (hiển thị lại tất cả)
     */
    public void clearAllHiddenDecks() {
        editor.remove(KEY_HIDDEN_DECK_IDS);
        editor.apply();
    }
}

