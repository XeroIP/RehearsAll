package com.rehearsall.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides Room database and DAO instances.
 * Populated in Phase 2 when the database is created.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
