package com.rehearsall.data.repository

import com.rehearsall.data.db.dao.AudioFileDao
import com.rehearsall.data.db.entity.AudioFileEntity
import com.rehearsall.domain.model.AudioFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface AudioFileRepository {
    fun getAllFiles(): Flow<List<AudioFile>>

    fun getRecentFiles(limit: Int = 20): Flow<List<AudioFile>>

    suspend fun getAllFilesList(): List<AudioFile>

    suspend fun getById(id: Long): AudioFile?

    suspend fun insert(entity: AudioFileEntity): Long

    suspend fun delete(id: Long)

    suspend fun updateDisplayName(
        id: Long,
        displayName: String,
    )

    suspend fun updateLastPlayed(
        id: Long,
        positionMs: Long,
    )

    suspend fun updateLastPosition(
        id: Long,
        positionMs: Long,
    )

    suspend fun updateLastSpeed(
        id: Long,
        speed: Float,
    )
}

@Singleton
class AudioFileRepositoryImpl
    @Inject
    constructor(
        private val dao: AudioFileDao,
    ) : AudioFileRepository {
        override fun getAllFiles(): Flow<List<AudioFile>> {
            return dao.getAll().map { entities -> entities.map { it.toDomain() } }
        }

        override fun getRecentFiles(limit: Int): Flow<List<AudioFile>> {
            return dao.getRecent(limit).map { entities -> entities.map { it.toDomain() } }
        }

        override suspend fun getAllFilesList(): List<AudioFile> =
            dao.getAllList().map { it.toDomain() }

        override suspend fun getById(id: Long): AudioFile? =
            dao.getById(id)?.toDomain()

        override suspend fun insert(entity: AudioFileEntity): Long =
            dao.insert(entity)

        override suspend fun delete(id: Long) =
            dao.delete(id)

        override suspend fun updateDisplayName(
            id: Long,
            displayName: String,
        ) = dao.updateDisplayName(id, displayName)

        override suspend fun updateLastPlayed(
            id: Long,
            positionMs: Long,
        ) = dao.updateLastPlayed(id, System.currentTimeMillis(), positionMs)

        override suspend fun updateLastPosition(
            id: Long,
            positionMs: Long,
        ) = dao.updateLastPosition(id, positionMs)

        override suspend fun updateLastSpeed(
            id: Long,
            speed: Float,
        ) = dao.updateLastSpeed(id, speed)
    }

private fun AudioFileEntity.toDomain(): AudioFile =
    AudioFile(
        id = id,
        displayName = displayName,
        artist = artist,
        title = title,
        format = format,
        durationMs = durationMs,
        fileSizeBytes = fileSizeBytes,
        internalPath = internalPath,
        importedAt = Instant.ofEpochMilli(importedAt),
        lastPlayedAt = lastPlayedAt?.let { Instant.ofEpochMilli(it) },
        lastPositionMs = lastPositionMs,
        lastSpeed = lastSpeed,
    )
