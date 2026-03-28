package com.rehearsall.ui.filelist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.rehearsall.domain.model.AudioFile
import com.rehearsall.domain.model.Playlist
import com.rehearsall.ui.common.EmptyStateMessage
import com.rehearsall.ui.common.PlaylistPickerDialog
import com.rehearsall.ui.common.SingleFieldInputDialog
import com.rehearsall.ui.common.SwipeToDismissBackground
import com.rehearsall.ui.common.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListScreen(
    onFileClick: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: FileListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedFile by remember { mutableStateOf<AudioFile?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val safLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris: List<Uri> ->
            viewModel.importFiles(uris)
        }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FileListEvent.ImportSuccess ->
                    snackbarHostState.showSnackbar("Imported \"${event.displayName}\"")
                is FileListEvent.ImportError ->
                    snackbarHostState.showSnackbar(event.message)
                is FileListEvent.DeleteSuccess ->
                    snackbarHostState.showSnackbar("Deleted \"${event.displayName}\"")
                is FileListEvent.RenameSuccess ->
                    snackbarHostState.showSnackbar("Renamed to \"${event.newName}\"")
                is FileListEvent.PlaylistCreated ->
                    snackbarHostState.showSnackbar("Created \"${event.name}\"")
                is FileListEvent.ImportBatchComplete ->
                    snackbarHostState.showSnackbar("Imported ${event.count} files")
                is FileListEvent.AddedToPlaylist ->
                    snackbarHostState.showSnackbar("Added \"${event.fileName}\" to \"${event.playlistName}\"")
                is FileListEvent.AddedBatchToPlaylist ->
                    snackbarHostState.showSnackbar("Added ${event.count} files to \"${event.playlistName}\"")
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("RehearsAll") },
                actions = {
                    val state = uiState
                    if (state is FileListUiState.Loaded && state.files.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playAll() }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play all",
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    safLauncher.launch(
                        arrayOf(
                            "audio/mpeg",
                            "audio/wav",
                            "audio/x-wav",
                            "audio/ogg",
                            "audio/flac",
                            "audio/mp4",
                            "audio/x-m4a",
                            "audio/aac",
                        ),
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import audio file")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (val state = uiState) {
                is FileListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is FileListUiState.Loaded -> {
                    if (state.files.isEmpty() && state.playlists.isEmpty()) {
                        EmptyStateMessage(
                            icon = Icons.Default.MusicNote,
                            title = "No audio files yet",
                            subtitle = "Tap + to import an audio file",
                        )
                    } else {
                        CombinedList(
                            files = state.files,
                            playlists = state.playlists,
                            selectedFileIds = state.selectedFileIds,
                            isInSelectionMode = state.isInSelectionMode,
                            onFileClick = { file ->
                                if (state.isInSelectionMode) {
                                    viewModel.toggleFileSelection(file.id)
                                } else {
                                    onFileClick(file.id)
                                }
                            },
                            onFileLongClick = { file ->
                                viewModel.toggleFileSelection(file.id)
                            },
                            onFileDelete = { id -> viewModel.deleteFile(id) },
                            onPlaylistClick = onPlaylistClick,
                            onNewPlaylist = { showNewPlaylistDialog = true },
                            onClearSelection = viewModel::clearSelection,
                            onAddSelectedToPlaylist = { showPlaylistPicker = true },
                        )
                    }
                }

                is FileListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
    }

    // Playlist picker for multi-select add
    if (showPlaylistPicker) {
        val playlists = (uiState as? FileListUiState.Loaded)?.playlists ?: emptyList()
        PlaylistPickerDialog(
            playlists = playlists,
            onSelect = { playlistId ->
                viewModel.addSelectedToPlaylist(playlistId)
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false },
        )
    }

    // New playlist dialog
    if (showNewPlaylistDialog) {
        SingleFieldInputDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                showNewPlaylistDialog = false
                viewModel.createPlaylist(name)
            },
            onDismiss = { showNewPlaylistDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombinedList(
    files: List<AudioFile>,
    playlists: List<Playlist>,
    selectedFileIds: Set<Long>,
    isInSelectionMode: Boolean,
    onFileClick: (AudioFile) -> Unit,
    onFileLongClick: (AudioFile) -> Unit,
    onFileDelete: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onNewPlaylist: () -> Unit,
    onClearSelection: () -> Unit,
    onAddSelectedToPlaylist: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Selection mode bar
        if (isInSelectionMode) {
            item(key = "selection-bar") {
                SelectionBar(
                    selectedCount = selectedFileIds.size,
                    onClear = onClearSelection,
                    onAddToPlaylist = onAddSelectedToPlaylist,
                )
            }
        }

        // Playlists section — always shown so "New Playlist" button is accessible
        item(key = "playlists-header") {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Playlists",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onNewPlaylist) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist")
                }
            }
        }

        items(
            items = playlists,
            key = { "playlist-${it.id}" },
        ) { playlist ->
            PlaylistCard(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) },
            )
        }

        if (files.isNotEmpty()) {
            item(key = "files-header") {
                Text(
                    text = "All Files",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        // Files section
        items(
            items = files,
            key = { "file-${it.id}" },
        ) { file ->
            val dismissState =
                rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onFileDelete(file.id)
                            true
                        } else {
                            false
                        }
                    },
                )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    SwipeToDismissBackground(targetValue = dismissState.targetValue)
                },
                enableDismissFromStartToEnd = false,
            ) {
                AudioFileCard(
                    audioFile = file,
                    isSelected = file.id in selectedFileIds,
                    onClick = { onFileClick(file) },
                    onLongClick = { onFileLongClick(file) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.trackCount} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = formatDuration(playlist.totalDurationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectionBar(
    selectedCount: Int,
    onClear: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClear,
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel selection",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onAddToPlaylist,
            enabled = selectedCount > 0,
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistAdd,
                contentDescription = "Add selected to playlist",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioFileCard(
    audioFile: AudioFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audioFile.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                audioFile.artist?.let { artist ->
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(audioFile.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = audioFile.format.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

