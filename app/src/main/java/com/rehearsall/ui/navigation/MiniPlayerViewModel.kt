package com.rehearsall.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.playback.PlaybackManager
import com.rehearsall.playback.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MiniPlayerState(
    val isVisible: Boolean = false,
    val trackName: String = "",
    val currentFileId: Long? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
)

@HiltViewModel
class MiniPlayerViewModel
    @Inject
    constructor(
        private val playbackManager: PlaybackManager,
        private val repository: AudioFileRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(MiniPlayerState())
        val state: StateFlow<MiniPlayerState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                combine(
                    playbackManager.currentFileId,
                    playbackManager.playbackState,
                ) { fileId, playback ->
                    Pair(fileId, playback)
                }.collect { (fileId, playback) ->
                    if (fileId != null) {
                        // Only look up the name if the file changed
                        val name =
                            if (fileId != _state.value.currentFileId) {
                                repository.getById(fileId)?.displayName ?: "Unknown"
                            } else {
                                _state.value.trackName
                            }
                        _state.update {
                            it.copy(
                                isVisible = true,
                                trackName = name,
                                currentFileId = fileId,
                                playbackState = playback,
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(isVisible = false, currentFileId = null)
                        }
                    }
                }
            }
        }

        fun togglePlayPause() {
            playbackManager.togglePlayPause()
        }
    }
