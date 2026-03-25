package com.rehearsall.ui.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.domain.model.QueueItem
import com.rehearsall.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val playlistRepository: PlaylistRepository,
        private val playbackManager: PlaybackManager,
    ) : ViewModel() {
        private val playlistId: Long =
            savedStateHandle["playlistId"]
                ?: throw IllegalArgumentException("playlistId required")

        private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
        val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<PlaylistEvent>()
        val events: SharedFlow<PlaylistEvent> = _events.asSharedFlow()

        init {
            loadPlaylist()
        }

        private fun loadPlaylist() {
            viewModelScope.launch {
                val playlist = playlistRepository.getById(playlistId)
                if (playlist == null) {
                    _uiState.value = PlaylistUiState.Error("Playlist not found")
                    return@launch
                }

                playlistRepository.getPlaylistItems(playlistId)
                    .map<_, PlaylistUiState> { items ->
                        PlaylistUiState.Loaded(
                            playlistName = playlist.name,
                            items = items,
                        )
                    }
                    .catch { e ->
                        Timber.e(e, "Error loading playlist items")
                        emit(PlaylistUiState.Error("Failed to load playlist"))
                    }
                    .collect { _uiState.value = it }
            }
        }

        fun playPlaylist(startIndex: Int = 0) {
            val state = _uiState.value as? PlaylistUiState.Loaded ?: return
            if (state.items.isEmpty()) return

            val queueItems =
                state.items.map { item ->
                    QueueItem(
                        fileId = item.audioFileId,
                        displayName = item.displayName,
                        artist = item.artist,
                        durationMs = item.durationMs,
                        path = item.internalPath,
                    )
                }
            playbackManager.setQueue(queueItems, startIndex)
        }

        fun removeItem(itemId: Long) {
            viewModelScope.launch {
                val state = _uiState.value as? PlaylistUiState.Loaded ?: return@launch
                val item = state.items.find { it.id == itemId } ?: return@launch
                playlistRepository.removeItem(itemId, playlistId)
                _events.emit(PlaylistEvent.ItemRemoved(item.displayName))
            }
        }

        fun reorderItems(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val state = _uiState.value as? PlaylistUiState.Loaded ?: return
            val items = state.items.toMutableList()
            val item = items.removeAt(fromIndex)
            items.add(toIndex, item)

            // Update order indices
            viewModelScope.launch {
                val updates =
                    items.mapIndexed { index, playlistItem ->
                        playlistItem.id to index
                    }
                playlistRepository.reorderItems(playlistId, updates)
            }
        }

        fun renamePlaylist(newName: String) {
            viewModelScope.launch {
                playlistRepository.renamePlaylist(playlistId, newName.trim())
                // Update local state immediately
                val current = _uiState.value as? PlaylistUiState.Loaded ?: return@launch
                _uiState.value = current.copy(playlistName = newName.trim())
                _events.emit(PlaylistEvent.PlaylistRenamed(newName.trim()))
            }
        }

        fun deletePlaylist() {
            viewModelScope.launch {
                val state = _uiState.value as? PlaylistUiState.Loaded
                playlistRepository.deletePlaylist(playlistId)
                _events.emit(PlaylistEvent.PlaylistDeleted(state?.playlistName ?: "Playlist"))
            }
        }
    }
