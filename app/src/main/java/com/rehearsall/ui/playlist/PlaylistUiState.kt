package com.rehearsall.ui.playlist

import com.rehearsall.domain.model.PlaylistItem

sealed interface PlaylistUiState {
    data object Loading : PlaylistUiState

    data class Loaded(
        val playlistName: String,
        val items: List<PlaylistItem>,
    ) : PlaylistUiState

    data class Error(val message: String) : PlaylistUiState
}

sealed interface PlaylistEvent {
    data class ItemRemoved(val displayName: String) : PlaylistEvent

    data class PlaylistDeleted(val name: String) : PlaylistEvent

    data class PlaylistRenamed(val newName: String) : PlaylistEvent
}
