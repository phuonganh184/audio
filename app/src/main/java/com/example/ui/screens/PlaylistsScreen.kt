package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterWithStory
import com.example.data.entity.PlaylistItemWithDetails
import com.example.data.entity.PlaylistWithItems
import com.example.data.entity.StoryEntity
import com.example.data.entity.StoryWithChapters
import com.example.player.RepeatModeOption
import com.example.ui.components.CoverArtwork
import com.example.ui.components.StoryCard
import com.example.utils.TimeFormatter

// Màn hình MODULE 8: Quản lý Playlist (1-N / N-N, đổi thứ tự, Repeat/Shuffle) & Tổng hợp Yêu thích
@Composable
fun PlaylistsScreen(
    playlists: List<PlaylistWithItems>,
    selectedPlaylist: PlaylistWithItems?,
    allChapters: List<ChapterWithStory>,
    favoriteStories: List<StoryWithChapters>,
    favoriteChapters: List<ChapterWithStory>,
    repeatMode: RepeatModeOption,
    shuffleEnabled: Boolean,
    onCreatePlaylist: (String, String) -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onClosePlaylistDetail: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayPlaylist: (PlaylistWithItems, Int) -> Unit,
    onAddChaptersToPlaylist: (Long, List<Long>) -> Unit,
    onRemovePlaylistItem: (Long) -> Unit,
    onMovePlaylistItem: (List<PlaylistItemWithDetails>, Int, Boolean) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onSelectStory: (Long) -> Unit,
    onPlayStory: (StoryWithChapters) -> Unit,
    onToggleStoryFavorite: (StoryEntity) -> Unit,
    onPlayFavoriteChapters: (List<ChapterWithStory>, Int) -> Unit,
    onToggleChapterFavorite: (ChapterEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedPlaylist != null) {
        PlaylistDetailContent(
            playlistWithItems = selectedPlaylist,
            allChapters = allChapters,
            repeatMode = repeatMode,
            shuffleEnabled = shuffleEnabled,
            onBack = onClosePlaylistDetail,
            onPlayPlaylist = onPlayPlaylist,
            onAddChapters = { ids -> onAddChaptersToPlaylist(selectedPlaylist.playlist.id, ids) },
            onRemoveItem = onRemovePlaylistItem,
            onMoveItem = onMovePlaylistItem,
            onDeletePlaylist = { onDeletePlaylist(selectedPlaylist.playlist.id) },
            onCycleRepeat = onCycleRepeat,
            onToggleShuffle = onToggleShuffle,
            modifier = modifier
        )
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlists_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bộ sưu tập & Yêu thích",
                style = MaterialTheme.typography.headlineMedium
            )
            if (selectedTab == 0) {
                Button(
                    onClick = { showCreateDialog = true },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("create_playlist_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo Playlist")
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Playlist (${playlists.size})") },
                modifier = Modifier.testTag("tab_playlists")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Yêu thích (${favoriteStories.size + favoriteChapters.size})") },
                modifier = Modifier.testTag("tab_favorites")
            )
        }

        if (selectedTab == 0) {
            if (playlists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Chưa có Danh sách phát nào")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(playlists, key = { it.playlist.id }) { pl ->
                        val firstCover = pl.sortedItems.firstOrNull()?.chapterWithStory?.story?.coverImagePath
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("playlist_card_${pl.playlist.id}")
                                .clickable { onSelectPlaylist(pl.playlist.id) },
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtwork(
                                    coverPath = firstCover,
                                    contentDescription = pl.playlist.name,
                                    modifier = Modifier.size(64.dp),
                                    shape = MaterialTheme.shapes.medium
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pl.playlist.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${pl.items.size} chương • ${TimeFormatter.formatReadableDuration(pl.totalDurationMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onPlayPlaylist(pl, 0) }) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Phát playlist ${pl.playlist.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDeletePlaylist(pl.playlist.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Xóa playlist",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tab Yêu thích: Hiển thị cả Truyện thả tim & Chương thả tim
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (favoriteStories.isEmpty() && favoriteChapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hãy nhấn biểu tượng Trái tim ở Truyện hoặc Chương để lưu vào mục Yêu thích.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (favoriteStories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Bộ truyện đã thả tim (${favoriteStories.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(favoriteStories, key = { "fav_story_${it.story.id}" }) { st ->
                        StoryCard(
                            storyWithChapters = st,
                            onClick = { onSelectStory(st.story.id) },
                            onPlayClick = { onPlayStory(st) },
                            onToggleFavorite = { onToggleStoryFavorite(st.story) }
                        )
                    }
                }

                if (favoriteChapters.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chương lẻ yêu thích (${favoriteChapters.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(favoriteChapters, key = { _, item -> "fav_ch_${item.chapter.id}" }) { idx, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayFavoriteChapters(favoriteChapters, idx) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CoverArtwork(
                                    coverPath = item.story.coverImagePath,
                                    contentDescription = item.chapter.title,
                                    modifier = Modifier.size(48.dp),
                                    shape = MaterialTheme.shapes.small
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.chapter.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.story.title} • ${TimeFormatter.formatDuration(item.chapter.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onToggleChapterFavorite(item.chapter) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Bỏ yêu thích chương",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Tạo Danh sách phát mới") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên Playlist *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Mô tả") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(name, desc)
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun PlaylistDetailContent(
    playlistWithItems: PlaylistWithItems,
    allChapters: List<ChapterWithStory>,
    repeatMode: RepeatModeOption,
    shuffleEnabled: Boolean,
    onBack: () -> Unit,
    onPlayPlaylist: (PlaylistWithItems, Int) -> Unit,
    onAddChapters: (List<Long>) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onMoveItem: (List<PlaylistItemWithDetails>, Int, Boolean) -> Unit,
    onDeletePlaylist: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val sortedItems = playlistWithItems.sortedItems
    var showAddChaptersDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                }
                IconButton(onClick = onDeletePlaylist) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa Playlist")
                }
            }
        }

        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = playlistWithItems.playlist.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = playlistWithItems.playlist.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Chế độ phát mở rộng trong Playlist: Repeat All / Repeat One / Shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = repeatMode != RepeatModeOption.OFF,
                            onClick = onCycleRepeat,
                            label = {
                                Text(
                                    when (repeatMode) {
                                        RepeatModeOption.OFF -> "Lặp: Tắt"
                                        RepeatModeOption.ALL -> "Lặp tất cả"
                                        RepeatModeOption.ONE -> "Lặp 1 bài"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (repeatMode == RepeatModeOption.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        FilterChip(
                            selected = shuffleEnabled,
                            onClick = onToggleShuffle,
                            label = { Text(if (shuffleEnabled) "Trộn bài: Bật" else "Trộn bài: Tắt") },
                            leadingIcon = {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onPlayPlaylist(playlistWithItems, 0) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phát tất cả")
                        }
                        OutlinedButton(onClick = { showAddChaptersDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm chương")
                        }
                    }
                }
            }
        }

        itemsIndexed(sortedItems, key = { _, item -> item.item.id }) { idx, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayPlaylist(playlistWithItems, idx) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverArtwork(
                        coverPath = item.chapterWithStory.story.coverImagePath,
                        contentDescription = item.chapterWithStory.chapter.title,
                        modifier = Modifier.size(44.dp),
                        shape = MaterialTheme.shapes.small
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.chapterWithStory.chapter.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.chapterWithStory.story.title} • ${TimeFormatter.formatDuration(item.chapterWithStory.chapter.durationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onMoveItem(sortedItems, idx, true) },
                        enabled = idx > 0
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Đưa lên trên", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { onMoveItem(sortedItems, idx, false) },
                        enabled = idx < sortedItems.lastIndex
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = "Đưa xuống dưới", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onRemoveItem(item.item.id) }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa khỏi playlist", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showAddChaptersDialog) {
        val selectedIds = remember { mutableStateListOf<Long>() }
        AlertDialog(
            onDismissRequest = { showAddChaptersDialog = false },
            title = { Text("Chọn chương thêm vào Playlist") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(allChapters, key = { it.chapter.id }) { chItem ->
                        val checked = chItem.chapter.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(chItem.chapter.id)
                                    else selectedIds.add(chItem.chapter.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) selectedIds.add(chItem.chapter.id)
                                    else selectedIds.remove(chItem.chapter.id)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chItem.chapter.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = chItem.story.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddChapters(selectedIds.toList())
                        showAddChaptersDialog = false
                    },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Text("Thêm (${selectedIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChaptersDialog = false }) { Text("Hủy") }
            }
        )
    }
}
