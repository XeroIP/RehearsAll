package com.rehearsall.domain.model

data class PracticeSettings(
    val repeatCount: Int = 3,
    val gapBetweenRepsMs: Long = 0,
    val gapBetweenChunksMs: Long = 1000,
    val mode: PracticeMode = PracticeMode.SINGLE_CHUNK_LOOP,
)
