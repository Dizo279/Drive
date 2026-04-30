package com.filemanager.android.features.trash;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.android.R;
import com.filemanager.android.network.dto.FileMetadataDto;
import com.filemanager.android.utils.DateUtils;
import com.filemanager.android.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho danh sách file trong Thùng rác.
 * Không có nút action trực tiếp — hành động qua swipe gesture.
 */
public class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.TrashViewHolder> {

    private final Context context;
    private List<FileMetadataDto> items;

    public TrashAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    /** Cập nhật toàn bộ danh sách */
    public void setData(List<FileMetadataDto> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Lấy item tại vị trí (dùng trong SwipeCallback) */
    public FileMetadataDto getItem(int position) {
        return items.get(position);
    }

    /** Xóa item sau khi swipe xử lý xong */
    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
        }
    }

    /** Xóa toàn bộ danh sách (sau empty trash) */
    public void clearAll() {
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_trash, parent, false);
        return new TrashViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class TrashViewHolder extends RecyclerView.ViewHolder {
        private final FrameLayout flIcon;
        private final TextView tvEmoji;
        private final TextView tvName;
        private final TextView tvDeletedAt;
        private final TextView tvSize;

        TrashViewHolder(@NonNull View itemView) {
            super(itemView);
            flIcon      = itemView.findViewById(R.id.fl_trash_icon);
            tvEmoji     = itemView.findViewById(R.id.tv_trash_emoji);
            tvName      = itemView.findViewById(R.id.tv_trash_name);
            tvDeletedAt = itemView.findViewById(R.id.tv_trash_deleted_at);
            tvSize      = itemView.findViewById(R.id.tv_trash_size);
        }

        void bind(FileMetadataDto file) {
            tvName.setText(file.getFileName());
            tvEmoji.setText(FileUtils.getFileEmoji(file.getMimeType(), file.isFolder()));

            // Icon màu mờ (alpha 25%)
            int iconColor = FileUtils.getFileIconColor(context, file.getMimeType(), file.isFolder());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(colorWithAlpha(iconColor, 40));
            flIcon.setBackground(circle);

            // Thời gian xóa
            String deletedTime = DateUtils.formatRelative(file.getDeletedAt());
            tvDeletedAt.setText("Đã xóa: " + deletedTime);

            // Kích thước
            tvSize.setText(file.isFolder() ? "Thư mục" : file.getFormattedSize());
        }

        private int colorWithAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
    }
}
