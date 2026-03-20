package com.rehearsall.domain.model

enum class PracticeMode(val displayName: String, val description: String) {
    SINGLE_CHUNK_LOOP(
        displayName = "Single Chunk",
        description = "Repeat each chunk individually before moving to the next.",
    ),
    CUMULATIVE_BUILD_UP(
        displayName = "Build-Up",
        description = "Start with chunk 1, then 1+2, then 1+2+3, building up cumulatively.",
    ),
    SEQUENTIAL_PLAY(
        displayName = "Sequential",
        description = "Play through all chunks once in order, no repetition.",
    ),
}
