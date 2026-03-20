package com.rehearsall.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.rehearsall.data.db.entity.AudioFileEntity
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.WaveformRepository
import com.rehearsall.domain.model.AudioFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AudioFileRepository,
    private val waveformRepository: WaveformRepository,
) {
    companion object {
        private val SUPPORTED_FORMATS = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac")
        private const val MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024 // 500MB
    }

    suspend fun import(uri: Uri): Result<AudioFile> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri)
                ?: return@withContext Result.failure(ImportError.UnknownFileName)

            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension !in SUPPORTED_FORMATS) {
                Timber.w("Import rejected: unsupported format '%s' for file '%s'", extension, fileName)
                return@withContext Result.failure(
                    ImportError.UnsupportedFormat(extension, SUPPORTED_FORMATS)
                )
            }

            val fileSize = getFileSize(uri)
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                Timber.w("Import rejected: file too large (%d bytes) for '%s'", fileSize, fileName)
                return@withContext Result.failure(ImportError.FileTooLarge(fileSize, MAX_FILE_SIZE_BYTES))
            }

            // Copy to internal storage with UUID name
            val internalFile = copyToInternal(uri, extension)

            // Extract metadata
            val metadata = extractMetadata(internalFile)

            val displayName = metadata.title
                ?: fileName.substringBeforeLast('.', fileName)

            val entity = AudioFileEntity(
                fileName = fileName,
                displayName = displayName,
                internalPath = internalFile.absolutePath,
                format = extension,
                durationMs = metadata.durationMs,
                fileSizeBytes = fileSize,
                artist = metadata.artist,
                title = metadata.title,
                importedAt = System.currentTimeMillis(),
                lastPlayedAt = null,
            )

            val id = repository.insert(entity)
            val audioFile = repository.getById(id)
                ?: return@withContext Result.failure(ImportError.DatabaseError)

            Timber.i("Imported '%s' (id=%d, format=%s, duration=%dms, size=%d bytes)",
                displayName, id, extension, metadata.durationMs, fileSize)

            // Extract waveform in background (non-blocking)
            try {
                waveformRepository.ensureCached(id, internalFile.absolutePath)
            } catch (e: Exception) {
                Timber.w(e, "Waveform extraction failed for '%s', will retry on playback", displayName)
            }

            Result.success(audioFile)
        } catch (e: Exception) {
            Timber.e(e, "Import failed")
            Result.failure(ImportError.Unknown(e))
        }
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex)
                }
            }
        }
        return 0L
    }

    private fun copyToInternal(uri: Uri, extension: String): File {
        val audioDir = File(context.filesDir, "audio")
        audioDir.mkdirs()
        val destFile = File(audioDir, "${UUID.randomUUID()}.$extension")

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw ImportError.CopyFailed

        return destFile
    }

    private fun extractMetadata(file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            AudioMetadata(
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
            )
        } finally {
            retriever.release()
        }
    }

    private data class AudioMetadata(
        val durationMs: Long,
        val artist: String?,
        val title: String?,
    )
}

sealed class ImportError : Exception() {
    data object UnknownFileName : ImportError() {
        private fun readResolve(): Any = UnknownFileName
    }

    data class UnsupportedFormat(
        val format: String,
        val supported: Set<String>,
    ) : ImportError() {
        override val message: String
            get() = "Unsupported format '$format'. Supported: ${supported.joinToString()}"
    }

    data class FileTooLarge(
        val sizeBytes: Long,
        val maxBytes: Long,
    ) : ImportError() {
        override val message: String
            get() = "File is ${sizeBytes / 1024 / 1024}MB, max is ${maxBytes / 1024 / 1024}MB"
    }

    data object CopyFailed : ImportError() {
        private fun readResolve(): Any = CopyFailed
    }

    data object DatabaseError : ImportError() {
        private fun readResolve(): Any = DatabaseError
    }

    data class Unknown(override val cause: Throwable) : ImportError()
}
