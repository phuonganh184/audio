package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.BookmarkEntity
import com.example.data.entity.BookmarkWithDetails
import com.example.data.entity.ChapterEntity
import com.example.data.entity.ChapterWithStory
import com.example.data.entity.PlaylistItemWithDetails
import com.example.data.entity.PlaylistWithItems
import com.example.data.entity.ProgressWithDetails
import com.example.data.entity.StoryEntity
import com.example.data.entity.StoryWithChapters
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseBackupManager
import com.example.data.local.DatabaseSeeder
import com.example.data.repository.AudiobookRepository
import com.example.player.AudioPlayerManager
import com.example.player.PlayerUiState
import com.example.player.QueueTrack
import com.example.utils.AppConstants
import com.example.utils.AudioFileScanner
import com.example.utils.ScannedAudioFile
import com.example.utils.VietnameseTextUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 5 Khu vực điều hướng chính của ứng dụng
enum class AppTab(val route: String, val label: String) {
    HOME("home", "Trang chủ"),
    LIBRARY("library", "Thư viện"),
    PLAYLISTS("playlists", "Danh sách"),
    HISTORY("history", "Tiến trình"),
    SETTINGS("settings", "Cài đặt")
}

// Các tiêu chí sắp xếp danh sách truyện trong Thư viện
enum class StorySortOption(val label: String) {
    NEWEST("Mới thêm nhất"),
    NAME_AZ("Tên (A - Z)"),
    DURATION_DESC("Thời lượng dài → ngắn"),
    DURATION_ASC("Thời lượng ngắn → dài")
}

data class LibraryFilterState(
    val searchQuery: String = "",
    val selectedCategory: String = "Tất cả",
    val sortOption: StorySortOption = StorySortOption.NEWEST,
    val onlyFavorites: Boolean = false
)

data class ImportUiState(
    val isScanning: Boolean = false,
    val scannedFiles: List<ScannedAudioFile> = emptyList(),
    val lastFolderLabel: String = ""
)

