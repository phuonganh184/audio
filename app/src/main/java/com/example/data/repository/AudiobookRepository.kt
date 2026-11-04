package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.BookmarkWithDetails
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterWithStory
import com.example.data.entity.PlaybackProgressEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import com.example.data.entity.PlaylistItemWithDetails
import com.example.data.entity.PlaylistWithItems
import com.example.data.entity.ProgressWithDetails
import com.example.data.entity.StoryEntity
import com.example.data.entity.StoryWithChapters
import com.example.utils.AppConstants
import com.example.utils.ScannedAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// Lớp Repository tập trung quản lý toàn bộ truy xuất DB trên luồng phụ (Dispatchers.IO), ngăn chặn UI Freeze
class AudiobookRepository(private val database: AppDatabase) {

    private val storyDao = database.storyDao()
    private val chapterDao = database.chapterDao()
    private val progressDao = database.playbackProgressDao()
    private val bookmarkDao = database.bookmarkDao()
    private val playlistDao = database.playlistDao()
    private val settingDao = database.appSettingDao()

    // Luồng dữ liệu Reactive cho UI
    val allStoriesFlow: Flow<List<StoryWithChapters>> = storyDao.observeAllStoriesWithChapters()
    val favoriteStoriesFlow: Flow<List<StoryWithChapters>> = storyDao.observeFavoriteStories()
    val allChaptersWithStoryFlow: Flow<List<ChapterWithStory>> = chapterDao.observeAllChaptersWithStory()
    val favoriteChaptersFlow: Flow<List<ChapterWithStory>> = chapterDao.observeFavoriteChapters()
    val latestProgressFlow: Flow<ProgressWithDetails?> = progressDao.observeLatestProgress()
    val progressHistoryFlow: Flow<List<ProgressWithDetails>> = progressDao.observeAllProgressHistory()
    val allPlaylistsFlow: Flow<List<PlaylistWithItems>> = playlistDao.observeAllPlaylistsWithItems()
    val allBookmarksFlow: Flow<List<BookmarkWithDetails>> = bookmarkDao.observeAllBookmarksWithDetails()
    val allSettingsFlow: Flow<List<AppSettingEntity>> = settingDao.observeAllSettings()

    fun observeStoryById(storyId: Long): Flow<StoryWithChapters?> =
        storyDao.observeStoryWithChapters(storyId)

