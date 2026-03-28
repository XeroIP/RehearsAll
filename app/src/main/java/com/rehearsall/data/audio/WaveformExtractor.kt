package com.rehearsall.data.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Extracts normalized amplitude data (0.0–1.0) from an audio file using
 * MediaExtractor + MediaCodec. Produces one RMS amplitude sample per ~10ms window.
 */
@Singleton
class WaveformExtractor
    @Inject
    constructor() {
        companion object {
            private const val WINDOW_MS = 10L // ~10ms per amplitude sample
        }

        /**
         * Extract waveform amplitudes from an audio file.
         * Returns a FloatArray of normalized amplitudes (0.0–1.0), one per 10ms window.
         */
        suspend fun extract(filePath: String): Result<FloatArray> =
            withContext(Dispatchers.Default) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(filePath)

                    // Find audio track
                    val audioTrackIndex =
                        (0 until extractor.trackCount).firstOrNull { i ->
                            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                                ?.startsWith("audio/") == true
                        } ?: return@withContext Result.failure(
                            IllegalArgumentException("No audio track found in $filePath"),
                        )

                    extractor.selectTrack(audioTrackIndex)
                    val format = extractor.getTrackFormat(audioTrackIndex)

                    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val durationUs = format.getLong(MediaFormat.KEY_DURATION)

                    // Samples per window
                    val samplesPerWindow = (sampleRate * WINDOW_MS / 1000).toInt()
                    val totalWindows = ((durationUs / 1000) / WINDOW_MS).toInt().coerceAtLeast(1)

                    val amplitudes = ArrayList<Float>(totalWindows + 1)
                    val mime = format.getString(MediaFormat.KEY_MIME)!!

                    val codec = MediaCodec.createDecoderByType(mime)
                    codec.configure(format, null, null, 0)
                    codec.start()

                    val bufferInfo = MediaCodec.BufferInfo()
                    var inputDone = false
                    var outputDone = false
                    var accumulatedSquares = 0.0
                    var samplesInWindow = 0
                    var maxAmplitude = 0f

                    while (!outputDone && isActive) {
                        // Feed input
                        if (!inputDone) {
                            val inputIndex = codec.dequeueInputBuffer(10_000)
                            if (inputIndex >= 0) {
                                val inputBuffer = codec.getInputBuffer(inputIndex)!!
                                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                                if (sampleSize < 0) {
                                    codec.queueInputBuffer(
                                        inputIndex,
                                        0,
                                        0,
                                        0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                    )
                                    inputDone = true
                                } else {
                                    codec.queueInputBuffer(
                                        inputIndex,
                                        0,
                                        sampleSize,
                                        extractor.sampleTime,
                                        0,
                                    )
                                    extractor.advance()
                                }
                            }
                        }

                        // Read output (decoded PCM)
                        val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                        if (outputIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                            val pcmData = processPcmBuffer(outputBuffer, bufferInfo.size, channelCount)

                            for (sample in pcmData) {
                                accumulatedSquares += sample * sample
                                samplesInWindow++

                                if (samplesInWindow >= samplesPerWindow) {
                                    val rms = sqrt(accumulatedSquares / samplesInWindow).toFloat()
                                    amplitudes.add(rms)
                                    if (rms > maxAmplitude) maxAmplitude = rms
                                    accumulatedSquares = 0.0
                                    samplesInWindow = 0
                                }
                            }

                            codec.releaseOutputBuffer(outputIndex, false)

                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                outputDone = true
                            }
                        }
                    }

                    // Flush remaining samples
                    if (samplesInWindow > 0) {
                        val rms = sqrt(accumulatedSquares / samplesInWindow).toFloat()
                        amplitudes.add(rms)
                        if (rms > maxAmplitude) maxAmplitude = rms
                    }

                    codec.stop()
                    codec.release()

                    // Normalize to 0.0–1.0
                    val result =
                        if (maxAmplitude > 0f) {
                            FloatArray(amplitudes.size) { (amplitudes[it] / maxAmplitude).coerceIn(0f, 1f) }
                        } else {
                            FloatArray(amplitudes.size) { 0f }
                        }

                    Timber.i("Waveform extracted: %d samples from %s", result.size, filePath)
                    Result.success(result)
                } catch (e: Exception) {
                    Timber.e(e, "Waveform extraction failed for %s", filePath)
                    Result.failure(e)
                } finally {
                    extractor.release()
                }
            }

        /**
         * Convert PCM buffer to mono float samples normalized to -1.0..1.0.
         * Assumes 16-bit PCM (standard MediaCodec output).
         */
        private fun processPcmBuffer(
            buffer: ByteBuffer,
            size: Int,
            channelCount: Int,
        ): FloatArray {
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.rewind()

            val shortCount = size / 2
            val monoSamples = shortCount / channelCount

            return FloatArray(monoSamples) { i ->
                // Average across channels
                var sum = 0f
                for (ch in 0 until channelCount) {
                    val pos = (i * channelCount + ch) * 2
                    if (pos + 1 < size) {
                        val sample = buffer.getShort(pos)
                        sum += sample.toFloat() / Short.MAX_VALUE
                    }
                }
                sum / channelCount
            }
        }
    }
