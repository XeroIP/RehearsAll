package com.rehearsall.ui.playlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rehearsall.domain.model.PlaylistItem
import com.rehearsall.ui.common.formatDuration
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlaylistEvent.ItemRemoved -> {
                    snackbarHostState.showSnackbar("Removed \"${event.displayName}\"")
                }
                is PlaylistEvent.PlaylistDeleted -> {
                    onNavigateBack()
                }
                is PlaylistEvent.PlaylistRenamed -> {
                    snackbarHostState.showSnackbar("Renamed to \"${event.newName}\"")
                }
            }
        }
    }

    val playlistName = (uiState as? PlaylistUiState.Loaded)?.playlistName ?: "Playlist"

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showOverflowMenu = false
                                    showRenameDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete playlist") },
                                onClick = {
                                    showOverflowMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            val state = uiState
            if (state is PlaylistUiState.Loaded && state.items.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.playPlaylist() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play playlist")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        when (val state = uiState) {
            is PlaylistUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PlaylistUiState.Loaded -> {
                if (state.items.isEmpty()) {
                    PlaylistEmptyState(modifier = Modifier.padding(innerPadding))
                } else {
                    PlaylistItemList(
                        items = state.items,
                        onItemClick = { index -> viewModel.playPlaylist(index) },
                        onRemove = { itemId -> viewModel.removeItem(itemId) },
                        onReorder = { from, to -> viewModel.reorderItems(from, to) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }

            is PlaylistUiState.Error -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        RenamePlaylistDialog(
            currentName = playlistName,
            onConfirm = { newName ->
                showRenameDialog = false
                viewModel.renamePlaylist(newName)
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete playlist?") },
            text = { Text("\"$playlistName\" will be permanently deleted. Your audio files will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePlaylist()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PlaylistEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Text(
                text = "No tracks in this playlist",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Long-press a file to add it to a playlist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistItemList(
    items: List<PlaylistItem>,
    onItemClick: (Int) -> Unit,
    onRemove: (Long) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local list tracks drag-in-progress order; resets when DB-backed items change.
    var localItems by remember(items) { mutableStateOf(items) }

    val lazyListState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            localItems =
                localItems.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            onReorder(from.index, to.index)
        }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(
            items = localItems,
            key = { _, item -> item.id },
        ) { index, item ->
            ReorderableItem(reorderState, key = item.id) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 6.dp else 0.dp,
                    label = "drag-elevation",
                )

                val dismissState =
                    rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onRemove(item.id)
                                true
                            } else {
                                false
                            }
                        },
                    )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color by animateColorAsState(
                            targetValue =
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            label = "swipe-bg",
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    },
                    enableDismissFromStartToEnd = false,
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                    ) {
                        PlaylistTrackRow(
                            index = index + 1,
                            item = item,
                            onClick = { onItemClick(index) },
                            dragHandleModifier = Modifier.draggableHandle(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    index: Int,
    item: PlaylistItem,
    onClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.artist?.let { artist ->
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = formatDuration(item.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = dragHandleModifier,
        )
    }
}

@Composable
private fun RenamePlaylistDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
