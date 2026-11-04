package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.player.PlayerUiState
import com.example.ui.viewmodel.DatabaseStorageState
import com.example.utils.AppConstants
import com.example.utils.TimeFormatter

// Màn hình MODULE 9: Cài đặt hệ thống, Giao diện Sáng/Tối, Bước nhảy tua, Dung lượng & Sao lưu/Khôi phục SQLite
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    isDarkMode: Boolean,
    playerState: PlayerUiState,
    storageState: DatabaseStorageState,
    onDarkModeChange: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onUpdateSkipSteps: (Int, Int) -> Unit,
    onQuickLocalBackup: () -> Unit,
    onQuickLocalRestore: () -> Unit,
    onExportBackupUri: (android.net.Uri) -> Unit,
    onImportRestoreUri: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    // Bộ chọn vị trí lưu file SQLite (.db) ra bộ nhớ máy
    val exportDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            onExportBackupUri(uri)
        }
    }

    // Bộ chọn file SQLite (.db) để khôi phục dữ liệu
    val importDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportRestoreUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Cài đặt Hệ thống",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // 1. Đổi giao diện Sáng / Tối (lưu vào bảng app_settings ngay khi đổi)
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Chế độ Tối (Dark Mode)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Phối màu Đen Obsidian - Tím Amethyst - Trắng dịu mắt ban đêm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            }
        }

        // 2. Tốc độ phát mặc định & Bước nhảy nút tua
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Trình phát & Bước nhảy nút tua",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Tốc độ đọc mặc định (Pitch Correction giữ giọng tự nhiên):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppConstants.PLAYBACK_SPEEDS.forEach { spd ->
                            FilterChip(
                                selected = playerState.playbackSpeed == spd,
                                onClick = { onSpeedChange(spd) },
                                label = { Text("${spd}x") }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Text(
                        text = "Bước nhảy tua lùi (Hiện tại: -${playerState.rewindSeconds} giây):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppConstants.REWIND_OPTIONS.forEach { sec ->
                            FilterChip(
                                selected = playerState.rewindSeconds == sec,
                                onClick = { onUpdateSkipSteps(sec, playerState.forwardSeconds) },
                                label = { Text("-${sec}s") },
                                modifier = Modifier.testTag("rewind_option_$sec")
                            )
                        }
                    }

                    Text(
                        text = "Bước nhảy tua tới (Hiện tại: +${playerState.forwardSeconds} giây):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppConstants.FORWARD_OPTIONS.forEach { sec ->
                            FilterChip(
                                selected = playerState.forwardSeconds == sec,
                                onClick = { onUpdateSkipSteps(playerState.rewindSeconds, sec) },
                                label = { Text("+${sec}s") },
                                modifier = Modifier.testTag("forward_option_$sec")
                            )
                        }
                    }
                }
            }
        }

        // 3. Dung lượng bộ nhớ CSDL & Sao lưu / Khôi phục SQLite
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cơ sở dữ liệu SQLite (7 bảng cốt lõi)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dung lượng đang chiếm dụng: ${TimeFormatter.formatFileSize(storageState.dbSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    storageState.latestLocalBackup?.let { backup ->
                        Text(
                            text = "Bản sao lưu dự phòng nội bộ gần nhất: ${TimeFormatter.formatTimestamp(backup.lastModified)} (${TimeFormatter.formatFileSize(backup.fileSize)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onQuickLocalBackup,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_backup_button")
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sao lưu nhanh")
                        }
                        OutlinedButton(
                            onClick = onQuickLocalRestore,
                            enabled = storageState.latestLocalBackup != null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_restore_button")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Khôi phục nhanh")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportDbLauncher.launch("bach_nhat_anh_audio_backup.db") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_db_file_button")
                        ) {
                            Text("Xuất tệp .db")
                        }
                        OutlinedButton(
                            onClick = { importDbLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_db_file_button")
                        ) {
                            Text("Nhập tệp .db")
                        }
                    }
                }
            }
        }

        // 4. Thông tin ứng dụng & Bản quyền đồ án sinh viên
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thông tin ứng dụng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Bạch Nhật Anh Audio — Phiên bản v1.0",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Đồ án môn học: Ứng dụng Java cho Công nghệ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Kiến trúc: Android Native, Room SQLite (7 bảng đồng nhất), Jetpack Media3 ExoPlayer & MediaSessionService, Material Design 3.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bản quyền © Sinh viên thực hiện • Hoạt động 100% Offline, tôn trọng tuyệt đối an toàn dữ liệu tệp gốc của người dùng.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
