package com.rehearsall.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TopAppBarScrollBehavior
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
import com.rehearsall.ui.common.FileDetailsBottomSheet
import com.rehearsall.ui.common.PlaylistPickerDialog
import com.rehearsall.ui.common.SingleFieldInputDialog
import com.rehearsall.ui.common.SwipeToDismissBackground
import com.rehearsall.ui.common.formatDuration
import com.rehearsall.ui.filelist.FileListEvent
import com.rehearsall.ui.filelist.FileListUiState
import com.rehearsall.ui.filelist.FileListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onFileClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: FileListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var fileForPlaylistPicker by remember { mutableStateOf<AudioFile?>(null) }
    var fileForRename by remember { mutableStateOf<AudioFile?>(null) }
    var fileForDetails by remember { mutableStateOf<AudioFile?>(null) }
    var fileForDelete by remember { mutableStateOf<AudioFile?>(null) }

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
                title = { Text("Library") },
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
                    if (state.files.isEmpty()) {
                        EmptyStateMessage(
                            icon = Icons.Default.MusicNote,
                            title = "No audio files yet",
                            subtitle = "Tap + to import an audio file",
                        )
                    } else {
                        FileList(
                            files = state.files,
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
                            onFilePlay = { file -> onFileClick(file.id) },
                            onFileAddToPlaylist = { file -> fileForPlaylistPicker = file },
                            onFileDetails = { file -> fileForDetails = file },
                            onFileRename = { file -> fileForRename = file },
                            onFileSwipeDelete = { file -> fileForDelete = file },
                            onFileDelete = { file -> fileForDelete = file },
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

    if (showPlaylistPicker) {
        val playlists = (uiState as? FileListUiState.Loaded)?.playlists ?: emptyList()
        PlaylistPickerDialog(
            playlists = playlists,
            onSelect = { playlistId ->
                viewModel.addSelectedToPlaylist(playlistId)
                showPlaylistPicker = false
            },
            onDismiss = { showPlaylistPicker = false },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
                showPlaylistPicker = false
            },
        )
    }

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

    // Per-file playlist picker
    fileForPlaylistPicker?.let { file ->
        val playlists = (uiState as? FileListUiState.Loaded)?.playlists ?: emptyList()
        PlaylistPickerDialog(
            playlists = playlists,
            onSelect = { playlistId ->
                viewModel.addFileToPlaylist(file.id, playlistId)
                fileForPlaylistPicker = null
            },
            onDismiss = { fileForPlaylistPicker = null },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
                fileForPlaylistPicker = null
            },
        )
    }

    // Per-file rename dialog
    fileForRename?.let { file ->
        SingleFieldInputDialog(
            title = "Rename",
            initialValue = file.displayName,
            confirmLabel = "Rename",
            onConfirm = { newName ->
                viewModel.renameFile(file.id, newName)
                fileForRename = null
            },
            onDismiss = { fileForRename = null },
        )
    }

    // File details bottom sheet
    fileForDetails?.let { file ->
        val playlists = (uiState as? FileListUiState.Loaded)?.playlists ?: emptyList()
        FileDetailsBottomSheet(
            audioFile = file,
            playlists = playlists,
            onDismiss = { fileForDetails = null },
            onRename = { newName ->
                viewModel.renameFile(file.id, newName)
                fileForDetails = null
            },
            onAddToPlaylist = { playlistId ->
                viewModel.addFileToPlaylist(file.id, playlistId)
                fileForDetails = null
            },
            onDelete = {
                viewModel.deleteFile(file.id)
                fileForDetails = null
            },
            onCreatePlaylist = { name ->
                viewModel.createPlaylist(name)
            },
        )
    }

    // Delete confirmation dialog
    fileForDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileForDelete = null },
            title = { Text("Remove from library?") },
            text = { Text("\"${file.displayName}\" will be removed from the app. The original file on your device will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFile(file.id)
                    fileForDelete = null
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileForDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileList(
    files: List<AudioFile>,
    selectedFileIds: Set<Long>,
    isInSelectionMode: Boolean,
    onFileClick: (AudioFile) -> Unit,
    onFileLongClick: (AudioFile) -> Unit,
    onFilePlay: (AudioFile) -> Unit,
    onFileAddToPlaylist: (AudioFile) -> Unit,
    onFileDetails: (AudioFile) -> Unit,
    onFileRename: (AudioFile) -> Unit,
    onFileSwipeDelete: (AudioFile) -> Unit,
    onFileDelete: (AudioFile) -> Unit,
    onClearSelection: () -> Unit,
    onAddSelectedToPlaylist: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (isInSelectionMode) {
            item(key = "selection-bar") {
                SelectionBar(
                    selectedCount = selectedFileIds.size,
                    onClear = onClearSelection,
                    onAddToPlaylist = onAddSelectedToPlaylist,
                )
            }
        }

        items(
            items = files,
            key = { "file-${it.id}" },
        ) { file ->
            val dismissState =
                rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onFileSwipeDelete(file)
                            false // Don't dismiss — let the dialog handle it
                        } else {
                            false
                        }
                    },
                )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    SwipeToDismissBackground(
                        targetValue = dismissState.targetValue,
                        icon = Icons.Outlined.RemoveCircleOutline,
                        contentDescription = "Remove from library",
                    )
                },
                enableDismissFromStartToEnd = false,
            ) {
                AudioFileCard(
                    audioFile = file,
                    isSelected = file.id in selectedFileIds,
                    isInSelectionMode = isInSelectionMode,
                    onClick = { onFileClick(file) },
                    onLongClick = { onFileLongClick(file) },
                    onPlay = { onFilePlay(file) },
                    onAddToPlaylist = { onFileAddToPlaylist(file) },
                    onDetails = { onFileDetails(file) },
                    onRename = { onFileRename(file) },
                    onDelete = { onFileDelete(file) },
                )
            }
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
        IconButton(onClick = onClear) {
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
                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
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
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDetails: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

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
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
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

            Spacer(modifier = Modifier.width(8.dp))

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

            if (!isInSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play") },
                            onClick = { showMenu = false; onPlay() },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = { showMenu = false; onAddToPlaylist() },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("File Details") },
                            onClick = { showMenu = false; onDetails() },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showMenu = false; onRename() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove from Library") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.RemoveCircleOutline,
                                    null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
