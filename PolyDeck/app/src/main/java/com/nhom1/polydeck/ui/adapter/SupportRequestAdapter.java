package com.nhom1.polydeck.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.YeuCauHoTro;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SupportRequestAdapter extends RecyclerView.Adapter<SupportRequestAdapter.ViewHolder> {

    private final List<YeuCauHoTro> requests;
    private final android.content.Context context;
    private OnDeleteListener onDeleteListener;
    private OnItemClickListener onItemClickListener;

    public interface OnDeleteListener {
        void onDelete(YeuCauHoTro request, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(YeuCauHoTro request, int position);
    }

    public SupportRequestAdapter(List<YeuCauHoTro> requests, android.content.Context context) {
        this.requests = requests;
        this.context = context;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.onDeleteListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_support_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YeuCauHoTro request = requests.get(position);
        
        holder.tvSenderName.setText(request.getTenNguoiGui() != null ? request.getTenNguoiGui() : "Không có tên");
        holder.tvSenderEmail.setText(request.getEmailNguoiGui() != null ? request.getEmailNguoiGui() : "");
        holder.tvContent.setText(request.getNoiDung() != null ? request.getNoiDung() : "");
        
        // Format date
        if (request.getNgayGui() != null && !request.getNgayGui().isEmpty()) {
            try {
                Date date = request.getNgayGuiAsDate();
                if (date != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    holder.tvDate.setText(dateFormat.format(date));
                } else {
                    // If parsing fails, show the raw string
                    holder.tvDate.setText(request.getNgayGui());
                }
            } catch (Exception e) {
                holder.tvDate.setText(request.getNgayGui());
            }
        } else {
            holder.tvDate.setText("");
        }
        
        // Click listener for delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteListener != null) {
                onDeleteListener.onDelete(request, position);
            }
        });

        // Click listener for entire item to view details
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(request, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests != null ? requests.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvSenderEmail, tvContent, tvDate;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvSenderEmail = itemView.findViewById(R.id.tvSenderEmail);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}


