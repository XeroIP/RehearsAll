package com.rehearsall.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-side wrapper around MediaBrowser/MediaController.
 *
 * Connects to [RehearsAllPlaybackService], sends transport commands,
 * and polls position at ~60 fps while playing to keep the UI scrubber smooth.
 */
@Singleton
class PlaybackManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var browser: MediaBrowser? = null
    private var isConnecting = false

    // -- Exposed state flows --
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentFileId = MutableStateFlow<Long?>(null)
    override val currentFileId: StateFlow<Long?> = _currentFileId.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    override val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    override val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    init {
        connectToService()
    }

    private fun connectToService() {
        if (isConnecting || browser != null) return
        isConnecting = true

        scope.launch {
            try {
                val token = SessionToken(
                    context,
                    ComponentName(context, RehearsAllPlaybackService::class.java),
                )
                val mediaBrowser = MediaBrowser.Builder(context, token)
                    .buildAsync()
                    .await()

                browser = mediaBrowser
                isConnecting = false

                // Sync initial state
                syncState(mediaBrowser)

                // Listen for player state changes
                mediaBrowser.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        syncState(mediaBrowser)
                        if (isPlaying) startPolling() else stopPolling()
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        syncState(mediaBrowser)
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        syncState(mediaBrowser)
                    }

                    override fun onRepeatModeChanged(mode: Int) {
                        _repeatMode.value = RepeatMode.fromExoPlayer(mode)
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _shuffleEnabled.value = shuffleModeEnabled
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                        updatePlaybackState(mediaBrowser)
                    }
                })

                if (mediaBrowser.isPlaying) startPolling()

                Timber.i("Connected to PlaybackService")
            } catch (e: Exception) {
                isConnecting = false
                Timber.e(e, "Failed to connect to PlaybackService")
            }
        }
    }

    // -- Position polling --

    private var pollingJob: kotlinx.coroutines.Job? = null

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (true) {
                val b = browser ?: break
                updatePlaybackState(b)
                delay(16) // ~60fps for smooth scrubber
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        // One final sync so position is accurate when paused
        browser?.let { updatePlaybackState(it) }
    }

    private fun syncState(player: MediaBrowser) {
        _repeatMode.value = RepeatMode.fromExoPlayer(player.repeatMode)
        _shuffleEnabled.value = player.shuffleModeEnabled
        updatePlaybackState(player)

        // Extract fileId from MediaItem extras
        val fileId = player.currentMediaItem?.mediaId?.toLongOrNull()
        _currentFileId.value = fileId
    }

    private fun updatePlaybackState(player: MediaBrowser) {
        _playbackState.value = PlaybackState(
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.coerceAtLeast(0L),
            isPlaying = player.isPlaying,
            speed = player.playbackParameters.speed,
        )
    }

    // -- Transport --

    override fun play() {
        browser?.play()
    }

    override fun pause() {
        browser?.pause()
    }

    override fun seekTo(positionMs: Long) {
        browser?.seekTo(positionMs)
        // Immediately update local state for responsive UI
        _playbackState.value = _playbackState.value.copy(positionMs = positionMs)
    }

    override fun skipForward(ms: Long) {
        val b = browser ?: return
        val newPos = (b.currentPosition + ms).coerceAtMost(b.duration)
        seekTo(newPos)
    }

    override fun skipBackward(ms: Long) {
        val b = browser ?: return
        val newPos = (b.currentPosition - ms).coerceAtLeast(0L)
        seekTo(newPos)
    }

    override fun skipToNext() {
        browser?.seekToNextMediaItem()
    }

    override fun skipToPrevious() {
        browser?.seekToPreviousMediaItem()
    }

    // -- Speed --

    override fun setSpeed(speed: Float) {
        val b = browser ?: return
        val args = Bundle().apply {
            putFloat(RehearsAllPlaybackService.ARG_SPEED, speed)
        }
        b.sendCustomCommand(
            SessionCommand(RehearsAllPlaybackService.CMD_SET_SPEED, Bundle.EMPTY),
            args,
        )
        // Optimistic UI update
        _playbackState.value = _playbackState.value.copy(
            speed = (speed * 20).toInt() / 20f
        )
    }

    // -- Queue --

    override fun playFile(fileId: Long, path: String, startPositionMs: Long) {
        val b = browser ?: run {
            Timber.w("Browser not connected, cannot play file %d", fileId)
            return
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(fileId.toString())
            .setUri(path)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(Bundle().apply { putLong("fileId", fileId) })
                    .build()
            )
            .build()

        b.setMediaItem(mediaItem, startPositionMs)
        b.prepare()
        b.play()

        _currentFileId.value = fileId
        Timber.i("Playing file id=%d from %dms", fileId, startPositionMs)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        browser?.repeatMode = mode.exoPlayerMode
        _repeatMode.value = mode
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        browser?.shuffleModeEnabled = enabled
        _shuffleEnabled.value = enabled
    }

    // -- A-B Loop --

    override fun setLoopRegion(region: LoopRegion) {
        val b = browser ?: return
        val args = Bundle().apply {
            putLong(RehearsAllPlaybackService.ARG_LOOP_START, region.startMs)
            putLong(RehearsAllPlaybackService.ARG_LOOP_END, region.endMs)
        }
        b.sendCustomCommand(
            SessionCommand(RehearsAllPlaybackService.CMD_SET_LOOP_REGION, Bundle.EMPTY),
            args,
        )
    }

    override fun clearLoopRegion() {
        val b = browser ?: return
        b.sendCustomCommand(
            SessionCommand(RehearsAllPlaybackService.CMD_CLEAR_LOOP_REGION, Bundle.EMPTY),
            Bundle.EMPTY,
        )
    }

    // -- Lifecycle --

    override fun release() {
        stopPolling()
        browser?.release()
        browser = null
        scope.cancel()
        Timber.i("PlaybackManager released")
    }
}
