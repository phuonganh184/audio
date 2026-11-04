package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.ProgressWithDetails
import com.example.data.entity.StoryEntity
import com.example.data.entity.StoryWithChapters
import com.example.ui.components.CoverArtwork
import com.example.ui.components.StoryCard
import com.example.utils.TimeFormatter

// Màn hình Trang chủ: Hero Banner, nút chuyển Dark/Light Mode, khối "Nghe tiếp" thông minh và danh sách nổi bật
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    stories: List<StoryWithChapters>,
    favoriteStories: List<StoryWithChapters>,
    latestProgress: ProgressWithDetails?,
    bookmarkCount: Int,
    onToggleTheme: () -> Unit,
    onResumeListening: (ProgressWithDetails) -> Unit,
    onOpenImport: () -> Unit,
    onOpenLibrary: () -> Unit,
    onSelectStory: (Long) -> Unit,
    onPlayStory: (StoryWithChapters) -> Unit,
    onToggleStoryFavorite: (StoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalChapters = stories.sumOf { it.chapters.size }
    val completedChapters = stories.sumOf { it.completedChapterCount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Thanh tiêu đề thương hiệu & Nút chuyển nhanh Dark/Light Mode
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bạch Nhật Anh Audio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Không gian sách nói Offline • Riêng tư & Tinh tế",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalIconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("home_theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDarkMode) "Chuyển giao diện Sáng" else "Chuyển giao diện Tối",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Hero Studio Banner nghệ thuật kèm nút Nhập tệp Audio từ thiết bị
        item {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_studio),
                        contentDescription = "Không gian phòng nghe Bạch Nhật Anh Audio",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEB09070F),
                                        Color(0xB8180E29),
                                        Color(0x55180E29)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color(0xFFA855F7).copy(alpha = 0.25f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, Color(0xFFD8B4FE).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "100% OFFLINE • LƯU TỪNG GIÂY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF3E8FF),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Thưởng thức trọn vẹn từng chương truyện",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 2
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onOpenImport,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFA855F7),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("hero_import_audio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Nhập tệp Audio")
                                }
                                OutlinedButton(
                                    onClick = onOpenLibrary,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.testTag("hero_open_library_button")
                                ) {
                                    Text("Mở Thư viện")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Khối "Nghe tiếp" (Resume Listening) - Tự nạp đúng file & tua đến giây nghe dở lần trước
        if (latestProgress != null) {
            item {
                ResumeListeningCard(
                    progressDetails = latestProgress,
                    onResumeClick = { onResumeListening(latestProgress) }
                )
            }
        }

        // Thống kê nhanh Thư viện
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChipCard(
                    icon = Icons.Filled.LibraryMusic,
                    value = "${stories.size}",
                    label = "Bộ truyện",
                    modifier = Modifier.weight(1f)
                )
                StatChipCard(
                    icon = Icons.Filled.Headphones,
                    value = "$totalChapters",
                    label = "Chương",
                    modifier = Modifier.weight(1f)
                )
                StatChipCard(
                    icon = Icons.Filled.CheckCircle,
                    value = "$completedChapters",
                    label = "Đã xong",
                    modifier = Modifier.weight(1f)
                )
                StatChipCard(
                    icon = Icons.Filled.Bookmark,
                    value = "$bookmarkCount",
                    label = "Dấu trang",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Mục Yêu thích nổi bật
        if (favoriteStories.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Bộ truyện Yêu thích",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favoriteStories, key = { it.story.id }) { favItem ->
                            Card(
                                modifier = Modifier
                                    .width(156.dp)
                                    .clickable { onSelectStory(favItem.story.id) },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    CoverArtwork(
                                        coverPath = favItem.story.coverImagePath,
                                        contentDescription = favItem.story.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(132.dp),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = favItem.story.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${favItem.chapters.size} chương",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Danh sách Bộ truyện gần đây
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kệ sách của bạn",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Xem tất cả →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onOpenLibrary)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        items(stories, key = { it.story.id }) { storyItem ->
            StoryCard(
                storyWithChapters = storyItem,
                onClick = { onSelectStory(storyItem.story.id) },
                onPlayClick = { onPlayStory(storyItem) },
                onToggleFavorite = { onToggleStoryFavorite(storyItem.story) }
            )
        }
    }
}

@Composable
private fun ResumeListeningCard(
    progressDetails: ProgressWithDetails,
    onResumeClick: () -> Unit
) {
    val dur = progressDetails.chapter.durationMs.coerceAtLeast(1L)
    val pos = progressDetails.progress.positionMs.coerceIn(0L, dur)
    val fraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    val percent = (fraction * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("resume_listening_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NGHE TIẾP GẦN NHẤT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Đã nghe $percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArtwork(
                    coverPath = progressDetails.story.coverImagePath,
                    contentDescription = progressDetails.story.title,
                    modifier = Modifier.size(68.dp),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = progressDetails.story.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = progressDetails.chapter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Vị trí dừng: ${TimeFormatter.formatDuration(pos)} / ${TimeFormatter.formatDuration(dur)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Button(
                onClick = onResumeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resume_listening_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nghe tiếp từ ${TimeFormatter.formatDuration(pos)}")
            }
        }
    }
}

@Composable
private fun StatChipCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
