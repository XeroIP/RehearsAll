package com.rehearsall.domain.model

data class PracticeStep(
    val startMs: Long,
    val endMs: Long,
    val label: String,
    val repeatCount: Int,
    val chunkRange: String,
)
