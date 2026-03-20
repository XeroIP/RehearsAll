package com.rehearsall.domain.model

import java.time.Instant

data class AudioFile(
    val id: Long,
    val displayName: String,
    val artist: String?,
    val title: String?,
    val format: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val internalPath: String,
    val importedAt: Instant,
    val lastPlayedAt: Instant?,
    val lastPositionMs: Long,
    val lastSpeed: Float,
)
