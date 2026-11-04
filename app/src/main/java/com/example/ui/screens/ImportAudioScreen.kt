package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.entity.StoryWithChapters
import com.example.ui.components.CoverArtwork
import com.example.ui.viewmodel.ImportUiState
import com.example.utils.AppConstants
import com.example.utils.TimeFormatter

private enum class ImportDestinationMode {
    FREE_LIBRARY,
    EXISTING_STORY,
    NEW_STORY
}

// Màn hình MODULE 3: Quét tệp & thư mục Audio, trích xuất Metadata, chống trùng lặp & gán vào Bộ truyện
@Composable
fun ImportAudioScreen(
    importState: ImportUiState,
    stories: List<StoryWithChapters>,
    preselectedStoryId: Long?,
    onClose: () -> Unit,
    onFilesPicked: (List<android.net.Uri>) -> Unit,
    onFolderPicked: (android.net.Uri) -> Unit,
    onToggleFileSelection: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onConfirmImport: (
        targetStoryId: Long?,
        newStoryTitle: String?,
        newStoryAuthor: String?,
        newStoryNarrator: String?,
        newStoryCategory: String?,
        newStoryDescription: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    // Trình chọn nhiều file âm thanh chuẩn SAF (không cần xin quyền bộ nhớ nhạy cảm)
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onFilesPicked(uris)
        }
    }

    // Trình chọn quét toàn bộ thư mục chứa audio từ thiết bị
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onFolderPicked(treeUri)
        }
    }

    var destinationMode by rememberSaveable {
        mutableStateOf(
            if (preselectedStoryId != null) ImportDestinationMode.EXISTING_STORY
            else ImportDestinationMode.FREE_LIBRARY
        )
    }
    var chosenStoryId by rememberSaveable {
        mutableStateOf(preselectedStoryId ?: stories.firstOrNull()?.story?.id)
    }
    var storyDropdownExpanded by remember { mutableStateOf(false) }

    var newTitle by rememberSaveable { mutableStateOf("") }
    var newAuthor by rememberSaveable { mutableStateOf("") }
    var newNarrator by rememberSaveable { mutableStateOf("") }
    var newCategory by rememberSaveable { mutableStateOf(AppConstants.DEFAULT_CATEGORIES.first()) }

    val selectedCount = importState.scannedFiles.count { it.isSelected && !it.isDuplicate }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("import_audio_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Thanh đầu trang
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("import_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Đóng màn hình nhập tệp")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nhập & Quét Tệp Audio",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Hỗ trợ MP3, WAV, M4A, AAC, OGG, FLAC",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cam kết an toàn tệp gốc
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Chỉ lưu đường dẫn tệp vào CSDL. Tuyệt đối không di chuyển, đổi tên hay xóa tệp gốc trên máy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 Nút chọn tệp lẻ hoặc chọn cả thư mục
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { multipleFilesLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_audio_files_button")
                ) {
                    Icon(Icons.Filled.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chọn tệp lẻ")
                }

                OutlinedButton(
                    onClick = { folderPickerLauncher.launch(null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_audio_folder_button")
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quét thư mục")
                }
            }

            if (importState.lastFolderLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Thư mục quét gần nhất: ${importState.lastFolderLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chọn đích lưu trữ cho các tệp quét được
            Text(
                text = "Gán tệp đã chọn vào:",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                FilterChip(
                    selected = destinationMode == ImportDestinationMode.FREE_LIBRARY,
                    onClick = { destinationMode = ImportDestinationMode.FREE_LIBRARY },
                    label = { Text("Thư viện tự do") }
                )
                FilterChip(
                    selected = destinationMode == ImportDestinationMode.EXISTING_STORY,
                    onClick = { destinationMode = ImportDestinationMode.EXISTING_STORY },
                    label = { Text("Truyện có sẵn") }
                )
                FilterChip(
                    selected = destinationMode == ImportDestinationMode.NEW_STORY,
                    onClick = { destinationMode = ImportDestinationMode.NEW_STORY },
                    label = { Text("Tạo truyện mới") }
                )
            }

            when (destinationMode) {
                ImportDestinationMode.EXISTING_STORY -> {
                    val currentSelectedStory = stories.find { it.story.id == chosenStoryId }
                        ?: stories.firstOrNull()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { storyDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentSelectedStory?.story?.title ?: "Chưa có bộ truyện nào",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = storyDropdownExpanded,
                            onDismissRequest = { storyDropdownExpanded = false }
                        ) {
                            stories.forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st.story.title) },
                                    onClick = {
                                        chosenStoryId = st.story.id
                                        storyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                ImportDestinationMode.NEW_STORY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Tên bộ truyện mới *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newAuthor,
                                onValueChange = { newAuthor = it },
                                label = { Text("Tác giả") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newNarrator,
                                onValueChange = { newNarrator = it },
                                label = { Text("Người đọc") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                ImportDestinationMode.FREE_LIBRARY -> Unit
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Danh sách tệp vừa quét được kèm Checkbox
            if (importState.isScanning) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Đang trích xuất Metadata & ảnh bìa nhúng…")
                    }
                }
            } else if (importState.scannedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bấm \"Chọn tệp lẻ\" hoặc \"Quét thư mục\" ở trên để đọc danh sách file âm thanh từ thiết bị.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kết quả quét (${importState.scannedFiles.size} tệp • Chọn $selectedCount)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(onClick = { onSelectAll(true) }) { Text("Chọn tất cả") }
                        TextButton(onClick = { onSelectAll(false) }) { Text("Bỏ chọn") }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(importState.scannedFiles, key = { it.uriString }) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !file.isDuplicate) {
                                    onToggleFileSelection(file.uriString)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = file.isSelected && !file.isDuplicate,
                                    onCheckedChange = {
                                        if (!file.isDuplicate) onToggleFileSelection(file.uriString)
                                    },
                                    enabled = !file.isDuplicate
                                )
                                CoverArtwork(
                                    coverPath = file.embeddedCoverPath,
                                    contentDescription = file.displayTitle,
                                    modifier = Modifier.size(44.dp),
                                    shape = MaterialTheme.shapes.small
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.displayTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${TimeFormatter.formatDuration(file.durationMs)} • ${TimeFormatter.formatFileSize(file.fileSize)} • ${file.author}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (file.isDuplicate) {
                                        Text(
                                            text = "Đã có trong thư viện (Chống trùng lặp)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        when (destinationMode) {
                            ImportDestinationMode.FREE_LIBRARY ->
                                onConfirmImport(null, null, null, null, null, null)
                            ImportDestinationMode.EXISTING_STORY ->
                                onConfirmImport(chosenStoryId ?: stories.firstOrNull()?.story?.id, null, null, null, null, null)
                            ImportDestinationMode.NEW_STORY ->
                                onConfirmImport(
                                    null,
                                    newTitle.ifBlank { "Bộ truyện Mới" },
                                    newAuthor,
                                    newNarrator,
                                    newCategory,
                                    null
                                )
                        }
                    },
                    enabled = selectedCount > 0 &&
                        (destinationMode != ImportDestinationMode.NEW_STORY || newTitle.isNotBlank()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("confirm_import_button")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nhập $selectedCount tệp đã chọn vào Thư viện")
                }
            }
        }
    }
}
