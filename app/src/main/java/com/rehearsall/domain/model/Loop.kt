package com.rehearsall.domain.model

import java.time.Instant

data class Loop(
    val id: Long,
    val audioFileId: Long,
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val createdAt: Instant,
) {
    val durationMs: Long get() = endMs - startMs
}
