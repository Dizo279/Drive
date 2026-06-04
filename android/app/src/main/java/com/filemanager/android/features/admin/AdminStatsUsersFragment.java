package com.filemanager.android.features.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.AdminStatsDto;
import com.filemanager.android.network.dto.UserDto;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatsUsersFragment extends Fragment implements AdminUsersAdapter.OnUserActionListener {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvTotalUsers;
    private TextView tvStorageUsed;
    private TextInputEditText etSearch;
    private RecyclerView recyclerUsers;
    private View layoutEmpty;

    private AdminUsersAdapter adapter;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_stats_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = ApiClient.getApiService(requireContext());
        
        initViews(view);
        setupRecyclerView();
        setupSearch();
        
        swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh_admin_users);
        tvTotalUsers = view.findViewById(R.id.tv_stat_total_users);
        tvStorageUsed = view.findViewById(R.id.tv_stat_storage_used);
        etSearch = view.findViewById(R.id.et_search_users);
        recyclerUsers = view.findViewById(R.id.recycler_admin_users);
        layoutEmpty = view.findViewById(R.id.layout_empty_users);
    }

    private void setupRecyclerView() {
        adapter = new AdminUsersAdapter(requireContext(), this);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerUsers.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                layoutEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);
        loadStats();
        loadUsers();
    }

    private void loadStats() {
        apiService.getAdminStats().enqueue(new Callback<AdminStatsDto>() {
            @Override
            public void onResponse(Call<AdminStatsDto> call, Response<AdminStatsDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AdminStatsDto stats = response.body();
                    tvTotalUsers.setText(String.valueOf(stats.getTotalUsers()));
                    tvStorageUsed.setText(formatSize(stats.getTotalStorageUsed()));
                }
            }

            @Override
            public void onFailure(Call<AdminStatsDto> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void loadUsers() {
        apiService.getAdminUsers().enqueue(new Callback<List<UserDto>>() {
            @Override
            public void onResponse(Call<List<UserDto>> call, Response<List<UserDto>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<UserDto> users = response.body();
                    adapter.setData(users);
                    // Retain search filter if any
                    adapter.filter(etSearch.getText() != null ? etSearch.getText().toString() : "");
                    layoutEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(requireContext(), "Lỗi lấy danh sách user", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UserDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public void onEditRole(UserDto user) {
        String newRole = user.isAdmin() ? "USER" : "ADMIN";
        String message = user.isAdmin() ? "Hạ cấp quyền của " + user.getUsername() + " xuống USER?" 
                : "Nâng cấp quyền cho " + user.getUsername() + " lên ADMIN?";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi quyền")
                .setMessage(message)
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    Map<String, String> body = new HashMap<>();
                    body.put("role", newRole);
                    apiService.updateUserRole(user.getId(), body).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Đã đổi quyền thành công", Toast.LENGTH_SHORT).show();
                                loadData();
                            } else {
                                Toast.makeText(requireContext(), "Lỗi đổi quyền", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onEditTier(UserDto user) {
        String newTier = user.isPremium() ? "FREE" : "PREMIUM";
        String message = user.isPremium() ? "Hạ cấp gói của " + user.getUsername() + " xuống FREE?" 
                : "Nâng cấp gói cho " + user.getUsername() + " lên PREMIUM?";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi gói")
                .setMessage(message)
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    Map<String, String> body = new HashMap<>();
                    body.put("tier", newTier);
                    apiService.updateUserTier(user.getId(), body).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Đã đổi gói thành công", Toast.LENGTH_SHORT).show();
                                loadData();
                            } else {
                                Toast.makeText(requireContext(), "Lỗi đổi gói", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDeleteUser(UserDto user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa người dùng")
                .setMessage("Bạn có chắc chắn muốn xóa tài khoản " + user.getUsername() + "? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    apiService.deleteUser(user.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
                                loadData();
                            } else {
                                Toast.makeText(requireContext(), "Lỗi xóa người dùng", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
