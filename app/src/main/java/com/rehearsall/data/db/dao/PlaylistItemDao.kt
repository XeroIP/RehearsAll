package com.rehearsall.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rehearsall.data.db.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistItemDao {
    @Insert
    suspend fun insert(entity: PlaylistItemEntity): Long

    @Insert
    suspend fun insertAll(entities: List<PlaylistItemEntity>)

    @Query(
        "SELECT pi.*, af.* FROM playlist_items pi " +
            "INNER JOIN audio_files af ON pi.audioFileId = af.id " +
            "WHERE pi.playlistId = :playlistId " +
            "ORDER BY pi.orderIndex ASC",
    )
    fun getItemsWithFiles(playlistId: Long): Flow<List<PlaylistItemWithFile>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getItemsForPlaylist(playlistId: Long): List<PlaylistItemEntity>

    @Query("SELECT MAX(orderIndex) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Query("DELETE FROM playlist_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllForPlaylist(playlistId: Long)

    @Query("UPDATE playlist_items SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrder(
        id: Long,
        orderIndex: Int,
    )

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getItemCount(playlistId: Long): Int

    @Query(
        "SELECT COALESCE(SUM(af.durationMs), 0) FROM playlist_items pi " +
            "INNER JOIN audio_files af ON pi.audioFileId = af.id " +
            "WHERE pi.playlistId = :playlistId",
    )
    suspend fun getTotalDuration(playlistId: Long): Long
}

data class PlaylistItemWithFile(
    val id: Long,
    val playlistId: Long,
    val audioFileId: Long,
    val orderIndex: Int,
    // AudioFileEntity fields via JOIN — Room maps these by column name
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
    val lastPositionMs: Long,
    val lastSpeed: Float,
)
