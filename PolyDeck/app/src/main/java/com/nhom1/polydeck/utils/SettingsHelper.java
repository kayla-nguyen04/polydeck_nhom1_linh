package com.nhom1.polydeck.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsHelper {
    private static final String PREFS_NAME = "PolyDeckSettings";
    private static final String KEY_SOUND = "sound_enabled";

    /**
     * Kiểm tra xem âm thanh có được bật không
     * @param context Context của ứng dụng
     * @return true nếu âm thanh được bật, false nếu tắt (mặc định là true)
     */
    public static boolean isSoundEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND, true);
    }
}