    fun observeBookmarksForChapter(chapterId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeBookmarksForChapter(chapterId)

    fun observePlaylistById(playlistId: Long): Flow<PlaylistWithItems?> =
        playlistDao.observePlaylistById(playlistId)

    // --- Quản lý Truyện & Chương ---

    suspend fun createStory(
        title: String,
        author: String,
        narrator: String,
        category: String,
        description: String,
        coverImagePath: String? = null
    ): Long = withContext(Dispatchers.IO) {
        storyDao.insertStory(
            StoryEntity(
                title = title.trim(),
                author = author.trim().ifBlank { "Chưa rõ tác giả" },
                narrator = narrator.trim().ifBlank { "Giọng đọc chuẩn" },
                coverImagePath = coverImagePath,
                description = description.trim().ifBlank { "Bộ sách nói lưu trữ cục bộ trên thiết bị." },
                category = category.trim().ifBlank { AppConstants.FREE_LIBRARY_CATEGORY },
                createdAt = System.currentTimeMillis(),
                isFavorite = 0
            )
        )
    }

    suspend fun toggleStoryFavorite(storyId: Long, currentlyFavorite: Boolean) =
        withContext(Dispatchers.IO) {
            storyDao.updateFavoriteStatus(storyId, if (currentlyFavorite) 0 else 1)
        }

    suspend fun toggleChapterFavorite(chapterId: Long, currentlyFavorite: Boolean) =
        withContext(Dispatchers.IO) {
            chapterDao.updateChapterFavorite(chapterId, if (currentlyFavorite) 0 else 1)
        }

    // Xóa truyện chỉ xóa bản ghi trong DB, tuyệt đối giữ nguyên file audio gốc trên thiết bị
    suspend fun deleteStoryFromLibrary(storyId: Long) = withContext(Dispatchers.IO) {
        storyDao.deleteStoryById(storyId)
    }

    // Xóa chương khỏi ứng dụng mà không chạm tới file gốc trên máy
    suspend fun deleteChapterFromLibrary(chapterId: Long) = withContext(Dispatchers.IO) {
        chapterDao.deleteChapterById(chapterId)
    }

    suspend fun getExistingFilePaths(): Set<String> = withContext(Dispatchers.IO) {
        chapterDao.getAllFilePaths().toSet()
    }

    suspend fun getChaptersForStoryOnce(storyId: Long): List<ChapterEntity> =
        withContext(Dispatchers.IO) {
            chapterDao.getChaptersByStoryOnce(storyId)
        }

    // Nhập danh sách tệp đã chọn vào một Bộ truyện có sẵn hoặc vào "Thư viện tự do"
    suspend fun importScannedFiles(
        selectedFiles: List<ScannedAudioFile>,
        targetStoryId: Long?,
        newStoryTitle: String? = null,
        newStoryAuthor: String? = null,
        newStoryNarrator: String? = null,
        newStoryCategory: String? = null,
        newStoryDescription: String? = null
    ): Int = withContext(Dispatchers.IO) {
        if (selectedFiles.isEmpty()) return@withContext 0

        val resolvedStoryId = when {
            !newStoryTitle.isNullOrBlank() -> {
                val firstCover = selectedFiles.firstNotNullOfOrNull { it.embeddedCoverPath }
                storyDao.insertStory(
                    StoryEntity(
                        title = newStoryTitle.trim(),
                        author = newStoryAuthor?.trim()?.ifBlank { null }
                            ?: selectedFiles.firstOrNull()?.author
                            ?: "Chưa rõ tác giả",
                        narrator = newStoryNarrator?.trim()?.ifBlank { "Giọng đọc gốc" } ?: "Giọng đọc gốc",
                        coverImagePath = firstCover,
                        description = newStoryDescription?.trim()?.ifBlank { "Nhập từ bộ nhớ thiết bị." }
                            ?: "Nhập từ bộ nhớ thiết bị.",
                        category = newStoryCategory?.trim()?.ifBlank { AppConstants.FREE_LIBRARY_CATEGORY }
                            ?: AppConstants.FREE_LIBRARY_CATEGORY
                    )
                )
            }
            targetStoryId != null && targetStoryId > 0L -> targetStoryId
            else -> {
                // Tự động tìm hoặc tạo bộ "Thư viện tự do"
                val existingFree = storyDao.findStoryByTitle(AppConstants.FREE_LIBRARY_TITLE)
                existingFree?.id ?: storyDao.insertStory(
                    StoryEntity(
                        title = AppConstants.FREE_LIBRARY_TITLE,
                        author = "Nhiều tác giả",
                        narrator = "Nhiều giọng đọc",
                        coverImagePath = selectedFiles.firstNotNullOfOrNull { it.embeddedCoverPath },
                        description = "Tập hợp các tệp âm thanh rời được thêm nhanh từ thiết bị.",
                        category = AppConstants.FREE_LIBRARY_CATEGORY
                    )
                )
            }
        }

        val existingPaths = chapterDao.getAllFilePaths().toSet()
        var currentOrder = chapterDao.getMaxOrderIndex(resolvedStoryId)
        var importedCount = 0

        for (file in selectedFiles) {
            if (file.uriString in existingPaths) continue
            currentOrder += 1
            val insertedId = chapterDao.insertChapter(
                ChapterEntity(
                    storyId = resolvedStoryId,
                    title = file.displayTitle,
                    orderIndex = currentOrder,
                    filePath = file.uriString,
                    durationMs = file.durationMs,
                    fileSize = file.fileSize,
                    isCompleted = 0,
                    isFavorite = 0
                )
            )
            if (insertedId != -1L) {
                importedCount += 1
            }
        }
        importedCount
    }

    // --- Tiến trình nghe & Hoàn thành 95% ---

    suspend fun savePlaybackProgress(
        storyId: Long,
        chapterId: Long,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        if (storyId <= 0L || chapterId <= 0L) return@withContext
        val safePosition = positionMs.coerceAtLeast(0L)
        progressDao.upsertProgress(
            PlaybackProgressEntity(
                storyId = storyId,
                chapterId = chapterId,
                positionMs = safePosition,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
        // Đánh dấu hoàn thành chương nếu đạt từ 95% thời lượng trở lên
        if (durationMs > 0L && safePosition >= (durationMs * 0.95).toLong()) {
            chapterDao.updateCompletedStatus(chapterId, 1)
        }
    }

    suspend fun markChapterCompleted(chapterId: Long, completed: Boolean) =
        withContext(Dispatchers.IO) {
            chapterDao.updateCompletedStatus(chapterId, if (completed) 1 else 0)
        }

    suspend fun getProgressForStory(storyId: Long): PlaybackProgressEntity? =
        withContext(Dispatchers.IO) {
            progressDao.getProgressForStory(storyId)
        }

    // --- Quản lý Bookmark ---

    suspend fun addBookmark(chapterId: Long, positionMs: Long, note: String): Long =
        withContext(Dispatchers.IO) {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    chapterId = chapterId,
                    positionMs = positionMs.coerceAtLeast(0L),
                    note = note.trim().ifBlank { "Mốc đánh dấu nhanh" },
                    createdAt = System.currentTimeMillis()
                )
            )
        }

    suspend fun deleteBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkById(bookmarkId)
    }

