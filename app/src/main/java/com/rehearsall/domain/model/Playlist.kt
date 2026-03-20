package com.rehearsall.domain.model

import java.time.Instant

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
