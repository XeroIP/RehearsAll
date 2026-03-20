package com.rehearsall.domain.model

data class QueueItem(
    val fileId: Long,
    val displayName: String,
    val artist: String?,
    val durationMs: Long,
    val path: String,
    val isCurrentlyPlaying: Boolean = false,
)
