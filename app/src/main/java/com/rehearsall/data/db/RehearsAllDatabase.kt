package com.rehearsall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rehearsall.data.db.dao.AudioFileDao
import com.rehearsall.data.db.dao.PlaylistDao
import com.rehearsall.data.db.dao.PlaylistItemDao
import com.rehearsall.data.db.entity.AudioFileEntity
import com.rehearsall.data.db.entity.PlaylistEntity
import com.rehearsall.data.db.entity.PlaylistItemEntity

@Database(
    entities = [
        AudioFileEntity::class,
        PlaylistEntity::class,
        PlaylistItemEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class RehearsAllDatabase : RoomDatabase() {
    abstract fun audioFileDao(): AudioFileDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao
}