    // --- Quản lý Playlist ---

    suspend fun createPlaylist(name: String, description: String): Long =
        withContext(Dispatchers.IO) {
            playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = name.trim(),
                    createdAt = System.currentTimeMillis(),
                    description = description.trim().ifBlank { "Danh sách phát cá nhân" }
                )
            )
        }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        // Chỉ xóa Playlist và liên kết trong bảng playlist_items (CASCADE), không ảnh hưởng file gốc
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addChaptersToPlaylist(playlistId: Long, chapterIds: List<Long>): Int =
        withContext(Dispatchers.IO) {
            var currentMaxOrder = playlistDao.getMaxItemOrder(playlistId)
            var added = 0
            for (chapterId in chapterIds) {
                if (playlistDao.countChapterInPlaylist(playlistId, chapterId) == 0) {
                    currentMaxOrder += 1
                    playlistDao.insertPlaylistItem(
                        PlaylistItemEntity(
                            playlistId = playlistId,
                            chapterId = chapterId,
                            orderIndex = currentMaxOrder
                        )
                    )
                    added += 1
                }
            }
            added
        }

    suspend fun removePlaylistItem(itemId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistItemById(itemId)
    }

    // Đổi thứ tự phát (Di chuyển lên / xuống) trong Playlist
    suspend fun swapPlaylistItemsOrder(
        firstItem: PlaylistItemWithDetails,
        secondItem: PlaylistItemWithDetails
    ) = withContext(Dispatchers.IO) {
        val firstOrder = firstItem.item.orderIndex
        val secondOrder = secondItem.item.orderIndex
        playlistDao.updatePlaylistItemOrder(firstItem.item.id, secondOrder)
        playlistDao.updatePlaylistItemOrder(secondItem.item.id, firstOrder)
    }

    // --- Quản lý Cài đặt hệ thống (app_settings) ---

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        settingDao.upsertSetting(AppSettingEntity(settingKey = key, settingValue = value))
    }

    suspend fun getSetting(key: String, defaultValue: String): String =
        withContext(Dispatchers.IO) {
            settingDao.getSettingValue(key) ?: defaultValue
        }
}
