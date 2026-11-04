package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.BookmarkWithDetails
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterWithStory
import com.example.data.entity.PlaybackProgressEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import com.example.data.entity.PlaylistWithItems
import com.example.data.entity.ProgressWithDetails
import com.example.data.entity.StoryEntity
import com.example.data.entity.StoryWithChapters
import kotlinx.coroutines.flow.Flow

// DAO 1: Truy vấn bảng stories an toàn chống SQL Injection bằng Room Annotations
@Dao
interface StoryDao {
    @Transaction
    @Query("SELECT * FROM stories ORDER BY created_at DESC")
    fun observeAllStoriesWithChapters(): Flow<List<StoryWithChapters>>

    @Transaction
    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    fun observeStoryWithChapters(storyId: Long): Flow<StoryWithChapters?>

    @Transaction
    @Query("SELECT * FROM stories WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun observeFavoriteStories(): Flow<List<StoryWithChapters>>

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int

    @Query("SELECT * FROM stories WHERE title = :title LIMIT 1")
    suspend fun findStoryByTitle(title: String): StoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Query("UPDATE stories SET is_favorite = :isFavorite WHERE id = :storyId")
    suspend fun updateFavoriteStatus(storyId: Long, isFavorite: Int)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: Long)
}

// DAO 2: Truy vấn bảng chapters (chỉ thao tác siêu dữ liệu trong DB, tuyệt đối không xóa file vật lý)
@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY order_index ASC")
    fun observeChaptersByStory(storyId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE story_id = :storyId ORDER BY order_index ASC")
    suspend fun getChaptersByStoryOnce(storyId: Long): List<ChapterEntity>

    @Transaction
    @Query("SELECT * FROM chapters ORDER BY story_id ASC, order_index ASC")
    fun observeAllChaptersWithStory(): Flow<List<ChapterWithStory>>

    @Transaction
    @Query("SELECT * FROM chapters WHERE is_favorite = 1 ORDER BY id DESC")
    fun observeFavoriteChapters(): Flow<List<ChapterWithStory>>

    @Query("SELECT file_path FROM chapters")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT COALESCE(MAX(order_index), 0) FROM chapters WHERE story_id = :storyId")
    suspend fun getMaxOrderIndex(storyId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Query("UPDATE chapters SET is_completed = :isCompleted WHERE id = :chapterId")
    suspend fun updateCompletedStatus(chapterId: Long, isCompleted: Int)

    @Query("UPDATE chapters SET is_favorite = :isFavorite WHERE id = :chapterId")
    suspend fun updateChapterFavorite(chapterId: Long, isFavorite: Int)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteChapterById(chapterId: Long)
}

// DAO 3: Truy vấn bảng playback_progress phục vụ lưu từng giây & tính năng "Nghe tiếp"
@Dao
interface PlaybackProgressDao {
    @Transaction
    @Query("SELECT * FROM playback_progress ORDER BY last_played_at DESC")
    fun observeAllProgressHistory(): Flow<List<ProgressWithDetails>>

    @Transaction
    @Query("SELECT * FROM playback_progress ORDER BY last_played_at DESC LIMIT 1")
    fun observeLatestProgress(): Flow<ProgressWithDetails?>

    @Query("SELECT * FROM playback_progress WHERE story_id = :storyId LIMIT 1")
    suspend fun getProgressForStory(storyId: Long): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE story_id = :storyId")
    suspend fun deleteProgressByStory(storyId: Long)
}

// DAO 4: Truy vấn bảng bookmarks đánh dấu đoạn hay
@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE chapter_id = :chapterId ORDER BY position_ms ASC")
    fun observeBookmarksForChapter(chapterId: Long): Flow<List<BookmarkEntity>>

    @Transaction
    @Query("SELECT * FROM bookmarks ORDER BY created_at DESC")
    fun observeAllBookmarksWithDetails(): Flow<List<BookmarkWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmarkById(bookmarkId: Long)
}

// DAO 5 & 6: Truy vấn bảng playlists và playlist_items (quan hệ 1-N / N-N)
@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun observeAllPlaylistsWithItems(): Flow<List<PlaylistWithItems>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    fun observePlaylistById(playlistId: Long): Flow<PlaylistWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Query("SELECT COALESCE(MAX(order_index), 0) FROM playlist_items WHERE playlist_id = :playlistId")
    suspend fun getMaxItemOrder(playlistId: Long): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlist_id = :playlistId AND chapter_id = :chapterId")
    suspend fun countChapterInPlaylist(playlistId: Long, chapterId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity): Long

    @Query("UPDATE playlist_items SET order_index = :orderIndex WHERE id = :itemId")
    suspend fun updatePlaylistItemOrder(itemId: Long, orderIndex: Int)

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deletePlaylistItemById(itemId: Long)
}

// DAO 7: Truy vấn bảng app_settings lưu cấu hình hệ thống
@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun observeAllSettings(): Flow<List<AppSettingEntity>>

    @Query("SELECT setting_value FROM app_settings WHERE setting_key = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: AppSettingEntity)
}
