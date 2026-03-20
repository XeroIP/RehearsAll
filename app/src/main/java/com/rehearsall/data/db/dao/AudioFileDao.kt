package com.rehearsall.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rehearsall.data.db.entity.AudioFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioFileDao {

    @Insert
    suspend fun insert(entity: AudioFileEntity): Long

    @Query("SELECT * FROM audio_files ORDER BY importedAt DESC")
    fun getAll(): Flow<List<AudioFileEntity>>

    @Query("SELECT * FROM audio_files WHERE id = :id")
    suspend fun getById(id: Long): AudioFileEntity?

    @Query(
        "SELECT * FROM audio_files WHERE lastPlayedAt IS NOT NULL " +
            "ORDER BY lastPlayedAt DESC LIMIT :limit"
    )
    fun getRecent(limit: Int = 20): Flow<List<AudioFileEntity>>

    @Query("DELETE FROM audio_files WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE audio_files SET displayName = :displayName WHERE id = :id")
    suspend fun updateDisplayName(id: Long, displayName: String)

    @Query(
        "UPDATE audio_files SET lastPlayedAt = :lastPlayedAt, lastPositionMs = :lastPositionMs " +
            "WHERE id = :id"
    )
    suspend fun updateLastPlayed(id: Long, lastPlayedAt: Long, lastPositionMs: Long)

    @Query("UPDATE audio_files SET lastPositionMs = :positionMs WHERE id = :id")
    suspend fun updateLastPosition(id: Long, positionMs: Long)

    @Query("UPDATE audio_files SET lastSpeed = :speed WHERE id = :id")
    suspend fun updateLastSpeed(id: Long, speed: Float)
}
