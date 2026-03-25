package com.rehearsall.playback

import android.annotation.SuppressLint
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
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rehearsall.data.preferences.UserPreferencesRepository
import com.rehearsall.di.ServiceEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Foreground service that owns the ExoPlayer instance and MediaLibrarySession.
 *
 * Architecture note: all direct player access (polling, A-B loop enforcement,
 * crossfade) lives here. The app-side PlaybackManagerImpl communicates via
 * MediaController commands only.
 *
 * Also serves the Android Auto content tree via MediaLibrarySession.Callback.
 */
class RehearsAllPlaybackService : MediaLibraryService() {
    companion object {
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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // A-B loop state — enforced by position polling
    private var loopRegion: LoopRegion? = null
    private var loopPollingJob: Job? = null
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    // Content tree for Android Auto browsing
    private lateinit var contentTreeBuilder: ContentTreeBuilder
    private lateinit var loopActionHandler: LoopActionHandler

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        Timber.i("PlaybackService created")

        // Access DAOs via Hilt EntryPoint (service can't use @AndroidEntryPoint with MediaLibraryService)
        val entryPoint =
            EntryPointAccessors.fromApplication(
                applicationContext,
                ServiceEntryPoint::class.java,
            )
        contentTreeBuilder =
            ContentTreeBuilder(
                audioFileDao = entryPoint.audioFileDao(),
                playlistDao = entryPoint.playlistDao(),
                playlistItemDao = entryPoint.playlistItemDao(),
                loopDao = entryPoint.loopDao(),
            )
        loopActionHandler = LoopActionHandler(entryPoint.loopDao())
        userPreferencesRepository = entryPoint.userPreferencesRepository()

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

        val exoPlayer =
            ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
                .setHandleAudioBecomingNoisy(true)
                .build()

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying && loopRegion != null) {
                        startLoopPolling(exoPlayer)
                    } else {
                        stopLoopPolling()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED && loopRegion != null) {
                        val region = loopRegion ?: return
                        exoPlayer.seekTo(region.startMs)
                        exoPlayer.play()
                    }
                }
            },
        )

        player = exoPlayer

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                packageManager.getLaunchIntentForPackage(packageName),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        session =
            MediaLibrarySession.Builder(this, exoPlayer, LibrarySessionCallback())
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
        serviceScope.cancel()
        session?.run {
            player.release()
            release()
            session = null
        }
        player = null
        super.onDestroy()
    }

    private fun startLoopPolling(exoPlayer: ExoPlayer) {
        stopLoopPolling()
        loopPollingJob =
            serviceScope.launch {
                val crossfadeEnabled = userPreferencesRepository.loopCrossfade.first()
                val fadeMs = 50L
                var fadingIn = false
                var fadeInStart = 0L

                while (true) {
                    val region = loopRegion ?: break
                    val pos = exoPlayer.currentPosition
                    val loopLen = region.endMs - region.startMs

                    // Disable crossfade for very short loops (< 150ms)
                    val useCrossfade = crossfadeEnabled && loopLen >= 150

                    if (pos >= region.endMs || pos < region.startMs - 500) {
                        if (useCrossfade) {
                            exoPlayer.volume = 0f
                            exoPlayer.seekTo(region.startMs)
                            fadingIn = true
                            fadeInStart = System.currentTimeMillis()
                        } else {
                            exoPlayer.seekTo(region.startMs)
                        }
                    } else if (useCrossfade) {
                        val msUntilEnd = region.endMs - pos
                        if (fadingIn) {
                            // Fade volume back in after seek
                            val elapsed = System.currentTimeMillis() - fadeInStart
                            val progress = (elapsed.toFloat() / fadeMs).coerceIn(0f, 1f)
                            exoPlayer.volume = progress
                            if (progress >= 1f) fadingIn = false
                        } else if (msUntilEnd <= fadeMs) {
                            // Fade out approaching loop end
                            val progress = (msUntilEnd.toFloat() / fadeMs).coerceIn(0f, 1f)
                            exoPlayer.volume = progress
                        } else {
                            exoPlayer.volume = 1f
                        }
                    }

                    delay(16) // ~60fps polling for smooth crossfade
                }
            }
    }

    private fun stopLoopPolling() {
        loopPollingJob?.cancel()
        loopPollingJob = null
    }

    /**
     * Callback that handles media browsing (Android Auto), custom session commands,
     * search, and loop-aware playback.
     */
    @OptIn(UnstableApi::class)
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands =
                MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                    .buildUpon()
                    .add(SessionCommand(CMD_SET_SPEED, Bundle.EMPTY))
                    .add(SessionCommand(CMD_GET_SPEED, Bundle.EMPTY))
                    .add(SessionCommand(CMD_SET_LOOP_REGION, Bundle.EMPTY))
                    .add(SessionCommand(CMD_CLEAR_LOOP_REGION, Bundle.EMPTY))
                    .add(SessionCommand(LoopActionHandler.ACTION_TOGGLE_LOOP, Bundle.EMPTY))
                    .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        // -- Content tree browsing --

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItemMapper.browsableFolder(ContentTreeBuilder.ROOT_ID, "RehearsAll")
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceScope.future {
                val children = contentTreeBuilder.getChildren(parentId)
                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
            }
        }

        @SuppressLint("WrongConstant")
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return serviceScope.future {
                val item = contentTreeBuilder.getItem(mediaId)
                if (item != null) {
                    LibraryResult.ofItem(item, null)
                } else {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                }
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.future {
                val results = contentTreeBuilder.search(query)
                session.notifySearchResultChanged(browser, query, results.size, params)
            }
            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return serviceScope.future {
                val results = contentTreeBuilder.search(query)
                LibraryResult.ofItemList(ImmutableList.copyOf(results), params)
            }
        }

        // -- Playback: handle item selection with loop awareness --

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            return serviceScope.future {
                // Resolve media items — add URI and handle loop activation
                val resolved =
                    mediaItems.map { item ->
                        val resolvedItem = contentTreeBuilder.getItem(item.mediaId) ?: item

                        // If this is a loop item, activate the loop region
                        val loopInfo = loopActionHandler.parseLoopFromMediaItem(resolvedItem)
                        if (loopInfo != null) {
                            loopRegion = loopInfo
                            Timber.d("Auto: activated loop %d-%d ms", loopInfo.startMs, loopInfo.endMs)
                        } else if (item.mediaId.endsWith(":full") || !item.mediaId.contains(":loop:")) {
                            // Full track or regular file — clear any active loop
                            loopRegion = null
                        }

                        resolvedItem
                    }
                resolved.toMutableList()
            }
        }

        // -- Custom commands --

        @SuppressLint("WrongConstant")
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val p =
                player ?: return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_UNKNOWN),
                )

            return when (customCommand.customAction) {
                CMD_SET_SPEED -> {
                    val speed =
                        args.getFloat(ARG_SPEED, 1.0f)
                            .coerceIn(0.25f, 3.0f)
                    val rounded = (speed * 20).toInt() / 20f
                    p.playbackParameters = PlaybackParameters(rounded)
                    Timber.d("Speed set to %.2fx", rounded)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_GET_SPEED -> {
                    val result =
                        Bundle().apply {
                            putFloat(ARG_SPEED, p.playbackParameters.speed)
                        }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, result))
                }

                CMD_SET_LOOP_REGION -> {
                    val start = args.getLong(ARG_LOOP_START, -1L)
                    val end = args.getLong(ARG_LOOP_END, -1L)
                    if (start >= 0 && end > start && (end - start) >= 100) {
                        loopRegion = LoopRegion(start, end)
                        if (p.isPlaying) startLoopPolling(p)
                        Timber.d("Loop region set: %d – %d ms", start, end)
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_CLEAR_LOOP_REGION -> {
                    loopRegion = null
                    stopLoopPolling()
                    Timber.d("Loop region cleared")
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                LoopActionHandler.ACTION_TOGGLE_LOOP -> {
                    serviceScope.future {
                        val currentFileId =
                            p.currentMediaItem?.mediaId
                                ?.removePrefix("file:")
                                ?.split(":")
                                ?.firstOrNull()
                                ?.toLongOrNull()
                        val newRegion = loopActionHandler.handleToggle(currentFileId, loopRegion)
                        loopRegion = newRegion
                        SessionResult(SessionResult.RESULT_SUCCESS)
                    }
                }

                else ->
                    Futures.immediateFuture(
                        SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED),
                    )
            }
        }
    }
}
