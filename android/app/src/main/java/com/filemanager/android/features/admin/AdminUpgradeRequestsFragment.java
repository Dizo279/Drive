package com.filemanager.android.features.admin;

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
import com.filemanager.android.network.dto.UpgradeRequestDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUpgradeRequestsFragment extends Fragment implements UpgradeRequestsAdapter.OnRequestActionListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerRequests;
    private View layoutEmpty;
    
    private UpgradeRequestsAdapter adapter;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_upgrade_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = ApiClient.getApiService(requireContext());
        
        swipeRefresh = view.findViewById(R.id.swipe_refresh_upgrade_requests);
        recyclerRequests = view.findViewById(R.id.recycler_upgrade_requests);
        layoutEmpty = view.findViewById(R.id.layout_empty_requests);
        
        adapter = new UpgradeRequestsAdapter(requireContext(), this);
        recyclerRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRequests.setAdapter(adapter);
        
        swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);
        apiService.getUpgradeRequests().enqueue(new Callback<List<UpgradeRequestDto>>() {
            @Override
            public void onResponse(Call<List<UpgradeRequestDto>> call, Response<List<UpgradeRequestDto>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<UpgradeRequestDto> requests = response.body();
                    adapter.setData(requests);
                    layoutEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(requireContext(), "Lỗi tải yêu cầu nâng cấp", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<UpgradeRequestDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onApprove(UpgradeRequestDto request) {
        processRequest(request.getId(), "APPROVED");
    }

    @Override
    public void onReject(UpgradeRequestDto request) {
        processRequest(request.getId(), "REJECTED");
    }
    
    private void processRequest(Long requestId, String action) {
        Map<String, String> body = new HashMap<>();
        body.put("action", action);
        
        apiService.processUpgradeRequest(requestId, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Đã xử lý yêu cầu", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(requireContext(), "Lỗi xử lý yêu cầu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
