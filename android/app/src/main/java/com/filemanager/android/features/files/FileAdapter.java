package com.filemanager.android.features.files;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
 * RecyclerView Adapter hiển thị danh sách file/folder.
 * Mỗi item hiển thị: icon màu (theo loại file), tên, kích thước + ngày.
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    /**
     * Interface callback cho các hành động người dùng trên từng item.
     */
    public interface OnFileActionListener {
        /** Nhấn vào item: vào thư mục hoặc mở file */
        void onFileClick(FileMetadataDto file);
        /** Nhấn vào nút ⋮ More Options */
        void onMoreOptionsClick(FileMetadataDto file);
    }

    private final Context context;
    private List<FileMetadataDto> files;
    private OnFileActionListener listener;

    public FileAdapter(Context context, OnFileActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.files = new ArrayList<>();
    }

    /** Cập nhật toàn bộ danh sách và refresh UI */
    public void setData(List<FileMetadataDto> newFiles) {
        this.files = newFiles != null ? newFiles : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Xóa 1 item khỏi danh sách (dùng sau khi API delete thành công) */
    public void removeItem(FileMetadataDto file) {
        int index = files.indexOf(file);
        if (index >= 0) {
            files.remove(index);
            notifyItemRemoved(index);
        }
    }

    /** Thêm item vào đầu danh sách (sau khi tạo folder/upload thành công) */
    public void addItem(FileMetadataDto file) {
        files.add(0, file);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileMetadataDto file = files.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    // ==========================
    // ViewHolder
    // ==========================
    class FileViewHolder extends RecyclerView.ViewHolder {

        private final FrameLayout flIconContainer;
        private final TextView tvEmoji;
        private final TextView tvFileName;
        private final TextView tvFileInfo;
        private final ImageButton btnMoreOptions;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            flIconContainer = itemView.findViewById(R.id.fl_icon_container);
            tvEmoji         = itemView.findViewById(R.id.tv_file_emoji);
            tvFileName      = itemView.findViewById(R.id.tv_file_name);
            tvFileInfo      = itemView.findViewById(R.id.tv_file_info);
            btnMoreOptions  = itemView.findViewById(R.id.btn_more_options);
        }

        void bind(FileMetadataDto file) {
            // Tên file
            tvFileName.setText(file.getFileName());

            // Icon emoji theo loại file
            tvEmoji.setText(FileUtils.getFileEmoji(file.getMimeType(), file.isFolder()));

            // Màu nền icon
            int iconColor = FileUtils.getFileIconColor(context, file.getMimeType(), file.isFolder());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            // Làm màu nhạt hơn (alpha ~25%) để nền trông tươi sáng
            circle.setColor(colorWithAlpha(iconColor, 40));
            flIconContainer.setBackground(circle);

            // Thông tin phụ: kích thước + thời gian
            String sizeText = file.isFolder() ? "Thư mục" : file.getFormattedSize();
            String dateText = DateUtils.formatRelative(file.getCreatedAt());
            tvFileInfo.setText(sizeText + "  •  " + dateText);

            // Click → mở/vào folder
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(file);
            });

            // More options
            btnMoreOptions.setOnClickListener(v -> {
                if (listener != null) listener.onMoreOptionsClick(file);
            });
        }

        /**
         * Áp dụng alpha lên màu (0 = trong suốt, 255 = đục).
         */
        private int colorWithAlpha(int color, int alpha) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
    }
}
