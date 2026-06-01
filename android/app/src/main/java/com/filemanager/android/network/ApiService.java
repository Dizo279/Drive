package com.filemanager.android.network;

import com.filemanager.android.network.dto.FileMetadataDto;
import com.filemanager.android.network.dto.LoginRequest;
import com.filemanager.android.network.dto.LoginResponse;
import com.filemanager.android.network.dto.NotificationDto;
import com.filemanager.android.network.dto.ProfileUpdateRequest;
import com.filemanager.android.network.dto.SharedItemDto;
import com.filemanager.android.network.dto.RegisterRequest;
import com.filemanager.android.network.dto.UpgradeRequestDto;
import com.filemanager.android.network.dto.UserDto;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface khai báo tất cả API endpoints của backend.
 * Retrofit sẽ tự generate implementation tại runtime.
 */
public interface ApiService {

    // =====================
    // AUTH — /api/auth
    // =====================

    /** Đăng nhập — trả về JWT token */
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    /** Đăng ký tài khoản mới */
    @POST("auth/register")
    Call<ResponseBody> register(@Body RegisterRequest request);


    // =====================
    // FILES — /api/files
    // =====================

    /** Lấy danh sách file/folder trong một thư mục (parentId = null → root) */
    @GET("files")
    Call<List<FileMetadataDto>> getFiles(@Query("parentId") Long parentId);

    /** Upload file (multipart). parentId truyền qua query để tránh lỗi parse multipart rỗng. */
    @Multipart
    @POST("files/upload")
    Call<FileMetadataDto> uploadFile(
            @Part MultipartBody.Part file,
            @Query("parentId") Long parentId
    );

    /** Download file theo ID */
    @GET("files/{id}/download")
    Call<ResponseBody> downloadFile(@Path("id") Long id);

    /** Lấy chi tiết 1 file */
    @GET("files/{id}")
    Call<FileMetadataDto> getFileDetail(@Path("id") Long id);

    /** Soft delete — chuyển file vào Trash */
    @DELETE("files/{id}")
    Call<ResponseBody> deleteFile(@Path("id") Long id);

    /** Khôi phục file từ Trash */
    @POST("files/{id}/restore")
    Call<ResponseBody> restoreFile(@Path("id") Long id);

    /** Xóa vĩnh viễn 1 file trong Trash */
    @DELETE("files/{id}/permanent")
    Call<ResponseBody> permanentlyDeleteFile(@Path("id") Long id);

    /** Tạo thư mục mới */
    @POST("files/folder")
    Call<FileMetadataDto> createFolder(@Body Map<String, Object> body);

    /** Đổi tên file/thư mục */
    @PUT("files/{id}/rename")
    Call<FileMetadataDto> renameFile(@Path("id") Long id, @Body Map<String, String> body);

    /** Lấy danh sách Trash */
    @GET("files/trash")
    Call<List<FileMetadataDto>> getTrash();

    /** Dọn sạch toàn bộ Trash */
    @DELETE("files/trash/empty")
    Call<ResponseBody> emptyTrash();

    /** Tạo share link */
    @POST("files/{id}/share")
    Call<ResponseBody> shareFile(@Path("id") Long id, @Body Map<String, Object> body);

    /** Download file qua share token (public) */
    @GET("files/shared/{token}")
    Call<ResponseBody> downloadSharedFile(@Path("token") String token);

    /** Lấy danh sách file tôi đã chia sẻ */
    @GET("files/list/shared-by-me")
    Call<List<SharedItemDto>> getFilesSharedByMe();

    /** Lấy danh sách file người khác chia sẻ cho tôi */
    @GET("files/list/shared-with-me")
    Call<List<SharedItemDto>> getFilesSharedWithMe();

    /** Thu hồi quyền chia sẻ */
    @DELETE("files/revoke-share/{shareId}")
    Call<ResponseBody> revokeShare(@Path("shareId") Long shareId);


    // =====================
    // USERS — /api/users
    // =====================

    /** Lấy thông tin profile */
    @GET("users/me")
    Call<UserDto> getMyProfile();

    /** Lấy thông tin quota */
    @GET("users/quota")
    Call<ResponseBody> getQuota();

    /** Yêu cầu nâng cấp lên Premium */
    @POST("users/upgrade-request")
    Call<ResponseBody> requestUpgrade();

    /** Cập nhật profile (fullName, avatar, username, email, password) */
    @PUT("users/profile")
    Call<UserDto> updateProfile(@Body ProfileUpdateRequest request);


    // =====================
    // NOTIFICATIONS — /api/notifications
    // =====================

    /** Lấy danh sách thông báo */
    @GET("notifications")
    Call<List<NotificationDto>> getNotifications();

    /** Đánh dấu thông báo đã đọc */
    @PUT("notifications/{id}/read")
    Call<ResponseBody> markNotificationRead(@Path("id") Long id);

    /** Xóa thông báo */
    @DELETE("notifications/{id}")
    Call<ResponseBody> deleteNotification(@Path("id") Long id);


    // =====================
    // ADMIN — /api/admin
    // =====================

    @GET("admin/stats")
    Call<Map<String, Object>> getAdminStats();

    @GET("admin/users")
    Call<List<UserDto>> getAdminUsers();

    @PUT("admin/users/{userId}/role")
    Call<ResponseBody> updateUserRole(@Path("userId") Long userId, @Body Map<String, String> body);

    @PUT("admin/users/{userId}/tier")
    Call<ResponseBody> updateUserTier(@Path("userId") Long userId, @Body Map<String, String> body);

    @DELETE("admin/users/{userId}")
    Call<ResponseBody> deleteAdminUser(@Path("userId") Long userId);

    @GET("admin/upgrade-requests")
    Call<List<UpgradeRequestDto>> getUpgradeRequests();

    @POST("admin/upgrade-requests/{id}/process")
    Call<ResponseBody> processUpgradeRequest(@Path("id") Long id, @Body Map<String, String> body);
}
