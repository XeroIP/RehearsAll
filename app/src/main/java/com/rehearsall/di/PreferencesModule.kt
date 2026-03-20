package com.rehearsall.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides DataStore preferences instance.
 * Populated in Phase 10 when settings are implemented.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule
