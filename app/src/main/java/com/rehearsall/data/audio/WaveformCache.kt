package com.rehearsall.data.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializes/deserializes waveform FloatArrays to binary files.
 * Format: magic (4 bytes "WAVE"), version (1 byte), count (4 bytes), floats...
 */
@Singleton
class WaveformCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val MAGIC = byteArrayOf('W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte())
        private const val VERSION: Byte = 1
    }

    private val cacheDir: File
        get() = File(context.filesDir, "waveforms").also { it.mkdirs() }

    fun getCacheFile(audioFileId: Long): File = File(cacheDir, "$audioFileId.waveform")

    fun isCached(audioFileId: Long): Boolean = getCacheFile(audioFileId).exists()

    suspend fun save(audioFileId: Long, amplitudes: FloatArray) = withContext(Dispatchers.IO) {
        try {
            val file = getCacheFile(audioFileId)
            DataOutputStream(file.outputStream().buffered()).use { out ->
                out.write(MAGIC)
                out.writeByte(VERSION.toInt())
                out.writeInt(amplitudes.size)
                for (amp in amplitudes) {
                    out.writeFloat(amp)
                }
            }
            Timber.d("Waveform cached for id=%d (%d samples)", audioFileId, amplitudes.size)
        } catch (e: Exception) {
            Timber.e(e, "Failed to cache waveform for id=%d", audioFileId)
        }
    }

    suspend fun load(audioFileId: Long): FloatArray? = withContext(Dispatchers.IO) {
        val file = getCacheFile(audioFileId)
        if (!file.exists()) return@withContext null

        try {
            DataInputStream(file.inputStream().buffered()).use { input ->
                // Validate magic
                val magic = ByteArray(4)
                input.readFully(magic)
                if (!magic.contentEquals(MAGIC)) {
                    Timber.w("Invalid waveform cache magic for id=%d", audioFileId)
                    file.delete()
                    return@withContext null
                }

                // Validate version
                val version = input.readByte()
                if (version != VERSION) {
                    Timber.w("Unsupported waveform cache version %d for id=%d", version, audioFileId)
                    file.delete()
                    return@withContext null
                }

                val count = input.readInt()
                if (count <= 0 || count > 1_000_000) {
                    Timber.w("Invalid waveform sample count %d for id=%d", count, audioFileId)
                    file.delete()
                    return@withContext null
                }

                val amplitudes = FloatArray(count) { input.readFloat() }
                Timber.d("Waveform loaded from cache for id=%d (%d samples)", audioFileId, count)
                amplitudes
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load waveform cache for id=%d", audioFileId)
            file.delete()
            null
        }
    }

    suspend fun delete(audioFileId: Long) = withContext(Dispatchers.IO) {
        getCacheFile(audioFileId).delete()
    }
}
