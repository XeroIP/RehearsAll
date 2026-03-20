package com.rehearsall.ui.playback

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.BookmarkRepository
import com.rehearsall.data.repository.WaveformRepository
import com.rehearsall.playback.PlaybackManager
import com.rehearsall.playback.RepeatMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackManager: PlaybackManager,
    private val repository: AudioFileRepository,
    private val waveformRepository: WaveformRepository,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    private val audioFileId: Long = savedStateHandle["audioFileId"]
        ?: throw IllegalArgumentException("audioFileId required")

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    init {
        loadFile()
        observePlaybackState()
        observeBookmarks()
    }

    private fun loadFile() {
        viewModelScope.launch {
            val file = repository.getById(audioFileId)
            if (file == null) {
                Timber.w("Audio file not found: id=%d", audioFileId)
                _uiState.update { it.copy(isLoading = false, fileName = "File not found") }
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

    private fun loadWaveform(fileId: Long, filePath: String) {
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
        if (_uiState.value.playbackState.isPlaying) {
            playbackManager.pause()
        } else {
            playbackManager.play()
        }
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun skipForward() {
        playbackManager.skipForward(10_000) // 10s default, configurable in settings later
    }

    fun skipBackward() {
        playbackManager.skipBackward(10_000)
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

    fun renameBookmark(id: Long, name: String) {
        viewModelScope.launch {
            bookmarkRepository.renameBookmark(id, name)
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

    // -- Lifecycle: save position & speed when leaving --

    override fun onCleared() {
        super.onCleared()
        val state = _uiState.value.playbackState
        viewModelScope.launch {
            repository.updateLastPlayed(audioFileId, state.positionMs)
            repository.updateLastSpeed(audioFileId, state.speed)
            Timber.d("Saved playback state for id=%d: pos=%dms, speed=%.2fx",
                audioFileId, state.positionMs, state.speed)
        }
    }
}
