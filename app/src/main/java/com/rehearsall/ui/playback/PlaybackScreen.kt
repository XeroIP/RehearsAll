package com.rehearsall.ui.playback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import com.rehearsall.data.repository.WaveformState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rehearsall.ui.common.formatDuration
import com.rehearsall.ui.playback.components.MarkersBottomSheet
import com.rehearsall.ui.playback.components.QueueBottomSheet
import com.rehearsall.ui.playback.components.WaveformOverviewBar
import com.rehearsall.ui.playback.components.WaveformView
import com.rehearsall.ui.playback.components.SpeedControlBottomSheet
import com.rehearsall.ui.playback.components.TransportBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlaybackViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Track info
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = uiState.fileName,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                uiState.artist?.let { artist ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Waveform or fallback slider
                val playback = uiState.playbackState
                val duration = playback.durationMs.coerceAtLeast(1L)
                val positionFraction = if (duration > 0) {
                    playback.positionMs.toFloat() / duration.toFloat()
                } else 0f

                when (val waveform = uiState.waveformState) {
                    is WaveformState.Ready -> {
                        val loopStartFrac = uiState.activeLoop?.let {
                            if (it.endMs > it.startMs) it.startMs.toFloat() / duration else null
                        }
                        val loopEndFrac = uiState.activeLoop?.let {
                            if (it.endMs > it.startMs) it.endMs.toFloat() / duration else null
                        }

                        WaveformView(
                            amplitudes = waveform.amplitudes,
                            positionFraction = positionFraction,
                            isPlaying = playback.isPlaying,
                            onSeek = { fraction ->
                                viewModel.seekTo((fraction * duration).toLong())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            loopStartFraction = loopStartFrac,
                            loopEndFraction = loopEndFrac,
                            onLoopBoundaryDrag = { isStart, fraction ->
                                viewModel.adjustLoopBoundary(isStart, (fraction * duration).toLong())
                            },
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        WaveformOverviewBar(
                            amplitudes = waveform.amplitudes,
                            positionFraction = positionFraction,
                            viewportStart = 0f, // TODO: wire actual viewport from WaveformView
                            viewportEnd = 1f,
                        )
                    }
                    else -> {
                        // Loading or error — show simple slider
                        Slider(
                            value = playback.positionMs.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Time labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(playback.positionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(playback.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transport controls
                TransportBar(
                    isPlaying = playback.isPlaying,
                    repeatMode = uiState.repeatMode,
                    shuffleEnabled = uiState.shuffleEnabled,
                    onPlayPause = viewModel::togglePlayPause,
                    onSkipForward = viewModel::skipForward,
                    onSkipBackward = viewModel::skipBackward,
                    onSkipToNext = viewModel::skipToNext,
                    onSkipToPrevious = viewModel::skipToPrevious,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onToggleShuffle = viewModel::toggleShuffle,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Speed, markers, and queue badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    FilledTonalButton(onClick = viewModel::toggleSpeedSheet) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("%.2fx".format(playback.speed))
                    }

                    FilledTonalButton(onClick = viewModel::toggleMarkersSheet) {
                        Icon(
                            imageVector = Icons.Default.Bookmarks,
                            contentDescription = "Markers",
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Markers")
                    }

                    FilledTonalButton(onClick = viewModel::toggleQueueSheet) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text("Queue")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Speed control bottom sheet
    if (uiState.showSpeedSheet) {
        SpeedControlBottomSheet(
            currentSpeed = uiState.playbackState.speed,
            onSpeedChange = viewModel::setSpeed,
            onDismiss = viewModel::dismissSpeedSheet,
        )
    }

    // Markers bottom sheet
    if (uiState.showMarkersSheet) {
        MarkersBottomSheet(
            bookmarks = uiState.bookmarks,
            activeLoop = uiState.activeLoop,
            savedLoops = uiState.savedLoops,
            onSeekToBookmark = viewModel::seekToBookmark,
            onAddBookmark = viewModel::addBookmark,
            onRenameBookmark = viewModel::renameBookmark,
            onDeleteBookmark = viewModel::deleteBookmark,
            onSetA = viewModel::setLoopA,
            onSetB = viewModel::setLoopB,
            onClearLoop = viewModel::clearLoop,
            onSaveLoop = viewModel::saveLoop,
            onLoadLoop = viewModel::loadLoop,
            onDeleteLoop = viewModel::deleteLoop,
            onDismiss = viewModel::dismissMarkersSheet,
        )
    }

    // Queue bottom sheet
    if (uiState.showQueueSheet) {
        QueueBottomSheet(
            queue = uiState.queue,
            onSkipTo = viewModel::skipToQueueItem,
            onRemove = viewModel::removeFromQueue,
            onClearQueue = viewModel::clearQueue,
            onDismiss = viewModel::dismissQueueSheet,
        )
    }
}
