package com.rehearsall.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation route definitions for the app.
 */
sealed class Screen(val route: String) {
    // Bottom nav destinations
    data object Library : Screen("library")
    data object PlaylistList : Screen("playlist_list")
    data object Recents : Screen("recents")

    // Detail screens
    data object Playback : Screen("playback/{audioFileId}") {
        fun createRoute(audioFileId: Long) = "playback/$audioFileId"
    }

    data object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    data object Settings : Screen("settings")
}

enum class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
) {
    Library(Screen.Library, Icons.Default.LibraryMusic, "Library"),
    Playlists(Screen.PlaylistList, Icons.AutoMirrored.Filled.QueueMusic, "Playlists"),
    Recents(Screen.Recents, Icons.Default.History, "Recents"),
}
