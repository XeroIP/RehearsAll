package com.rehearsall.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "loops",
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
data class LoopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioFileId: Long,
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val createdAt: Long,
)
