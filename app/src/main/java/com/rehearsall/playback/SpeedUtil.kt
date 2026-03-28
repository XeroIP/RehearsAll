package com.rehearsall.playback

/** Rounds a speed value to the nearest 0.05 step. */
fun Float.roundToSpeedStep(): Float = (this * 20).toInt() / 20f
