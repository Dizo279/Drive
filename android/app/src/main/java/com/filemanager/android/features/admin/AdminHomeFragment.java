package com.filemanager.android.features.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.utils.FileUtils;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminHomeFragment extends Fragment {

    private TextView tvStatsUsers;
    private TextView tvStatsStorage;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvStatsUsers = view.findViewById(R.id.tv_stats_users);
        tvStatsStorage = view.findViewById(R.id.tv_stats_storage);
        apiService = ApiClient.getApiService(requireContext());
        loadStats();
    }

    private void loadStats() {
        apiService.getAdminStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> stats = response.body();
                    tvStatsUsers.setText(getString(R.string.admin_stats_users,
                            stats.get("totalUsers"), stats.get("totalAdmins")));
                    Object storage = stats.get("totalStorageUsed");
                    long bytes = storage instanceof Number ? ((Number) storage).longValue() : 0L;
                    tvStatsStorage.setText(getString(R.string.admin_stats_storage,
                            FileUtils.formatSize(bytes)));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
