package com.rehearsall.di

import com.rehearsall.playback.PlaybackManager
import com.rehearsall.playback.PlaybackManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindPlaybackManager(impl: PlaybackManagerImpl): PlaybackManager
}
