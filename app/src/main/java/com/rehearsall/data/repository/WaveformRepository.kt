package com.rehearsall.data.repository

import com.rehearsall.data.audio.WaveformCache
import com.rehearsall.data.audio.WaveformExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WaveformState {
    data object Loading : WaveformState

    data class Ready(val amplitudes: FloatArray) : WaveformState {
        override fun equals(other: Any?): Boolean = other is Ready && amplitudes.contentEquals(other.amplitudes)

        override fun hashCode(): Int = amplitudes.contentHashCode()
    }

    data class Error(val message: String) : WaveformState
}

@Singleton
class WaveformRepository
    @Inject
    constructor(
        private val extractor: WaveformExtractor,
        private val cache: WaveformCache,
    ) {
        /**
         * Returns a Flow that emits Loading, then Ready (from cache or extraction).
         * Cache-first: if cached, skips extraction entirely.
         */
        fun getWaveform(
            audioFileId: Long,
            filePath: String,
        ): Flow<WaveformState> =
            flow {
                emit(WaveformState.Loading)

                // Try cache first
                val cached = cache.load(audioFileId)
                if (cached != null) {
                    emit(WaveformState.Ready(cached))
                    return@flow
                }

                // Extract from file
                val result = extractor.extract(filePath)
                result.fold(
                    onSuccess = { amplitudes ->
                        cache.save(audioFileId, amplitudes)
                        emit(WaveformState.Ready(amplitudes))
                    },
                    onFailure = { error ->
                        Timber.e(error, "Waveform extraction failed for id=%d", audioFileId)
                        emit(WaveformState.Error(error.message ?: "Extraction failed"))
                    },
                )
            }

        /**
         * Extract and cache waveform in the background (e.g., after import).
         * Does nothing if already cached.
         */
        suspend fun ensureCached(
            audioFileId: Long,
            filePath: String,
        ) {
            if (cache.isCached(audioFileId)) return

            val result = extractor.extract(filePath)
            result.onSuccess { amplitudes ->
                cache.save(audioFileId, amplitudes)
            }
        }
    }
