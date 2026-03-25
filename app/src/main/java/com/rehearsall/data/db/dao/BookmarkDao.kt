package com.rehearsall.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rehearsall.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("SELECT * FROM bookmarks WHERE audioFileId = :audioFileId ORDER BY positionMs ASC")
    fun getAllForFile(audioFileId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: Long): BookmarkEntity?

    @Query("UPDATE bookmarks SET name = :name WHERE id = :id")
    suspend fun updateName(
        id: Long,
        name: String,
    )

    @Query("UPDATE bookmarks SET positionMs = :positionMs WHERE id = :id")
    suspend fun updatePosition(
        id: Long,
        positionMs: Long,
    )

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}
