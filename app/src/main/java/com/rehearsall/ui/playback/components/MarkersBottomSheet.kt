package com.rehearsall.ui.playback.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rehearsall.domain.model.Bookmark
import com.rehearsall.domain.model.ChunkMarker
import com.rehearsall.domain.model.Loop
import com.rehearsall.playback.LoopRegion
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
    onDeleteBookmark: (Long) -> Unit,
    onSetA: () -> Unit,
    onSetB: () -> Unit,
    onClearLoop: () -> Unit,
    onSaveLoop: (String) -> Unit,
    onLoadLoop: (Loop) -> Unit,
    onDeleteLoop: (Long) -> Unit,
    chunkMarkers: List<ChunkMarker>,
    onSeekToChunk: (Long) -> Unit,
    onAddChunkMarker: () -> Unit,
    onDeleteChunkMarker: (Long) -> Unit,
    onStartPractice: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val tabs = listOf("Bookmarks", "Loops", "Chunks")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.6f),
    ) {
        Column {
            Text(
                text = "Markers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

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
                    0 -> BookmarkTabContent(
                        bookmarks = bookmarks,
                        onSeekTo = onSeekToBookmark,
                        onAdd = onAddBookmark,
                        onRename = onRenameBookmark,
                        onDelete = onDeleteBookmark,
                    )
                    1 -> LoopTabContent(
                        activeLoop = activeLoop,
                        savedLoops = savedLoops,
                        onSetA = onSetA,
                        onSetB = onSetB,
                        onClearLoop = onClearLoop,
                        onSaveLoop = onSaveLoop,
                        onLoadLoop = onLoadLoop,
                        onDeleteLoop = onDeleteLoop,
                    )
                    2 -> ChunkTabContent(
                        markers = chunkMarkers,
                        onSeekTo = onSeekToChunk,
                        onAddMarker = onAddChunkMarker,
                        onDeleteMarker = onDeleteChunkMarker,
                        onStartPractice = onStartPractice,
                    )
                }
            }
        }
    }
}
