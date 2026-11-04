package com.example.data.local

import android.content.Context
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.PlaybackProgressEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import com.example.data.entity.StoryEntity
import com.example.utils.AppConstants
import com.example.utils.SampleAudioGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Bộ khởi tạo dữ liệu mẫu (Seeder): Tạo 2 bộ truyện mẫu kèm các chương WAV có thể nghe thật ngay lập tức
object DatabaseSeeder {

    suspend fun seedIfEmpty(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val storyDao = db.storyDao()
        if (storyDao.getStoryCount() > 0) return@withContext

        val settingDao = db.appSettingDao()
        // Khởi tạo cài đặt mặc định trong bảng app_settings
        if (settingDao.getSettingValue(AppConstants.KEY_DARK_MODE) == null) {
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_DARK_MODE, "true"))
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_DEFAULT_SPEED, "1.0"))
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_REWIND_SECONDS, "10"))
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_FORWARD_SECONDS, "30"))
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_VOLUME_LEVEL, "1.0"))
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_REPEAT_MODE, "OFF"))
            settingDao.upsertSetting(AppSettingEntity(AppConstants.KEY_SHUFFLE_ENABLED, "false"))
        }

        val now = System.currentTimeMillis()

        // Tạo các tệp WAV giai điệu thực tế cho Bộ truyện mẫu 1: Bóng Trăng Cổ Độ
        val moonTrack1 = SampleAudioGenerator.ensureSampleWav(
            context = context,
            fileName = "bong_trang_co_do_chuong_01.wav",
            durationSeconds = 54,
            baseFrequencies = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 392.00, 329.63)
        )
        val moonTrack2 = SampleAudioGenerator.ensureSampleWav(
            context = context,
            fileName = "bong_trang_co_do_chuong_02.wav",
            durationSeconds = 66,
            baseFrequencies = doubleArrayOf(293.66, 349.23, 440.00, 587.33, 440.00, 349.23)
        )
        val moonTrack3 = SampleAudioGenerator.ensureSampleWav(
            context = context,
            fileName = "bong_trang_co_do_chuong_03.wav",
            durationSeconds = 60,
            baseFrequencies = doubleArrayOf(220.00, 261.63, 329.63, 440.00, 329.63, 261.63)
        )

        val story1Id = storyDao.insertStory(
            StoryEntity(
                title = "Bóng Trăng Cổ Độ",
                author = "Mạc Thiên Thanh",
                narrator = "MC Bạch Nhật Anh",
                coverImagePath = "res:moonlight",
                description = "Giữa màn sương đêm bảng lảng nơi bến đò cổ, tiếng tiêu trầm mặc mở ra hành trình đi tìm cuốn cổ thư thất truyền và lời hẹn ước dưới trăng rằm mười năm trước.",
                category = "Cổ phong & Kiếm hiệp",
                createdAt = now - 3_600_000L,
                isFavorite = 1
            )
        )

        val chapterDao = db.chapterDao()
        val s1Ch1Id = chapterDao.insertChapter(
            ChapterEntity(
                storyId = story1Id,
                title = "Chương 1: Đêm Sương Trên Bến Cổ Độ",
                orderIndex = 1,
                filePath = moonTrack1.filePath,
                durationMs = moonTrack1.durationMs,
                fileSize = moonTrack1.fileSize,
                isCompleted = 1,
                isFavorite = 1
            )
        )
        val s1Ch2Id = chapterDao.insertChapter(
            ChapterEntity(
                storyId = story1Id,
                title = "Chương 2: Tiếng Đàn Dưới Ánh Trăng Bạc",
                orderIndex = 2,
                filePath = moonTrack2.filePath,
                durationMs = moonTrack2.durationMs,
                fileSize = moonTrack2.fileSize,
                isCompleted = 0,
                isFavorite = 0
            )
        )
        chapterDao.insertChapter(
            ChapterEntity(
                storyId = story1Id,
                title = "Chương 3: Lời Thề Nơi Đỉnh Vân Sơn",
                orderIndex = 3,
                filePath = moonTrack3.filePath,
                durationMs = moonTrack3.durationMs,
                fileSize = moonTrack3.fileSize,
                isCompleted = 0,
                isFavorite = 0
            )
        )

        // Tạo các tệp WAV giai điệu không gian cho Bộ truyện mẫu 2: Hành Tinh Tĩnh Lặng
        val cosmosTrack1 = SampleAudioGenerator.ensureSampleWav(
            context = context,
            fileName = "hanh_tinh_tinh_lang_chuong_01.wav",
            durationSeconds = 48,
            baseFrequencies = doubleArrayOf(196.00, 293.66, 392.00, 493.88, 392.00, 293.66)
        )
        val cosmosTrack2 = SampleAudioGenerator.ensureSampleWav(
            context = context,
            fileName = "hanh_tinh_tinh_lang_chuong_02.wav",
            durationSeconds = 72,
            baseFrequencies = doubleArrayOf(246.94, 329.63, 493.88, 659.25, 493.88, 329.63)
        )

        val story2Id = storyDao.insertStory(
            StoryEntity(
                title = "Hành Tinh Tĩnh Lặng",
                author = "Lê Hoàng Vũ",
                narrator = "NSƯT Minh Khuê",
                coverImagePath = "res:cosmos",
                description = "Nhật ký âm thanh của nhà du hành đơn độc đặt chân lên rặng pha lê tím ngoài rìa tinh vân Andromeda, nơi thời gian trôi theo nhịp sóng vọng của vũ trụ.",
                category = "Khoa học viễn tưởng",
                createdAt = now,
                isFavorite = 0
            )
        )

        val s2Ch1Id = chapterDao.insertChapter(
            ChapterEntity(
                storyId = story2Id,
                title = "Chương 1: Tín Hiệu Từ Tinh Vân Tím",
                orderIndex = 1,
                filePath = cosmosTrack1.filePath,
                durationMs = cosmosTrack1.durationMs,
                fileSize = cosmosTrack1.fileSize,
                isCompleted = 0,
                isFavorite = 1
            )
        )
        chapterDao.insertChapter(
            ChapterEntity(
                storyId = story2Id,
                title = "Chương 2: Bình Minh Trên Vách Pha Lê",
                orderIndex = 2,
                filePath = cosmosTrack2.filePath,
                durationMs = cosmosTrack2.durationMs,
                fileSize = cosmosTrack2.fileSize,
                isCompleted = 0,
                isFavorite = 0
            )
        )

        // Khởi tạo tiến trình nghe dở mẫu để kiểm tra ngay khối "Nghe tiếp" trên Trang chủ
        db.playbackProgressDao().upsertProgress(
            PlaybackProgressEntity(
                storyId = story1Id,
                chapterId = s1Ch2Id,
                positionMs = 18_000L,
                lastPlayedAt = now - 300_000L
            )
        )

        // Khởi tạo Bookmark mẫu
        db.bookmarkDao().insertBookmark(
            BookmarkEntity(
                chapterId = s1Ch2Id,
                positionMs = 18_000L,
                note = "Đoạn độc thoại dưới ánh trăng cực kỳ sâu lắng",
                createdAt = now - 250_000L
            )
        )
        db.bookmarkDao().insertBookmark(
            BookmarkEntity(
                chapterId = s1Ch1Id,
                positionMs = 12_000L,
                note = "Mở đầu bối cảnh bến đò sương khói",
                createdAt = now - 600_000L
            )
        )

        // Khởi tạo 1 Playlist mẫu nghe trước giờ đi ngủ
        val playlistId = db.playlistDao().insertPlaylist(
            PlaylistEntity(
                name = "Góc Đêm Thư Giãn",
                createdAt = now,
                description = "Tuyển tập các chương truyện êm dịu nghe trước khi ngủ"
            )
        )
        db.playlistDao().insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                chapterId = s1Ch2Id,
                orderIndex = 1
            )
        )
        db.playlistDao().insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                chapterId = s2Ch1Id,
                orderIndex = 2
            )
        )
    }
}
