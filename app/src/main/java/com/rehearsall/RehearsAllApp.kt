package com.rehearsall

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.rehearsall.logging.FileLoggingTree

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
