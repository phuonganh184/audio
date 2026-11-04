package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

// Trình quản lý dung lượng, Sao lưu (Backup) và Khôi phục (Restore) cơ sở dữ liệu SQLite an toàn
object DatabaseBackupManager {

    private val SQLITE_MAGIC_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    data class LocalBackupInfo(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        val lastModified: Long
    )

    // Tính tổng dung lượng bộ nhớ mà cơ sở dữ liệu đang chiếm dụng (bytes)
    fun getDatabaseSizeBytes(context: Context): Long {
        val dbFile = context.getDatabasePath(AppConstants.DATABASE_NAME)
        val journalFile = File(dbFile.path + "-journal")
        val walFile = File(dbFile.path + "-wal")
        val shmFile = File(dbFile.path + "-shm")
        return listOf(dbFile, journalFile, walFile, shmFile)
            .filter { it.exists() }
            .sumOf { it.length() }
    }

    // Lấy thông tin bản sao lưu nội bộ gần nhất (nếu có)
    fun getLatestLocalBackup(context: Context): LocalBackupInfo? {
        val backupDir = File(context.filesDir, "db_backups")
        if (!backupDir.exists()) return null
        val latest = backupDir.listFiles { f -> f.isFile && f.extension == "db" }
            ?.maxByOrNull { it.lastModified() }
            ?: return null
        return LocalBackupInfo(
            fileName = latest.name,
            filePath = latest.absolutePath,
            fileSize = latest.length(),
            lastModified = latest.lastModified()
        )
    }

    // Thực hiện đồng bộ dữ liệu xuống đĩa trước khi sao chép file SQLite
    private fun flushDatabase(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (_: Exception) {
        }
    }

    // Sao lưu nhanh vào thư mục dự phòng nội bộ của ứng dụng
    suspend fun createLocalSnapshotBackup(context: Context): Result<LocalBackupInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                flushDatabase(context)
                val sourceDb = context.getDatabasePath(AppConstants.DATABASE_NAME)
                require(sourceDb.exists()) { "Không tìm thấy tệp cơ sở dữ liệu gốc." }

                val backupDir = File(context.filesDir, "db_backups")
                if (!backupDir.exists()) backupDir.mkdirs()

                val targetFile = File(backupDir, "bach_nhat_anh_backup_snapshot.db")
                FileInputStream(sourceDb).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                        output.fd.sync()
                    }
                }
                LocalBackupInfo(
                    fileName = targetFile.name,
                    filePath = targetFile.absolutePath,
                    fileSize = targetFile.length(),
                    lastModified = targetFile.lastModified()
                )
            }
        }

    // Xuất bản sao lưu SQLite ra vị trí bất kỳ do người dùng chọn qua SAF CreateDocument
    suspend fun exportDatabaseToUri(context: Context, destinationUri: Uri): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                flushDatabase(context)
                val sourceDb = context.getDatabasePath(AppConstants.DATABASE_NAME)
                require(sourceDb.exists()) { "Không tìm thấy tệp cơ sở dữ liệu để sao lưu." }

                var copiedBytes = 0L
                FileInputStream(sourceDb).use { input ->
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        copiedBytes = input.copyTo(output)
                        output.flush()
                    } ?: error("Không thể mở luồng ghi tại vị trí đã chọn.")
                }
                copiedBytes
            }
        }

    // Khôi phục cơ sở dữ liệu từ tệp URI người dùng chọn (kiểm tra chữ ký SQLite hợp lệ trước khi ghi đè)
    suspend fun restoreDatabaseFromUri(context: Context, sourceUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tempRestoreFile = File(context.cacheDir, "temp_restore_validate.db")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(tempRestoreFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Không thể đọc tệp sao lưu đã chọn.")

                validateAndReplaceDatabase(context, tempRestoreFile)
            }
        }

    // Khôi phục từ bản sao lưu dự phòng nội bộ
    suspend fun restoreFromLocalSnapshot(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backupInfo = getLatestLocalBackup(context)
                    ?: error("Chưa có bản sao lưu nội bộ nào.")
                val sourceFile = File(backupInfo.filePath)
                require(sourceFile.exists()) { "Tệp sao lưu nội bộ không tồn tại." }

                val tempCopy = File(context.cacheDir, "temp_local_restore.db")
                sourceFile.copyTo(tempCopy, overwrite = true)
                validateAndReplaceDatabase(context, tempCopy)
            }
        }

    private fun validateAndReplaceDatabase(context: Context, candidateFile: File) {
        try {
            require(candidateFile.length() > 100L) { "Tệp sao lưu rỗng hoặc không hợp lệ." }

            // Kiểm tra 16 byte đầu tiên đúng chuẩn SQLite 3
            val header = ByteArray(16)
            FileInputStream(candidateFile).use { it.read(header) }
            require(header.contentEquals(SQLITE_MAGIC_HEADER)) {
                "Tệp đã chọn không phải là cơ sở dữ liệu SQLite hợp lệ."
            }

            // Kiểm tra sự tồn tại của bảng cốt lõi stories trong bản sao lưu
            val checkDb = SQLiteDatabase.openDatabase(
                candidateFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
            val hasStoriesTable = checkDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='stories'",
                null
            ).use { cursor -> cursor.moveToFirst() }
            checkDb.close()
            require(hasStoriesTable) {
                "Tệp sao lưu không đúng cấu trúc bảng của Bạch Nhật Anh Audio."
            }

            // Đóng Room hiện tại và chép đè file SQLite an toàn
            AppDatabase.closeDatabaseInstance()
            val targetDb = context.getDatabasePath(AppConstants.DATABASE_NAME)
            targetDb.parentFile?.mkdirs()

            File(targetDb.path + "-journal").delete()
            File(targetDb.path + "-wal").delete()
            File(targetDb.path + "-shm").delete()

            candidateFile.copyTo(targetDb, overwrite = true)
        } finally {
            candidateFile.delete()
        }
    }
}
