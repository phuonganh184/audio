package com.example.utils

// Tập hợp hằng số cấu hình dùng chung cho toàn bộ ứng dụng Bạch Nhật Anh Audio
object AppConstants {
    const val DATABASE_NAME = "bach_nhat_anh_audio.db"

    // Các khóa cấu hình trong bảng app_settings
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_DEFAULT_SPEED = "default_speed"
    const val KEY_REWIND_SECONDS = "rewind_seconds"
    const val KEY_FORWARD_SECONDS = "forward_seconds"
    const val KEY_VOLUME_LEVEL = "volume_level"
    const val KEY_LAST_SCAN_FOLDER = "last_scan_folder"
    const val KEY_REPEAT_MODE = "repeat_mode"
    const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"

    // Các giá trị mặc định
    const val DEFAULT_DARK_MODE = true
    const val DEFAULT_SPEED = 1.0f
    const val DEFAULT_REWIND_SEC = 10
    const val DEFAULT_FORWARD_SEC = 30
    const val DEFAULT_VOLUME = 1.0f

    // Tên bộ truyện mặc định khi nhập file vào Thư viện tự do
    const val FREE_LIBRARY_TITLE = "Thư viện tự do"
    const val FREE_LIBRARY_CATEGORY = "Tự do"

    // Các mức tốc độ phát chuẩn hỗ trợ giữ nguyên cao độ giọng đọc (Pitch Correction)
    val PLAYBACK_SPEEDS = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    // Các tùy chọn bước nhảy tua (giây)
    val REWIND_OPTIONS = listOf(5, 10, 15, 20, 30)
    val FORWARD_OPTIONS = listOf(10, 15, 30, 45, 60)

    // Định dạng tệp âm thanh hỗ trợ quét local
    val SUPPORTED_AUDIO_EXTENSIONS = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac")

    // Danh mục thể loại truyện mặc định
    val DEFAULT_CATEGORIES = listOf(
        "Cổ phong & Kiếm hiệp",
        "Khoa học viễn tưởng",
        "Văn học & Tiểu thuyết",
        "Kỹ năng & Triết lý",
        "Trinh thám & Kỳ bí",
        "Tự do"
    )
}
