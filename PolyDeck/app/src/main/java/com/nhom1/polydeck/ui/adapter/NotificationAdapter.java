package com.nhom1.polydeck.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nhom1.polydeck.R;
import com.nhom1.polydeck.data.model.ThongBao;

import java.util.List;

public class NotificationAdapter extends BaseAdapter {
    private Context context;
    private List<ThongBao> notifications;

    public NotificationAdapter(Context context, List<ThongBao> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    @Override
    public int getCount() {
        return notifications.size();
    }

    @Override
    public Object getItem(int position) {
        return notifications.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
            holder = new ViewHolder();
            holder.tvTitle = convertView.findViewById(R.id.tvTitle);
            holder.dotUnread = convertView.findViewById(R.id.dotUnread);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ThongBao notification = notifications.get(position);
        holder.tvTitle.setText(notification.getTieuDe());
        
        // Hiển thị dấu chấm đỏ nếu chưa đọc
        if (notification.getDaDoc() == null || !notification.getDaDoc()) {
            holder.dotUnread.setVisibility(View.VISIBLE);
        } else {
            holder.dotUnread.setVisibility(View.GONE);
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvTitle;
        View dotUnread;
    }
}

