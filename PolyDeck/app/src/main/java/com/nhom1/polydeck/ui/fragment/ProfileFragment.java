package com.nhom1.polydeck.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.LoginResponse;
import com.nhom1.polydeck.ui.activity.FavoritesActivity;
import com.nhom1.polydeck.ui.activity.LoginActivity;
import com.nhom1.polydeck.utils.SessionManager;

public class ProfileFragment extends Fragment {
    
    private TextView tvName, tvEmail, tvLevel, tvXp;
    private ProgressBar pb;
    private ImageView avatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        tvName = view.findViewById(R.id.tv_name);
        tvEmail = view.findViewById(R.id.tv_email);
        tvLevel = view.findViewById(R.id.tv_level);
        tvXp = view.findViewById(R.id.tv_xp);
        pb = view.findViewById(R.id.progress_xp);
        avatar = view.findViewById(R.id.img_avatar);
        
        loadUserData();

        view.findViewById(R.id.row_edit_profile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.nhom1.polydeck.ui.activity.EditProfileActivity.class)));
        view.findViewById(R.id.row_favorites).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), FavoritesActivity.class)));
        view.findViewById(R.id.row_settings).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.nhom1.polydeck.ui.activity.SettingsActivity.class)));
        view.findViewById(R.id.row_ranking).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.nhom1.polydeck.ui.activity.LeaderboardActivity.class)));
        view.findViewById(R.id.row_support).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), com.nhom1.polydeck.ui.activity.SupportActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning from EditProfileActivity
        loadUserData();
    }

    private void loadUserData() {
        SessionManager sm = new SessionManager(requireContext());
        LoginResponse user = sm.getUserData();

        if (user != null) {
            tvName.setText(user.getHoTen());
            tvEmail.setText(user.getEmail());
            int level = Math.max(1, user.getCapDo());
            int xp = Math.max(0, user.getDiemTichLuy());
            int next = Math.max(100, ((level + 1) * 400));
            tvLevel.setText("Level " + level);
            tvXp.setText(xp + " / " + next + " XP");
            int percent = Math.min(100, (int) Math.round((xp * 100.0) / next));
            pb.setProgress(percent);
            
            // Load avatar if exists
            if (user.getLinkAnhDaiDien() != null && !user.getLinkAnhDaiDien().isEmpty()) {
                com.bumptech.glide.Glide.with(requireContext())
                        .load(user.getLinkAnhDaiDien())
                        .error(R.drawable.circle_purple)
                        .into(avatar);
            }
        }
    }
}

