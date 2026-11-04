package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.R
import com.example.data.repository.AudiobookRepository
import com.example.utils.AppConstants
import com.example.utils.AudioFileScanner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

// Bộ quản lý điều khiển trình phát trung tâm: Kết nối MediaSessionService, đồng bộ UI, lưu tiến trình & Hẹn giờ tắt
class AudioPlayerManager(
    context: Context,
    private val repository: AudiobookRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private val controllerReady = CompletableDeferred<MediaController>()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var positionTickerJob: Job? = null
    private var periodicSaveJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        connectToService()
        restorePersistedPlayerSettings()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, PlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    attachPlayerListener(controller)
                    applySavedSettingsToController(controller)
                    if (!controllerReady.isCompleted) {
                        controllerReady.complete(controller)
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(errorMessage = "Không thể kết nối dịch vụ phát âm thanh: ${e.localizedMessage}")
                    }
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun restorePersistedPlayerSettings() {
        scope.launch {
            val savedSpeed = repository.getSetting(AppConstants.KEY_DEFAULT_SPEED, "1.0")
                .toFloatOrNull() ?: AppConstants.DEFAULT_SPEED
            val savedVolume = repository.getSetting(AppConstants.KEY_VOLUME_LEVEL, "1.0")
                .toFloatOrNull() ?: AppConstants.DEFAULT_VOLUME
            val savedRewind = repository.getSetting(AppConstants.KEY_REWIND_SECONDS, "10")
                .toIntOrNull() ?: AppConstants.DEFAULT_REWIND_SEC
            val savedForward = repository.getSetting(AppConstants.KEY_FORWARD_SECONDS, "30")
                .toIntOrNull() ?: AppConstants.DEFAULT_FORWARD_SEC
            val savedRepeat = runCatching {
                RepeatModeOption.valueOf(repository.getSetting(AppConstants.KEY_REPEAT_MODE, "OFF"))
            }.getOrDefault(RepeatModeOption.OFF)
            val savedShuffle = repository.getSetting(AppConstants.KEY_SHUFFLE_ENABLED, "false")
                .toBooleanStrictOrNull() ?: false

            _uiState.update {
                it.copy(
                    playbackSpeed = savedSpeed.coerceIn(0.5f, 2.5f),
                    volume = savedVolume.coerceIn(0f, 1f),
                    rewindSeconds = savedRewind,
                    forwardSeconds = savedForward,
                    repeatMode = savedRepeat,
                    shuffleEnabled = savedShuffle
                )
            }
            mediaController?.let { applySavedSettingsToController(it) }
        }
    }

    private fun applySavedSettingsToController(controller: MediaController) {
        val state = _uiState.value
        controller.setPlaybackParameters(PlaybackParameters(state.playbackSpeed, 1.0f))
        controller.volume = state.volume
        controller.repeatMode = when (state.repeatMode) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
        }
        controller.shuffleModeEnabled = state.shuffleEnabled
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val pos = controller.currentPosition.coerceAtLeast(0L)
                val dur = controller.duration.coerceAtLeast(0L)
                _uiState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        currentPositionMs = pos,
                        durationMs = if (dur > 0L) dur else it.currentTrack?.chapter?.durationMs ?: 0L
                    )
                }
                if (isPlaying) {
                    startPositionTicker()
                    startPeriodicProgressSaver()
                } else {
                    stopPositionTicker()
                    stopPeriodicProgressSaver()
                    // Lưu tiến trình ngay lập tức khi người dùng nhấn Tạm dừng (Pause)
                    persistCurrentProgressNow()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Nếu đang bật hẹn giờ "Khi kết thúc chương hiện tại" và vừa tự động chuyển sang bài kế
                if (_uiState.value.sleepTimer.isActive &&
                    _uiState.value.sleepTimer.endOfCurrentChapter &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) {
                    val prevIdx = (controller.currentMediaItemIndex - 1).coerceAtLeast(0)
                    val prevTrack = _uiState.value.queue.getOrNull(prevIdx)
                    prevTrack?.let {
                        scope.launch {
                            repository.markChapterCompleted(it.chapter.id, true)
                        }
                    }
                    controller.pause()
                    cancelSleepTimer()
                }

                val newIdx = controller.currentMediaItemIndex
                val newTrack = _uiState.value.queue.getOrNull(newIdx)
                if (newTrack != null) {
                    // Kiểm tra tính khả dụng của file chương tiếp theo
                    if (!AudioFileScanner.isAudioSourceAccessible(appContext, newTrack.chapter.filePath)) {
                        controller.pause()
                        _uiState.update {
                            it.copy(
                                currentTrack = newTrack,
                                currentIndex = newIdx,
                                isPlaying = false,
                                errorMessage = "Không tìm thấy tệp gốc của \"${newTrack.chapter.title}\". Tệp có thể đã bị xóa hoặc rút thẻ nhớ."
                            )
                        }
                        return
                    }

                    _uiState.update {
                        it.copy(
                            currentTrack = newTrack,
                            currentIndex = newIdx,
                            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                            durationMs = newTrack.chapter.durationMs
                        )
                    }
                    persistCurrentProgressNow()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = controller.duration
                    if (dur > 0L) {
                        _uiState.update { it.copy(durationMs = dur) }
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    // Đánh dấu hoàn thành chương cuối khi phát hết danh sách
                    _uiState.value.currentTrack?.let { track ->
                        scope.launch {
                            repository.markChapterCompleted(track.chapter.id, true)
                            repository.savePlaybackProgress(
                                storyId = track.story.id,
                                chapterId = track.chapter.id,
                                positionMs = track.chapter.durationMs,
                                durationMs = track.chapter.durationMs
                            )
                        }
                    }
                    if (_uiState.value.sleepTimer.endOfCurrentChapter) {
                        cancelSleepTimer()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val trackTitle = _uiState.value.currentTrack?.chapter?.title ?: "chương này"
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        errorMessage = "Không thể phát \"$trackTitle\". Đường dẫn tệp không còn tồn tại hoặc định dạng bị hỏng."
                    )
                }
            }
        })
    }

    // Nạp danh sách chương (Queue), nhảy tới chương chỉ định và vị trí mili-giây cần phát
    fun playQueue(
        tracks: List<QueueTrack>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true
    ) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val targetTrack = tracks[safeIndex]

        // Lưu tiến trình bài cũ trước khi chuyển sang hàng đợi mới
        persistCurrentProgressNow()

        // Kiểm tra tệp tồn tại trước khi nạp vào ExoPlayer tránh lỗi ngoài ý muốn
        if (!AudioFileScanner.isAudioSourceAccessible(appContext, targetTrack.chapter.filePath)) {
            _uiState.update {
                it.copy(
                    errorMessage = "Không tìm thấy tệp âm thanh gốc cho \"${targetTrack.chapter.title}\". Vui lòng kiểm tra bộ nhớ hoặc thẻ nhớ."
                )
            }
            return
        }

        scope.launch {
            val controller = controllerReady.await()
            val mediaItems = tracks.map { buildMediaItem(it) }

            _uiState.update {
                it.copy(
                    queue = tracks,
                    currentTrack = targetTrack,
                    currentIndex = safeIndex,
                    currentPositionMs = startPositionMs.coerceAtLeast(0L),
                    durationMs = targetTrack.chapter.durationMs,
                    errorMessage = null
                )
            }

            // ExoPlayer tự động dọn dẹp buffer bài cũ trước khi nạp danh sách mới
            controller.stop()
            controller.clearMediaItems()
            controller.setMediaItems(mediaItems, safeIndex, startPositionMs.coerceAtLeast(0L))
            applySavedSettingsToController(controller)
            controller.prepare()
            if (autoPlay) {
                controller.play()
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        val track = _uiState.value.currentTrack ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (!AudioFileScanner.isAudioSourceAccessible(appContext, track.chapter.filePath)) {
                _uiState.update {
                    it.copy(errorMessage = "Tệp gốc không còn tồn tại trên thiết bị.")
                }
                return
            }
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekTo(0L)
            }
            controller.play()
        }
    }

    fun stopPlayback() {
        persistCurrentProgressNow()
        mediaController?.stop()
        _uiState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val duration = _uiState.value.durationMs.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, duration)
        controller.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
        persistCurrentProgressNow(overridePositionMs = clamped)
    }

    fun rewind() {
        val stepMs = _uiState.value.rewindSeconds * 1000L
        val current = mediaController?.currentPosition ?: _uiState.value.currentPositionMs
        seekTo((current - stepMs).coerceAtLeast(0L))
    }

    fun fastForward() {
        val stepMs = _uiState.value.forwardSeconds * 1000L
        val current = mediaController?.currentPosition ?: _uiState.value.currentPositionMs
        val maxDur = _uiState.value.durationMs
        seekTo(if (maxDur > 0L) (current + stepMs).coerceAtMost(maxDur) else current + stepMs)
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        persistCurrentProgressNow()
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (_uiState.value.queue.isNotEmpty() && _uiState.value.repeatMode == RepeatModeOption.ALL) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        persistCurrentProgressNow()
        if (controller.currentPosition > 4000L) {
            controller.seekTo(0L)
            _uiState.update { it.copy(currentPositionMs = 0L) }
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        }
    }

    // Thay đổi tốc độ đọc (0.75x -> 2.0x) giữ nguyên cao độ giọng đọc tự nhiên (pitch = 1.0f)
    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.5f)
        mediaController?.setPlaybackParameters(PlaybackParameters(clamped, 1.0f))
        _uiState.update { it.copy(playbackSpeed = clamped) }
        scope.launch {
            repository.setSetting(AppConstants.KEY_DEFAULT_SPEED, clamped.toString())
        }
    }

    // Thay đổi và lưu mức âm lượng vào bảng app_settings
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        if (!_uiState.value.sleepTimer.isFadingOut) {
            mediaController?.volume = clamped
        }
        _uiState.update { it.copy(volume = clamped) }
        scope.launch {
            repository.setSetting(AppConstants.KEY_VOLUME_LEVEL, clamped.toString())
        }
    }

    fun updateSkipSteps(rewindSec: Int, forwardSec: Int) {
        _uiState.update {
            it.copy(rewindSeconds = rewindSec, forwardSeconds = forwardSec)
        }
        scope.launch {
            repository.setSetting(AppConstants.KEY_REWIND_SECONDS, rewindSec.toString())
            repository.setSetting(AppConstants.KEY_FORWARD_SECONDS, forwardSec.toString())
        }
    }

    fun cycleRepeatMode() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatModeOption.OFF -> RepeatModeOption.ALL
            RepeatModeOption.ALL -> RepeatModeOption.ONE
            RepeatModeOption.ONE -> RepeatModeOption.OFF
        }
        mediaController?.repeatMode = when (nextMode) {
            RepeatModeOption.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeOption.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeOption.ONE -> Player.REPEAT_MODE_ONE
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
        scope.launch {
            repository.setSetting(AppConstants.KEY_REPEAT_MODE, nextMode.name)
        }
    }

    fun toggleShuffle() {
        val nextShuffle = !_uiState.value.shuffleEnabled
        mediaController?.shuffleModeEnabled = nextShuffle
        _uiState.update { it.copy(shuffleEnabled = nextShuffle) }
        scope.launch {
            repository.setSetting(AppConstants.KEY_SHUFFLE_ENABLED, nextShuffle.toString())
        }
    }

    // Đồng bộ lại trạng thái Yêu thích của chương đang phát nếu người dùng thả tim
    fun refreshCurrentTrackFavorite(chapterId: Long, isFavorite: Int) {
        _uiState.update { state ->
            val updatedQueue = state.queue.map { qt ->
                if (qt.chapter.id == chapterId) {
                    qt.copy(chapter = qt.chapter.copy(isFavorite = isFavorite))
                } else {
                    qt
                }
            }
            val updatedCurrent = state.currentTrack?.let { ct ->
                if (ct.chapter.id == chapterId) {
                    ct.copy(chapter = ct.chapter.copy(isFavorite = isFavorite))
                } else {
                    ct
                }
            }
            state.copy(queue = updatedQueue, currentTrack = updatedCurrent)
        }
    }

    // --- Hẹn giờ tắt (Sleep Timer) kèm cơ chế giảm âm lượng từ từ (Fade-out) trong 15 giây cuối ---

    fun startSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        // Khôi phục lại âm lượng chuẩn trước khi đếm mới
        mediaController?.volume = _uiState.value.volume

        val totalMs = minutes * 60_000L
        val endTimestamp = System.currentTimeMillis() + totalMs

        _uiState.update {
            it.copy(
                sleepTimer = SleepTimerState(
                    isActive = true,
                    endOfCurrentChapter = false,
                    remainingMs = totalMs,
                    totalDurationMs = totalMs,
                    isFadingOut = false
                )
            )
        }

        sleepTimerJob = scope.launch {
            while (isActive) {
                val remaining = (endTimestamp - System.currentTimeMillis()).coerceAtLeast(0L)
                val fading = remaining in 1L..15_000L

                if (fading) {
                    // Giảm dần tuyến tính âm lượng trong 15 giây cuối để người nghe không giật mình
                    val baseVol = _uiState.value.volume
                    val fadeRatio = (remaining.toFloat() / 15_000f).coerceIn(0.03f, 1.0f)
                    mediaController?.volume = baseVol * fadeRatio
                }

                _uiState.update {
                    it.copy(
                        sleepTimer = it.sleepTimer.copy(
                            remainingMs = remaining,
                            isFadingOut = fading
                        )
                    )
                }

                if (remaining <= 0L) {
                    mediaController?.pause()
                    // Trả lại âm lượng gốc của người dùng sau khi đã tạm dừng hoàn toàn
                    mediaController?.volume = _uiState.value.volume
                    _uiState.update { it.copy(sleepTimer = SleepTimerState()) }
                    break
                }
                delay(250L)
            }
        }
    }

    fun startSleepTimerEndOfChapter() {
        sleepTimerJob?.cancel()
        mediaController?.volume = _uiState.value.volume
        _uiState.update {
            it.copy(
                sleepTimer = SleepTimerState(
                    isActive = true,
                    endOfCurrentChapter = true,
                    remainingMs = 0L,
                    totalDurationMs = 0L,
                    isFadingOut = false
                )
            )
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        mediaController?.volume = _uiState.value.volume
        _uiState.update { it.copy(sleepTimer = SleepTimerState()) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Bộ đếm thời gian thực & Lưu tiến trình tự động mỗi 5 giây ---

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L }
                        ?: _uiState.value.durationMs
                    _uiState.update {
                        it.copy(currentPositionMs = pos, durationMs = dur)
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun startPeriodicProgressSaver() {
        periodicSaveJob?.cancel()
        periodicSaveJob = scope.launch {
            while (isActive) {
                delay(5_000L) // Định kỳ mỗi 5 giây ghi lại tiến trình vào bảng playback_progress
                persistCurrentProgressNow()
            }
        }
    }

    private fun stopPeriodicProgressSaver() {
        periodicSaveJob?.cancel()
        periodicSaveJob = null
    }

    fun persistCurrentProgressNow(overridePositionMs: Long? = null) {
        val state = _uiState.value
        val activeTrack = state.currentTrack ?: return
        val pos = overridePositionMs
            ?: mediaController?.currentPosition?.coerceAtLeast(0L)
            ?: state.currentPositionMs
        val dur = mediaController?.duration?.takeIf { it > 0L }
            ?: state.durationMs.takeIf { it > 0L }
            ?: activeTrack.chapter.durationMs

        scope.launch {
            repository.savePlaybackProgress(
                storyId = activeTrack.story.id,
                chapterId = activeTrack.chapter.id,
                positionMs = pos,
                durationMs = dur
            )
        }
    }

    private fun buildMediaItem(track: QueueTrack): MediaItem {
        val audioUri = if (track.chapter.filePath.startsWith("content://")) {
            Uri.parse(track.chapter.filePath)
        } else {
            Uri.fromFile(File(track.chapter.filePath))
        }

        val artworkUri = resolveArtworkUri(track.story.coverImagePath)

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.chapter.title)
            .setArtist(track.story.title)
            .setSubtitle("${track.story.title} • ${track.story.narrator}")
            .setAlbumTitle(track.story.title)
            .setExtras(
                Bundle().apply {
                    putLong("story_id", track.story.id)
                    putLong("chapter_id", track.chapter.id)
                }
            )

        if (artworkUri != null) {
            metadataBuilder.setArtworkUri(artworkUri)
        }

        return MediaItem.Builder()
            .setMediaId(track.chapter.id.toString())
            .setUri(audioUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun resolveArtworkUri(coverPath: String?): Uri? {
        if (coverPath.isNullOrBlank()) return null
        return when {
            coverPath == "res:moonlight" ->
                Uri.parse("android.resource://${appContext.packageName}/${R.drawable.img_cover_moonlight}")
            coverPath == "res:cosmos" ->
                Uri.parse("android.resource://${appContext.packageName}/${R.drawable.img_cover_cosmos}")
            coverPath.startsWith("content://") -> Uri.parse(coverPath)
            else -> {
                val f = File(coverPath)
                if (f.exists()) Uri.fromFile(f) else null
            }
        }
    }

    fun release() {
        persistCurrentProgressNow()
        stopPositionTicker()
        stopPeriodicProgressSaver()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
