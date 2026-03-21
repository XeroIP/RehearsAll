package com.rehearsall.data.repository

import com.rehearsall.data.db.dao.LoopDao
import com.rehearsall.data.db.entity.LoopEntity
import com.rehearsall.domain.model.Loop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface LoopRepository {
    fun getLoopsForFile(audioFileId: Long): Flow<List<Loop>>
    suspend fun saveLoop(audioFileId: Long, name: String, startMs: Long, endMs: Long): Long
    suspend fun renameLoop(id: Long, name: String)
    suspend fun updateBounds(id: Long, startMs: Long, endMs: Long)
    suspend fun deleteLoop(id: Long)
}

@Singleton
class LoopRepositoryImpl @Inject constructor(
    private val dao: LoopDao,
) : LoopRepository {

    override fun getLoopsForFile(audioFileId: Long): Flow<List<Loop>> {
        return dao.getAllForFile(audioFileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveLoop(audioFileId: Long, name: String, startMs: Long, endMs: Long): Long =
        withContext(Dispatchers.IO) {
            dao.insert(
                LoopEntity(
                    audioFileId = audioFileId,
                    name = name,
                    startMs = startMs,
                    endMs = endMs,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }

    override suspend fun renameLoop(id: Long, name: String) = withContext(Dispatchers.IO) {
        dao.updateName(id, name)
    }

    override suspend fun updateBounds(id: Long, startMs: Long, endMs: Long) = withContext(Dispatchers.IO) {
        dao.updateRegion(id, startMs, endMs)
    }

    override suspend fun deleteLoop(id: Long) = withContext(Dispatchers.IO) {
        dao.delete(id)
    }
}

private fun LoopEntity.toDomain(): Loop = Loop(
    id = id,
    audioFileId = audioFileId,
    name = name,
    startMs = startMs,
    endMs = endMs,
    createdAt = Instant.ofEpochMilli(createdAt),
)
