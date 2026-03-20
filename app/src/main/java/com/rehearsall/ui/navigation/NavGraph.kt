package com.rehearsall.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rehearsall.ui.filelist.FileListScreen
import com.rehearsall.ui.playback.PlaybackScreen
import com.rehearsall.ui.playlist.PlaylistScreen
import com.rehearsall.ui.settings.SettingsScreen

@Composable
fun RehearsAllNavGraph(
    windowSizeClass: WindowSizeClass,
) {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.FileList.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Screen.FileList.route) {
                FileListScreen(
                    onFileClick = { audioFileId ->
                        navController.navigate(Screen.Playback.createRoute(audioFileId))
                    },
                    onPlaylistClick = { playlistId ->
                        navController.navigate(Screen.Playlist.createRoute(playlistId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                )
            }

            composable(
                route = Screen.Playback.route,
                arguments = listOf(
                    navArgument("audioFileId") { type = NavType.LongType }
                ),
            ) {
                PlaybackScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.Playlist.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.LongType }
                ),
            ) {
                PlaylistScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
