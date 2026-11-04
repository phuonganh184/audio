package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AppSettingDao
import com.example.data.dao.BookmarkDao
import com.example.data.dao.ChapterDao
import com.example.data.dao.PlaybackProgressDao
import com.example.data.dao.PlaylistDao
import com.example.data.dao.StoryDao
import com.example.data.entity.AppSettingEntity
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.data.entity.PlaybackProgressEntity
import com.example.data.entity.PlaylistEntity
import com.example.data.entity.PlaylistItemEntity
import com.example.data.entity.StoryEntity
import com.example.utils.AppConstants

// Cơ sở dữ liệu Room SQLite đồng nhất gồm 7 bảng cốt lõi chạy hoàn toàn Offline
@Database(
    entities = [
        StoryEntity::class,
        ChapterEntity::class,
        PlaybackProgressEntity::class,
        BookmarkEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        AppSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun chapterDao(): ChapterDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    AppConstants.DATABASE_NAME
                )
                    // Dùng JournalMode.TRUNCATE để hợp nhất dữ liệu vào 1 file .db duy nhất, hỗ trợ Backup/Restore chính xác
                    .setJournalMode(JournalMode.TRUNCATE)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        // Đóng kết nối an toàn trước khi ghi đè file SQLite trong quá trình Khôi phục (Restore)
        fun closeDatabaseInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
