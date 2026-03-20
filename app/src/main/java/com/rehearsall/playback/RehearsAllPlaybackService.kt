package com.rehearsall.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber

/**
 * Foreground service that owns the ExoPlayer instance and MediaLibrarySession.
 *
 * Architecture note: all direct player access (polling, A-B loop enforcement,
 * crossfade) lives here. The app-side PlaybackManagerImpl communicates via
 * MediaController commands only.
 */
class RehearsAllPlaybackService : MediaLibraryService() {

    companion object {
        // Custom session commands for features not in standard MediaController API
        const val CMD_SET_SPEED = "com.rehearsall.CMD_SET_SPEED"
        const val CMD_GET_SPEED = "com.rehearsall.CMD_GET_SPEED"
        const val CMD_SET_LOOP_REGION = "com.rehearsall.CMD_SET_LOOP_REGION"
        const val CMD_CLEAR_LOOP_REGION = "com.rehearsall.CMD_CLEAR_LOOP_REGION"
        const val ARG_SPEED = "speed"
        const val ARG_LOOP_START = "loop_start"
        const val ARG_LOOP_END = "loop_end"
    }

    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null

    // A-B loop state — enforced in the player listener
    private var loopRegion: LoopRegion? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Timber.i("PlaybackService created")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // When track ends near a loop region, seek back and resume
                if (playbackState == Player.STATE_ENDED && loopRegion != null) {
                    val region = loopRegion ?: return
                    exoPlayer.seekTo(region.startMs)
                    exoPlayer.play()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // Enforce A-B loop boundary during normal playback
                enforceLoopBoundary(exoPlayer)
            }
        })

        player = exoPlayer

        // PendingIntent to reopen the app when tapping the notification
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        session = MediaLibrarySession.Builder(this, exoPlayer, LibrarySessionCallback())
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player ?: return
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Timber.i("PlaybackService destroyed")
        session?.run {
            player.release()
            release()
            session = null
        }
        player = null
        super.onDestroy()
    }

    private fun enforceLoopBoundary(player: ExoPlayer) {
        val region = loopRegion ?: return
        val pos = player.currentPosition
        if (pos >= region.endMs) {
            player.seekTo(region.startMs)
        }
    }

    /**
     * Callback that handles custom session commands (speed, A-B loop)
     * and validates connecting controllers.
     */
    @OptIn(UnstableApi::class)
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            // Allow own package and system UI (notifications, Auto)
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(CMD_SET_SPEED, Bundle.EMPTY))
                .add(SessionCommand(CMD_GET_SPEED, Bundle.EMPTY))
                .add(SessionCommand(CMD_SET_LOOP_REGION, Bundle.EMPTY))
                .add(SessionCommand(CMD_CLEAR_LOOP_REGION, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val p = player ?: return Futures.immediateFuture(
                SessionResult(SessionResult.RESULT_ERROR_UNKNOWN)
            )

            return when (customCommand.customAction) {
                CMD_SET_SPEED -> {
                    val speed = args.getFloat(ARG_SPEED, 1.0f)
                        .coerceIn(0.25f, 3.0f)
                    val rounded = (speed * 20).toInt() / 20f
                    p.playbackParameters = PlaybackParameters(rounded)
                    Timber.d("Speed set to %.2fx", rounded)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_GET_SPEED -> {
                    val result = Bundle().apply {
                        putFloat(ARG_SPEED, p.playbackParameters.speed)
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
                }

                CMD_SET_LOOP_REGION -> {
                    val start = args.getLong(ARG_LOOP_START, -1L)
                    val end = args.getLong(ARG_LOOP_END, -1L)
                    if (start >= 0 && end > start && (end - start) >= 100) {
                        loopRegion = LoopRegion(start, end)
                        Timber.d("Loop region set: %d – %d ms", start, end)
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_CLEAR_LOOP_REGION -> {
                    loopRegion = null
                    Timber.d("Loop region cleared")
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
                )
            }
        }
    }
}
