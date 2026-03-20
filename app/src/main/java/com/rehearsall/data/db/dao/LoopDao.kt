package com.rehearsall.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rehearsall.data.db.entity.LoopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopDao {

    @Insert
    suspend fun insert(loop: LoopEntity): Long

    @Query("SELECT * FROM loops WHERE audioFileId = :audioFileId ORDER BY createdAt DESC")
    fun getAllForFile(audioFileId: Long): Flow<List<LoopEntity>>

    @Query("SELECT * FROM loops WHERE audioFileId = :audioFileId ORDER BY createdAt DESC")
    suspend fun getAllForFileList(audioFileId: Long): List<LoopEntity>

    @Query("SELECT * FROM loops WHERE id = :id")
    suspend fun getById(id: Long): LoopEntity?

    @Query("UPDATE loops SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String)

    @Query("UPDATE loops SET startMs = :startMs, endMs = :endMs WHERE id = :id")
    suspend fun updateRegion(id: Long, startMs: Long, endMs: Long)

    @Query("DELETE FROM loops WHERE id = :id")
    suspend fun delete(id: Long)
}
