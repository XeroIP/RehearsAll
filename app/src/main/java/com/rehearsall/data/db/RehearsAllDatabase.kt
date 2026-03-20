package com.rehearsall.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rehearsall.data.db.dao.AudioFileDao
import com.rehearsall.data.db.entity.AudioFileEntity

@Database(
    entities = [AudioFileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RehearsAllDatabase : RoomDatabase() {
    abstract fun audioFileDao(): AudioFileDao
}
