package com.rehearsall.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.domain.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface PlaylistListUiState {
    data object Loading : PlaylistListUiState
    data class Loaded(val playlists: List<Playlist>) : PlaylistListUiState
    data class Error(val message: String) : PlaylistListUiState
}

sealed interface PlaylistListEvent {
    data class PlaylistCreated(val name: String) : PlaylistListEvent
}

@HiltViewModel
class PlaylistListViewModel
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PlaylistListUiState>(PlaylistListUiState.Loading)
        val uiState: StateFlow<PlaylistListUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<PlaylistListEvent>()
        val events: SharedFlow<PlaylistListEvent> = _events.asSharedFlow()

        init {
            viewModelScope.launch {
                playlistRepository.getAllPlaylists()
                    .catch { e ->
                        Timber.e(e, "Error loading playlists")
                        emit(emptyList())
                    }
                    .collect { playlists ->
                        _uiState.value = PlaylistListUiState.Loaded(playlists = playlists)
                    }
            }
        }

        fun createPlaylist(name: String) {
            viewModelScope.launch {
                playlistRepository.createPlaylist(name.trim())
                _events.emit(PlaylistListEvent.PlaylistCreated(name.trim()))
            }
        }
    }
