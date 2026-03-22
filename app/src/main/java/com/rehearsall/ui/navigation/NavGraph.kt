package com.rehearsall.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rehearsall.ui.common.MiniPlayer
import com.rehearsall.ui.filelist.FileListScreen
import com.rehearsall.ui.playback.PlaybackScreen
import com.rehearsall.ui.playlist.PlaylistScreen
import com.rehearsall.ui.settings.SettingsScreen

@Composable
fun RehearsAllNavGraph(
    windowSizeClass: WindowSizeClass,
) {
    val navController = rememberNavController()

    // MiniPlayer needs playback state — use a shared ViewModel scoped to the nav graph
    val miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
    val miniPlayerState by miniPlayerViewModel.state.collectAsStateWithLifecycle()

    // Hide MiniPlayer on the Playback screen (already showing full controls)
    val currentRoute = navController.currentBackStackEntryAsState().value
        ?.destination?.route
    val showMiniPlayer = miniPlayerState.isVisible && currentRoute != Screen.Playback.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MiniPlayer(
                isVisible = showMiniPlayer,
                trackName = miniPlayerState.trackName,
                playbackState = miniPlayerState.playbackState,
                onPlayPause = miniPlayerViewModel::togglePlayPause,
                onTap = {
                    val fileId = miniPlayerState.currentFileId ?: return@MiniPlayer
                    navController.navigate(Screen.Playback.createRoute(fileId)) {
                        launchSingleTop = true
                    }
                },
            )
        },
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
                enterTransition = { fadeIn(animationSpec = tween(150)) },
                exitTransition = { fadeOut(animationSpec = tween(150)) },
                popEnterTransition = { fadeIn(animationSpec = tween(150)) },
                popExitTransition = { fadeOut(animationSpec = tween(150)) },
            ) {
                PlaybackScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onNavigateToFile = { fileId ->
                        navController.navigate(Screen.Playback.createRoute(fileId)) {
                            launchSingleTop = true
                            popUpTo(Screen.Playback.route) { inclusive = true }
                        }
                    },
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
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
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
