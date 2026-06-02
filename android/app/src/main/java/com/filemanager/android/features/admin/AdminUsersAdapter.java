package com.filemanager.android.features.admin;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.filemanager.android.R;
import com.filemanager.android.network.dto.UserDto;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder> {

    private final Context context;
    private List<UserDto> userList = new ArrayList<>();
    private List<UserDto> filteredList = new ArrayList<>();
    private final OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEditRole(UserDto user);
        void onEditTier(UserDto user);
        void onDeleteUser(UserDto user);
    }

    public AdminUsersAdapter(Context context, OnUserActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setData(List<UserDto> list) {
        this.userList = list;
        this.filteredList = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        if (TextUtils.isEmpty(query)) {
            filteredList = new ArrayList<>(userList);
        } else {
            query = query.toLowerCase();
            filteredList = new ArrayList<>();
            for (UserDto u : userList) {
                boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(query);
                boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(query);
                boolean matchUsername = u.getUsername() != null && u.getUsername().toLowerCase().contains(query);
                if (matchName || matchEmail || matchUsername) {
                    filteredList.add(u);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserDto user = filteredList.get(position);

        String displayName = user.getFullName() != null && !user.getFullName().isEmpty() 
            ? user.getFullName() : user.getUsername();
        holder.tvName.setText(displayName);
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Không có email");

        // Avatar
        if (!TextUtils.isEmpty(user.getAvatarUrl())) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatarLetter.setVisibility(View.GONE);
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.bg_avatar_circle)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatarLetter.setVisibility(View.VISIBLE);
            String letter = displayName != null && !displayName.isEmpty() 
                ? String.valueOf(displayName.charAt(0)).toUpperCase() : "U";
            holder.tvAvatarLetter.setText(letter);
        }

        // Badges
        if (user.isAdmin()) {
            holder.tvRole.setVisibility(View.VISIBLE);
            holder.tvRole.setText("ADMIN");
        } else {
            holder.tvRole.setVisibility(View.GONE);
        }

        if (user.isPremium()) {
            holder.tvTier.setVisibility(View.VISIBLE);
            holder.tvTier.setText("PREMIUM");
        } else {
            holder.tvTier.setVisibility(View.GONE);
        }

        holder.btnMenu.setOnClickListener(v -> showPopupMenu(v, user));
    }

    private void showPopupMenu(View view, UserDto user) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.menu_admin_user, popup.getMenu());
        
        // Update menu items based on current status
        popup.getMenu().findItem(R.id.action_edit_role).setTitle(user.isAdmin() ? "Hạ xuống USER" : "Nâng cấp lên ADMIN");
        popup.getMenu().findItem(R.id.action_edit_tier).setTitle(user.isPremium() ? "Hạ xuống FREE" : "Nâng cấp lên PREMIUM");

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_role) {
                listener.onEditRole(user);
                return true;
            } else if (itemId == R.id.action_edit_tier) {
                listener.onEditTier(user);
                return true;
            } else if (itemId == R.id.action_delete_user) {
                listener.onDeleteUser(user);
                return true;
            }
            return false;
        });
        popup.show();
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvTier, tvAvatarLetter;
        ImageView ivAvatar;
        ImageButton btnMenu;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_admin_user_name);
            tvEmail = itemView.findViewById(R.id.tv_admin_user_email);
            tvRole = itemView.findViewById(R.id.tv_admin_user_role);
            tvTier = itemView.findViewById(R.id.tv_admin_user_tier);
            tvAvatarLetter = itemView.findViewById(R.id.tv_admin_user_avatar_letter);
            ivAvatar = itemView.findViewById(R.id.iv_admin_user_avatar);
            btnMenu = itemView.findViewById(R.id.btn_admin_user_menu);
        }
    }
}
