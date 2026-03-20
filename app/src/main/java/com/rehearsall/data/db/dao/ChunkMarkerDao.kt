package com.rehearsall.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rehearsall.data.db.entity.ChunkMarkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkMarkerDao {

    @Insert
    suspend fun insert(marker: ChunkMarkerEntity): Long

    @Query("SELECT * FROM chunk_markers WHERE audioFileId = :audioFileId ORDER BY positionMs ASC")
    fun getAllForFile(audioFileId: Long): Flow<List<ChunkMarkerEntity>>

    @Query("UPDATE chunk_markers SET label = :label WHERE id = :id")
    suspend fun updateLabel(id: Long, label: String)

    @Query("UPDATE chunk_markers SET positionMs = :positionMs WHERE id = :id")
    suspend fun updatePosition(id: Long, positionMs: Long)

    @Query("DELETE FROM chunk_markers WHERE id = :id")
    suspend fun delete(id: Long)
}
