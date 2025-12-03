package com.nhom1.polydeck.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nhom1.polydeck.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LearningStatusManager {
    private static final String PREF_NAME = "PolyDeckLearningStatus";
    private static final String KEY_UNKNOWN_WORDS = "unknown_words_";
    private static final String KEY_KNOWN_WORDS = "known_words_";
    private static final String KEY_FLASHCARD_PROGRESS = "flashcard_progress_";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Gson gson;
    private String userId;

    public LearningStatusManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
        
        SessionManager sm = new SessionManager(context);
        if (sm.getUserData() != null) {
            userId = sm.getUserData().getId();
        }
    }

    /**
     * Lưu từ vào danh sách "chưa nhớ" của một deck
     */
    public void markAsUnknown(String deckId, String wordId) {
        if (userId == null || deckId == null || wordId == null) return;
        
        String key = KEY_UNKNOWN_WORDS + deckId + "_" + userId;
        Set<String> unknownWords = getUnknownWords(deckId);
        unknownWords.add(wordId);
        
        // Xóa khỏi danh sách "đã nhớ" nếu có
        Set<String> knownWords = getKnownWords(deckId);
        knownWords.remove(wordId);
        saveSet(key, unknownWords);
        saveSet(KEY_KNOWN_WORDS + deckId + "_" + userId, knownWords);
    }

    /**
     * Lưu từ vào danh sách "đã nhớ" của một deck
     */
    public void markAsKnown(String deckId, String wordId) {
        if (userId == null || deckId == null || wordId == null) return;
        
        String key = KEY_KNOWN_WORDS + deckId + "_" + userId;
        Set<String> knownWords = getKnownWords(deckId);
        knownWords.add(wordId);
        
        // Xóa khỏi danh sách "chưa nhớ" nếu có
        Set<String> unknownWords = getUnknownWords(deckId);
        unknownWords.remove(wordId);
        saveSet(key, knownWords);
        saveSet(KEY_UNKNOWN_WORDS + deckId + "_" + userId, unknownWords);
    }

    /**
     * Lấy danh sách ID các từ "chưa nhớ" của một deck
     */
    public Set<String> getUnknownWords(String deckId) {
        if (userId == null || deckId == null) return new HashSet<>();
        String key = KEY_UNKNOWN_WORDS + deckId + "_" + userId;
        return getSet(key);
    }

    /**
     * Lấy danh sách ID các từ "đã nhớ" của một deck
     */
    public Set<String> getKnownWords(String deckId) {
        if (userId == null || deckId == null) return new HashSet<>();
        String key = KEY_KNOWN_WORDS + deckId + "_" + userId;
        return getSet(key);
    }

    /**
     * Kiểm tra từ có trong danh sách "chưa nhớ" không
     */
    public boolean isUnknown(String deckId, String wordId) {
        return getUnknownWords(deckId).contains(wordId);
    }

    /**
     * Kiểm tra từ có trong danh sách "đã nhớ" không
     */
    public boolean isKnown(String deckId, String wordId) {
        return getKnownWords(deckId).contains(wordId);
    }

    /**
     * Xóa từ khỏi danh sách "chưa nhớ"
     */
    public void removeFromUnknown(String deckId, String wordId) {
        if (userId == null || deckId == null || wordId == null) return;
        Set<String> unknownWords = getUnknownWords(deckId);
        unknownWords.remove(wordId);
        saveSet(KEY_UNKNOWN_WORDS + deckId + "_" + userId, unknownWords);
    }

    /**
     * Xóa từ khỏi danh sách "đã nhớ"
     */
    public void removeFromKnown(String deckId, String wordId) {
        if (userId == null || deckId == null || wordId == null) return;
        Set<String> knownWords = getKnownWords(deckId);
        knownWords.remove(wordId);
        saveSet(KEY_KNOWN_WORDS + deckId + "_" + userId, knownWords);
    }

    /**
     * Xóa tất cả trạng thái học tập của một deck
     */
    public void clearDeckStatus(String deckId) {
        if (userId == null || deckId == null) return;
        editor.remove(KEY_UNKNOWN_WORDS + deckId + "_" + userId);
        editor.remove(KEY_KNOWN_WORDS + deckId + "_" + userId);
        editor.apply();
    }

    /**
     * Lấy số lượng từ "chưa nhớ" của một deck
     */
    public int getUnknownCount(String deckId) {
        return getUnknownWords(deckId).size();
    }

    /**
     * Lấy số lượng từ "đã nhớ" của một deck
     */
    public int getKnownCount(String deckId) {
        return getKnownWords(deckId).size();
    }

    // Helper methods
    private void saveSet(String key, Set<String> set) {
        String json = gson.toJson(set);
        editor.putString(key, json);
        editor.apply();
    }

    private Set<String> getSet(String key) {
        String json = pref.getString(key, null);
        if (json == null) return new HashSet<>();
        Type type = new TypeToken<HashSet<String>>(){}.getType();
        Set<String> set = gson.fromJson(json, type);
        return set != null ? set : new HashSet<>();
    }

    /**
     * Lưu tiến độ học flashcard (index hiện tại)
     */
    public void saveFlashcardProgress(String deckId, int index, boolean reviewUnknownOnly) {
        if (userId == null || deckId == null) return;
        String key = KEY_FLASHCARD_PROGRESS + deckId + "_" + userId;
        FlashcardProgress progress = new FlashcardProgress(index, reviewUnknownOnly);
        String json = gson.toJson(progress);
        editor.putString(key, json);
        editor.apply();
    }

    /**
     * Lấy tiến độ học flashcard đã lưu
     * @return FlashcardProgress hoặc null nếu chưa có
     */
    public FlashcardProgress getFlashcardProgress(String deckId) {
        if (userId == null || deckId == null) return null;
        String key = KEY_FLASHCARD_PROGRESS + deckId + "_" + userId;
        String json = pref.getString(key, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, FlashcardProgress.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Xóa tiến độ học flashcard
     */
    public void clearFlashcardProgress(String deckId) {
        if (userId == null || deckId == null) return;
        String key = KEY_FLASHCARD_PROGRESS + deckId + "_" + userId;
        editor.remove(key);
        editor.apply();
    }

    /**
     * Class để lưu tiến độ học flashcard
     */
    public static class FlashcardProgress {
        public int index;
        public boolean reviewUnknownOnly;

        public FlashcardProgress() {}

        public FlashcardProgress(int index, boolean reviewUnknownOnly) {
            this.index = index;
            this.reviewUnknownOnly = reviewUnknownOnly;
        }
    }
}

