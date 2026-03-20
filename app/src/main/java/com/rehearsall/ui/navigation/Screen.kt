package com.rehearsall.ui.navigation

/**
 * Navigation route definitions for the app.
 */
sealed class Screen(val route: String) {
    data object FileList : Screen("file_list")
    data object Playback : Screen("playback/{audioFileId}") {
        fun createRoute(audioFileId: Long) = "playback/$audioFileId"
    }
    data object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    data object Settings : Screen("settings")
}
