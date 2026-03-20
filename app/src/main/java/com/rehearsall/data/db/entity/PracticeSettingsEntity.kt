package com.rehearsall.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "practice_settings",
    foreignKeys = [
        ForeignKey(
            entity = AudioFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PracticeSettingsEntity(
    @PrimaryKey val audioFileId: Long,
    val repeatCount: Int = 3,
    val gapBetweenRepsMs: Long = 0,
    val gapBetweenChunksMs: Long = 1000,
    val selectedMode: String = "SINGLE_CHUNK_LOOP",
)
