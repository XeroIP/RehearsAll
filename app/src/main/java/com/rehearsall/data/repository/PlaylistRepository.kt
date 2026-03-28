package com.rehearsall.data.repository

import com.rehearsall.data.db.dao.PlaylistDao
import com.rehearsall.data.db.dao.PlaylistItemDao
import com.rehearsall.data.db.dao.PlaylistItemWithFile
import com.rehearsall.data.db.entity.PlaylistEntity
import com.rehearsall.data.db.entity.PlaylistItemEntity
import com.rehearsall.domain.model.Playlist
import com.rehearsall.domain.model.PlaylistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>>

    suspend fun getById(id: Long): Playlist?

    suspend fun createPlaylist(name: String): Long

    suspend fun renamePlaylist(
        id: Long,
        name: String,
    )

    suspend fun deletePlaylist(id: Long)

    suspend fun addFileToPlaylist(
        playlistId: Long,
        audioFileId: Long,
    )

    suspend fun removeItem(
        itemId: Long,
        playlistId: Long,
    )

    suspend fun reorderItems(
        playlistId: Long,
        items: List<Pair<Long, Int>>,
    )
}

@Singleton
class PlaylistRepositoryImpl
    @Inject
    constructor(
        private val playlistDao: PlaylistDao,
        private val playlistItemDao: PlaylistItemDao,
    ) : PlaylistRepository {
        override fun getAllPlaylists(): Flow<List<Playlist>> {
            return playlistDao.getAll().map { entities ->
                entities.map { entity ->
                    val count = playlistItemDao.getItemCount(entity.id)
                    val duration = playlistItemDao.getTotalDuration(entity.id)
                    entity.toDomain(count, duration)
                }
            }
        }

        override fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>> {
            return playlistItemDao.getItemsWithFiles(playlistId).map { items ->
                items.map { it.toDomain() }
            }
        }

        override suspend fun getById(id: Long): Playlist? {
            val entity = playlistDao.getById(id) ?: return null
            val count = playlistItemDao.getItemCount(id)
            val duration = playlistItemDao.getTotalDuration(id)
            return entity.toDomain(count, duration)
        }

        override suspend fun createPlaylist(name: String): Long {
            val now = System.currentTimeMillis()
            return playlistDao.insert(
                PlaylistEntity(name = name, createdAt = now, updatedAt = now),
            )
        }

        override suspend fun renamePlaylist(
            id: Long,
            name: String,
        ) = playlistDao.updateName(id, name)

        override suspend fun deletePlaylist(id: Long) =
            playlistDao.delete(id) // CASCADE deletes items

        override suspend fun addFileToPlaylist(
            playlistId: Long,
            audioFileId: Long,
        ) {
            val maxOrder = playlistItemDao.getMaxOrderIndex(playlistId) ?: -1
            playlistItemDao.insert(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    audioFileId = audioFileId,
                    orderIndex = maxOrder + 1,
                ),
            )
            playlistDao.touch(playlistId)
        }

        override suspend fun removeItem(
            itemId: Long,
            playlistId: Long,
        ) {
            playlistItemDao.delete(itemId)
            playlistDao.touch(playlistId)
        }

        override suspend fun reorderItems(
            playlistId: Long,
            items: List<Pair<Long, Int>>,
        ) {
            items.forEach { (id, order) ->
                playlistItemDao.updateOrder(id, order)
            }
            playlistDao.touch(playlistId)
        }
    }

private fun PlaylistEntity.toDomain(
    trackCount: Int,
    totalDurationMs: Long,
): Playlist =
    Playlist(
        id = id,
        name = name,
        trackCount = trackCount,
        totalDurationMs = totalDurationMs,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

private fun PlaylistItemWithFile.toDomain(): PlaylistItem =
    PlaylistItem(
        id = id,
        playlistId = playlistId,
        audioFileId = audioFileId,
        orderIndex = orderIndex,
        displayName = displayName,
        artist = artist,
        durationMs = durationMs,
        internalPath = internalPath,
        format = format,
    )
