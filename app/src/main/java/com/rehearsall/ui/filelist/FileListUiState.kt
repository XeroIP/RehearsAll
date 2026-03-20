package com.rehearsall.ui.filelist

import com.rehearsall.domain.model.AudioFile

sealed interface FileListUiState {
    data object Loading : FileListUiState
    data class Loaded(val files: List<AudioFile>) : FileListUiState
    data class Error(val message: String) : FileListUiState
}

sealed interface FileListEvent {
    data class ImportSuccess(val displayName: String) : FileListEvent
    data class ImportError(val message: String) : FileListEvent
    data class DeleteSuccess(val displayName: String) : FileListEvent
    data class RenameSuccess(val newName: String) : FileListEvent
}
