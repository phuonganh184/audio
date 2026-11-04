package com.example.data.entity

import androidx.room.Embedded
import androidx.room.Relation

// Mô hình kết hợp Bộ truyện kèm toàn bộ danh sách Chương để tính tổng thời lượng và tiến độ
data class StoryWithChapters(
    @Embedded
    val story: StoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "story_id"
    )
    val chapters: List<ChapterEntity>
) {
    val sortedChapters: List<ChapterEntity>
        get() = chapters.sortedBy { it.orderIndex }

    val totalDurationMs: Long
        get() = chapters.sumOf { it.durationMs }

    val completedChapterCount: Int
        get() = chapters.count { it.isCompleted == 1 }
}

// Mô hình Chương kèm thông tin Bộ truyện cha (dùng cho Playlist, Yêu thích, Trình phát)
data class ChapterWithStory(
    @Embedded
    val chapter: ChapterEntity,

    @Relation(
        parentColumn = "story_id",
        entityColumn = "id"
    )
    val story: StoryEntity
)

// Mô hình Tiến trình nghe chi tiết kết hợp Story + Chapter (dùng cho khối "Nghe tiếp" & Lịch sử)
data class ProgressWithDetails(
    @Embedded
    val progress: PlaybackProgressEntity,

    @Relation(
        parentColumn = "story_id",
        entityColumn = "id"
    )
    val story: StoryEntity,

    @Relation(
        parentColumn = "chapter_id",
        entityColumn = "id"
    )
    val chapter: ChapterEntity
)

// Mô hình Mục trong Playlist kèm thông tin Chương và Bộ truyện
data class PlaylistItemWithDetails(
    @Embedded
    val item: PlaylistItemEntity,

    @Relation(
        entity = ChapterEntity::class,
        parentColumn = "chapter_id",
        entityColumn = "id"
    )
    val chapterWithStory: ChapterWithStory
)

// Mô hình Playlist đầy đủ kèm danh sách các mục bên trong
data class PlaylistWithItems(
    @Embedded
    val playlist: PlaylistEntity,

    @Relation(
        entity = PlaylistItemEntity::class,
        parentColumn = "id",
        entityColumn = "playlist_id"
    )
    val items: List<PlaylistItemWithDetails>
) {
    val sortedItems: List<PlaylistItemWithDetails>
        get() = items.sortedBy { it.item.orderIndex }

    val totalDurationMs: Long
        get() = items.sumOf { it.chapterWithStory.chapter.durationMs }
}

// Mô hình Bookmark kèm thông tin Chương & Truyện cho màn hình Lịch sử / Đánh dấu
data class BookmarkWithDetails(
    @Embedded
    val bookmark: BookmarkEntity,

    @Relation(
        entity = ChapterEntity::class,
        parentColumn = "chapter_id",
        entityColumn = "id"
    )
    val chapterWithStory: ChapterWithStory
)
