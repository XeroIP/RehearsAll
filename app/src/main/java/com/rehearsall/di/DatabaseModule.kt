package com.rehearsall.di

import android.content.Context
import androidx.room.Room
import com.rehearsall.data.db.RehearsAllDatabase
import com.rehearsall.data.db.dao.AudioFileDao
import com.rehearsall.data.db.dao.BookmarkDao
import com.rehearsall.data.db.dao.ChunkMarkerDao
import com.rehearsall.data.db.dao.LoopDao
import com.rehearsall.data.db.dao.PlaylistDao
import com.rehearsall.data.db.dao.PlaylistItemDao
import com.rehearsall.data.db.dao.PracticeSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): RehearsAllDatabase {
        return Room.databaseBuilder(
            context,
            RehearsAllDatabase::class.java,
            "rehearsall.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAudioFileDao(database: RehearsAllDatabase): AudioFileDao {
        return database.audioFileDao()
    }

    @Provides
    fun providePlaylistDao(database: RehearsAllDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun providePlaylistItemDao(database: RehearsAllDatabase): PlaylistItemDao {
        return database.playlistItemDao()
    }

    @Provides
    fun provideBookmarkDao(database: RehearsAllDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideLoopDao(database: RehearsAllDatabase): LoopDao {
        return database.loopDao()
    }

    @Provides
    fun provideChunkMarkerDao(database: RehearsAllDatabase): ChunkMarkerDao {
        return database.chunkMarkerDao()
    }

    @Provides
    fun providePracticeSettingsDao(database: RehearsAllDatabase): PracticeSettingsDao {
        return database.practiceSettingsDao()
    }
}
