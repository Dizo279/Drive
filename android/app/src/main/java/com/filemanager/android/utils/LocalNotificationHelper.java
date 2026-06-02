package com.filemanager.android.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.filemanager.android.R;
import com.filemanager.android.network.dto.NotificationDto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class LocalNotificationHelper {

    private static final String PREF_NAME = "local_notifications_pref";
    private static final String KEY_NOTIFICATIONS = "saved_notifications";
    private static final String CHANNEL_ID = "share_channel_id";
    
    // Add local notification and trigger system notification
    public static void addShareNotification(Context context, String fileName, boolean isPublicLink) {
        // 1. Create a NotificationDto
        String message = isPublicLink ? "Đã tạo link chia sẻ cho file " + fileName 
                                      : "Đã chia sẻ file " + fileName + " thành công";
        
        String dateString = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
        
        NotificationDto dto = new NotificationDto(
                -System.currentTimeMillis(), // use negative ID to prevent collision with real API IDs
                "FILE_SHARED",
                message,
                null,
                false,
                dateString
        );

        // 2. Save to SharedPreferences
        saveLocalNotification(context, dto);

        // 3. Show System Push Notification
        showSystemNotification(context, "Chia sẻ thành công", message);
    }

    private static void saveLocalNotification(Context context, NotificationDto dto) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTIFICATIONS, "[]");
        
        Gson gson = new Gson();
        Type type = new TypeToken<List<NotificationDto>>(){}.getType();
        List<NotificationDto> currentList = gson.fromJson(json, type);
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        // Add to the top
        currentList.add(0, dto);
        
        // Save back
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(currentList)).apply();
    }

    public static List<NotificationDto> getLocalNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTIFICATIONS, "[]");
        
        Gson gson = new Gson();
        Type type = new TypeToken<List<NotificationDto>>(){}.getType();
        List<NotificationDto> currentList = gson.fromJson(json, type);
        
        return currentList != null ? currentList : new ArrayList<>();
    }
    
    public static void clearLocalNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_NOTIFICATIONS).apply();
    }

    public static void showSystemNotification(Context context, String title, String content) {
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nav_shared)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, do not show
                return;
            }
        }
        notificationManager.notify(new Random().nextInt(10000), builder.build());
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Chia sẻ File";
            String description = "Thông báo khi chia sẻ file thành công";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // --- SHARED ITEMS LOCAL STORAGE ---
    private static final String KEY_SHARED_ITEMS = "saved_shared_items";

    public static void saveLocalSharedItem(Context context, com.filemanager.android.network.dto.SharedItemDto dto) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SHARED_ITEMS, "[]");
        
        Gson gson = new Gson();
        Type type = new TypeToken<List<com.filemanager.android.network.dto.SharedItemDto>>(){}.getType();
        List<com.filemanager.android.network.dto.SharedItemDto> currentList = gson.fromJson(json, type);
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        // Add to the top
        currentList.add(0, dto);
        
        // Save back
        prefs.edit().putString(KEY_SHARED_ITEMS, gson.toJson(currentList)).apply();
    }

    public static List<com.filemanager.android.network.dto.SharedItemDto> getLocalSharedItems(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SHARED_ITEMS, "[]");
        
        Gson gson = new Gson();
        Type type = new TypeToken<List<com.filemanager.android.network.dto.SharedItemDto>>(){}.getType();
        List<com.filemanager.android.network.dto.SharedItemDto> currentList = gson.fromJson(json, type);
        
        return currentList != null ? currentList : new ArrayList<>();
    }
}
