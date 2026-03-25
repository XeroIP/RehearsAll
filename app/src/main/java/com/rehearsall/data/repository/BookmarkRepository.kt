package com.rehearsall.data.repository

import com.rehearsall.data.db.dao.BookmarkDao
import com.rehearsall.data.db.entity.BookmarkEntity
import com.rehearsall.domain.model.Bookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface BookmarkRepository {
    fun getBookmarksForFile(audioFileId: Long): Flow<List<Bookmark>>

    suspend fun addBookmark(
        audioFileId: Long,
        positionMs: Long,
        name: String,
    ): Long

    suspend fun renameBookmark(
        id: Long,
        name: String,
    )

    suspend fun updateBookmarkPosition(
        id: Long,
        positionMs: Long,
    )

    suspend fun deleteBookmark(id: Long)
}

@Singleton
class BookmarkRepositoryImpl
    @Inject
    constructor(
        private val dao: BookmarkDao,
    ) : BookmarkRepository {
        override fun getBookmarksForFile(audioFileId: Long): Flow<List<Bookmark>> {
            return dao.getAllForFile(audioFileId).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override suspend fun addBookmark(
            audioFileId: Long,
            positionMs: Long,
            name: String,
        ): Long =
            withContext(Dispatchers.IO) {
                dao.insert(
                    BookmarkEntity(
                        audioFileId = audioFileId,
                        positionMs = positionMs,
                        name = name,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
            }

        override suspend fun renameBookmark(
            id: Long,
            name: String,
        ) = withContext(Dispatchers.IO) {
            dao.updateName(id, name)
        }

        override suspend fun updateBookmarkPosition(
            id: Long,
            positionMs: Long,
        ) = withContext(Dispatchers.IO) {
            dao.updatePosition(id, positionMs)
        }

        override suspend fun deleteBookmark(id: Long) =
            withContext(Dispatchers.IO) {
                dao.delete(id)
            }
    }

private fun BookmarkEntity.toDomain(): Bookmark =
    Bookmark(
        id = id,
        audioFileId = audioFileId,
        positionMs = positionMs,
        name = name,
        createdAt = Instant.ofEpochMilli(createdAt),
    )
