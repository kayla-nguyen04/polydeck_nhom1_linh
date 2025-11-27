package com.nhom1.polydeck.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom1.polydeck.R;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS = "PolyDeckSettings";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_DARK = "dark_mode";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        Switch swSound = findViewById(R.id.sw_sound);
        Switch swDark = findViewById(R.id.sw_dark);

        swSound.setChecked(prefs.getBoolean(KEY_SOUND, true));
        swDark.setChecked(prefs.getBoolean(KEY_DARK, false));

        CompoundButton.OnCheckedChangeListener l = (buttonView, isChecked) -> {
            if (buttonView == swSound) {
                prefs.edit().putBoolean(KEY_SOUND, isChecked).apply();
            } else if (buttonView == swDark) {
                prefs.edit().putBoolean(KEY_DARK, isChecked).apply();
            }
        };
        swSound.setOnCheckedChangeListener(l);
        swDark.setOnCheckedChangeListener(l);
    }
}