package com.rehearsall.di

import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.AudioFileRepositoryImpl
import com.rehearsall.data.repository.BookmarkRepository
import com.rehearsall.data.repository.BookmarkRepositoryImpl
import com.rehearsall.data.repository.ChunkMarkerRepository
import com.rehearsall.data.repository.ChunkMarkerRepositoryImpl
import com.rehearsall.data.repository.LoopRepository
import com.rehearsall.data.repository.LoopRepositoryImpl
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.data.repository.PlaylistRepositoryImpl
import com.rehearsall.data.repository.PracticeSettingsRepository
import com.rehearsall.data.repository.PracticeSettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAudioFileRepository(impl: AudioFileRepositoryImpl): AudioFileRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindLoopRepository(impl: LoopRepositoryImpl): LoopRepository

    @Binds
    @Singleton
    abstract fun bindChunkMarkerRepository(impl: ChunkMarkerRepositoryImpl): ChunkMarkerRepository

    @Binds
    @Singleton
    abstract fun bindPracticeSettingsRepository(impl: PracticeSettingsRepositoryImpl): PracticeSettingsRepository
}
