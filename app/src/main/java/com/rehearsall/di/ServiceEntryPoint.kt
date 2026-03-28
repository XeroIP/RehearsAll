package com.rehearsall.di

import com.rehearsall.data.db.dao.AudioFileDao
import com.rehearsall.data.db.dao.LoopDao
import com.rehearsall.data.db.dao.PlaylistDao
import com.rehearsall.data.db.dao.PlaylistItemDao
import com.rehearsall.data.preferences.UserPreferencesRepository
import com.rehearsall.data.repository.LoopRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for accessing DAOs and preferences from RehearsAllPlaybackService.
 * MediaLibraryService can't use @AndroidEntryPoint, so we use EntryPointAccessors instead.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceEntryPoint {
    fun audioFileDao(): AudioFileDao

    fun playlistDao(): PlaylistDao

    fun playlistItemDao(): PlaylistItemDao

    fun loopDao(): LoopDao

    fun loopRepository(): LoopRepository

    fun userPreferencesRepository(): UserPreferencesRepository
}
