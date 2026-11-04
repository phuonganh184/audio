package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.ChapterEntity
import com.example.player.PlayerUiState
import com.example.player.RepeatModeOption
import com.example.ui.components.CoverArtwork
import com.example.utils.AppConstants
import com.example.utils.TimeFormatter

// Màn hình Player chi tiết toàn màn hình: Tua Seekbar, Tốc độ 0.75x-2.0x, Tua -10s/+30s, Sleep Timer Fade-out 15s & Bookmark
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerUiState,
    chapterBookmarks: List<BookmarkEntity>,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleChapterFavorite: (ChapterEntity) -> Unit,
    onStartSleepTimerMinutes: (Int) -> Unit,
    onStartSleepTimerEndOfChapter: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onAddBookmark: (String) -> Unit,
    onJumpToBookmark: (BookmarkEntity) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val track = playerState.currentTrack ?: return
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }

    val totalDuration = playerState.durationMs.coerceAtLeast(1L)
    val effectivePos = if (isDraggingSlider) {
        dragPositionMs.toLong()
    } else {
        playerState.currentPositionMs.coerceIn(0L, totalDuration)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("full_player_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thanh đóng thu nhỏ Player & trạng thái Hẹn giờ tắt
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_full_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Thu nhỏ trình phát",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ĐANG PHÁT SÁCH NÓI",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Chương ${playerState.currentIndex + 1} / ${playerState.queue.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onToggleChapterFavorite(track.chapter) },
                        modifier = Modifier.testTag("full_player_fav_button")
                    ) {
                        Icon(
                            imageVector = if (track.chapter.isFavorite == 1) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Thả tim chương đang phát",
                            tint = if (track.chapter.isFavorite == 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Ảnh bìa lớn trung tâm
            item {
                CoverArtwork(
                    coverPath = track.story.coverImagePath,
                    contentDescription = track.story.title,
                    modifier = Modifier.size(236.dp),
                    shape = MaterialTheme.shapes.extraLarge
                )
            }

            // Tiêu đề Chương & Bộ truyện
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track.chapter.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.story.title} • ${track.story.narrator}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Hiển thị đồng hồ đếm ngược Hẹn giờ tắt (Sleep Timer) & cảnh báo Fade-out 15s cuối
            if (playerState.sleepTimer.isActive) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            val timerLabel = when {
                                playerState.sleepTimer.endOfCurrentChapter ->
                                    "Tự dừng khi kết thúc chương hiện tại"
                                playerState.sleepTimer.isFadingOut ->
                                    "Đang giảm âm lượng dần (Fade-out): ${TimeFormatter.formatDuration(playerState.sleepTimer.remainingMs)}"
                                else ->
                                    "Hẹn giờ tắt còn: ${TimeFormatter.formatDuration(playerState.sleepTimer.remainingMs)}"
                            }
                            Text(
                                text = timerLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Thanh trượt SeekBar và Thời gian thực (Current Time / Duration)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = effectivePos.toFloat(),
                        onValueChange = { newVal ->
                            isDraggingSlider = true
                            dragPositionMs = newVal
                        },
                        onValueChangeFinished = {
                            onSeekTo(dragPositionMs.toLong())
                            isDraggingSlider = false
                        },
                        valueRange = 0f..totalDuration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = TimeFormatter.formatDuration(effectivePos),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = TimeFormatter.formatDuration(totalDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bộ nút điều khiển chính: Shuffle, Prev, Tua lùi (-10s), Play/Pause, Tua tới (+30s), Next, Repeat
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Phát ngẫu nhiên",
                            tint = if (playerState.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.testTag("player_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Chương trước",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onRewind,
                            modifier = Modifier.testTag("player_rewind_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Tua lùi ${playerState.rewindSeconds} giây",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text("-${playerState.rewindSeconds}s", style = MaterialTheme.typography.labelSmall)
                    }

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("player_main_play_pause_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Tạm dừng" else "Phát",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onForward,
                            modifier = Modifier.testTag("player_forward_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward30,
                                contentDescription = "Tua tới ${playerState.forwardSeconds} giây",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text("+${playerState.forwardSeconds}s", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Chương sau",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(onClick = onCycleRepeat) {
                        Icon(
                            imageVector = if (playerState.repeatMode == RepeatModeOption.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Chế độ lặp",
                            tint = if (playerState.repeatMode != RepeatModeOption.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hàng tiện ích mở rộng: Tốc độ phát, Hẹn giờ tắt, Thêm Bookmark
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSpeedDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_speed_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${playerState.playbackSpeed}x")
                    }

                    OutlinedButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("player_sleep_timer_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (playerState.sleepTimer.isActive) "Đang hẹn" else "Hẹn giờ")
                    }

                    Button(
                        onClick = { showAddBookmarkDialog = true },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("player_add_bookmark_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đánh dấu")
                    }
                }
            }

            // Thanh điều chỉnh Âm lượng lưu trạng thái vào bảng app_settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Âm lượng",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = playerState.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(playerState.volume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Danh sách Bookmark đã lưu của chương hiện tại (bấm vào để nhảy tức thì tới giây đó)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dấu trang chương này (${chapterBookmarks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (chapterBookmarks.isEmpty()) {
                item {
                    Text(
                        text = "Chưa có dấu trang cho chương này. Bấm \"Đánh dấu\" để ghi lại đoạn tâm đắc!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(chapterBookmarks, key = { it.id }) { bm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJumpToBookmark(bm) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bm.note,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Nhảy tới ${TimeFormatter.formatDuration(bm.positionMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa dấu trang")
                            }
                        }
                    }
                }
            }
        }
    }

    // Hộp thoại chọn tốc độ phát (0.75x -> 2.0x)
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Tốc độ phát giọng đọc") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppConstants.PLAYBACK_SPEEDS.forEach { spd ->
                        FilterChip(
                            selected = playerState.playbackSpeed == spd,
                            onClick = {
                                onSpeedChange(spd)
                                showSpeedDialog = false
                            },
                            label = { Text(if (spd == 1.0f) "1.0x (Chuẩn)" else "${spd}x") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Đóng") }
            }
        )
    }

    // Hộp thoại Hẹn giờ tắt (Sleep Timer: 15, 30, 45, 60 phút, Hết chương, hoặc 1 phút thử nghiệm Fade-out)
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Hẹn giờ dừng phát (Sleep Timer)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Âm lượng sẽ tự động giảm êm dần (Fade-out) trong 15 giây cuối trước khi dừng hẳn.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(15, 30, 45, 60).forEach { mins ->
                        OutlinedButton(
                            onClick = {
                                onStartSleepTimerMinutes(mins)
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sau $mins phút")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            onStartSleepTimerEndOfChapter()
                            showSleepTimerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Khi kết thúc chương hiện tại")
                    }
                    OutlinedButton(
                        onClick = {
                            onStartSleepTimerMinutes(1)
                            showSleepTimerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sau 1 phút (Kiểm thử nhanh Fade-out 15s)")
                    }
                    if (playerState.sleepTimer.isActive) {
                        Button(
                            onClick = {
                                onCancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tắt hẹn giờ hiện tại")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) { Text("Đóng") }
            }
        )
    }

    // Hộp thoại thêm Bookmark tại thời điểm đang nghe kèm ghi chú
    if (showAddBookmarkDialog) {
        var note by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = {
                Text("Thêm Dấu trang tại ${TimeFormatter.formatDuration(playerState.currentPositionMs)}")
            },
            text = {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú đoạn tâm đắc") },
                    placeholder = { Text("Ví dụ: Đoạn trích dẫn triết lý hay…") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddBookmark(note)
                        showAddBookmarkDialog = false
                    }
                ) {
                    Text("Lưu dấu trang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) { Text("Hủy") }
            }
        )
    }
}
