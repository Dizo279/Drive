package com.filemanager.android.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.filemanager.android.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/**
 * Các hàm tiện ích xử lý file: icon, MIME type, đọc/ghi file.
 */
public class FileUtils {

    // --- Phân loại MIME type ---
    public static final String TYPE_FOLDER  = "folder";
    public static final String TYPE_IMAGE   = "image";
    public static final String TYPE_VIDEO   = "video";
    public static final String TYPE_AUDIO   = "audio";
    public static final String TYPE_PDF     = "pdf";
    public static final String TYPE_DOC     = "doc";
    public static final String TYPE_ARCHIVE = "archive";
    public static final String TYPE_OTHER   = "other";

    /**
     * Xác định loại file từ MIME type để chọn icon/màu phù hợp.
     */
    public static String getFileCategory(String mimeType, boolean isFolder) {
        if (isFolder) return TYPE_FOLDER;
        if (mimeType == null) return TYPE_OTHER;

        String m = mimeType.toLowerCase();
        if (m.startsWith("image/")) return TYPE_IMAGE;
        if (m.startsWith("video/")) return TYPE_VIDEO;
        if (m.startsWith("audio/")) return TYPE_AUDIO;
        if (m.equals("application/pdf")) return TYPE_PDF;
        if (m.contains("document") || m.contains("word") || m.contains("text/")) return TYPE_DOC;
        if (m.contains("zip") || m.contains("rar") || m.contains("archive") || m.contains("compressed")) return TYPE_ARCHIVE;
        return TYPE_OTHER;
    }

    /**
     * Trả về màu nền (color resource id) cho icon file theo loại.
     */
    public static int getFileIconColor(Context ctx, String mimeType, boolean isFolder) {
        String category = getFileCategory(mimeType, isFolder);
        switch (category) {
            case TYPE_FOLDER:  return ctx.getColor(R.color.file_type_folder);
            case TYPE_IMAGE:   return ctx.getColor(R.color.file_type_image);
            case TYPE_VIDEO:   return ctx.getColor(R.color.file_type_video);
            case TYPE_AUDIO:   return ctx.getColor(R.color.file_type_audio);
            case TYPE_PDF:
            case TYPE_DOC:     return ctx.getColor(R.color.file_type_doc);
            case TYPE_ARCHIVE: return ctx.getColor(R.color.apple_orange);
            default:           return ctx.getColor(R.color.file_type_other);
        }
    }

    /**
     * Trả về ký tự emoji đại diện cho loại file (hiển thị trong icon).
     */
    public static String getFileEmoji(String mimeType, boolean isFolder) {
        String category = getFileCategory(mimeType, isFolder);
        switch (category) {
            case TYPE_FOLDER:  return "📁";
            case TYPE_IMAGE:   return "🖼";
            case TYPE_VIDEO:   return "🎬";
            case TYPE_AUDIO:   return "🎵";
            case TYPE_PDF:     return "📕";
            case TYPE_DOC:     return "📄";
            case TYPE_ARCHIVE: return "📦";
            default:           return "📎";
        }
    }

    /**
     * Lấy tên file từ Uri (cho file picker).
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "unknown_file";
    }

    /**
     * Lấy MIME type từ Uri.
     */
    public static String getMimeType(Context context, Uri uri) {
        ContentResolver cr = context.getContentResolver();
        String mimeType = cr.getType(uri);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * Lưu ResponseBody (download từ API) vào thư mục Downloads.
     * @return File đã lưu, hoặc null nếu thất bại
     */
    public static File saveToDownloads(Context context, ResponseBody body, String fileName)
            throws IOException {
        // Thư mục Downloads của app (không cần quyền WRITE_EXTERNAL_STORAGE từ Android 10+)
        File downloadsDir = context.getExternalFilesDir(
                android.os.Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) {
            downloadsDir = context.getFilesDir();
        }

        File outputFile = new File(downloadsDir, fileName);

        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        return outputFile;
    }

    /**
     * Format bytes sang chuỗi dễ đọc (B, KB, MB, GB).
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
