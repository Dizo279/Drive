package com.filemanager.android.features.shared;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.android.R;
import com.filemanager.android.network.dto.SharedItemDto;
import com.filemanager.android.utils.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho danh sách file chia sẻ.
 * Dùng chung cho 2 tab: Tôi đã chia sẻ (isByMe=true) / Chia sẻ với tôi (isByMe=false).
 */
public class SharedItemAdapter extends RecyclerView.Adapter<SharedItemAdapter.SharedViewHolder> {

    public interface OnSharedActionListener {
        void onRevoke(SharedItemDto item);       // Thu hồi quyền chia sẻ
        void onDownload(SharedItemDto item);     // Tải file về
    }

    private final Context context;
    private List<SharedItemDto> items;
    private boolean isByMe;         // true = "Tôi đã chia sẻ", false = "Chia sẻ với tôi"
    private OnSharedActionListener listener;

    public SharedItemAdapter(Context context, boolean isByMe, OnSharedActionListener listener) {
        this.context = context;
        this.isByMe = isByMe;
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    /** Cập nhật dữ liệu và chế độ hiển thị */
    public void setData(List<SharedItemDto> newItems, boolean isByMe) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        this.isByMe = isByMe;
        notifyDataSetChanged();
    }

    /** Xóa item sau khi revoke thành công */
    public void removeItem(SharedItemDto item) {
        int index = items.indexOf(item);
        if (index >= 0) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }

    @NonNull
    @Override
    public SharedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_shared, parent, false);
        return new SharedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SharedViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class SharedViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvFileName;
        private final TextView tvTarget;
        private final TextView tvExpires;
        private final MaterialButton btnRevoke;
        private final MaterialButton btnDownload;

        SharedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName  = itemView.findViewById(R.id.tv_shared_file_name);
            tvTarget    = itemView.findViewById(R.id.tv_shared_target);
            tvExpires   = itemView.findViewById(R.id.tv_shared_expires);
            btnRevoke   = itemView.findViewById(R.id.btn_revoke);
            btnDownload = itemView.findViewById(R.id.btn_download_shared);
        }

        void bind(SharedItemDto item) {
            tvFileName.setText(item.getFileName() != null ? item.getFileName() : "—");

            // Target: email người nhận (by-me) hoặc "Public link" (with-me)
            if (isByMe) {
                String target = item.getTargetEmail() != null
                        ? item.getTargetEmail() : "Public link";
                tvTarget.setText(target);
            } else {
                tvTarget.setText("Chia sẻ bởi: " + (item.getTargetEmail() != null
                        ? item.getTargetEmail() : "—"));
            }

            // Hết hạn
            if (item.getExpiresAt() != null && !item.getExpiresAt().isEmpty()) {
                tvExpires.setText("Hết hạn: " + DateUtils.formatDate(item.getExpiresAt()));
            } else {
                tvExpires.setText("Không giới hạn thời gian");
            }

            // Nút tương ứng với tab
            if (isByMe) {
                btnRevoke.setVisibility(View.VISIBLE);
                btnDownload.setVisibility(View.GONE);
                btnRevoke.setOnClickListener(v -> {
                    if (listener != null) listener.onRevoke(item);
                });
            } else {
                btnRevoke.setVisibility(View.GONE);
                btnDownload.setVisibility(View.VISIBLE);
                btnDownload.setOnClickListener(v -> {
                    if (listener != null) listener.onDownload(item);
                });
            }
        }
    }
}
