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
import com.nhom1.polydeck.ui.activity.LoginActivity;
import com.nhom1.polydeck.utils.SessionManager;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionManager sm = new SessionManager(requireContext());
        LoginResponse user = sm.getUserData();

        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        TextView tvLevel = view.findViewById(R.id.tv_level);
        TextView tvXp = view.findViewById(R.id.tv_xp);
        ProgressBar pb = view.findViewById(R.id.progress_xp);
        ImageView avatar = view.findViewById(R.id.img_avatar);

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
        }

        view.findViewById(R.id.row_edit_profile).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Chức năng chỉnh sửa sẽ sớm có", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.row_favorites).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Từ yêu thích", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.row_settings).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.nhom1.polydeck.ui.activity.SettingsActivity.class);
            startActivity(intent);
        });
        view.findViewById(R.id.row_ranking).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Bảng xếp hạng", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.row_support).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Hỗ trợ", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.row_logout).setOnClickListener(v -> {
            sm.logout();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }
}

