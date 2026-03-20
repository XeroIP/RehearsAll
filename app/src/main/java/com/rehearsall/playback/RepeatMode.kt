package com.rehearsall.playback

import androidx.media3.common.Player

enum class RepeatMode(val exoPlayerMode: Int) {
    OFF(Player.REPEAT_MODE_OFF),
    ONE(Player.REPEAT_MODE_ONE),
    ALL(Player.REPEAT_MODE_ALL);

    fun next(): RepeatMode = when (this) {
        OFF -> ONE
        ONE -> ALL
        ALL -> OFF
    }

    companion object {
        fun fromExoPlayer(mode: Int): RepeatMode = entries.first { it.exoPlayerMode == mode }
    }
}
