package com.rehearsall.ui.playback.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rehearsall.domain.model.ChunkMarker
import com.rehearsall.ui.common.formatDuration

@Composable
fun ChunkTabContent(
    markers: List<ChunkMarker>,
    onSeekTo: (Long) -> Unit,
    onAddMarker: () -> Unit,
    onUpdatePosition: (Long, Long) -> Unit,
    onDeleteMarker: (Long) -> Unit,
    onStartPractice: () -> Unit,
    durationMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    var timeEditTarget by remember { mutableStateOf<ChunkMarker?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (markers.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No chunk markers yet.\nTap + to add one at the current position.\nMarkers divide the track into chunks for practice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(markers, key = { _, m -> m.id }) { index, marker ->
                        ChunkMarkerRow(
                            index = index + 1,
                            marker = marker,
                            onTap = { onSeekTo(marker.positionMs) },
                            onEditTime = { timeEditTarget = marker },
                            onDelete = { onDeleteMarker(marker.id) },
                        )
                    }
                }

                HorizontalDivider()

                // Start Practice button
                FilledTonalButton(
                    onClick = onStartPractice,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Start Practice")
                }
            }
        }

        FloatingActionButton(
            onClick = onAddMarker,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = if (markers.isNotEmpty()) 56.dp else 0.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add chunk marker")
        }
    }

    timeEditTarget?.let { marker ->
        TimeEditDialog(
            title = "Edit Marker Time",
            currentMs = marker.positionMs,
            durationMs = durationMs,
            onConfirm = { newMs ->
                onUpdatePosition(marker.id, newMs)
                timeEditTarget = null
            },
            onDismiss = { timeEditTarget = null },
        )
    }
}

@Composable
private fun ChunkMarkerRow(
    index: Int,
    marker: ChunkMarker,
    onTap: () -> Unit,
    onEditTime: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = marker.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDuration(marker.positionMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEditTime) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = "Edit time",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
