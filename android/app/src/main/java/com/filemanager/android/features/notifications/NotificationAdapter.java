package com.filemanager.android.features.notifications;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.android.R;
import com.filemanager.android.network.dto.NotificationDto;
import com.filemanager.android.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho danh sách thông báo.
 * Hiển thị icon theo loại thông báo, bold nếu chưa đọc, hiển thị dot chưa đọc.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotifViewHolder> {

    public interface OnNotifClickListener {
        void onClick(NotificationDto notification);
    }

    private final Context context;
    private List<NotificationDto> items;
    private OnNotifClickListener listener;

    public NotificationAdapter(Context context, OnNotifClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    public void setData(List<NotificationDto> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Đánh dấu tất cả đã đọc (cập nhật UI, không gọi API — để Fragment xử lý) */
    public void markAllRead() {
        for (NotificationDto item : items) {
            // Không có setter trong DTO → tạo local flag
        }
        notifyDataSetChanged();
    }

    /** Đếm số thông báo chưa đọc */
    public int getUnreadCount() {
        int count = 0;
        for (NotificationDto item : items) {
            if (!item.isRead()) count++;
        }
        return count;
    }

    @NonNull
    @Override
    public NotifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new NotifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotifViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ======================
    // ViewHolder
    // ======================
    class NotifViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout flIcon;
        private final TextView tvIcon;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final View viewUnreadDot;

        NotifViewHolder(@NonNull View itemView) {
            super(itemView);
            flIcon       = itemView.findViewById(R.id.fl_notif_icon);
            tvIcon       = itemView.findViewById(R.id.tv_notif_icon);
            tvMessage    = itemView.findViewById(R.id.tv_notif_message);
            tvTime       = itemView.findViewById(R.id.tv_notif_time);
            viewUnreadDot = itemView.findViewById(R.id.view_unread_dot);
        }

        void bind(NotificationDto notif) {
            // Message
            tvMessage.setText(notif.getMessage());

            // Bold nếu chưa đọc
            tvMessage.setTypeface(null, notif.isRead()
                    ? android.graphics.Typeface.NORMAL
                    : android.graphics.Typeface.BOLD);

            // Thời gian
            tvTime.setText(DateUtils.formatRelative(notif.getCreatedAt()));

            // Icon và màu nền theo loại thông báo
            NotifConfig config = getConfigForType(notif.getType());
            tvIcon.setText(config.emoji);

            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(colorWithAlpha(config.color, 40));
            flIcon.setBackground(circle);

            // Unread dot
            viewUnreadDot.setVisibility(notif.isRead() ? View.GONE : View.VISIBLE);

            // Click handler
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(notif);
            });
        }

        private int colorWithAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }

        /**
         * Cấu hình icon/màu dựa theo loại thông báo từ backend.
         * Các type: FILE_SHARED, UPGRADE_REQUEST, SYSTEM, DOWNLOAD, etc.
         */
        private NotifConfig getConfigForType(String type) {
            if (type == null) return new NotifConfig("🔔", context.getColor(R.color.apple_gray));

            switch (type.toUpperCase()) {
                case "FILE_SHARED":
                    return new NotifConfig("🔗", context.getColor(R.color.apple_blue));
                case "UPGRADE_REQUEST":
                case "UPGRADE_APPROVED":
                    return new NotifConfig("⭐", context.getColor(R.color.apple_orange));
                case "FILE_DELETED":
                    return new NotifConfig("🗑️", context.getColor(R.color.apple_red));
                case "SYSTEM":
                    return new NotifConfig("📢", context.getColor(R.color.apple_gray));
                case "DOWNLOAD":
                    return new NotifConfig("⬇️", context.getColor(R.color.apple_green));
                default:
                    return new NotifConfig("🔔", context.getColor(R.color.apple_blue));
            }
        }
    }

    /** Cấu hình icon và màu cho mỗi loại thông báo */
    private static class NotifConfig {
        final String emoji;
        final int color;

        NotifConfig(String emoji, int color) {
            this.emoji = emoji;
            this.color = color;
        }
    }
}
