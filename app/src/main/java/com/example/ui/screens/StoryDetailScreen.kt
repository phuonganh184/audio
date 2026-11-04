package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.PlaylistWithItems
import com.example.data.entity.StoryEntity
import com.example.data.entity.StoryWithChapters
import com.example.ui.components.CoverArtwork
import com.example.utils.TimeFormatter

// Màn hình Chi tiết Bộ truyện & Danh sách Chương
@Composable
fun StoryDetailScreen(
    storyWithChapters: StoryWithChapters,
    activeChapterId: Long?,
    playlists: List<PlaylistWithItems>,
    onBack: () -> Unit,
    onPlayStoryFromLastProgress: () -> Unit,
    onPlaySpecificChapter: (ChapterEntity) -> Unit,
    onToggleStoryFavorite: (StoryEntity) -> Unit,
    onToggleChapterFavorite: (ChapterEntity) -> Unit,
    onAddChapterToPlaylist: (Long, Long) -> Unit,
    onImportChaptersHere: (Long) -> Unit,
    onDeleteChapter: (Long) -> Unit,
    onDeleteStory: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val story = storyWithChapters.story
    val chapters = storyWithChapters.sortedChapters
    var chapterForPlaylistDialog by remember { mutableStateOf<ChapterEntity?>(null) }
    var confirmDeleteStory by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("story_detail_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Thanh điều hướng quay lại
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("story_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại"
                    )
                }

                Row {
                    IconButton(onClick = { onToggleStoryFavorite(story) }) {
                        Icon(
                            imageVector = if (story.isFavorite == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Yêu thích bộ truyện",
                            tint = if (story.isFavorite == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { confirmDeleteStory = true }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Xóa bộ truyện khỏi thư viện",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Khối thông tin đầu trang Bộ truyện
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoverArtwork(
                            coverPath = story.coverImagePath,
                            contentDescription = story.title,
                            modifier = Modifier.size(112.dp),
                            shape = MaterialTheme.shapes.large
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = story.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = story.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tác giả: ${story.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Giọng đọc: ${story.narrator}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = story.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPlayStoryFromLastProgress,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("story_detail_play_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phát / Nghe tiếp")
                        }
                        OutlinedButton(
                            onClick = { onImportChaptersHere(story.id) },
                            modifier = Modifier.testTag("story_detail_import_chapters_button")
                        ) {
                            Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm chương")
                        }
                    }
                }
            }
        }

        // Tiêu đề Danh sách Chương
        item {
            Text(
                text = "Danh sách chương (${chapters.size} chương • Đã xong ${storyWithChapters.completedChapterCount})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(chapters, key = { _, ch -> ch.id }) { idx, chapter ->
            val isPlayingThis = activeChapterId == chapter.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chapter_item_${chapter.id}")
                    .clickable { onPlaySpecificChapter(chapter) },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (isPlayingThis) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isPlayingThis) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isPlayingThis) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = "Đang phát",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (chapter.isCompleted == 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Đã nghe hoàn thành",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = "${TimeFormatter.formatDuration(chapter.durationMs)} • ${TimeFormatter.formatFileSize(chapter.fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { onToggleChapterFavorite(chapter) }) {
                        Icon(
                            imageVector = if (chapter.isFavorite == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Thả tim chương",
                            tint = if (chapter.isFavorite == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { chapterForPlaylistDialog = chapter }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Thêm chương vào Playlist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { onDeleteChapter(chapter.id) }) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Gỡ chương khỏi thư viện",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Hộp thoại chọn Playlist để thêm chương
    chapterForPlaylistDialog?.let { selectedChapter ->
        AlertDialog(
            onDismissRequest = { chapterForPlaylistDialog = null },
            title = { Text("Thêm vào Danh sách phát") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Chưa có Playlist nào. Hãy mở mục \"Danh sách\" để tạo Playlist trước!")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        playlists.forEach { pl ->
                            OutlinedButton(
                                onClick = {
                                    onAddChapterToPlaylist(pl.playlist.id, selectedChapter.id)
                                    chapterForPlaylistDialog = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${pl.playlist.name} (${pl.items.size} bài)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { chapterForPlaylistDialog = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    if (confirmDeleteStory) {
        AlertDialog(
            onDismissRequest = { confirmDeleteStory = false },
            title = { Text("Xóa khỏi thư viện ứng dụng?") },
            text = {
                Text("Bộ truyện \"${story.title}\" sẽ được gỡ khỏi cơ sở dữ liệu ứng dụng. Các tệp âm thanh gốc trên điện thoại vẫn được giữ nguyên an toàn 100%.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteStory = false
                        onDeleteStory(story.id)
                    }
                ) {
                    Text("Xác nhận gỡ")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteStory = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
