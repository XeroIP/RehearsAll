package com.rehearsall.ui.filelist

import com.rehearsall.domain.model.AudioFile
import com.rehearsall.domain.model.Playlist

sealed interface FileListUiState {
    data object Loading : FileListUiState

    data class Loaded(
        val files: List<AudioFile>,
        val playlists: List<Playlist> = emptyList(),
        val selectedFileIds: Set<Long> = emptySet(),
    ) : FileListUiState {
        val isInSelectionMode: Boolean get() = selectedFileIds.isNotEmpty()
    }

    data class Error(val message: String) : FileListUiState
}

sealed interface FileListEvent {
    data class ImportSuccess(val displayName: String) : FileListEvent

    data class ImportBatchComplete(val count: Int) : FileListEvent

    data class ImportError(val message: String) : FileListEvent

    data class DeleteSuccess(val displayName: String) : FileListEvent

    data class RenameSuccess(val newName: String) : FileListEvent

    data class PlaylistCreated(val name: String, val id: Long) : FileListEvent

    data class AddedToPlaylist(val fileName: String, val playlistName: String) : FileListEvent

    data class AddedBatchToPlaylist(val count: Int, val playlistName: String) : FileListEvent
}
