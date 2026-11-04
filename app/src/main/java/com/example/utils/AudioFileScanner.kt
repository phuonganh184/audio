package com.example.utils

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

// Mô hình dữ liệu trung gian đại diện cho một tệp âm thanh vừa quét từ thiết bị
data class ScannedAudioFile(
    val uriString: String,
    val fileName: String,
    val displayTitle: String,
    val author: String,
    val durationMs: Long,
    val fileSize: Long,
    val embeddedCoverPath: String?,
    val isDuplicate: Boolean,
    val isSelected: Boolean = !isDuplicate
)

// Bộ quét tệp và thư mục Audio local, trích xuất Metadata và ảnh bìa nhúng an toàn (chỉ đọc URI, không sửa/xóa file gốc)
object AudioFileScanner {

    // Giữ quyền đọc URI lâu dài từ Storage Access Framework để phát lại sau khi khởi động lại máy
    fun persistReadPermission(context: Context, uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Một số nhà cung cấp tài liệu không hỗ trợ persistable permission, bỏ qua an toàn
        } catch (_: Exception) {
        }
    }

    // Quét danh sách các URI tệp riêng lẻ do người dùng chọn
    suspend fun scanDocumentUris(
        context: Context,
        uris: List<Uri>,
        existingPaths: Set<String>
    ): List<ScannedAudioFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ScannedAudioFile>()
        val seenInBatch = mutableSetOf<String>()

        for (uri in uris) {
            persistReadPermission(context, uri)
            val uriStr = uri.toString()
            if (!seenInBatch.add(uriStr)) continue

            val nameAndSize = queryFileNameAndSize(context, uri)
            val fileName = nameAndSize.first
            val fileSize = nameAndSize.second

            if (!isSupportedAudioName(fileName, context.contentResolver.getType(uri))) {
                continue
            }

            results.add(
                extractMetadataForUri(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    fileSize = fileSize,
                    isDuplicate = existingPaths.contains(uriStr)
                )
            )
        }
        results.sortedBy { it.fileName.lowercase() }
    }

    // Quét đệ quy toàn bộ một thư mục chứa tệp âm thanh do người dùng chọn
    suspend fun scanFolderTreeUri(
        context: Context,
        treeUri: Uri,
        existingPaths: Set<String>
    ): List<ScannedAudioFile> = withContext(Dispatchers.IO) {
        persistReadPermission(context, treeUri)
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val audioDocs = mutableListOf<DocumentFile>()
        collectAudioFilesRecursively(rootDoc, audioDocs, maxDepth = 4)

        val results = mutableListOf<ScannedAudioFile>()
        for (doc in audioDocs) {
            val uri = doc.uri
            val uriStr = uri.toString()
            val fileName = doc.name ?: "Audio_${ results.size + 1 }.mp3"
            val fileSize = doc.length()

            results.add(
                extractMetadataForUri(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    fileSize = fileSize,
                    isDuplicate = existingPaths.contains(uriStr)
                )
            )
        }
        results.sortedBy { it.fileName.lowercase() }
    }

    private fun collectAudioFilesRecursively(
        directory: DocumentFile,
        collector: MutableList<DocumentFile>,
        maxDepth: Int
    ) {
        if (maxDepth < 0) return
        val children = directory.listFiles()
        for (child in children) {
            if (child.isDirectory) {
                collectAudioFilesRecursively(child, collector, maxDepth - 1)
            } else if (child.isFile) {
                val name = child.name ?: ""
                if (isSupportedAudioName(name, child.type)) {
                    collector.add(child)
                }
            }
        }
    }

    private fun isSupportedAudioName(fileName: String, mimeType: String?): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext in AppConstants.SUPPORTED_AUDIO_EXTENSIONS) return true
        if (mimeType != null && mimeType.startsWith("audio/")) return true
        return false
    }

    private fun queryFileNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "tep_am_thanh.mp3"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                        name = cursor.getString(nameIndex)
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return name to size
    }

    // Trích xuất tiêu đề, tác giả, thời lượng mili-giây và ảnh bìa nhúng bằng MediaMetadataRetriever
    private fun extractMetadataForUri(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        isDuplicate: Boolean
    ): ScannedAudioFile {
        val fallbackTitle = fileName.substringBeforeLast('.').ifBlank { fileName }
        var title = fallbackTitle
        var author = ""
        var durationMs = 0L
        var coverPath: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!metaTitle.isNullOrBlank()) {
                title = metaTitle.trim()
            }
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
            if (!metaArtist.isNullOrBlank()) {
                author = metaArtist.trim()
            }
            val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = metaDuration?.toLongOrNull() ?: 0L

            // Trích xuất ảnh bìa nhúng (Cover Art) và lưu bản đệm vào thư mục riêng của ứng dụng
            val embeddedBytes = retriever.embeddedPicture
            if (embeddedBytes != null && embeddedBytes.isNotEmpty()) {
                coverPath = saveEmbeddedCoverArt(context, uri.toString(), embeddedBytes)
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        return ScannedAudioFile(
            uriString = uri.toString(),
            fileName = fileName,
            displayTitle = title,
            author = author.ifBlank { "Chưa rõ tác giả" },
            durationMs = durationMs,
            fileSize = fileSize,
            embeddedCoverPath = coverPath,
            isDuplicate = isDuplicate,
            isSelected = !isDuplicate
        )
    }

    private fun saveEmbeddedCoverArt(context: Context, key: String, bytes: ByteArray): String? {
        return try {
            val coverDir = File(context.filesDir, "extracted_covers")
            if (!coverDir.exists()) coverDir.mkdirs()
            val digest = MessageDigest.getInstance("MD5")
                .digest(key.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val coverFile = File(coverDir, "cover_$digest.jpg")
            if (!coverFile.exists()) {
                FileOutputStream(coverFile).use { it.write(bytes) }
            }
            coverFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    // Kiểm tra tệp local hoặc URI còn tồn tại và truy cập đọc được hay không trước khi phát
    fun isAudioSourceAccessible(context: Context, filePath: String): Boolean {
        if (filePath.isBlank()) return false
        return try {
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
            } else {
                val file = File(filePath)
                file.exists() && file.canRead()
            }
        } catch (_: Exception) {
            false
        }
    }
}
