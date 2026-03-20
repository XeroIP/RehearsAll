package com.rehearsall.playback

data class PlaybackState(
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val speed: Float,
) {
    companion object {
        val IDLE = PlaybackState(0L, 0L, false, 1.0f)
    }
}

data class LoopRegion(val startMs: Long, val endMs: Long)
