package com.rehearsall.ui.playback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rehearsall.data.repository.WaveformState
import com.rehearsall.domain.model.Bookmark
import com.rehearsall.domain.model.ChunkMarker
import com.rehearsall.domain.model.Loop
import com.rehearsall.playback.LoopRegion
import com.rehearsall.playback.PracticeState
import com.rehearsall.ui.common.formatDuration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkersBottomSheet(
    bookmarks: List<Bookmark>,
    activeLoop: LoopRegion?,
    savedLoops: List<Loop>,
    onSeekToBookmark: (Long) -> Unit,
    onAddBookmark: () -> Unit,
    onRenameBookmark: (Long, String) -> Unit,
    onUpdateBookmarkPosition: (Long, Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onCreateLoop: (startMs: Long, endMs: Long) -> Unit,
    onClearLoop: () -> Unit,
    onSaveLoop: (String) -> Unit,
    onLoadLoop: (Loop) -> Unit,
    onDeleteLoop: (Long) -> Unit,
    onUpdateLoopBounds: (Long) -> Unit,
    onAdjustLoopBoundary: (isStart: Boolean, newMs: Long) -> Unit,
    chunkMarkers: List<ChunkMarker>,
    onSeekToChunk: (Long) -> Unit,
    onAddChunkMarker: () -> Unit,
    onUpdateChunkMarkerPosition: (Long, Long) -> Unit,
    onDeleteChunkMarker: (Long) -> Unit,
    onStartPractice: () -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onDismiss: () -> Unit,
    durationMs: Long = 0L,
    // Waveform data
    waveformState: WaveformState = WaveformState.Loading,
    positionMs: Long = 0L,
    isPlaying: Boolean = false,
    practiceState: PracticeState = PracticeState.Idle,
    onSeek: (Long) -> Unit = {},
    onLoopBoundaryDrag: (isStart: Boolean, newMs: Long) -> Unit = { _, _ -> },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("Loops", "Chunks", "Bookmarks")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.75f),
    ) {
        Column {
            Text(
                text = "Markers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Waveform display
            val waveform = waveformState
            if (waveform is WaveformState.Ready && durationMs > 0) {
                val duration = durationMs.coerceAtLeast(1L)
                val positionFraction = positionMs.toFloat() / duration.toFloat()

                val loopStartFrac = activeLoop?.let {
                    if (it.endMs > it.startMs) it.startMs.toFloat() / duration else null
                }
                val loopEndFrac = activeLoop?.let {
                    if (it.endMs > it.startMs) it.endMs.toFloat() / duration else null
                }
                val chunkFractions = chunkMarkers.map {
                    it.positionMs.toFloat() / duration
                }
                val practiceStep = (practiceState as? PracticeState.Playing)?.currentStep
                val activeChunkStart = practiceStep?.let { it.startMs.toFloat() / duration }
                val activeChunkEnd = practiceStep?.let { it.endMs.toFloat() / duration }

                var wfZoom by remember { mutableFloatStateOf(1f) }
                var wfScroll by remember { mutableFloatStateOf(0f) }

                WaveformView(
                    amplitudes = waveform.amplitudes,
                    positionFraction = positionFraction,
                    isPlaying = isPlaying,
                    onSeek = { fraction ->
                        onSeek((fraction * duration).toLong())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    height = 160.dp,
                    zoom = wfZoom,
                    scrollOffset = wfScroll,
                    onZoomChange = { wfZoom = it },
                    onScrollOffsetChange = { wfScroll = it },
                    loopStartFraction = loopStartFrac,
                    loopEndFraction = loopEndFrac,
                    onLoopBoundaryDrag = { isStart, fraction ->
                        onLoopBoundaryDrag(isStart, (fraction * duration).toLong())
                    },
                    editable = true,
                    showPositionHandle = true,
                    chunkMarkerFractions = chunkFractions,
                    activeChunkStartFraction = activeChunkStart,
                    activeChunkEndFraction = activeChunkEnd,
                )

                // Play/pause and zoom controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTogglePlayPause) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                            )
                        }
                        Text(
                            text = formatDuration(positionMs),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
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

                WaveformOverviewBar(
                    amplitudes = waveform.amplitudes,
                    positionFraction = positionFraction,
                    viewportStart = wfScroll,
                    viewportEnd = (wfScroll + 1f / wfZoom).coerceAtMost(1f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onViewportDrag = { wfScroll = it },
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> LoopTabContent(
                        activeLoop = activeLoop,
                        savedLoops = savedLoops,
                        onCreateLoop = onCreateLoop,
                        onClearLoop = onClearLoop,
                        onSaveLoop = onSaveLoop,
                        onLoadLoop = onLoadLoop,
                        onDeleteLoop = onDeleteLoop,
                        onUpdateLoopBounds = onUpdateLoopBounds,
                        onAdjustBoundary = onAdjustLoopBoundary,
                        onSeekTo = onSeek,
                        durationMs = durationMs,
                    )
                    1 -> ChunkTabContent(
                        markers = chunkMarkers,
                        onSeekTo = onSeekToChunk,
                        onAddMarker = onAddChunkMarker,
                        onUpdatePosition = onUpdateChunkMarkerPosition,
                        onDeleteMarker = onDeleteChunkMarker,
                        onStartPractice = onStartPractice,
                        durationMs = durationMs,
                    )
                    2 -> BookmarkTabContent(
                        bookmarks = bookmarks,
                        onSeekTo = onSeekToBookmark,
                        onAdd = onAddBookmark,
                        onRename = onRenameBookmark,
                        onUpdatePosition = onUpdateBookmarkPosition,
                        onDelete = onDeleteBookmark,
                        durationMs = durationMs,
                    )
                }
            }
        }
    }
}
