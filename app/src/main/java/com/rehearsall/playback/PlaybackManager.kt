package com.rehearsall.playback

import com.rehearsall.domain.model.QueueItem
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
    val currentQueue: StateFlow<List<QueueItem>>
    fun playFile(fileId: Long, path: String, startPositionMs: Long = 0L)
    fun setQueue(items: List<QueueItem>, startIndex: Int = 0)
    fun skipToQueueItem(index: Int)
    fun removeFromQueue(index: Int)
    fun moveQueueItem(fromIndex: Int, toIndex: Int)
    fun clearQueue()
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleEnabled(enabled: Boolean)

    // -- A-B loop --
    val loopRegion: StateFlow<LoopRegion?>
    fun setLoopRegion(region: LoopRegion)
    fun clearLoopRegion()

    // -- Lifecycle --
    fun release()
}
