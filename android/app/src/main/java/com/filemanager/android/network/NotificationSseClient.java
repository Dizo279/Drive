package com.filemanager.android.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.filemanager.android.storage.SessionManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

/**
 * Kết nối SSE thông báo real-time (GET /api/notifications/stream?token=...).
 * Dùng OkHttp streaming — không cần thư viện okhttp-sse riêng.
 */
public class NotificationSseClient {

    public interface Listener {
        void onNotificationEvent();
    }

    private static NotificationSseClient instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executor;
    private Call activeCall;
    private Listener listener;

    public static NotificationSseClient getInstance() {
        if (instance == null) {
            instance = new NotificationSseClient();
        }
        return instance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void connect(Context context) {
        disconnect();

        String token = SessionManager.getInstance(context).getToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8.name());
            String url = ApiClient.BASE_URL + "notifications/stream?token=" + encoded;

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .header("Accept", "text/event-stream")
                    .build();

            activeCall = client.newCall(request);
            executor = Executors.newSingleThreadExecutor();

            executor.execute(() -> {
                try (Response response = activeCall.execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        return;
                    }
                    readEventStream(response.body().source());
                } catch (IOException ignored) {
                    // Đóng kết nối / hủy call
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void readEventStream(BufferedSource source) throws IOException {
        while (activeCall != null && !activeCall.isCanceled()) {
            String line = source.readUtf8Line();
            if (line == null) {
                break;
            }
            if (line.startsWith("data:")) {
                notifyListener();
            }
        }
    }

    private void notifyListener() {
        if (listener == null) {
            return;
        }
        mainHandler.post(() -> {
            if (listener != null) {
                listener.onNotificationEvent();
            }
        });
    }

    public void disconnect() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
