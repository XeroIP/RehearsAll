package com.rehearsall.domain.usecase

import com.rehearsall.domain.model.ChunkMarker
import com.rehearsall.domain.model.PracticeStep

/**
 * Pure functions that generate practice steps from chunk markers.
 * Chunks are defined as the regions between consecutive markers
 * (and from start-of-file to first marker, and last marker to end-of-file).
 *
 * Given N markers, there are N+1 chunks:
 *   (0..marker1), (marker1..marker2), ..., (markerN..duration)
 */
object GeneratePracticeStepsUseCase {
    /**
     * Derives chunk boundaries from markers.
     * Returns list of (startMs, endMs, label) triples sorted by position.
     */
    private fun buildChunks(
        markers: List<ChunkMarker>,
        durationMs: Long,
    ): List<Triple<Long, Long, String>> {
        if (markers.isEmpty()) return listOf(Triple(0L, durationMs, "Full Track"))

        val sorted = markers.sortedBy { it.positionMs }
        val chunks = mutableListOf<Triple<Long, Long, String>>()

        // First chunk: 0 to first marker
        chunks.add(Triple(0L, sorted.first().positionMs, "Chunk 1"))

        // Middle chunks: between consecutive markers
        for (i in 0 until sorted.size - 1) {
            chunks.add(
                Triple(
                    sorted[i].positionMs,
                    sorted[i + 1].positionMs,
                    "Chunk ${i + 2}",
                ),
            )
        }

        // Last chunk: last marker to end
        chunks.add(Triple(sorted.last().positionMs, durationMs, "Chunk ${sorted.size + 1}"))

        return chunks
    }

    /**
     * Single Chunk Loop: repeat each chunk N times before moving to the next.
     */
    fun generateSingleChunkSteps(
        markers: List<ChunkMarker>,
        durationMs: Long,
        repeatCount: Int,
    ): List<PracticeStep> {
        val chunks = buildChunks(markers, durationMs)
        return chunks.map { (start, end, label) ->
            PracticeStep(
                startMs = start,
                endMs = end,
                label = label,
                repeatCount = repeatCount,
                chunkRange = label,
            )
        }
    }

    /**
     * Cumulative Build-Up: chunk 1 x N, then chunks 1-2 x N, then 1-3 x N, etc.
     * For M chunks, produces 2M-1 steps (each new chunk solo, then cumulative).
     */
    fun generateCumulativeBuildUpSteps(
        markers: List<ChunkMarker>,
        durationMs: Long,
        repeatCount: Int,
    ): List<PracticeStep> {
        val chunks = buildChunks(markers, durationMs)
        if (chunks.size <= 1) {
            return chunks.map { (start, end, label) ->
                PracticeStep(start, end, label, repeatCount, label)
            }
        }

        val steps = mutableListOf<PracticeStep>()

        // First chunk solo
        val first = chunks.first()
        steps.add(
            PracticeStep(
                startMs = first.first,
                endMs = first.second,
                label = first.third,
                repeatCount = repeatCount,
                chunkRange = first.third,
            ),
        )

        // For each subsequent chunk: solo, then cumulative from start
        for (i in 1 until chunks.size) {
            val current = chunks[i]

            // Solo the new chunk
            steps.add(
                PracticeStep(
                    startMs = current.first,
                    endMs = current.second,
                    label = "${current.third} (solo)",
                    repeatCount = repeatCount,
                    chunkRange = current.third,
                ),
            )

            // Cumulative: chunk 1 through current
            steps.add(
                PracticeStep(
                    startMs = chunks.first().first,
                    endMs = current.second,
                    label = "Chunks 1–${i + 1}",
                    repeatCount = repeatCount,
                    chunkRange = "1–${i + 1}",
                ),
            )
        }

        return steps
    }

    /**
     * Sequential: play each chunk once in order, no repetition.
     */
    fun generateSequentialSteps(
        markers: List<ChunkMarker>,
        durationMs: Long,
    ): List<PracticeStep> {
        val chunks = buildChunks(markers, durationMs)
        return chunks.map { (start, end, label) ->
            PracticeStep(
                startMs = start,
                endMs = end,
                label = label,
                repeatCount = 1,
                chunkRange = label,
            )
        }
    }
}
