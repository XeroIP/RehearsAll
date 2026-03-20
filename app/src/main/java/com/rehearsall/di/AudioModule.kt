package com.rehearsall.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides PlaybackManager and audio-related dependencies.
 * Populated in Phase 3 when the playback service is created.
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule
