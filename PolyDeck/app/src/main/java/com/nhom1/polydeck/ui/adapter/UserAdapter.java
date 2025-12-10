package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.api.APIService;
import com.nhom1.polydeck.data.api.RetrofitClient;
import com.nhom1.polydeck.data.model.User;
import com.nhom1.polydeck.ui.activity.UserDetailActivity;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnUserStatusChangedListener {
        void onStatusChanged();
    }

    private final List<User> userList;
    private final Context context;
    private final APIService apiService;
    private OnUserStatusChangedListener statusChangedListener;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.apiService = RetrofitClient.getApiService();
    }
    
    public void setOnUserStatusChangedListener(OnUserStatusChangedListener listener) {
        this.statusChangedListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null) return;

        holder.tvUserName.setText(user.getHoTen());
        holder.tvEmail.setText(user.getEmail());
        holder.tvUserInfo.setText(String.format(Locale.getDefault(), "Level %d • %d XP", user.getLevel(), user.getXp()));

        Glide.with(context)
                .load(user.getLinkAnhDaiDien())
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.circle_purple)
                .error(R.drawable.circle_purple)
                .into(holder.ivAvatar);

        updateStatusUI(holder, user);

        holder.btnDetail.setOnClickListener(v -> {
            Intent intent = new Intent(context, UserDetailActivity.class);
            intent.putExtra(UserDetailActivity.EXTRA_USER_ID, user.getId());
            context.startActivity(intent);
        });

        holder.btnBlock.setOnClickListener(v -> showBlockConfirmationDialog(user, holder));
    }

    private void showBlockConfirmationDialog(User user, UserViewHolder holder) {
        String action = "active".equals(user.getTrangThai()) ? "khóa" : "mở khóa";
        new AlertDialog.Builder(context)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc chắn muốn " + action + " tài khoản '" + user.getHoTen() + "'?")
                .setPositiveButton(action.toUpperCase(), (dialog, which) -> toggleBlockStatus(user, holder))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toggleBlockStatus(User user, UserViewHolder holder) {
        apiService.blockUser(user.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    user.setTrangThai("active".equals(user.getTrangThai()) ? "banned" : "active");
                    updateStatusUI(holder, user);
                    Toast.makeText(context, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                    // FIX: Notify the activity that the status has changed
                    if (statusChangedListener != null) {
                        statusChangedListener.onStatusChanged();
                    }
                } else {
                    Toast.makeText(context, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(context, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusUI(UserViewHolder holder, User user) {
        if ("active".equalsIgnoreCase(user.getTrangThai())) {
            holder.tvStatus.setText(R.string.status_active);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_active_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            holder.btnBlock.setText("Khóa");
        } else {
            holder.tvStatus.setText(R.string.status_banned);
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_banned_text));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_banned);
            holder.btnBlock.setText("Mở khóa");
        }
    }


    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    public void updateData(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUserName, tvEmail, tvStatus, tvUserInfo;
        Button btnDetail, btnBlock;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvUserInfo = itemView.findViewById(R.id.tvUserInfo);
            btnDetail = itemView.findViewById(R.id.btnDetail);
            btnBlock = itemView.findViewById(R.id.btnBlock);
        }
    }
}
