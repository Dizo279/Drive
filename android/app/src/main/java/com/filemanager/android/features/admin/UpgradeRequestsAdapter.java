package com.filemanager.android.features.admin;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.filemanager.android.R;
import com.filemanager.android.network.dto.UpgradeRequestDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UpgradeRequestsAdapter extends RecyclerView.Adapter<UpgradeRequestsAdapter.ViewHolder> {

    private final Context context;
    private List<UpgradeRequestDto> requests = new ArrayList<>();
    private final OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onApprove(UpgradeRequestDto request);
        void onReject(UpgradeRequestDto request);
    }

    public UpgradeRequestsAdapter(Context context, OnRequestActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<UpgradeRequestDto> list) {
        this.requests = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_upgrade_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpgradeRequestDto request = requests.get(position);

        holder.tvName.setText(request.getUsername());
        holder.tvEmail.setText(request.getEmail());

        String timeAgo = "Vừa xong";
        if (request.getRequestedAt() != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = sdf.parse(request.getRequestedAt());
                if (date != null) {
                    timeAgo = (String) DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        holder.tvTime.setText(timeAgo);

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(request));
        holder.btnReject.setOnClickListener(v -> listener.onReject(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvTime;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_request_user_name);
            tvEmail = itemView.findViewById(R.id.tv_request_email);
            tvTime = itemView.findViewById(R.id.tv_request_time);
            btnApprove = itemView.findViewById(R.id.btn_approve_request);
            btnReject = itemView.findViewById(R.id.btn_reject_request);
        }
    }
}
