package com.filemanager.android.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tiện ích định dạng ngày giờ cho giao diện người dùng.
 */
public class DateUtils {

    // Backend trả về format ISO 8601
    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String ISO_FORMAT_NANO = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";

    /**
     * Định dạng chuỗi ngày từ backend sang dạng thân thiện.
     * Ví dụ: "2025-04-15T10:30:00" → "15/04/2025 10:30"
     */
    public static String formatDateTime(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "—";

        // Cắt nano-seconds nếu có
        String dateStr = isoDateString;
        if (dateStr.length() > 19) {
            dateStr = dateStr.substring(0, 19);
        }

        try {
            SimpleDateFormat parser = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
            Date date = parser.parse(dateStr);
            if (date == null) return dateStr;

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return formatter.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    /**
     * Định dạng chỉ ngày (không giờ).
     * Ví dụ: "2025-04-15T10:30:00" → "15 Th4 2025"
     */
    public static String formatDate(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "—";

        String dateStr = isoDateString.length() > 19
                ? isoDateString.substring(0, 19) : isoDateString;

        try {
            SimpleDateFormat parser = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
            Date date = parser.parse(dateStr);
            if (date == null) return dateStr;

            SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", new Locale("vi", "VN"));
            return formatter.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    /**
     * Trả về chuỗi thời gian tương đối.
     * Ví dụ: "vừa xong", "5 phút trước", "hôm qua", "15/04/2025"
     */
    public static String formatRelative(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "—";

        String dateStr = isoDateString.length() > 19
                ? isoDateString.substring(0, 19) : isoDateString;

        try {
            SimpleDateFormat parser = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
            Date date = parser.parse(dateStr);
            if (date == null) return dateStr;

            long diffMs = System.currentTimeMillis() - date.getTime();
            long diffMin  = diffMs / 60000;
            long diffHour = diffMin / 60;
            long diffDay  = diffHour / 24;

            if (diffMin < 1)   return "vừa xong";
            if (diffMin < 60)  return diffMin + " phút trước";
            if (diffHour < 24) return diffHour + " giờ trước";
            if (diffDay == 1)  return "hôm qua";
            if (diffDay < 7)   return diffDay + " ngày trước";

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return formatter.format(date);

        } catch (ParseException e) {
            return dateStr;
        }
    }
}
