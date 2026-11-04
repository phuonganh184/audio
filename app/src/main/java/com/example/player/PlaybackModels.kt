package com.example.player

import com.example.data.entity.ChapterEntity
import com.example.data.entity.StoryEntity

// Chế độ lặp danh sách phát trong trình phát
enum class RepeatModeOption {
    OFF,
    ALL,
    ONE
}

// Một mục âm thanh trong hàng đợi phát kèm đầy đủ thông tin Chương & Bộ truyện
data class QueueTrack(
    val chapter: ChapterEntity,
    val story: StoryEntity
)

// Trạng thái Hẹn giờ tắt (Sleep Timer)
data class SleepTimerState(
    val isActive: Boolean = false,
    val endOfCurrentChapter: Boolean = false,
    val remainingMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val isFadingOut: Boolean = false
)

// Trạng thái đồng bộ thời gian thực cho cả Mini-Player và Màn hình Player chi tiết
data class PlayerUiState(
    val currentTrack: QueueTrack? = null,
    val queue: List<QueueTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val repeatMode: RepeatModeOption = RepeatModeOption.OFF,
    val shuffleEnabled: Boolean = false,
    val rewindSeconds: Int = 10,
    val forwardSeconds: Int = 30,
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val errorMessage: String? = null
) {
    val hasActiveTrack: Boolean
        get() = currentTrack != null

    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
