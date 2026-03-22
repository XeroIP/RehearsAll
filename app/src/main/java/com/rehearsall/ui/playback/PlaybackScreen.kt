package com.rehearsall.ui.playback

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.draw.clip
import com.rehearsall.data.repository.WaveformState
import com.rehearsall.domain.model.ChunkMarker
import com.rehearsall.domain.model.Loop
import com.rehearsall.domain.model.OverlayMode
import com.rehearsall.playback.PracticeState
import com.rehearsall.ui.common.formatDuration
import com.rehearsall.ui.playback.components.MarkersBottomSheet
import com.rehearsall.ui.playback.components.PracticeControlsBottomSheet
import com.rehearsall.ui.playback.components.QueueBottomSheet
import com.rehearsall.ui.playback.components.SpeedControlBottomSheet
import com.rehearsall.ui.playback.components.TransportBar
import com.rehearsall.ui.playback.components.WaveformView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFile: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {},
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

    // Navigate to a different file when skip-to-next/previous changes the current track
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { fileId ->
            onNavigateToFile(fileId)
        }
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
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            onUpdateBookmarkPosition = viewModel::updateBookmarkPosition,
            onDeleteBookmark = viewModel::deleteBookmark,
            onCreateLoop = { start, end -> viewModel.createLoop(start, end) },
            onClearLoop = viewModel::clearLoop,
            onSaveLoop = viewModel::saveLoop,
            onLoadLoop = viewModel::loadLoop,
            onDeleteLoop = viewModel::deleteLoop,
            onUpdateLoopBounds = viewModel::updateLoopBounds,
            onAdjustLoopBoundary = { isStart, ms -> viewModel.adjustLoopBoundary(isStart, ms) },
            chunkMarkers = uiState.chunkMarkers,
            onSeekToChunk = viewModel::seekToChunk,
            onAddChunkMarker = viewModel::addChunkMarker,
            onUpdateChunkMarkerPosition = viewModel::updateChunkMarkerPosition,
            onDeleteChunkMarker = viewModel::deleteChunkMarker,
            onStartPractice = {
                viewModel.dismissMarkersSheet()
                viewModel.togglePracticeSheet()
            },
            onTogglePlayPause = viewModel::togglePlayPause,
            onDismiss = viewModel::dismissMarkersSheet,
            durationMs = uiState.playbackState.durationMs,
            waveformState = uiState.waveformState,
            positionMs = uiState.playbackState.positionMs,
            isPlaying = uiState.playbackState.isPlaying,
            practiceState = uiState.practiceState,
            onSeek = { viewModel.seekTo(it) },
            onLoopBoundaryDrag = { isStart, ms -> viewModel.adjustLoopBoundary(isStart, ms) },
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
        // Track info + overlay area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Default: track info centered
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }

            // Overlay: saved loops or chunks list
            when (uiState.overlayMode) {
                OverlayMode.LOOPS -> if (uiState.savedLoops.isNotEmpty()) {
                    OverlayList(
                        title = "Saved Loops",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(0.5f)
                            .align(Alignment.TopStart),
                    ) {
                        items(uiState.savedLoops, key = { it.id }) { loop ->
                            OverlayLoopRow(
                                loop = loop,
                                onTap = {
                                    viewModel.loadLoop(loop)
                                },
                            )
                        }
                    }
                }
                OverlayMode.CHUNKS -> if (uiState.chunkMarkers.isNotEmpty()) {
                    OverlayList(
                        title = "Chunk Markers",
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(0.5f)
                            .align(Alignment.TopStart),
                    ) {
                        items(uiState.chunkMarkers, key = { it.id }) { marker ->
                            OverlayChunkRow(
                                marker = marker,
                                durationMs = uiState.playbackState.durationMs,
                                onTap = { viewModel.seekToChunk(marker.positionMs) },
                            )
                        }
                    }
                }
                OverlayMode.NONE -> { /* show track info only */ }
            }
        }

        val playback = uiState.playbackState
        val duration = playback.durationMs.coerceAtLeast(1L)
        val waveform = uiState.waveformState
        val overlayMode = uiState.overlayMode

        if (waveform is WaveformState.Ready && duration > 1L) {
            val positionFraction = playback.positionMs.toFloat() / duration.toFloat()

            val loopStartFrac = if (overlayMode == OverlayMode.LOOPS) {
                uiState.activeLoop?.let {
                    if (it.endMs > it.startMs) it.startMs.toFloat() / duration else null
                }
            } else null
            val loopEndFrac = if (overlayMode == OverlayMode.LOOPS) {
                uiState.activeLoop?.let {
                    if (it.endMs > it.startMs) it.endMs.toFloat() / duration else null
                }
            } else null

            val chunkFractions = if (overlayMode == OverlayMode.CHUNKS) {
                uiState.chunkMarkers.map { it.positionMs.toFloat() / duration }
            } else emptyList()
            val practiceStep = (uiState.practiceState as? PracticeState.Playing)?.currentStep
            val activeChunkStart = if (overlayMode == OverlayMode.CHUNKS) {
                practiceStep?.let { it.startMs.toFloat() / duration }
            } else null
            val activeChunkEnd = if (overlayMode == OverlayMode.CHUNKS) {
                practiceStep?.let { it.endMs.toFloat() / duration }
            } else null

            var wfZoom by remember { mutableFloatStateOf(1f) }
            var wfScroll by remember { mutableFloatStateOf(0f) }

            WaveformView(
                amplitudes = waveform.amplitudes,
                positionFraction = positionFraction,
                isPlaying = playback.isPlaying,
                onSeek = { fraction -> viewModel.seekTo((fraction * duration).toLong()) },
                modifier = Modifier.fillMaxWidth(),
                height = 200.dp,
                zoom = wfZoom,
                scrollOffset = wfScroll,
                onZoomChange = { wfZoom = it },
                onScrollOffsetChange = { wfScroll = it },
                loopStartFraction = loopStartFrac,
                loopEndFraction = loopEndFrac,
                editable = false,
                showPositionHandle = true,
                chunkMarkerFractions = chunkFractions,
                activeChunkStartFraction = activeChunkStart,
                activeChunkEndFraction = activeChunkEnd,
            )

            // Zoom controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row {
                    IconButton(
                        onClick = {
                            val newZ = (wfZoom * 0.67f).coerceAtLeast(1f)
                            val center = wfScroll + (1f / wfZoom) / 2f
                            val newVW = 1f / newZ
                            wfZoom = newZ
                            wfScroll = (center - newVW / 2f).coerceIn(0f, (1f - newVW).coerceAtLeast(0f))
                        },
                        enabled = wfZoom > 1.01f,
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom out")
                    }
                    IconButton(
                        onClick = {
                            val newZ = (wfZoom * 1.5f).coerceAtMost(50f)
                            val center = wfScroll + (1f / wfZoom) / 2f
                            val newVW = 1f / newZ
                            wfZoom = newZ
                            wfScroll = (center - newVW / 2f).coerceIn(0f, (1f - newVW).coerceAtLeast(0f))
                        },
                        enabled = wfZoom < 49f,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom in")
                    }
                }
            }

            Slider(
                value = playback.positionMs.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Slider(
                value = playback.positionMs.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
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

        // Speed, markers, and queue buttons (icon-only)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            BadgedBox(
                badge = { Badge { Text("%.1fx".format(playback.speed)) } },
            ) {
                FilledTonalIconButton(onClick = viewModel::toggleSpeedSheet) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed: %.2fx".format(playback.speed),
                    )
                }
            }

            FilledTonalIconButton(onClick = viewModel::toggleMarkersSheet) {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = "Markers",
                )
            }

            FilledTonalIconButton(onClick = viewModel::toggleQueueSheet) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun OverlayList(
    title: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .padding(start = 4.dp, top = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f))
            .padding(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            content = content,
        )
    }
}

@Composable
private fun OverlayLoopRow(
    loop: Loop,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onTap)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = loop.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = "${formatDuration(loop.startMs)} – ${formatDuration(loop.endMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false,
        )
    }
}

@Composable
private fun OverlayChunkRow(
    marker: ChunkMarker,
    durationMs: Long,
    onTap: () -> Unit,
) {
    Text(
        text = formatDuration(marker.positionMs),
        style = MaterialTheme.typography.bodySmall,
        softWrap = false,
        modifier = Modifier
            .clickable(onClick = onTap)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    )
}
