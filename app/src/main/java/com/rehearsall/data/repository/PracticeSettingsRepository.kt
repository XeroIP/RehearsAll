package com.rehearsall.data.repository

import com.rehearsall.data.db.dao.PracticeSettingsDao
import com.rehearsall.data.db.entity.PracticeSettingsEntity
import com.rehearsall.domain.model.PracticeMode
import com.rehearsall.domain.model.PracticeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface PracticeSettingsRepository {
    suspend fun getForFile(audioFileId: Long): PracticeSettings

    suspend fun save(
        audioFileId: Long,
        settings: PracticeSettings,
    )
}

@Singleton
class PracticeSettingsRepositoryImpl
    @Inject
    constructor(
        private val dao: PracticeSettingsDao,
    ) : PracticeSettingsRepository {
        override suspend fun getForFile(audioFileId: Long): PracticeSettings =
            withContext(Dispatchers.IO) {
                dao.getForFile(audioFileId)?.toDomain() ?: PracticeSettings()
            }

        override suspend fun save(
            audioFileId: Long,
            settings: PracticeSettings,
        ) = withContext(Dispatchers.IO) {
            dao.insertOrUpdate(
                PracticeSettingsEntity(
                    audioFileId = audioFileId,
                    repeatCount = settings.repeatCount,
                    gapBetweenRepsMs = settings.gapBetweenRepsMs,
                    gapBetweenChunksMs = settings.gapBetweenChunksMs,
                    selectedMode = settings.mode.name,
                ),
            )
        }
    }

private fun PracticeSettingsEntity.toDomain(): PracticeSettings =
    PracticeSettings(
        repeatCount = repeatCount,
        gapBetweenRepsMs = gapBetweenRepsMs,
        gapBetweenChunksMs = gapBetweenChunksMs,
        mode =
            try {
                PracticeMode.valueOf(selectedMode)
            } catch (_: IllegalArgumentException) {
                PracticeMode.SINGLE_CHUNK_LOOP
            },
    )
