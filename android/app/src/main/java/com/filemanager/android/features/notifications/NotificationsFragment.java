package com.filemanager.android.features.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.NotificationDto;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment Thông báo.
 *
 * Tính năng:
 * - Tải danh sách thông báo (GET /api/notifications)
 * - Hiển thị icon và màu theo loại thông báo
 * - Làm nổi bật (bold + dot xanh) các thông báo chưa đọc
 * - Pull-to-refresh
 * - Nút "Đọc hết" để đánh dấu tất cả đã đọc (TODO: thêm API khi backend hỗ trợ)
 */
public class NotificationsFragment extends Fragment
        implements NotificationAdapter.OnNotifClickListener {

    private RecyclerView recyclerNotifications;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private MaterialButton btnMarkAllRead;

    private ApiService apiService;
    private NotificationAdapter notificationAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        apiService = ApiClient.getApiService(requireContext());

        setupRecyclerView();
        setupSwipeToDelete();
        setupSwipeRefresh();
        setupMarkAllRead();
        loadNotifications();
    }

    private void initViews(View view) {
        recyclerNotifications = view.findViewById(R.id.recycler_notifications);
        swipeRefresh          = view.findViewById(R.id.swipe_refresh_notifications);
        layoutEmpty           = view.findViewById(R.id.layout_empty_notifications);
        btnMarkAllRead        = view.findViewById(R.id.btn_mark_all_read);
    }

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(requireContext(), this);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerNotifications.setAdapter(notificationAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.apple_blue));
        swipeRefresh.setOnRefreshListener(this::loadNotifications);
    }

    private void setupMarkAllRead() {
        btnMarkAllRead.setOnClickListener(v -> {
            for (NotificationDto notif : notificationAdapter.getItems()) {
                if (!notif.isRead()) {
                    apiService.markNotificationRead(notif.getId()).enqueue(new Callback<okhttp3.ResponseBody>() {
                        @Override
                        public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {}
                        @Override
                        public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
                    });
                }
            }
            notificationAdapter.markAllRead();
            btnMarkAllRead.setVisibility(View.GONE);
            showToast("✅ Đã đánh dấu tất cả là đã đọc");
        });
    }

    private void setupSwipeToDelete() {
        androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback swipeCallback =
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.LEFT | androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationDto notif = notificationAdapter.getItem(position);
                
                apiService.deleteNotification(notif.getId()).enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                        if (response.isSuccessful()) {
                            notificationAdapter.removeItem(position);
                            toggleEmptyState(notificationAdapter.getItemCount() == 0);
                        } else {
                            notificationAdapter.notifyItemChanged(position); // Hoàn tác swipe
                            showToast("❌ Không thể xóa thông báo");
                        }
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        notificationAdapter.notifyItemChanged(position);
                        showToast(getString(R.string.err_network));
                    }
                });
            }
        };
        new androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerNotifications);
    }

    // ==================================================================
    // Load Notifications
    // ==================================================================

    private void loadNotifications() {
        swipeRefresh.setRefreshing(true);

        apiService.getNotifications().enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(Call<List<NotificationDto>> call,
                                   Response<List<NotificationDto>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationDto> items = response.body();
                    notificationAdapter.setData(items);
                    toggleEmptyState(items.isEmpty());

                    // Cập nhật badge nếu có thông báo chưa đọc
                    int unread = notificationAdapter.getUnreadCount();
                    if (unread > 0) {
                        btnMarkAllRead.setVisibility(View.VISIBLE);
                        btnMarkAllRead.setText("Đọc hết (" + unread + ")");
                    } else {
                        btnMarkAllRead.setVisibility(View.GONE);
                    }
                } else {
                    toggleEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<List<NotificationDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showToast(getString(R.string.err_network));
                toggleEmptyState(true);
            }
        });
    }

    // ==================================================================
    // OnNotifClickListener
    // ==================================================================

    @Override
    public void onClick(NotificationDto notification) {
        if (!notification.isRead()) {
            apiService.markNotificationRead(notification.getId()).enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        notification.setRead(true);
                        // Tìm và cập nhật lại adapter
                        int position = notificationAdapter.getItems().indexOf(notification);
                        if (position != -1) {
                            notificationAdapter.notifyItemChanged(position);
                        }
                        
                        // Cập nhật lại badge nút
                        int unread = notificationAdapter.getUnreadCount();
                        if (unread > 0) {
                            btnMarkAllRead.setVisibility(View.VISIBLE);
                            btnMarkAllRead.setText("Đọc hết (" + unread + ")");
                        } else {
                            btnMarkAllRead.setVisibility(View.GONE);
                        }
                    }
                }
                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {}
            });
        }

        // Điều hướng đến targetUrl nếu có
        if (notification.getTargetUrl() != null && !notification.getTargetUrl().isEmpty()) {
            if (notification.getTargetUrl().contains("/shared")) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                    requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_shared);
                }
            } else if (notification.getTargetUrl().contains("/trash")) {
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                    requireActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_trash);
                }
            }
        } else {
            // Hiển thị nội dung nếu không có URL điều hướng
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Thông báo")
                    .setMessage(notification.getMessage())
                    .setPositiveButton("Đóng", null)
                    .show();
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void toggleEmptyState(boolean isEmpty) {
        recyclerNotifications.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
