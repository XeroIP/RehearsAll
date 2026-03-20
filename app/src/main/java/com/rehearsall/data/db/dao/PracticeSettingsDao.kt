package com.rehearsall.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rehearsall.data.db.entity.PracticeSettingsEntity

@Dao
interface PracticeSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: PracticeSettingsEntity)

    @Query("SELECT * FROM practice_settings WHERE audioFileId = :audioFileId")
    suspend fun getForFile(audioFileId: Long): PracticeSettingsEntity?
}
