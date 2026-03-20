package com.rehearsall.ui.playback

import com.rehearsall.data.repository.WaveformState
import com.rehearsall.domain.model.Bookmark
import com.rehearsall.domain.model.ChunkMarker
import com.rehearsall.domain.model.Loop
import com.rehearsall.domain.model.PracticeSettings
import com.rehearsall.domain.model.QueueItem
import com.rehearsall.playback.LoopRegion
import com.rehearsall.playback.PlaybackState
import com.rehearsall.playback.PracticeState
import com.rehearsall.playback.RepeatMode

data class PlaybackUiState(
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val fileName: String = "",
    val artist: String? = null,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val fileNotFound: Boolean = false,
    val showSpeedSheet: Boolean = false,
    val showQueueSheet: Boolean = false,
    val showMarkersSheet: Boolean = false,
    val showPracticeSheet: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val activeLoop: LoopRegion? = null,
    val savedLoops: List<Loop> = emptyList(),
    val chunkMarkers: List<ChunkMarker> = emptyList(),
    val practiceState: PracticeState = PracticeState.Idle,
    val practiceSettings: PracticeSettings = PracticeSettings(),
    val waveformState: WaveformState = WaveformState.Loading,
    val skipIncrementMs: Long = 5000L,
)
