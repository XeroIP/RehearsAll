package com.rehearsall.di

import android.content.Context
import androidx.room.Room
import com.rehearsall.data.db.RehearsAllDatabase
import com.rehearsall.data.db.dao.AudioFileDao
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
    fun provideDatabase(@ApplicationContext context: Context): RehearsAllDatabase {
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
}
