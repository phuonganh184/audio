package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.FullPlayerSheet
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportAudioScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoryDetailScreen
import com.example.ui.theme.BachNhatAnhAudioTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(application)
            )
            val isDarkMode by mainViewModel.isDarkMode.collectAsStateWithLifecycle()

            BachNhatAnhAudioTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BachNhatAnhAudioApp(
                        viewModel = mainViewModel,
                        onRestartAppAfterRestore = { restartApplicationCleanly() }
                    )
                }
            }
        }
    }

    // Khởi động lại Activity sạch sẽ sau khi khôi phục thành công tệp SQLite (.db)
    private fun restartApplicationCleanly() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
    }
}

private data class NavDestinationItem(
    val tab: AppTab,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_ITEMS = listOf(
    NavDestinationItem(AppTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(AppTab.LIBRARY, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    NavDestinationItem(AppTab.PLAYLISTS, Icons.AutoMirrored.Filled.QueueMusic, Icons.AutoMirrored.Outlined.QueueMusic),
    NavDestinationItem(AppTab.HISTORY, Icons.Filled.History, Icons.Outlined.History),
    NavDestinationItem(AppTab.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun BachNhatAnhAudioApp(
    viewModel: MainViewModel,
    onRestartAppAfterRestore: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val playerState by viewModel.playerUiState.collectAsStateWithLifecycle()

    val allStories by viewModel.allStories.collectAsStateWithLifecycle()
    val filteredStories by viewModel.filteredStories.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val categories by viewModel.availableCategories.collectAsStateWithLifecycle()

    val selectedStoryDetail by viewModel.selectedStoryDetail.collectAsStateWithLifecycle()
    val selectedPlaylistDetail by viewModel.selectedPlaylistDetail.collectAsStateWithLifecycle()
    val favoriteStories by viewModel.favoriteStories.collectAsStateWithLifecycle()
    val favoriteChapters by viewModel.favoriteChapters.collectAsStateWithLifecycle()
    val allChapters by viewModel.allChaptersWithStory.collectAsStateWithLifecycle()

    val latestResume by viewModel.latestResumeProgress.collectAsStateWithLifecycle()
    val progressHistory by viewModel.progressHistory.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val allBookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val currentChapterBookmarks by viewModel.currentChapterBookmarks.collectAsStateWithLifecycle()

    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsStateWithLifecycle()
    val isImportScreenOpen by viewModel.isImportScreenOpen.collectAsStateWithLifecycle()
    val preselectedImportStoryId by viewModel.preselectedImportStoryId.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val storageState by viewModel.storageState.collectAsStateWithLifecycle()

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage, playerState.errorMessage) {
        val message = snackbarMessage ?: playerState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!isExpandedScreen) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (playerState.hasActiveTrack) {
                                MiniPlayerBar(
                                    playerState = playerState,
                                    onOpenFullPlayer = { viewModel.openFullPlayer() },
                                    onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                                    onRewind = { viewModel.playerManager.rewind() },
                                    onNext = { viewModel.playerManager.skipToNext() }
                                )
                            }
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                NAV_ITEMS.forEach { item ->
                                    val selected = currentTab == item.tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(item.tab) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.tab.label
                                            )
                                        },
                                        label = { Text(item.tab.label, maxLines = 1) },
                                        modifier = Modifier.testTag("nav_item_${item.tab.route}")
                                    )
                                }
                            }
                        }
                    } else if (playerState.hasActiveTrack) {
                        MiniPlayerBar(
                            playerState = playerState,
                            onOpenFullPlayer = { viewModel.openFullPlayer() },
                            onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                            onRewind = { viewModel.playerManager.rewind() },
                            onNext = { viewModel.playerManager.skipToNext() },
                            modifier = Modifier.navigationBarsPadding()
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .testTag("side_navigation_rail")
                        ) {
                            NAV_ITEMS.forEach { item ->
                                val selected = currentTab == item.tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(item.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.tab.label
                                        )
                                    },
                                    label = { Text(item.tab.label) },
                                    modifier = Modifier.testTag("rail_item_${item.tab.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val activeStoryDetail = selectedStoryDetail
                        if (activeStoryDetail != null) {
                            StoryDetailScreen(
                                storyWithChapters = activeStoryDetail,
                                activeChapterId = playerState.currentTrack?.chapter?.id,
                                playlists = allPlaylists,
                                onBack = { viewModel.closeStoryDetail() },
                                onPlayStoryFromLastProgress = {
                                    viewModel.playStory(activeStoryDetail, openExpandedPlayer = true)
                                },
                                onPlaySpecificChapter = { ch ->
                                    viewModel.playStory(
                                        storyWithChapters = activeStoryDetail,
                                        chapterId = ch.id,
                                        startPositionMs = 0L,
                                        openExpandedPlayer = true
                                    )
                                },
                                onToggleStoryFavorite = { viewModel.toggleStoryFavorite(it) },
                                onToggleChapterFavorite = { viewModel.toggleChapterFavorite(it) },
                                onAddChapterToPlaylist = { playlistId, chapterId ->
                                    viewModel.addChaptersToPlaylist(playlistId, listOf(chapterId))
                                },
                                onImportChaptersHere = { storyId ->
                                    viewModel.openImportScreen(storyId)
                                },
                                onDeleteChapter = { viewModel.deleteChapter(it) },
                                onDeleteStory = { viewModel.deleteStory(it) }
                            )
                        } else {
                            when (currentTab) {
                                AppTab.HOME -> HomeScreen(
                                    isDarkMode = isDarkMode,
                                    stories = allStories,
                                    favoriteStories = favoriteStories,
                                    latestProgress = latestResume,
                                    bookmarkCount = allBookmarks.size,
                                    onToggleTheme = { viewModel.toggleDarkMode() },
                                    onResumeListening = { viewModel.resumeFromProgress(it, openExpandedPlayer = true) },
                                    onOpenImport = { viewModel.openImportScreen(null) },
                                    onOpenLibrary = { viewModel.selectTab(AppTab.LIBRARY) },
                                    onSelectStory = { viewModel.openStoryDetail(it) },
                                    onPlayStory = { viewModel.playStory(it, openExpandedPlayer = false) },
                                    onToggleStoryFavorite = { viewModel.toggleStoryFavorite(it) }
                                )

                                AppTab.LIBRARY -> LibraryScreen(
                                    stories = filteredStories,
                                    filterState = filterState,
                                    categories = categories,
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onCategorySelect = { viewModel.updateSelectedCategory(it) },
                                    onSortChange = { viewModel.updateSortOption(it) },
                                    onToggleFavoritesOnly = { viewModel.toggleOnlyFavoritesFilter() },
                                    onOpenImport = { viewModel.openImportScreen(null) },
                                    onCreateStory = { title, author, narrator, cat, desc ->
                                        viewModel.createNewStory(title, author, narrator, cat, desc)
                                    },
                                    onSelectStory = { viewModel.openStoryDetail(it) },
                                    onPlayStory = { viewModel.playStory(it, openExpandedPlayer = false) },
                                    onToggleStoryFavorite = { viewModel.toggleStoryFavorite(it) }
                                )

                                AppTab.PLAYLISTS -> PlaylistsScreen(
                                    playlists = allPlaylists,
                                    selectedPlaylist = selectedPlaylistDetail,
                                    allChapters = allChapters,
                                    favoriteStories = favoriteStories,
                                    favoriteChapters = favoriteChapters,
                                    repeatMode = playerState.repeatMode,
                                    shuffleEnabled = playerState.shuffleEnabled,
                                    onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                    onSelectPlaylist = { viewModel.openPlaylistDetail(it) },
                                    onClosePlaylistDetail = { viewModel.closePlaylistDetail() },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                    onPlayPlaylist = { pl, idx -> viewModel.playPlaylist(pl, idx) },
                                    onAddChaptersToPlaylist = { plId, chIds ->
                                        viewModel.addChaptersToPlaylist(plId, chIds)
                                    },
                                    onRemovePlaylistItem = { viewModel.removePlaylistItem(it) },
                                    onMovePlaylistItem = { items, idx, up ->
                                        viewModel.movePlaylistItem(items, idx, up)
                                    },
                                    onCycleRepeat = { viewModel.playerManager.cycleRepeatMode() },
                                    onToggleShuffle = { viewModel.playerManager.toggleShuffle() },
                                    onSelectStory = { viewModel.openStoryDetail(it) },
                                    onPlayStory = { viewModel.playStory(it, openExpandedPlayer = false) },
                                    onToggleStoryFavorite = { viewModel.toggleStoryFavorite(it) },
                                    onPlayFavoriteChapters = { list, idx ->
                                        viewModel.playChapterList(list, idx)
                                    },
                                    onToggleChapterFavorite = { viewModel.toggleChapterFavorite(it) }
                                )

                                AppTab.HISTORY -> HistoryScreen(
                                    progressHistory = progressHistory,
                                    bookmarks = allBookmarks,
                                    onResumeProgress = { viewModel.resumeFromProgress(it, openExpandedPlayer = true) },
                                    onJumpToBookmark = { viewModel.jumpToBookmarkWithDetails(it) },
                                    onDeleteBookmark = { viewModel.deleteBookmark(it) }
                                )

                                AppTab.SETTINGS -> SettingsScreen(
                                    isDarkMode = isDarkMode,
                                    playerState = playerState,
                                    storageState = storageState,
                                    onDarkModeChange = { viewModel.setDarkMode(it) },
                                    onSpeedChange = { viewModel.playerManager.setPlaybackSpeed(it) },
                                    onUpdateSkipSteps = { rew, fwd ->
                                        viewModel.playerManager.updateSkipSteps(rew, fwd)
                                    },
                                    onQuickLocalBackup = { viewModel.createQuickLocalBackup() },
                                    onQuickLocalRestore = {
                                        viewModel.restoreFromQuickLocalBackup(onRestartAppAfterRestore)
                                    },
                                    onExportBackupUri = { viewModel.exportDatabaseBackup(it) },
                                    onImportRestoreUri = { uri ->
                                        viewModel.restoreDatabaseFromUri(uri, onRestartAppAfterRestore)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Màn hình Quét & Nhập tệp Audio dạng Full Overlay
        AnimatedVisibility(
            visible = isImportScreenOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            ImportAudioScreen(
                importState = importState,
                stories = allStories,
                preselectedStoryId = preselectedImportStoryId,
                onClose = { viewModel.closeImportScreen() },
                onFilesPicked = { viewModel.scanSelectedFileUris(it) },
                onFolderPicked = { viewModel.scanSelectedFolderUri(it) },
                onToggleFileSelection = { viewModel.toggleScannedFileSelection(it) },
                onSelectAll = { viewModel.selectAllScannedFiles(it) },
                onConfirmImport = { storyId, title, author, narrator, cat, desc ->
                    viewModel.confirmImportSelectedFiles(storyId, title, author, narrator, cat, desc)
                }
            )
        }

        // Màn hình Trình phát Chi tiết dạng Full Sheet trượt lên
        AnimatedVisibility(
            visible = isFullPlayerOpen && playerState.hasActiveTrack,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullPlayerSheet(
                playerState = playerState,
                chapterBookmarks = currentChapterBookmarks,
                onDismiss = { viewModel.closeFullPlayer() },
                onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                onSeekTo = { viewModel.playerManager.seekTo(it) },
                onRewind = { viewModel.playerManager.rewind() },
                onForward = { viewModel.playerManager.fastForward() },
                onPrevious = { viewModel.playerManager.skipToPrevious() },
                onNext = { viewModel.playerManager.skipToNext() },
                onSpeedChange = { viewModel.playerManager.setPlaybackSpeed(it) },
                onVolumeChange = { viewModel.playerManager.setVolume(it) },
                onCycleRepeat = { viewModel.playerManager.cycleRepeatMode() },
                onToggleShuffle = { viewModel.playerManager.toggleShuffle() },
                onToggleChapterFavorite = { viewModel.toggleChapterFavorite(it) },
                onStartSleepTimerMinutes = { viewModel.playerManager.startSleepTimerMinutes(it) },
                onStartSleepTimerEndOfChapter = { viewModel.playerManager.startSleepTimerEndOfChapter() },
                onCancelSleepTimer = { viewModel.playerManager.cancelSleepTimer() },
                onAddBookmark = { viewModel.addBookmarkAtCurrentPosition(it) },
                onJumpToBookmark = { viewModel.jumpToBookmark(it) },
                onDeleteBookmark = { viewModel.deleteBookmark(it) }
            )
        }
    }
}
