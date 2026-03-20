package com.rehearsall.ui.playback

import com.rehearsall.data.repository.WaveformState
import com.rehearsall.domain.model.Bookmark
import com.rehearsall.domain.model.QueueItem
import com.rehearsall.playback.PlaybackState
import com.rehearsall.playback.RepeatMode

data class PlaybackUiState(
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val fileName: String = "",
    val artist: String? = null,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val showSpeedSheet: Boolean = false,
    val showQueueSheet: Boolean = false,
    val showMarkersSheet: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val waveformState: WaveformState = WaveformState.Loading,
)
