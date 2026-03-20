package com.rehearsall.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunk_markers",
    foreignKeys = [
        ForeignKey(
            entity = AudioFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("audioFileId")],
)
data class ChunkMarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioFileId: Long,
    val positionMs: Long,
    val label: String,
    val createdAt: Long,
)
