package com.rehearsall.data.repository

import com.rehearsall.data.db.dao.ChunkMarkerDao
import com.rehearsall.data.db.entity.ChunkMarkerEntity
import com.rehearsall.domain.model.ChunkMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface ChunkMarkerRepository {
    fun getMarkersForFile(audioFileId: Long): Flow<List<ChunkMarker>>
    suspend fun addMarker(audioFileId: Long, positionMs: Long, label: String): Long
    suspend fun updateLabel(id: Long, label: String)
    suspend fun updatePosition(id: Long, positionMs: Long)
    suspend fun deleteMarker(id: Long)
}

@Singleton
class ChunkMarkerRepositoryImpl @Inject constructor(
    private val dao: ChunkMarkerDao,
) : ChunkMarkerRepository {

    override fun getMarkersForFile(audioFileId: Long): Flow<List<ChunkMarker>> {
        return dao.getAllForFile(audioFileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMarker(audioFileId: Long, positionMs: Long, label: String): Long =
        withContext(Dispatchers.IO) {
            dao.insert(
                ChunkMarkerEntity(
                    audioFileId = audioFileId,
                    positionMs = positionMs,
                    label = label,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }

    override suspend fun updateLabel(id: Long, label: String) = withContext(Dispatchers.IO) {
        dao.updateLabel(id, label)
    }

    override suspend fun updatePosition(id: Long, positionMs: Long) = withContext(Dispatchers.IO) {
        dao.updatePosition(id, positionMs)
    }

    override suspend fun deleteMarker(id: Long) = withContext(Dispatchers.IO) {
        dao.delete(id)
    }
}

private fun ChunkMarkerEntity.toDomain(): ChunkMarker = ChunkMarker(
    id = id,
    positionMs = positionMs,
    label = label,
)
