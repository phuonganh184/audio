package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Bảng 1: stories - Lưu thông tin bộ truyện / sách nói
@Entity(
    tableName = "stories",
    indices = [
        Index(value = ["title"]),
        Index(value = ["category"]),
        Index(value = ["is_favorite"])
    ]
)
data class StoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "narrator")
    val narrator: String,

    @ColumnInfo(name = "cover_image_path")
    val coverImagePath: String?,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Int = 0 // 0: Bình thường, 1: Yêu thích
)

// Bảng 2: chapters - Lưu danh sách chương thuộc từng bộ truyện và đường dẫn file local
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["story_id"]),
        Index(value = ["file_path"], unique = true),
        Index(value = ["is_favorite"])
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "story_id")
    val storyId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    @ColumnInfo(name = "file_size")
    val fileSize: Long,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Int = 0, // 0: Chưa hoàn thành, 1: Đã nghe >= 95%

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Int = 0 // 0: Bình thường, 1: Chương yêu thích
)

// Bảng 3: playback_progress - Lưu vị trí nghe dở chính xác từng mili-giây cho mỗi bộ truyện
@Entity(
    tableName = "playback_progress",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["story_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapter_id"]),
        Index(value = ["last_played_at"])
    ]
)
data class PlaybackProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "story_id")
    val storyId: Long,

    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,

    @ColumnInfo(name = "position_ms")
    val positionMs: Long,

    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long = System.currentTimeMillis()
)

// Bảng 4: bookmarks - Lưu các mốc đánh dấu đoạn hay kèm ghi chú của từng chương
@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapter_id"])]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,

    @ColumnInfo(name = "position_ms")
    val positionMs: Long,

    @ColumnInfo(name = "note")
    val note: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

// Bảng 5: playlists - Lưu danh sách phát tùy chỉnh của người dùng
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "description")
    val description: String
)

// Bảng 6: playlist_items - Bảng liên kết 1-N / N-N giữa Playlist và Chapter kèm thứ tự phát
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlist_id"]),
        Index(value = ["chapter_id"])
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,

    @ColumnInfo(name = "order_index")
    val orderIndex: Int
)

// Bảng 7: app_settings - Lưu cấu hình hệ thống (Theme, tốc độ mặc định, bước nhảy tua, âm lượng, thư mục quét...)
@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "setting_key")
    val settingKey: String,

    @ColumnInfo(name = "setting_value")
    val settingValue: String
)
