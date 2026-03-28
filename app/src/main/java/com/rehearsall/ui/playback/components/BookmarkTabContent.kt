package com.rehearsall.ui.playback.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FloatingActionButton
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
import com.rehearsall.domain.model.Bookmark
import com.rehearsall.ui.common.SingleFieldInputDialog
import com.rehearsall.ui.common.formatDuration

@Composable
fun BookmarkTabContent(
    bookmarks: List<Bookmark>,
    onSeekTo: (Long) -> Unit,
    onAdd: () -> Unit,
    onRename: (Long, String) -> Unit,
    onUpdatePosition: (Long, Long) -> Unit,
    onDelete: (Long) -> Unit,
    durationMs: Long = 0L,
    modifier: Modifier = Modifier,
) {
    var renameTarget by remember { mutableStateOf<Bookmark?>(null) }
    var timeEditTarget by remember { mutableStateOf<Bookmark?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (bookmarks.isEmpty()) {
            Text(
                text = "No bookmarks yet.\nTap + to add one at the current position.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(bookmarks, key = { it.id }) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        onTap = { onSeekTo(bookmark.positionMs) },
                        onRename = { renameTarget = bookmark },
                        onEditTime = { timeEditTarget = bookmark },
                        onDelete = { onDelete(bookmark.id) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add bookmark")
        }
    }

    renameTarget?.let { bookmark ->
        SingleFieldInputDialog(
            title = "Rename bookmark",
            initialValue = bookmark.name,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                onRename(bookmark.id, newName)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    timeEditTarget?.let { bookmark ->
        TimeEditDialog(
            title = "Edit Bookmark Time",
            currentMs = bookmark.positionMs,
            durationMs = durationMs,
            onConfirm = { newMs ->
                onUpdatePosition(bookmark.id, newMs)
                timeEditTarget = null
            },
            onDismiss = { timeEditTarget = null },
        )
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onTap: () -> Unit,
    onRename: () -> Unit,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatDuration(bookmark.positionMs),
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
        IconButton(onClick = onRename) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Rename",
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

