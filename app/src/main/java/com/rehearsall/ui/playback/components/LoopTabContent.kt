package com.rehearsall.ui.playback.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rehearsall.domain.model.Loop
import com.rehearsall.playback.LoopRegion
import com.rehearsall.ui.common.SingleFieldInputDialog
import com.rehearsall.ui.common.formatDuration

@Composable
fun LoopTabContent(
    activeLoop: LoopRegion?,
    savedLoops: List<Loop>,
    onCreateLoop: (startMs: Long, endMs: Long) -> Unit,
    onClearLoop: () -> Unit,
    onSaveLoop: (String) -> Unit,
    onLoadLoop: (Loop) -> Unit,
    onDeleteLoop: (Long) -> Unit,
    onUpdateLoopBounds: (Long) -> Unit = {},
    onAdjustBoundary: (isStart: Boolean, newMs: Long) -> Unit = { _, _ -> },
    onSeekTo: (Long) -> Unit = {},
    durationMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var editingBoundary by remember { mutableStateOf<Boolean?>(null) }
    // Track which saved loop is being edited (null = new/unsaved loop)
    var editingLoop by remember { mutableStateOf<Loop?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
        ) {
            // Active loop editor — distinct card with close button
            if (activeLoop != null && activeLoop.endMs > activeLoop.startMs) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                    border =
                        CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                        ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Header row with title and close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = editingLoop?.name ?: "New Loop",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(
                                onClick = {
                                    onClearLoop()
                                    editingLoop = null
                                },
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close editor",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Begin / End with editable times and +/- buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Begin column
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "Begin",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { editingBoundary = true }) {
                                    Text(
                                        text = formatDuration(activeLoop.startMs),
                                        style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledIconButton(
                                        onClick = {
                                            val newMs = (activeLoop.startMs - 250).coerceAtLeast(0)
                                            onAdjustBoundary(true, newMs)
                                            onSeekTo(newMs)
                                        },
                                        colors =
                                            IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            ),
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "−0.25s")
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            val newMs = (activeLoop.startMs + 250).coerceAtMost(activeLoop.endMs - 100)
                                            onAdjustBoundary(true, newMs)
                                            onSeekTo(newMs)
                                        },
                                        colors =
                                            IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            ),
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "+0.25s")
                                    }
                                }
                            }

                            // End column
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "End",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { editingBoundary = false }) {
                                    Text(
                                        text = formatDuration(activeLoop.endMs),
                                        style =
                                            MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                textDecoration = TextDecoration.Underline,
                                            ),
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledIconButton(
                                        onClick = {
                                            onAdjustBoundary(false, (activeLoop.endMs - 250).coerceAtLeast(activeLoop.startMs + 100))
                                        },
                                        colors =
                                            IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            ),
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "−0.25s")
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            onAdjustBoundary(false, (activeLoop.endMs + 250).coerceAtMost(durationMs))
                                        },
                                        colors =
                                            IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            ),
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "+0.25s")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Duration: ${formatDuration(activeLoop.endMs - activeLoop.startMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Save action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (editingLoop != null) {
                                        showOverwriteDialog = true
                                    } else {
                                        showSaveDialog = true
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text(if (editingLoop != null) "Save" else "Save As…")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Saved Loops",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (savedLoops.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No saved loops yet.\nTap + to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(savedLoops, key = { it.id }) { loop ->
                        SavedLoopRow(
                            loop = loop,
                            onTap = {
                                editingLoop = loop
                                onLoadLoop(loop)
                            },
                            onDelete = { onDeleteLoop(loop.id) },
                        )
                    }
                }
            }
        }

        // FAB to create a new loop — defaults to full track (0 to durationMs)
        FloatingActionButton(
            onClick = {
                editingLoop = null
                onCreateLoop(0L, durationMs)
            },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create loop")
        }
    }

    // Save as new loop dialog
    if (showSaveDialog) {
        SingleFieldInputDialog(
            title = "Save loop",
            label = "Loop name",
            confirmLabel = "Save",
            onConfirm = { name ->
                onSaveLoop(name)
                onClearLoop()
                editingLoop = null
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    // Overwrite existing loop confirmation
    if (showOverwriteDialog) {
        val loop = editingLoop
        if (loop != null) {
            AlertDialog(
                onDismissRequest = { showOverwriteDialog = false },
                title = { Text("Overwrite loop?") },
                text = {
                    Text("Update \"${loop.name}\" with the current boundaries?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onUpdateLoopBounds(loop.id)
                            onClearLoop()
                            editingLoop = null
                            showOverwriteDialog = false
                        },
                    ) { Text("Overwrite") }
                },
                dismissButton = {
                    TextButton(onClick = { showOverwriteDialog = false }) { Text("Cancel") }
                },
            )
        }
    }

    editingBoundary?.let { isStart ->
        val currentMs = if (isStart) activeLoop?.startMs ?: 0L else activeLoop?.endMs ?: 0L
        TimeEditDialog(
            title = if (isStart) "Edit Begin Time" else "Edit End Time",
            currentMs = currentMs,
            durationMs = durationMs,
            onConfirm = { newMs ->
                onAdjustBoundary(isStart, newMs)
                if (isStart) onSeekTo(newMs)
                editingBoundary = null
            },
            onDismiss = { editingBoundary = null },
        )
    }
}

@Composable
private fun SavedLoopRow(
    loop: Loop,
    onTap: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(horizontal = 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = loop.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatDuration(loop.startMs)} – ${formatDuration(loop.endMs)}  (${formatDuration(loop.durationMs)})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