data class DatabaseStorageState(
    val dbSizeBytes: Long = 0L,
    val latestLocalBackup: DatabaseBackupManager.LocalBackupInfo? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    val repository = AudiobookRepository(database)
    val playerManager = AudioPlayerManager(appContext, repository)

    val playerUiState: StateFlow<PlayerUiState> = playerManager.uiState

    // Điều hướng màn hình & Modal
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _selectedStoryId = MutableStateFlow<Long?>(null)
    val selectedStoryId: StateFlow<Long?> = _selectedStoryId.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    private val _isFullPlayerOpen = MutableStateFlow(false)
    val isFullPlayerOpen: StateFlow<Boolean> = _isFullPlayerOpen.asStateFlow()

    private val _isImportScreenOpen = MutableStateFlow(false)
    val isImportScreenOpen: StateFlow<Boolean> = _isImportScreenOpen.asStateFlow()

    private val _preselectedImportStoryId = MutableStateFlow<Long?>(null)
    val preselectedImportStoryId: StateFlow<Long?> = _preselectedImportStoryId.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Cài đặt hệ thống từ bảng app_settings
    private val settingsMapFlow = repository.allSettingsFlow.map { list ->
        list.associate { it.settingKey to it.settingValue }
    }

    val isDarkMode: StateFlow<Boolean> = settingsMapFlow
        .map { map -> map[AppConstants.KEY_DARK_MODE]?.toBooleanStrictOrNull() ?: AppConstants.DEFAULT_DARK_MODE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConstants.DEFAULT_DARK_MODE)

    // Bộ lọc tìm kiếm, thể loại & sắp xếp Thư viện
    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState: StateFlow<LibraryFilterState> = _filterState.asStateFlow()

    val allStories: StateFlow<List<StoryWithChapters>> = repository.allStoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredStories: StateFlow<List<StoryWithChapters>> = combine(
        allStories,
        _filterState
    ) { stories, filter ->
        val filtered = stories.filter { item ->
            val matchesFav = !filter.onlyFavorites || item.story.isFavorite == 1
            val matchesCategory = filter.selectedCategory == "Tất cả" ||
                item.story.category.equals(filter.selectedCategory, ignoreCase = true)

            val query = filter.searchQuery
            val matchesQuery = query.isBlank() ||
                VietnameseTextUtils.matchesSearch(item.story.title, query) ||
                VietnameseTextUtils.matchesSearch(item.story.author, query) ||
                VietnameseTextUtils.matchesSearch(item.story.narrator, query) ||
                item.chapters.any { ch -> VietnameseTextUtils.matchesSearch(ch.title, query) }

            matchesFav && matchesCategory && matchesQuery
        }

        when (filter.sortOption) {
            StorySortOption.NEWEST -> filtered.sortedByDescending { it.story.createdAt }
            StorySortOption.NAME_AZ -> filtered.sortedBy { VietnameseTextUtils.normalize(it.story.title) }
            StorySortOption.DURATION_DESC -> filtered.sortedByDescending { it.totalDurationMs }
            StorySortOption.DURATION_ASC -> filtered.sortedBy { it.totalDurationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCategories: StateFlow<List<String>> = allStories.map { list ->
        val dynamic = list.map { it.story.category }.filter { it.isNotBlank() }
        (listOf("Tất cả") + (AppConstants.DEFAULT_CATEGORIES + dynamic).distinct())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Tất cả"))

    val selectedStoryDetail: StateFlow<StoryWithChapters?> = _selectedStoryId
        .flatMapLatest { id -> if (id != null) repository.observeStoryById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedPlaylistDetail: StateFlow<PlaylistWithItems?> = _selectedPlaylistId
        .flatMapLatest { id -> if (id != null) repository.observePlaylistById(id) else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val favoriteStories: StateFlow<List<StoryWithChapters>> = repository.favoriteStoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChapters: StateFlow<List<ChapterWithStory>> = repository.favoriteChaptersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChaptersWithStory: StateFlow<List<ChapterWithStory>> = repository.allChaptersWithStoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestResumeProgress: StateFlow<ProgressWithDetails?> = repository.latestProgressFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val progressHistory: StateFlow<List<ProgressWithDetails>> = repository.progressHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistWithItems>> = repository.allPlaylistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkWithDetails>> = repository.allBookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Danh sách Bookmark của chương hiện đang phát trong Player
    val currentChapterBookmarks: StateFlow<List<BookmarkEntity>> = playerUiState
        .map { it.currentTrack?.chapter?.id }
        .distinctUntilChanged()
        .flatMapLatest { chapterId ->
            if (chapterId != null) repository.observeBookmarksForChapter(chapterId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    private val _storageState = MutableStateFlow(DatabaseStorageState())
    val storageState: StateFlow<DatabaseStorageState> = _storageState.asStateFlow()

    init {
        viewModelScope.launch {
            DatabaseSeeder.seedIfEmpty(appContext, database)
            val savedFolder = repository.getSetting(AppConstants.KEY_LAST_SCAN_FOLDER, "")
            _importState.update { it.copy(lastFolderLabel = savedFolder) }
            refreshDatabaseStorageInfo()
        }
    }

    // --- Điều hướng giao diện ---

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        _selectedStoryId.value = null
        _selectedPlaylistId.value = null
    }

    fun openStoryDetail(storyId: Long) {
        _selectedStoryId.value = storyId
    }

    fun closeStoryDetail() {
        _selectedStoryId.value = null
    }

    fun openPlaylistDetail(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
    }

    fun closePlaylistDetail() {
        _selectedPlaylistId.value = null
    }

    fun openFullPlayer() {
        if (playerUiState.value.hasActiveTrack) {
            _isFullPlayerOpen.value = true
        }
    }

    fun closeFullPlayer() {
        _isFullPlayerOpen.value = false
    }

    fun openImportScreen(targetStoryId: Long? = null) {
        _preselectedImportStoryId.value = targetStoryId
        _isImportScreenOpen.value = true
    }

    fun closeImportScreen() {
        _isImportScreenOpen.value = false
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
        playerManager.clearError()
    }

    // --- Chuyển đổi Dark / Light Mode & Cài đặt ---

    fun toggleDarkMode() {
        val next = !isDarkMode.value
        viewModelScope.launch {
            repository.setSetting(AppConstants.KEY_DARK_MODE, next.toString())
            refreshDatabaseStorageInfo()
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSetting(AppConstants.KEY_DARK_MODE, enabled.toString())
            refreshDatabaseStorageInfo()
        }
    }

    // --- Tìm kiếm & Lọc Thư viện ---

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun updateSelectedCategory(category: String) {
        _filterState.update { it.copy(selectedCategory = category) }
    }

    fun updateSortOption(option: StorySortOption) {
        _filterState.update { it.copy(sortOption = option) }
    }

    fun toggleOnlyFavoritesFilter() {
        _filterState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    // --- Nghiệp vụ Phát Truyện, Nghe tiếp (Resume), Playlist & Bookmark ---

    // Phát một bộ truyện từ chương chỉ định (hoặc tự động khôi phục vị trí nghe dở nếu không truyền chương)
    fun playStory(
        storyWithChapters: StoryWithChapters,
        chapterId: Long? = null,
        startPositionMs: Long? = null,
        openExpandedPlayer: Boolean = false
    ) {
        val chapters = storyWithChapters.sortedChapters
        if (chapters.isEmpty()) {
            showMessage("Bộ truyện chưa có chương âm thanh nào. Hãy thêm tệp Audio!")
            return
        }

        viewModelScope.launch {
            val savedProgress = repository.getProgressForStory(storyWithChapters.story.id)
            val resolvedChapterId = chapterId ?: savedProgress?.chapterId ?: chapters.first().id
            val startIdx = chapters.indexOfFirst { it.id == resolvedChapterId }.coerceAtLeast(0)
            val resolvedPosition = startPositionMs ?: if (chapterId == null && savedProgress != null &&
                savedProgress.chapterId == chapters[startIdx].id
            ) {
                savedProgress.positionMs
            } else {
                0L
            }

            val queue = chapters.map { ch -> QueueTrack(chapter = ch, story = storyWithChapters.story) }
            playerManager.playQueue(
                tracks = queue,
                startIndex = startIdx,
                startPositionMs = resolvedPosition,
                autoPlay = true
            )
            if (openExpandedPlayer) {
                _isFullPlayerOpen.value = true
            }
        }
    }

    // Khối "Nghe tiếp" trên Trang chủ hoặc Lịch sử: Nhảy đúng đến chương và mili-giây đã lưu
    fun resumeFromProgress(progressDetails: ProgressWithDetails, openExpandedPlayer: Boolean = true) {
        viewModelScope.launch {
            val chapters = repository.getChaptersForStoryOnce(progressDetails.story.id)
            if (chapters.isEmpty()) {
                showMessage("Không tìm thấy danh sách chương của bộ truyện này.")
                return@launch
            }
            val startIdx = chapters.indexOfFirst { it.id == progressDetails.chapter.id }.coerceAtLeast(0)
            val queue = chapters.map { ch -> QueueTrack(chapter = ch, story = progressDetails.story) }
            playerManager.playQueue(
                tracks = queue,
                startIndex = startIdx,
                startPositionMs = progressDetails.progress.positionMs,
                autoPlay = true
            )
            if (openExpandedPlayer) {
                _isFullPlayerOpen.value = true
            }
        }
    }

    fun playPlaylist(playlistWithItems: PlaylistWithItems, startIndex: Int = 0) {
        val sorted = playlistWithItems.sortedItems
        if (sorted.isEmpty()) {
            showMessage("Playlist đang trống. Hãy thêm chương vào Playlist trước khi phát!")
            return
        }
        val queue = sorted.map {
            QueueTrack(
                chapter = it.chapterWithStory.chapter,
                story = it.chapterWithStory.story
            )
        }
        playerManager.playQueue(tracks = queue, startIndex = startIndex, startPositionMs = 0L, autoPlay = true)
        _isFullPlayerOpen.value = true
    }

    fun playChapterList(items: List<ChapterWithStory>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        val queue = items.map { QueueTrack(chapter = it.chapter, story = it.story) }
        playerManager.playQueue(tracks = queue, startIndex = startIndex, startPositionMs = 0L, autoPlay = true)
    }

    fun jumpToBookmark(bookmark: BookmarkEntity) {
        val activeChapterId = playerUiState.value.currentTrack?.chapter?.id
        if (activeChapterId == bookmark.chapterId) {
            playerManager.seekTo(bookmark.positionMs)
            showMessage("Đã nhảy tới mốc ${com.example.utils.TimeFormatter.formatDuration(bookmark.positionMs)}")
        }
    }

    fun jumpToBookmarkWithDetails(details: BookmarkWithDetails) {
        viewModelScope.launch {
            val story = details.chapterWithStory.story
            val chapter = details.chapterWithStory.chapter
            val chapters = repository.getChaptersForStoryOnce(story.id)
            val idx = chapters.indexOfFirst { it.id == chapter.id }.coerceAtLeast(0)
            val queue = if (chapters.isNotEmpty()) {
                chapters.map { QueueTrack(it, story) }
            } else {
                listOf(QueueTrack(chapter, story))
            }
            playerManager.playQueue(
                tracks = queue,
                startIndex = idx,
                startPositionMs = details.bookmark.positionMs,
                autoPlay = true
            )
            _isFullPlayerOpen.value = true
        }
    }

    fun addBookmarkAtCurrentPosition(note: String) {
        val current = playerUiState.value.currentTrack ?: return
        val pos = playerUiState.value.currentPositionMs
        viewModelScope.launch {
            repository.addBookmark(current.chapter.id, pos, note)
            refreshDatabaseStorageInfo()
            showMessage("Đã lưu dấu trang tại ${com.example.utils.TimeFormatter.formatDuration(pos)}")
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmarkId)
            refreshDatabaseStorageInfo()
        }
    }

    // --- Yêu thích & Quản lý Truyện / Chương ---

    fun toggleStoryFavorite(story: StoryEntity) {
        viewModelScope.launch {
            repository.toggleStoryFavorite(story.id, story.isFavorite == 1)
        }
    }

    fun toggleChapterFavorite(chapter: ChapterEntity) {
        val nextFav = if (chapter.isFavorite == 1) 0 else 1
        viewModelScope.launch {
            repository.toggleChapterFavorite(chapter.id, chapter.isFavorite == 1)
            playerManager.refreshCurrentTrackFavorite(chapter.id, nextFav)
        }
    }

    fun createNewStory(
        title: String,
        author: String,
        narrator: String,
        category: String,
        description: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createStory(title, author, narrator, category, description)
            refreshDatabaseStorageInfo()
            showMessage("Đã tạo bộ truyện \"$title\"")
        }
    }

    fun deleteStory(storyId: Long) {
        viewModelScope.launch {
            if (_selectedStoryId.value == storyId) {
                _selectedStoryId.value = null
            }
            repository.deleteStoryFromLibrary(storyId)
            refreshDatabaseStorageInfo()
            showMessage("Đã xóa khỏi thư viện ứng dụng (File gốc trên máy được giữ nguyên an toàn).")
        }
    }

    fun deleteChapter(chapterId: Long) {
        viewModelScope.launch {
            repository.deleteChapterFromLibrary(chapterId)
            refreshDatabaseStorageInfo()
            showMessage("Đã gỡ chương khỏi thư viện (File gốc trên thiết bị vẫn an toàn).")
        }
    }

    // --- Quản lý Playlist ---

    fun createPlaylist(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            refreshDatabaseStorageInfo()
            showMessage("Đã tạo danh sách phát \"$name\"")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
            repository.deletePlaylist(playlistId)
            refreshDatabaseStorageInfo()
            showMessage("Đã xóa danh sách phát (Không ảnh hưởng tới file âm thanh gốc).")
        }
    }

    fun addChaptersToPlaylist(playlistId: Long, chapterIds: List<Long>) {
        viewModelScope.launch {
            val count = repository.addChaptersToPlaylist(playlistId, chapterIds)
            refreshDatabaseStorageInfo()
            showMessage(
                if (count > 0) "Đã thêm $count chương vào danh sách phát."
                else "Chương này đã có sẵn trong danh sách phát."
            )
        }
    }

    fun removePlaylistItem(itemId: Long) {
        viewModelScope.launch {
            repository.removePlaylistItem(itemId)
            refreshDatabaseStorageInfo()
        }
    }

    fun movePlaylistItem(items: List<PlaylistItemWithDetails>, index: Int, moveUp: Boolean) {
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (index !in items.indices || targetIndex !in items.indices) return
        viewModelScope.launch {
            repository.swapPlaylistItemsOrder(items[index], items[targetIndex])
        }
    }

    // --- Quét tệp & Nhập Thư viện (MODULE 3) ---

    fun scanSelectedFileUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _importState.update { it.copy(isScanning = true) }
            val existing = repository.getExistingFilePaths()
            val scanned = AudioFileScanner.scanDocumentUris(appContext, uris, existing)
            _importState.update {
                it.copy(isScanning = false, scannedFiles = scanned)
            }
            if (scanned.isEmpty()) {
                showMessage("Không tìm thấy tệp âm thanh hỗ trợ (MP3, WAV, M4A, AAC).")
            }
        }
    }

    fun scanSelectedFolderUri(treeUri: Uri) {
        viewModelScope.launch {
            _importState.update { it.copy(isScanning = true) }
            val existing = repository.getExistingFilePaths()
            val scanned = AudioFileScanner.scanFolderTreeUri(appContext, treeUri, existing)
            val folderLabel = treeUri.lastPathSegment ?: treeUri.toString()
            repository.setSetting(AppConstants.KEY_LAST_SCAN_FOLDER, folderLabel)
            _importState.update {
                it.copy(
                    isScanning = false,
                    scannedFiles = scanned,
                    lastFolderLabel = folderLabel
                )
            }
            if (scanned.isEmpty()) {
                showMessage("Thư mục không chứa tệp âm thanh hỗ trợ (MP3, WAV, M4A, AAC).")
            }
        }
    }

    fun toggleScannedFileSelection(uriString: String) {
        _importState.update { state ->
            state.copy(
                scannedFiles = state.scannedFiles.map { file ->
                    if (file.uriString == uriString && !file.isDuplicate) {
                        file.copy(isSelected = !file.isSelected)
                    } else {
                        file
                    }
                }
            )
        }
    }

    fun selectAllScannedFiles(selectAll: Boolean) {
        _importState.update { state ->
            state.copy(
                scannedFiles = state.scannedFiles.map { file ->
                    if (!file.isDuplicate) file.copy(isSelected = selectAll) else file
                }
            )
        }
    }

    fun clearScannedFiles() {
        _importState.update { it.copy(scannedFiles = emptyList()) }
    }

    fun confirmImportSelectedFiles(
        targetStoryId: Long?,
        newStoryTitle: String? = null,
        newStoryAuthor: String? = null,
        newStoryNarrator: String? = null,
        newStoryCategory: String? = null,
        newStoryDescription: String? = null
    ) {
        val selected = _importState.value.scannedFiles.filter { it.isSelected && !it.isDuplicate }
        if (selected.isEmpty()) {
            showMessage("Vui lòng tích chọn ít nhất 1 tệp âm thanh chưa trùng lặp.")
            return
        }
        viewModelScope.launch {
            val count = repository.importScannedFiles(
                selectedFiles = selected,
                targetStoryId = targetStoryId,
                newStoryTitle = newStoryTitle,
                newStoryAuthor = newStoryAuthor,
                newStoryNarrator = newStoryNarrator,
                newStoryCategory = newStoryCategory,
                newStoryDescription = newStoryDescription
            )
            _importState.update { it.copy(scannedFiles = emptyList()) }
            _isImportScreenOpen.value = false
            refreshDatabaseStorageInfo()
            showMessage("Đã nhập thành công $count tệp âm thanh vào thư viện!")
        }
    }

    // --- Sao lưu & Khôi phục SQLite Database (MODULE 9) ---

    fun refreshDatabaseStorageInfo() {
        viewModelScope.launch {
            val size = DatabaseBackupManager.getDatabaseSizeBytes(appContext)
            val localBackup = DatabaseBackupManager.getLatestLocalBackup(appContext)
            _storageState.value = DatabaseStorageState(
                dbSizeBytes = size,
                latestLocalBackup = localBackup
            )
        }
    }

    fun createQuickLocalBackup() {
        viewModelScope.launch {
            playerManager.persistCurrentProgressNow()
            DatabaseBackupManager.createLocalSnapshotBackup(appContext)
                .onSuccess {
                    refreshDatabaseStorageInfo()
                    showMessage("Đã tạo bản sao lưu dự phòng nội bộ thành công!")
                }
                .onFailure { err ->
                    showMessage("Lỗi sao lưu: ${err.localizedMessage}")
                }
        }
    }

    fun exportDatabaseBackup(destinationUri: Uri) {
        viewModelScope.launch {
            playerManager.persistCurrentProgressNow()
            DatabaseBackupManager.exportDatabaseToUri(appContext, destinationUri)
                .onSuccess { bytes ->
                    refreshDatabaseStorageInfo()
                    showMessage("Đã xuất bản sao lưu CSDL (${com.example.utils.TimeFormatter.formatFileSize(bytes)})!")
                }
                .onFailure { err ->
                    showMessage("Không thể sao lưu CSDL: ${err.localizedMessage}")
                }
        }
    }

    fun restoreDatabaseFromUri(sourceUri: Uri, onRestartRequired: () -> Unit) {
        viewModelScope.launch {
            playerManager.stopPlayback()
            DatabaseBackupManager.restoreDatabaseFromUri(appContext, sourceUri)
                .onSuccess {
                    onRestartRequired()
                }
                .onFailure { err ->
                    showMessage("Khôi phục thất bại: ${err.localizedMessage}")
                }
        }
    }

    fun restoreFromQuickLocalBackup(onRestartRequired: () -> Unit) {
        viewModelScope.launch {
            playerManager.stopPlayback()
            DatabaseBackupManager.restoreFromLocalSnapshot(appContext)
                .onSuccess {
                    onRestartRequired()
                }
                .onFailure { err ->
                    showMessage("Khôi phục thất bại: ${err.localizedMessage}")
                }
        }
    }

    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }
}
