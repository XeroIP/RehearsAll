package com.rehearsall.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * Clean API for the app UI to control playback.
 * Wraps MediaController commands under the hood.
 */
interface PlaybackManager {

    // -- State observation --
    val playbackState: StateFlow<PlaybackState>
    val currentFileId: StateFlow<Long?>
    val repeatMode: StateFlow<RepeatMode>
    val shuffleEnabled: StateFlow<Boolean>

    // -- Transport --
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipForward(ms: Long)
    fun skipBackward(ms: Long)
    fun skipToNext()
    fun skipToPrevious()

    // -- Speed (0.25x – 3.0x, pitch-preserved) --
    fun setSpeed(speed: Float)

    // -- Queue --
    fun playFile(fileId: Long, path: String, startPositionMs: Long = 0L)
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleEnabled(enabled: Boolean)

    // -- A-B loop (later phases populate UI; engine enforces here) --
    fun setLoopRegion(region: LoopRegion)
    fun clearLoopRegion()

    // -- Lifecycle --
    fun release()
}
