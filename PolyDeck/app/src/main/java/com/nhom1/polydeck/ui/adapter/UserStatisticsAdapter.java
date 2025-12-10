package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserStatisticsAdapter extends RecyclerView.Adapter<UserStatisticsAdapter.UserViewHolder> {

    private final List<User> userList;
    private final Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public UserStatisticsAdapter(Context context) {
        this.context = context;
        this.userList = new ArrayList<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_statistics, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;

        holder.tvUserName.setText(user.getHoTen());
        holder.tvEmail.setText(user.getEmail());
        
        // Format ngày tham gia
        String joinDateText = "Ngày tham gia: ";
        if (user.getNgayThamGia() != null && !user.getNgayThamGia().isEmpty()) {
            try {
                SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = apiDateFormat.parse(user.getNgayThamGia());
                if (date != null) {
                    joinDateText += dateFormat.format(date);
                } else {
                    joinDateText += user.getNgayThamGia();
                }
            } catch (Exception e) {
                joinDateText += user.getNgayThamGia();
            }
        } else {
            joinDateText += "N/A";
        }
        holder.tvUserInfo.setText(joinDateText);

        // Load avatar
        Glide.with(context)
                .load(user.getLinkAnhDaiDien())
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.circle_purple)
                .error(R.drawable.circle_purple)
                .into(holder.ivAvatar);

        // Update status
        if ("active".equalsIgnoreCase(user.getTrangThai())) {
            holder.tvStatus.setText(R.string.status_active);
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_active_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
        } else {
            holder.tvStatus.setText(R.string.status_banned);
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.status_banned_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_banned);
        }
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateData(List<User> newList) {
        userList.clear();
        if (newList != null) {
            userList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName, tvEmail, tvStatus, tvUserInfo;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUserInfo = itemView.findViewById(R.id.tvUserInfo);
        }
    }
}

