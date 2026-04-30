package com.filemanager.android.features.trash;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.filemanager.android.R;
import com.filemanager.android.network.ApiClient;
import com.filemanager.android.network.ApiService;
import com.filemanager.android.network.dto.FileMetadataDto;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment Thùng rác.
 *
 * Tính năng:
 * - Hiển thị danh sách file đã xóa
 * - Vuốt phải (xanh) → Khôi phục file (POST /api/files/{id}/restore)
 * - Vuốt trái (đỏ) → Xóa vĩnh viễn (DELETE /api/files/{id}/permanent)
 * - Nút "Dọn sạch" → Empty Trash (DELETE /api/files/trash/empty)
 * - Pull-to-refresh
 */
public class TrashFragment extends Fragment {

    private RecyclerView recyclerTrash;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private MaterialButton btnEmptyTrash;

    private ApiService apiService;
    private TrashAdapter trashAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        apiService = ApiClient.getApiService(requireContext());

        setupRecyclerView();
        setupSwipeRefresh();
        setupEmptyTrashButton();
        loadTrash();
    }

    private void initViews(View view) {
        recyclerTrash  = view.findViewById(R.id.recycler_trash);
        swipeRefresh   = view.findViewById(R.id.swipe_refresh_trash);
        layoutEmpty    = view.findViewById(R.id.layout_empty_trash);
        btnEmptyTrash  = view.findViewById(R.id.btn_empty_trash);
    }

    private void setupRecyclerView() {
        trashAdapter = new TrashAdapter(requireContext());
        recyclerTrash.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTrash.setAdapter(trashAdapter);

        // Gắn SwipeCallback cho thao tác vuốt
        new ItemTouchHelper(createSwipeCallback()).attachToRecyclerView(recyclerTrash);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(requireContext().getColor(R.color.apple_blue));
        swipeRefresh.setOnRefreshListener(this::loadTrash);
    }

    private void setupEmptyTrashButton() {
        btnEmptyTrash.setOnClickListener(v -> confirmEmptyTrash());
    }

    // ==================================================================
    // Load Trash
    // ==================================================================

    private void loadTrash() {
        swipeRefresh.setRefreshing(true);

        apiService.getTrash().enqueue(new Callback<List<FileMetadataDto>>() {
            @Override
            public void onResponse(Call<List<FileMetadataDto>> call,
                                   Response<List<FileMetadataDto>> response) {
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<FileMetadataDto> items = response.body();
                    trashAdapter.setData(items);
                    toggleEmptyState(items.isEmpty());
                } else {
                    toggleEmptyState(true);
                }
            }

            @Override
            public void onFailure(Call<List<FileMetadataDto>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                showToast(getString(R.string.err_network));
                toggleEmptyState(true);
            }
        });
    }

    // ==================================================================
    // Swipe Callback — vuốt để Restore hoặc Xóa vĩnh viễn
    // ==================================================================

    private ItemTouchHelper.SimpleCallback createSwipeCallback() {
        return new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // Không hỗ trợ drag-and-drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                FileMetadataDto file = trashAdapter.getItem(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // ← Vuốt phải = Khôi phục
                    confirmRestore(file, position);
                } else {
                    // → Vuốt trái = Xóa vĩnh viễn
                    confirmPermanentDelete(file, position);
                }
            }

            /**
             * Vẽ nền màu và text gợi ý khi người dùng đang vuốt item.
             */
            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState,
                                    boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                Paint paint = new Paint();
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(40f);
                textPaint.setAntiAlias(true);

                if (dX > 0) {
                    // Vuốt PHẢI → nền xanh lá (Khôi phục)
                    paint.setColor(0xFF34C759); // apple_green
                    RectF bg = new RectF(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + dX, itemView.getBottom());
                    c.drawRoundRect(bg, 14f, 14f, paint);

                    // Text hint
                    c.drawText("↩ Khôi phục",
                            itemView.getLeft() + 36f,
                            itemView.getTop() + (float)(itemView.getHeight() / 2) + 14f,
                            textPaint);
                } else if (dX < 0) {
                    // Vuốt TRÁI → nền đỏ (Xóa vĩnh viễn)
                    paint.setColor(0xFFFF3B30); // apple_red
                    RectF bg = new RectF(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    c.drawRoundRect(bg, 14f, 14f, paint);

                    // Text hint
                    float textWidth = textPaint.measureText("Xóa vĩnh viễn ✕");
                    c.drawText("Xóa vĩnh viễn ✕",
                            itemView.getRight() - textWidth - 36f,
                            itemView.getTop() + (float)(itemView.getHeight() / 2) + 14f,
                            textPaint);
                }

                super.onChildDraw(c, recyclerView, viewHolder,
                        dX, dY, actionState, isCurrentlyActive);
            }
        };
    }

    // ==================================================================
    // Restore File
    // ==================================================================

    private void confirmRestore(FileMetadataDto file, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Khôi phục file")
                .setMessage("Khôi phục \"" + file.getFileName() + "\" về thư mục ban đầu?")
                .setPositiveButton("Khôi phục", (d, w) -> restoreFile(file, position))
                .setNegativeButton(getString(R.string.btn_cancel), (d, w) -> {
                    // Hoàn tác animation khi huỷ
                    trashAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(d -> trashAdapter.notifyItemChanged(position))
                .show();
    }

    private void restoreFile(FileMetadataDto file, int position) {
        apiService.restoreFile(file.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    trashAdapter.removeItem(position);
                    showToast("✅ Đã khôi phục: " + file.getFileName());
                    toggleEmptyState(trashAdapter.getItemCount() == 0);
                } else {
                    trashAdapter.notifyItemChanged(position);
                    showToast("❌ Không thể khôi phục");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                trashAdapter.notifyItemChanged(position);
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Permanent Delete
    // ==================================================================

    private void confirmPermanentDelete(FileMetadataDto file, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Xóa vĩnh viễn")
                .setMessage("Xóa vĩnh viễn \"" + file.getFileName() + "\"?\n\nHành động này không thể hoàn tác.")
                .setPositiveButton("Xóa vĩnh viễn", (d, w) -> permanentDelete(file, position))
                .setNegativeButton(getString(R.string.btn_cancel), (d, w) -> {
                    trashAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(d -> trashAdapter.notifyItemChanged(position))
                .show();
    }

    private void permanentDelete(FileMetadataDto file, int position) {
        apiService.permanentlyDeleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    trashAdapter.removeItem(position);
                    showToast("🗑️ Đã xóa vĩnh viễn: " + file.getFileName());
                    toggleEmptyState(trashAdapter.getItemCount() == 0);
                } else {
                    trashAdapter.notifyItemChanged(position);
                    showToast("❌ Không thể xóa");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                trashAdapter.notifyItemChanged(position);
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Empty Trash
    // ==================================================================

    private void confirmEmptyTrash() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Dọn sạch thùng rác")
                .setMessage("Xóa vĩnh viễn tất cả " + trashAdapter.getItemCount()
                        + " file trong thùng rác?\n\nHành động này không thể hoàn tác.")
                .setPositiveButton("Dọn sạch", (d, w) -> emptyTrash())
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void emptyTrash() {
        apiService.emptyTrash().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    trashAdapter.clearAll();
                    toggleEmptyState(true);
                    showToast("✅ Đã dọn sạch thùng rác");
                } else {
                    showToast("❌ Không thể dọn sạch");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast(getString(R.string.err_network));
            }
        });
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void toggleEmptyState(boolean isEmpty) {
        recyclerTrash.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        btnEmptyTrash.setEnabled(!isEmpty);
    }

    private void showToast(String msg) {
        if (getContext() != null)
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
