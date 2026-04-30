package com.filemanager.android.network;

import com.filemanager.android.storage.SessionManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor tự động gắn JWT token vào header Authorization
 * cho tất cả API request yêu cầu xác thực.
 */
public class AuthInterceptor implements Interceptor {

    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = sessionManager.getToken();

        // Nếu chưa có token (chưa đăng nhập), gửi request gốc
        if (token == null || token.isEmpty()) {
            return chain.proceed(originalRequest);
        }

        // Gắn Bearer token vào header
        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        Response response = chain.proceed(authenticatedRequest);

        // Nếu server trả về 401 (token hết hạn/không hợp lệ)
        // ActivityHelper.redirectToLogin() sẽ được gọi từ Activity khi nhận được lỗi này
        if (response.code() == 401) {
            sessionManager.clearSession();
        }

        return response;
    }
}
