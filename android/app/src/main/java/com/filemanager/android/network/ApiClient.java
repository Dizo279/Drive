package com.filemanager.android.network;

import android.content.Context;

import com.filemanager.android.BuildConfig;
import com.filemanager.android.storage.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Singleton cung cấp instance Retrofit đã được cấu hình sẵn.
 *
 * BASE_URL:
 *   - Android Emulator: http://10.0.2.2:8080/api/
 *   - Thiết bị thật (LAN): http://192.168.1.x:8080/api/ (thay x bằng IP máy bạn)
 */
public class ApiClient {

    // ============================================================
    // ⚠️ THAY ĐỔI URL NÀY KHI TEST TRÊN THIẾT BỊ THẬT:
    // private static final String BASE_URL = "http://192.168.1.x:8080/api/";
    // ============================================================
    /** Đọc từ gradle.properties (API_BASE_URL); đổi IP khi test trên thiết bị thật. */
    public static final String BASE_URL = BuildConfig.API_BASE_URL;

    private static Retrofit retrofit;
    private static ApiService apiService;
    private static OkHttpClient httpClient;

    /**
     * Lấy instance ApiService đã được inject JWT interceptor.
     * @param context Context để khởi tạo SessionManager
     */
    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = getRetrofit(context).create(ApiService.class);
        }
        return apiService;
    }

    private static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = SessionManager.getInstance(context);

            // Logging interceptor (chỉ hiện trong debug)
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttpClient với Auth + Logging interceptors
            httpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(sessionManager))
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Reset singleton — dùng khi cần thay đổi URL (switch emulator ↔ device)
     */
    public static OkHttpClient getHttpClient(Context context) {
        if (httpClient == null) {
            getRetrofit(context);
        }
        return httpClient;
    }

    public static void reset() {
        retrofit = null;
        apiService = null;
        httpClient = null;
    }
}
