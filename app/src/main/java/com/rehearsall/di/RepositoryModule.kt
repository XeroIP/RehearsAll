package com.rehearsall.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds repository interfaces to their implementations.
 * Populated in Phase 2 as repositories are created.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
