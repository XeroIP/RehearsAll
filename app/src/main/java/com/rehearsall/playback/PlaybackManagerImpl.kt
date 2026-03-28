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
import com.rehearsall.domain.model.QueueItem
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
class PlaybackManagerImpl
    @Inject
    constructor(
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

        private val _currentQueue = MutableStateFlow<List<QueueItem>>(emptyList())
        override val currentQueue: StateFlow<List<QueueItem>> = _currentQueue.asStateFlow()

        private val _loopRegion = MutableStateFlow<LoopRegion?>(null)
        override val loopRegion: StateFlow<LoopRegion?> = _loopRegion.asStateFlow()

        // Local queue metadata — kept in sync with ExoPlayer's media items
        private val queueItems = mutableListOf<QueueItem>()

        init {
            connectToService()
        }

        private fun connectToService() {
            if (isConnecting || browser != null) return
            isConnecting = true

            scope.launch {
                try {
                    val token =
                        SessionToken(
                            context,
                            ComponentName(context, RehearsAllPlaybackService::class.java),
                        )
                    val mediaBrowser =
                        MediaBrowser.Builder(context, token)
                            .buildAsync()
                            .await()

                    browser = mediaBrowser
                    isConnecting = false

                    // Sync initial state
                    syncState(mediaBrowser)

                    // Listen for player state changes
                    mediaBrowser.addListener(
                        object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                syncState(mediaBrowser)
                                if (isPlaying) startPolling() else stopPolling()
                            }

                            override fun onPlaybackStateChanged(state: Int) {
                                syncState(mediaBrowser)
                            }

                            override fun onMediaItemTransition(
                                mediaItem: MediaItem?,
                                reason: Int,
                            ) {
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
                        },
                    )

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
            pollingJob =
                scope.launch {
                    while (true) {
                        val b = browser ?: break
                        updatePlaybackState(b)
                        delay(32) // ~30fps — sufficient for smooth scrubber
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

            // Update queue with now-playing indicator
            updateQueueState(fileId)
        }

        private fun updateQueueState(currentFileId: Long?) {
            val currentIndex = browser?.currentMediaItemIndex ?: -1
            _currentQueue.value =
                queueItems.mapIndexed { index, item ->
                    item.copy(isCurrentlyPlaying = index == currentIndex)
                }
        }

        private fun updatePlaybackState(player: MediaBrowser) {
            val newState =
                PlaybackState(
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L),
                    isPlaying = player.isPlaying,
                    speed = player.playbackParameters.speed,
                )
            // Only emit when state has meaningfully changed to avoid unnecessary recompositions
            val current = _playbackState.value
            if (newState.isPlaying != current.isPlaying ||
                newState.durationMs != current.durationMs ||
                newState.speed != current.speed ||
                kotlin.math.abs(newState.positionMs - current.positionMs) >= 16
            ) {
                _playbackState.value = newState
            }
        }

        // -- Transport --

        override fun play() {
            browser?.play()
        }

        override fun pause() {
            browser?.pause()
        }

        override fun togglePlayPause() {
            val b = browser ?: return
            if (b.isPlaying) b.pause() else b.play()
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
            val b = browser ?: return
            // If more than 3 s into the track, or no previous item exists, restart from the beginning.
            // Otherwise, skip to the previous track in the queue.
            if (b.currentPosition > 3_000L || !b.hasPreviousMediaItem()) {
                b.seekTo(0L)
                _playbackState.value = _playbackState.value.copy(positionMs = 0L)
            } else {
                b.seekToPreviousMediaItem()
            }
        }

        // -- Speed --

        override fun setSpeed(speed: Float) {
            val b = browser ?: return
            val args =
                Bundle().apply {
                    putFloat(RehearsAllPlaybackService.ARG_SPEED, speed)
                }
            b.sendCustomCommand(
                SessionCommand(RehearsAllPlaybackService.CMD_SET_SPEED, Bundle.EMPTY),
                args,
            )
            // Optimistic UI update
            _playbackState.value =
                _playbackState.value.copy(
                    speed = speed.roundToSpeedStep(),
                )
        }

        // -- Queue --

        override fun playFile(
            fileId: Long,
            path: String,
            startPositionMs: Long,
        ) {
            val b =
                browser ?: run {
                    Timber.w("Browser not connected, cannot play file %d", fileId)
                    return
                }

            val mediaItem = buildMediaItem(fileId, path)

            b.setMediaItem(mediaItem, startPositionMs)
            b.prepare()
            b.play()

            queueItems.clear()
            queueItems.add(QueueItem(fileId, "", null, 0L, path))
            _currentFileId.value = fileId
            updateQueueState(fileId)
            Timber.i("Playing file id=%d from %dms", fileId, startPositionMs)
        }

        override fun setQueue(
            items: List<QueueItem>,
            startIndex: Int,
        ) {
            val b = browser ?: return
            val mediaItems = items.map { buildMediaItem(it.fileId, it.path) }

            b.setMediaItems(mediaItems, startIndex, 0L)
            b.prepare()
            b.play()

            queueItems.clear()
            queueItems.addAll(items)
            updateQueueState(items.getOrNull(startIndex)?.fileId)
            Timber.i("Queue set: %d items, starting at index %d", items.size, startIndex)
        }

        override fun skipToQueueItem(index: Int) {
            val b = browser ?: return
            if (index in 0 until b.mediaItemCount) {
                b.seekToDefaultPosition(index)
            }
        }

        override fun removeFromQueue(index: Int) {
            val b = browser ?: return
            if (index in 0 until b.mediaItemCount) {
                b.removeMediaItem(index)
                if (index < queueItems.size) {
                    queueItems.removeAt(index)
                }
                updateQueueState(_currentFileId.value)
            }
        }

        override fun moveQueueItem(
            fromIndex: Int,
            toIndex: Int,
        ) {
            val b = browser ?: return
            if (fromIndex in 0 until b.mediaItemCount && toIndex in 0 until b.mediaItemCount) {
                b.moveMediaItem(fromIndex, toIndex)
                if (fromIndex < queueItems.size && toIndex <= queueItems.size) {
                    val item = queueItems.removeAt(fromIndex)
                    queueItems.add(toIndex.coerceAtMost(queueItems.size), item)
                }
                updateQueueState(_currentFileId.value)
            }
        }

        override fun clearQueue() {
            val b = browser ?: return
            b.clearMediaItems()
            queueItems.clear()
            _currentQueue.value = emptyList()
            _currentFileId.value = null
        }

        private fun buildMediaItem(
            fileId: Long,
            path: String,
        ): MediaItem {
            return MediaItem.Builder()
                .setMediaId(fileId.toString())
                .setUri(path)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setExtras(Bundle().apply { putLong("fileId", fileId) })
                        .build(),
                )
                .build()
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
            val args =
                Bundle().apply {
                    putLong(RehearsAllPlaybackService.ARG_LOOP_START, region.startMs)
                    putLong(RehearsAllPlaybackService.ARG_LOOP_END, region.endMs)
                }
            b.sendCustomCommand(
                SessionCommand(RehearsAllPlaybackService.CMD_SET_LOOP_REGION, Bundle.EMPTY),
                args,
            )
            _loopRegion.value = region
        }

        override fun clearLoopRegion() {
            val b = browser ?: return
            b.sendCustomCommand(
                SessionCommand(RehearsAllPlaybackService.CMD_CLEAR_LOOP_REGION, Bundle.EMPTY),
                Bundle.EMPTY,
            )
            _loopRegion.value = null
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
