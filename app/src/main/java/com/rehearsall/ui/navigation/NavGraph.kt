package com.rehearsall.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rehearsall.ui.common.MiniPlayer
import com.rehearsall.ui.library.LibraryScreen
import com.rehearsall.ui.playback.PlaybackScreen
import com.rehearsall.ui.playlist.PlaylistScreen
import com.rehearsall.ui.playlists.PlaylistListScreen
import com.rehearsall.ui.recents.RecentsScreen
import com.rehearsall.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehearsAllNavGraph(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()

    val miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel()
    val miniPlayerState by miniPlayerViewModel.state.collectAsStateWithLifecycle()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Show bottom nav and mini player only on top-level tabs
    val bottomNavRoutes = BottomNavItem.entries.map { it.screen.route }
    val isOnBottomNavScreen = currentRoute in bottomNavRoutes
    val showMiniPlayer = miniPlayerState.isVisible && currentRoute != Screen.Playback.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Stack: MiniPlayer above NavigationBar (both in bottomBar slot)
            Column {
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

                if (isOnBottomNavScreen) {
                    NavigationBar {
                        BottomNavItem.entries.forEach { item ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                    )
                                },
                                label = { Text(item.label) },
                                selected = currentRoute == item.screen.route,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Bottom nav tabs
            composable(Screen.Library.route) {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                LibraryScreen(
                    onFileClick = { audioFileId ->
                        navController.navigate(Screen.Playback.createRoute(audioFileId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }

            composable(Screen.PlaylistList.route) {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                PlaylistListScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate(Screen.Playlist.createRoute(playlistId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }

            composable(Screen.Recents.route) {
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                RecentsScreen(
                    onFileClick = { audioFileId ->
                        navController.navigate(Screen.Playback.createRoute(audioFileId))
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    scrollBehavior = scrollBehavior,
                )
            }

            // Detail screens
            composable(
                route = Screen.Playback.route,
                arguments =
                    listOf(
                        navArgument("audioFileId") { type = NavType.LongType },
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
                arguments =
                    listOf(
                        navArgument("playlistId") { type = NavType.LongType },
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
