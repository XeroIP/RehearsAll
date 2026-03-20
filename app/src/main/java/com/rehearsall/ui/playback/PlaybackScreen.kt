package com.rehearsall.ui.playback

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import com.rehearsall.data.repository.WaveformState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rehearsall.playback.PracticeState
import com.rehearsall.ui.common.formatDuration
import com.rehearsall.ui.playback.components.MarkersBottomSheet
import com.rehearsall.ui.playback.components.PracticeControlsBottomSheet
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Predictive back: confirm if practice session is active
    val isPracticing = uiState.practiceState is PracticeState.Playing
            || uiState.practiceState is PracticeState.Pausing
    BackHandler(enabled = isPracticing) {
        showExitConfirmation = true
    }

    // Show error messages via snackbar
    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        if (uiState.fileNotFound) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Remove",
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.removeDeletedFile()
                onNavigateBack()
            }
        } else {
            snackbarHostState.showSnackbar(message)
        }
        viewModel.clearError()
    }

    // Exit confirmation dialog
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Leave practice?") },
            text = { Text("A practice session is in progress. Leaving will stop it.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    viewModel.stopPractice()
                    onNavigateBack()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text("Stay")
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = {
                        if (isPracticing) {
                            showExitConfirmation = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.fileNotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "File not available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                PlaybackContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp),
                )
            }
        }
    }

    // Bottom sheets
    if (uiState.showSpeedSheet) {
        SpeedControlBottomSheet(
            currentSpeed = uiState.playbackState.speed,
            onSpeedChange = viewModel::setSpeed,
            onDismiss = viewModel::dismissSpeedSheet,
        )
    }

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
            chunkMarkers = uiState.chunkMarkers,
            onSeekToChunk = viewModel::seekToChunk,
            onAddChunkMarker = viewModel::addChunkMarker,
            onDeleteChunkMarker = viewModel::deleteChunkMarker,
            onStartPractice = {
                viewModel.dismissMarkersSheet()
                viewModel.togglePracticeSheet()
            },
            onDismiss = viewModel::dismissMarkersSheet,
        )
    }

    if (uiState.showQueueSheet) {
        QueueBottomSheet(
            queue = uiState.queue,
            onSkipTo = viewModel::skipToQueueItem,
            onRemove = viewModel::removeFromQueue,
            onClearQueue = viewModel::clearQueue,
            onDismiss = viewModel::dismissQueueSheet,
        )
    }

    if (uiState.showPracticeSheet) {
        PracticeControlsBottomSheet(
            practiceState = uiState.practiceState,
            settings = uiState.practiceSettings,
            onModeChange = viewModel::updatePracticeMode,
            onRepeatCountChange = viewModel::updateRepeatCount,
            onGapBetweenRepsChange = viewModel::updateGapBetweenReps,
            onGapBetweenChunksChange = viewModel::updateGapBetweenChunks,
            onStart = viewModel::startPractice,
            onStop = viewModel::stopPractice,
            onSkipNext = viewModel::practiceSkipNext,
            onSkipPrevious = viewModel::practiceSkipPrevious,
            onDismiss = viewModel::dismissPracticeSheet,
        )
    }
}

@Composable
private fun PlaybackContent(
    uiState: PlaybackUiState,
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
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

                val chunkFractions = uiState.chunkMarkers.map {
                    it.positionMs.toFloat() / duration
                }
                val practiceStep = (uiState.practiceState as? PracticeState.Playing)?.currentStep
                val activeChunkStart = practiceStep?.let { it.startMs.toFloat() / duration }
                val activeChunkEnd = practiceStep?.let { it.endMs.toFloat() / duration }

                WaveformView(
                    amplitudes = waveform.amplitudes,
                    positionFraction = positionFraction,
                    isPlaying = playback.isPlaying,
                    onSeek = { fraction ->
                        viewModel.seekTo((fraction * duration).toLong())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Audio waveform. ${formatDuration(playback.positionMs)} of ${formatDuration(playback.durationMs)}"
                        },
                    loopStartFraction = loopStartFrac,
                    loopEndFraction = loopEndFrac,
                    onLoopBoundaryDrag = { isStart, fraction ->
                        viewModel.adjustLoopBoundary(isStart, (fraction * duration).toLong())
                    },
                    chunkMarkerFractions = chunkFractions,
                    activeChunkStartFraction = activeChunkStart,
                    activeChunkEndFraction = activeChunkEnd,
                )

                Spacer(modifier = Modifier.height(4.dp))

                WaveformOverviewBar(
                    amplitudes = waveform.amplitudes,
                    positionFraction = positionFraction,
                    viewportStart = 0f,
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
            skipIncrementLabel = "${uiState.skipIncrementMs / 1000} seconds",
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
