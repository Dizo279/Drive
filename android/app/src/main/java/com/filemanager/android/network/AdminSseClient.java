package com.filemanager.android.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.filemanager.android.storage.SessionManager;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class AdminSseClient {
    private static final String TAG = "AdminSseClient";
    private EventSource eventSource;
    private final Context context;

    public AdminSseClient(Context context) {
        this.context = context;
    }

    public void startListening() {
        if (eventSource != null) {
            return; // Already listening
        }

        String token = SessionManager.getInstance(context).getToken();
        if (token == null) return;

        OkHttpClient client = ApiClient.getRetrofit(context).callFactory() instanceof OkHttpClient
                ? (OkHttpClient) ApiClient.getRetrofit(context).callFactory()
                : new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url(ApiClient.BASE_URL + "admin/sse/notifications")
                .header("Authorization", "Bearer " + token)
                .header("Accept", "text/event-stream")
                .build();

        EventSource.Factory factory = EventSources.createFactory(client);
        eventSource = factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(EventSource eventSource, Response response) {
                Log.d(TAG, "SSE Connection Opened");
            }

            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                Log.d(TAG, "SSE Event Received: " + data);
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        String message = jsonObject.optString("message", "Có thông báo mới!");
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(context, data, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onClosed(EventSource eventSource) {
                Log.d(TAG, "SSE Connection Closed");
                AdminSseClient.this.eventSource = null;
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                Log.e(TAG, "SSE Connection Failed", t);
                AdminSseClient.this.eventSource = null;
            }
        });
    }

    public void stopListening() {
        if (eventSource != null) {
            eventSource.cancel();
            eventSource = null;
        }
    }
}
