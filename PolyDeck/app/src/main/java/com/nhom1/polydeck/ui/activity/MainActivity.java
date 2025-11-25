package com.nhom1.polydeck.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.ui.fragment.HomeFragment;
import com.nhom1.polydeck.ui.fragment.ProfileFragment;
import com.nhom1.polydeck.ui.fragment.StatsFragment;
import com.nhom1.polydeck.ui.fragment.TopicsFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                return insets;
            });
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
                Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                lp.bottomMargin = sb.bottom; // lift the bar above system nav
                v.setLayoutParams(lp);
                return insets;
            });

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    switchFragment(new HomeFragment());
                    return true;
                } else if (id == R.id.nav_topics) {
                    switchFragment(new TopicsFragment());
                    return true;
                } else if (id == R.id.nav_stats) {
                    switchFragment(new StatsFragment());
                    return true;
                } else if (id == R.id.nav_profile) {
                    switchFragment(new ProfileFragment());
                    return true;
                }
                return false;
            });
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}