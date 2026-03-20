package com.rehearsall.domain.model

import java.time.Instant

data class Bookmark(
    val id: Long,
    val audioFileId: Long,
    val positionMs: Long,
    val name: String,
    val createdAt: Instant,
)
