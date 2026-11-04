package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.AudiobookRepository
import com.example.utils.TimeFormatter
import com.example.utils.VietnameseTextUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AudiobookRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AudiobookRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `verify app name and vietnamese accent insensitive search`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Bạch Nhật Anh Audio", appName)

        assertTrue(VietnameseTextUtils.matchesSearch("Bóng Trăng Cổ Độ", "bong trang co do"))
        assertTrue(VietnameseTextUtils.matchesSearch("Đêm Sương Trên Bến Đò", "dem suong"))
        assertEquals("01:05:09", TimeFormatter.formatDuration(3_909_000L))
    }

    @Test
    fun `verify room 7 tables progress 95 percent completion and bookmarks`() = runBlocking {
        val storyId = repository.createStory(
            title = "Truyện Kiểm Thử",
            author = "Nguyễn Nhật",
            narrator = "MC Anh",
            category = "Văn học",
            description = "Mô tả kiểm thử"
        )
        val chapterId = db.chapterDao().insertChapter(
            com.example.data.entity.ChapterEntity(
                storyId = storyId,
                title = "Chương 1",
                orderIndex = 1,
                filePath = "/tmp/test_audio_ch1.wav",
                durationMs = 100_000L,
                fileSize = 2048L
            )
        )

        // Khi đạt 96% thời lượng (>= 95%), chương phải tự động đánh dấu is_completed = 1
        repository.savePlaybackProgress(
            storyId = storyId,
            chapterId = chapterId,
            positionMs = 96_000L,
            durationMs = 100_000L
        )

        val chapters = repository.getChaptersForStoryOnce(storyId)
        assertEquals(1, chapters.first().isCompleted)

        // Kiểm tra thêm Bookmark và Playlist
        repository.addBookmark(chapterId, 45_000L, "Đoạn cao trào")
        val bookmarks = repository.observeBookmarksForChapter(chapterId).first()
        assertEquals(1, bookmarks.size)
        assertEquals("Đoạn cao trào", bookmarks.first().note)

        val playlistId = repository.createPlaylist("Nghe Đêm", "Playlist kiểm thử")
        val addedCount = repository.addChaptersToPlaylist(playlistId, listOf(chapterId))
        assertEquals(1, addedCount)
    }
}
