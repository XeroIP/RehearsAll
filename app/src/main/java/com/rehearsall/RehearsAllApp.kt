package com.rehearsall

import android.app.Application
import com.rehearsall.logging.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class RehearsAllApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(FileLoggingTree(filesDir))
        }

        Timber.d("RehearsAll application started")
    }
}
