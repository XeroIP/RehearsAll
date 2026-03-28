package com.rehearsall.ui.playback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.preferences.UserPreferencesRepository
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.BookmarkRepository
import com.rehearsall.data.repository.ChunkMarkerRepository
import com.rehearsall.data.repository.LoopRepository
import com.rehearsall.data.repository.PracticeSettingsRepository
import com.rehearsall.data.repository.WaveformRepository
import com.rehearsall.domain.model.Loop
import com.rehearsall.domain.model.OverlayMode
import com.rehearsall.domain.model.PracticeMode
import com.rehearsall.domain.model.PracticeSettings
import com.rehearsall.domain.usecase.GeneratePracticeStepsUseCase
import com.rehearsall.playback.ChunkedPracticeEngine
import com.rehearsall.playback.LoopRegion
import com.rehearsall.playback.PlaybackManager
import com.rehearsall.playback.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val playbackManager: PlaybackManager,
        private val repository: AudioFileRepository,
        private val waveformRepository: WaveformRepository,
        private val bookmarkRepository: BookmarkRepository,
        private val loopRepository: LoopRepository,
        private val chunkMarkerRepository: ChunkMarkerRepository,
        private val practiceSettingsRepository: PracticeSettingsRepository,
        private val practiceEngine: ChunkedPracticeEngine,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : ViewModel() {
        private val audioFileId: Long =
            savedStateHandle["audioFileId"]
                ?: throw IllegalArgumentException("audioFileId required")

        private val _uiState = MutableStateFlow(PlaybackUiState())
        val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

        // Emits a file ID when skip-to-next/previous transitions to a different track
        private val _navigationEvent = MutableSharedFlow<Long>(extraBufferCapacity = 1)
        val navigationEvent: SharedFlow<Long> = _navigationEvent.asSharedFlow()

        private val skipIncrementMs: StateFlow<Long> =
            userPreferencesRepository.skipIncrementMs
                .stateIn(viewModelScope, SharingStarted.Eagerly, 5000L)

        init {
            loadFile()
            observePlaybackState()
            observeCurrentFileId()
            observeBookmarks()
            observeLoops()
            observeLoopRegion()
            observeChunkMarkers()
            observePracticeState()
            loadPracticeSettings()
            observeSkipIncrement()
            observeOverlayMode()
        }

        private fun loadFile() {
            viewModelScope.launch {
                val file = repository.getById(audioFileId)
                if (file == null) {
                    Timber.w("Audio file not found in database: id=%d", audioFileId)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fileName = "File not found",
                            fileNotFound = true,
                            errorMessage = "This file no longer exists",
                        )
                    }
                    return@launch
                }

                // Verify the audio file still exists on disk
                if (!File(file.internalPath).exists()) {
                    Timber.w("Audio file missing from disk: id=%d path=%s", audioFileId, file.internalPath)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fileName = file.displayName,
                            fileNotFound = true,
                            errorMessage = "Audio file was deleted externally. Remove from library?",
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        fileName = file.displayName,
                        artist = file.artist,
                    )
                }

                // Load waveform
                loadWaveform(audioFileId, file.internalPath)

                // Only start playback if this file isn't already playing
                if (playbackManager.currentFileId.value != audioFileId) {
                    playbackManager.playFile(
                        fileId = audioFileId,
                        path = file.internalPath,
                        startPositionMs = file.lastPositionMs,
                    )
                    if (file.lastSpeed != 1.0f) {
                        playbackManager.setSpeed(file.lastSpeed)
                    }
                }
            }
        }

        fun removeDeletedFile() {
            viewModelScope.launch {
                repository.delete(audioFileId)
                Timber.i("Removed missing file from library: id=%d", audioFileId)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        // When ExoPlayer transitions to a different track (skip-to-next/previous in a queue),
        // emit a navigation event so the screen can replace itself with the new file's screen.
        private fun observeCurrentFileId() {
            viewModelScope.launch {
                playbackManager.currentFileId.collect { fileId ->
                    if (fileId != null && fileId != audioFileId) {
                        _navigationEvent.tryEmit(fileId)
                    }
                }
            }
        }

        private fun observeSkipIncrement() {
            viewModelScope.launch {
                skipIncrementMs.collect { ms ->
                    _uiState.update { it.copy(skipIncrementMs = ms) }
                }
            }
        }

        private fun observeOverlayMode() {
            viewModelScope.launch {
                userPreferencesRepository.waveformOverlay.collect { mode ->
                    _uiState.update { it.copy(overlayMode = mode) }
                }
            }
        }

        private fun loadWaveform(
            fileId: Long,
            filePath: String,
        ) {
            viewModelScope.launch {
                waveformRepository.getWaveform(fileId, filePath).collect { waveformState ->
                    _uiState.update { it.copy(waveformState = waveformState) }
                }
            }
        }

        private fun observePlaybackState() {
            viewModelScope.launch {
                combine(
                    playbackManager.playbackState,
                    playbackManager.repeatMode,
                    playbackManager.shuffleEnabled,
                    playbackManager.currentQueue,
                ) { playback, repeat, shuffle, queue ->
                    PlaybackCombined(playback, repeat, shuffle, queue)
                }.collect { combined ->
                    _uiState.update {
                        it.copy(
                            playbackState = combined.playback,
                            repeatMode = combined.repeat,
                            shuffleEnabled = combined.shuffle,
                            queue = combined.queue,
                        )
                    }
                }
            }
        }

        private data class PlaybackCombined(
            val playback: com.rehearsall.playback.PlaybackState,
            val repeat: RepeatMode,
            val shuffle: Boolean,
            val queue: List<com.rehearsall.domain.model.QueueItem>,
        )

        // -- Transport controls --

        fun togglePlayPause() {
            playbackManager.togglePlayPause()
        }

        fun seekTo(positionMs: Long) {
            playbackManager.seekTo(positionMs)
        }

        fun skipForward() {
            playbackManager.skipForward(skipIncrementMs.value)
        }

        fun skipBackward() {
            playbackManager.skipBackward(skipIncrementMs.value)
        }

        fun skipToNext() {
            playbackManager.skipToNext()
        }

        fun skipToPrevious() {
            playbackManager.skipToPrevious()
        }

        // -- Repeat & shuffle --

        fun cycleRepeatMode() {
            val next = _uiState.value.repeatMode.next()
            playbackManager.setRepeatMode(next)
        }

        fun toggleShuffle() {
            playbackManager.setShuffleEnabled(!_uiState.value.shuffleEnabled)
        }

        // -- Speed --

        fun setSpeed(speed: Float) {
            playbackManager.setSpeed(speed)
        }

        fun toggleSpeedSheet() {
            _uiState.update { it.copy(showSpeedSheet = !it.showSpeedSheet) }
        }

        fun dismissSpeedSheet() {
            _uiState.update { it.copy(showSpeedSheet = false) }
        }

        // -- Queue --

        fun toggleQueueSheet() {
            _uiState.update { it.copy(showQueueSheet = !it.showQueueSheet) }
        }

        fun dismissQueueSheet() {
            _uiState.update { it.copy(showQueueSheet = false) }
        }

        fun skipToQueueItem(index: Int) {
            playbackManager.skipToQueueItem(index)
        }

        fun removeFromQueue(index: Int) {
            playbackManager.removeFromQueue(index)
        }

        fun clearQueue() {
            playbackManager.clearQueue()
        }

        // -- Bookmarks --

        private fun observeBookmarks() {
            viewModelScope.launch {
                bookmarkRepository.getBookmarksForFile(audioFileId).collect { bookmarks ->
                    _uiState.update { it.copy(bookmarks = bookmarks) }
                }
            }
        }

        fun toggleMarkersSheet() {
            _uiState.update { it.copy(showMarkersSheet = !it.showMarkersSheet) }
        }

        fun dismissMarkersSheet() {
            _uiState.update { it.copy(showMarkersSheet = false) }
        }

        fun addBookmark() {
            val positionMs = _uiState.value.playbackState.positionMs
            val count = _uiState.value.bookmarks.size + 1
            viewModelScope.launch {
                bookmarkRepository.addBookmark(
                    audioFileId = audioFileId,
                    positionMs = positionMs,
                    name = "Bookmark $count",
                )
            }
        }

        fun renameBookmark(
            id: Long,
            name: String,
        ) {
            viewModelScope.launch {
                bookmarkRepository.renameBookmark(id, name)
            }
        }

        fun updateBookmarkPosition(
            id: Long,
            positionMs: Long,
        ) {
            viewModelScope.launch {
                bookmarkRepository.updateBookmarkPosition(id, positionMs)
            }
        }

        fun deleteBookmark(id: Long) {
            viewModelScope.launch {
                bookmarkRepository.deleteBookmark(id)
            }
        }

        fun seekToBookmark(positionMs: Long) {
            playbackManager.seekTo(positionMs)
        }

        // -- A-B Loops --

        private fun observeLoops() {
            viewModelScope.launch {
                loopRepository.getLoopsForFile(audioFileId).collect { loops ->
                    _uiState.update { it.copy(savedLoops = loops) }
                }
            }
        }

        private fun observeLoopRegion() {
            viewModelScope.launch {
                playbackManager.loopRegion.collect { region ->
                    _uiState.update { state ->
                        state.copy(
                            activeLoop = region,
                            // Auto-show the loops overlay when a loop is activated; auto-hide when cleared.
                            overlayMode =
                                when {
                                    region != null -> OverlayMode.LOOPS
                                    state.overlayMode == OverlayMode.LOOPS -> OverlayMode.NONE
                                    else -> state.overlayMode
                                },
                        )
                    }
                }
            }
        }

        fun dismissWaveform() {
            playbackManager.clearLoopRegion()
            _uiState.update { it.copy(overlayMode = OverlayMode.NONE) }
            viewModelScope.launch {
                userPreferencesRepository.setWaveformOverlay(OverlayMode.NONE)
            }
        }

        fun setLoopA() {
            val positionMs = _uiState.value.playbackState.positionMs
            val current = _uiState.value.activeLoop
            if (current != null && positionMs < current.endMs - 100) {
                playbackManager.setLoopRegion(LoopRegion(positionMs, current.endMs))
            } else {
                // Store A temporarily — we'll set the full region when B is set
                _uiState.update { it.copy(activeLoop = LoopRegion(positionMs, positionMs)) }
            }
        }

        fun setLoopB() {
            val positionMs = _uiState.value.playbackState.positionMs
            val current = _uiState.value.activeLoop
            val startMs = current?.startMs ?: 0L
            if (positionMs > startMs + 100) {
                playbackManager.setLoopRegion(LoopRegion(startMs, positionMs))
            }
        }

        fun createLoop(
            startMs: Long,
            endMs: Long,
        ) {
            if (endMs - startMs >= 100) {
                playbackManager.setLoopRegion(LoopRegion(startMs, endMs))
            }
        }

        fun clearLoop() {
            playbackManager.clearLoopRegion()
        }

        fun saveLoop(name: String) {
            val region = _uiState.value.activeLoop ?: return
            viewModelScope.launch {
                loopRepository.saveLoop(
                    audioFileId = audioFileId,
                    name = name,
                    startMs = region.startMs,
                    endMs = region.endMs,
                )
            }
        }

        fun loadLoop(loop: Loop) {
            playbackManager.setLoopRegion(LoopRegion(loop.startMs, loop.endMs))
            playbackManager.seekTo(loop.startMs)
        }

        fun deleteLoop(id: Long) {
            viewModelScope.launch {
                loopRepository.deleteLoop(id)
            }
        }

        fun updateLoopBounds(loopId: Long) {
            val region = _uiState.value.activeLoop ?: return
            viewModelScope.launch {
                loopRepository.updateBounds(loopId, region.startMs, region.endMs)
            }
        }

        fun adjustLoopBoundary(
            isStart: Boolean,
            newMs: Long,
        ) {
            val current = _uiState.value.activeLoop ?: return
            val updated =
                if (isStart) {
                    var start = newMs.coerceAtLeast(0)
                    // Don't let begin go past current playback position
                    val pos = _uiState.value.playbackState.positionMs
                    if (pos > current.startMs) {
                        start = start.coerceAtMost(pos)
                    }
                    LoopRegion(start, current.endMs)
                } else {
                    LoopRegion(current.startMs, newMs.coerceAtMost(_uiState.value.playbackState.durationMs))
                }
            if (updated.endMs - updated.startMs >= 100) {
                playbackManager.setLoopRegion(updated)
            }
        }

        // -- Chunks & Practice --

        private fun observeChunkMarkers() {
            viewModelScope.launch {
                chunkMarkerRepository.getMarkersForFile(audioFileId).collect { markers ->
                    _uiState.update { it.copy(chunkMarkers = markers) }
                }
            }
        }

        private fun observePracticeState() {
            viewModelScope.launch {
                practiceEngine.state.collect { state ->
                    _uiState.update { it.copy(practiceState = state) }
                }
            }
        }

        private fun loadPracticeSettings() {
            viewModelScope.launch {
                val settings = practiceSettingsRepository.getForFile(audioFileId)
                _uiState.update { it.copy(practiceSettings = settings) }
            }
        }

        fun addChunkMarker() {
            val positionMs = _uiState.value.playbackState.positionMs
            val count = _uiState.value.chunkMarkers.size + 1
            viewModelScope.launch {
                chunkMarkerRepository.addMarker(
                    audioFileId = audioFileId,
                    positionMs = positionMs,
                    label = "Marker $count",
                )
            }
        }

        fun updateChunkMarkerPosition(
            id: Long,
            positionMs: Long,
        ) {
            viewModelScope.launch {
                chunkMarkerRepository.updatePosition(id, positionMs)
            }
        }

        fun deleteChunkMarker(id: Long) {
            viewModelScope.launch {
                chunkMarkerRepository.deleteMarker(id)
            }
        }

        fun seekToChunk(positionMs: Long) {
            playbackManager.seekTo(positionMs)
        }

        fun togglePracticeSheet() {
            _uiState.update { it.copy(showPracticeSheet = !it.showPracticeSheet) }
        }

        fun dismissPracticeSheet() {
            _uiState.update { it.copy(showPracticeSheet = false) }
        }

        fun updatePracticeMode(mode: PracticeMode) {
            val updated = _uiState.value.practiceSettings.copy(mode = mode)
            _uiState.update { it.copy(practiceSettings = updated) }
            savePracticeSettings(updated)
        }

        fun updateRepeatCount(count: Int) {
            val updated = _uiState.value.practiceSettings.copy(repeatCount = count)
            _uiState.update { it.copy(practiceSettings = updated) }
            savePracticeSettings(updated)
        }

        fun updateGapBetweenReps(ms: Long) {
            val updated = _uiState.value.practiceSettings.copy(gapBetweenRepsMs = ms)
            _uiState.update { it.copy(practiceSettings = updated) }
            savePracticeSettings(updated)
        }

        fun updateGapBetweenChunks(ms: Long) {
            val updated = _uiState.value.practiceSettings.copy(gapBetweenChunksMs = ms)
            _uiState.update { it.copy(practiceSettings = updated) }
            savePracticeSettings(updated)
        }

        private fun savePracticeSettings(settings: PracticeSettings) {
            viewModelScope.launch {
                practiceSettingsRepository.save(audioFileId, settings)
            }
        }

        fun startPractice() {
            val markers = _uiState.value.chunkMarkers
            val settings = _uiState.value.practiceSettings
            val durationMs = _uiState.value.playbackState.durationMs

            val steps =
                when (settings.mode) {
                    PracticeMode.SINGLE_CHUNK_LOOP ->
                        GeneratePracticeStepsUseCase.generateSingleChunkSteps(markers, durationMs, settings.repeatCount)
                    PracticeMode.CUMULATIVE_BUILD_UP ->
                        GeneratePracticeStepsUseCase.generateCumulativeBuildUpSteps(markers, durationMs, settings.repeatCount)
                    PracticeMode.SEQUENTIAL_PLAY ->
                        GeneratePracticeStepsUseCase.generateSequentialSteps(markers, durationMs)
                }

            practiceEngine.startPractice(steps, settings, viewModelScope)
        }

        fun stopPractice() {
            practiceEngine.stopPractice()
        }

        fun practiceSkipNext() {
            practiceEngine.skipToNextStep()
        }

        fun practiceSkipPrevious() {
            practiceEngine.skipToPreviousStep()
        }

        // -- Lifecycle: save position & speed when leaving --

        override fun onCleared() {
            super.onCleared()
            val state = _uiState.value.playbackState
            viewModelScope.launch {
                withContext(NonCancellable) {
                    repository.updateLastPlayed(audioFileId, state.positionMs)
                    repository.updateLastSpeed(audioFileId, state.speed)
                    Timber.d(
                        "Saved playback state for id=%d: pos=%dms, speed=%.2fx",
                        audioFileId,
                        state.positionMs,
                        state.speed,
                    )
                }
            }
        }
    }
