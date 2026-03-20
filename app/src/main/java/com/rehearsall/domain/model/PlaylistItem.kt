package com.rehearsall.domain.model

data class PlaylistItem(
    val id: Long,
    val playlistId: Long,
    val audioFileId: Long,
    val orderIndex: Int,
    val displayName: String,
    val artist: String?,
    val durationMs: Long,
    val internalPath: String,
    val format: String,
)
