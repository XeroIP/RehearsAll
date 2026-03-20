package com.rehearsall.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_files")
data class AudioFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val displayName: String,
    val internalPath: String,
    val format: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val artist: String?,
    val title: String?,
    val importedAt: Long,
    val lastPlayedAt: Long?,
    val lastPositionMs: Long = 0,
    val lastSpeed: Float = 1.0f,
)
