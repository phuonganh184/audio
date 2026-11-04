package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Bộ định dạng thời gian chuẩn cho trình phát Audiobook (mm:ss hoặc hh:mm:ss) và dung lượng file
object TimeFormatter {

    // Chuyển đổi mili-giây sang chuỗi mm:ss hoặc hh:mm:ss
    fun formatDuration(durationMs: Long): String {
        val safeMs = durationMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    // Chuyển đổi thời lượng sang dạng văn bản thân thiện (ví dụ: "1 giờ 25 phút" hoặc "12 phút 30 giây")
    fun formatReadableDuration(durationMs: Long): String {
        val safeMs = durationMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return when {
            hours > 0 && minutes > 0 -> "$hours giờ $minutes phút"
            hours > 0 -> "$hours giờ"
            minutes > 0 -> "$minutes phút ${seconds}s"
            else -> "$seconds giây"
        }
    }

    // Định dạng dung lượng bộ nhớ byte sang KB / MB / GB
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            else -> String.format(Locale.US, "%.1f KB", kb)
        }
    }

    // Định dạng mốc thời gian lịch sử (dd/MM/yyyy HH:mm)
    fun formatTimestamp(timestampMs: Long): String {
        if (timestampMs <= 0L) return "Chưa phát"
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(timestampMs))
    }
}
