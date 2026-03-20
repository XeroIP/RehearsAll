package com.rehearsall.playback

import android.content.Intent
import android.os.IBinder
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

/**
 * Stub playback service — fully implemented in Phase 3.
 *
 * Registered in the manifest now so the project structure is complete.
 * The real implementation will add ExoPlayer, MediaLibrarySession,
 * and all playback logic.
 */
class RehearsAllPlaybackService : MediaLibraryService() {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        // Phase 3: return the MediaLibrarySession
        return null
    }
}
