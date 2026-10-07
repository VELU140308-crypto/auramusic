package com.aura.music.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.aura.music.ui.components.MiniPlayer
import com.aura.music.ui.screens.albums.AlbumDetailScreen
import com.aura.music.ui.screens.albums.AlbumsScreen
import com.aura.music.ui.screens.artists.ArtistDetailScreen
import com.aura.music.ui.screens.artists.ArtistsScreen
import com.aura.music.ui.screens.favorites.FavoritesScreen
import com.aura.music.ui.screens.history.HistoryScreen
import com.aura.music.ui.screens.home.HomeScreen
import com.aura.music.ui.screens.nowplaying.NowPlayingScreen
import com.aura.music.ui.screens.playlists.PlaylistDetailScreen
import com.aura.music.ui.screens.playlists.PlaylistsScreen
import com.aura.music.ui.screens.search.SearchScreen
import com.aura.music.ui.screens.settings.SettingsScreen
import com.aura.music.ui.screens.songs.SongsScreen
import com.aura.music.ui.screens.splash.SplashScreen
import com.aura.music.ui.viewmodel.MusicViewModel
import com.aura.music.ui.viewmodel.SettingsViewModel
import java.net.URLDecoder

@Composable
fun AuraNavGraph(
    navController: NavHostController,
    musicViewModel: MusicViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val playbackState by musicViewModel.playbackState.collectAsState()

    val isFullscreenScreen = currentRoute == Screen.Splash.route || currentRoute == Screen.NowPlaying.route

    Scaffold(
        bottomBar = {
            if (!isFullscreenScreen) {
                Column {
                    // Persistent Mini Player above Bottom Bar
                    MiniPlayer(
                        playbackState = playbackState,
                        onPlayPause = { musicViewModel.togglePlayPause() },
                        onSkipNext = { musicViewModel.skipToNext() },
                        onClick = {
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        bottomNavigationItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = (if (isSelected) screen.selectedIcon else screen.unselectedIcon)
                                            ?: screen.selectedIcon!!,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding)
        ) {
            // Splash Screen
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateNext = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Bottom Nav Screen: Home
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = musicViewModel,
                    onNavigateToAllSongs = { navController.navigate(Screen.Songs.route) },
                    onNavigateToRecentlyPlayed = { navController.navigate(Screen.RecentlyPlayed.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToPlaylist = { id, name ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                    }
                )
            }

            // Bottom Nav Screen: Songs
            composable(Screen.Songs.route) {
                SongsScreen(
                    viewModel = musicViewModel,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) }
                )
            }

            // Bottom Nav Screen: Playlists
            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    viewModel = musicViewModel,
                    onPlaylistClick = { id, name ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                    }
                )
            }

            // Bottom Nav Screen: Favorites
            composable(Screen.Favorites.route) {
                FavoritesScreen(viewModel = musicViewModel)
            }

            // Artists
            composable(Screen.Artists.route) {
                ArtistsScreen(
                    viewModel = musicViewModel,
                    onArtistClick = { artistName ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                    }
                )
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("artistName") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("artistName") ?: ""
                val artistName = try { URLDecoder.decode(encoded, "UTF-8") } catch (e: Exception) { encoded }
                ArtistDetailScreen(
                    artistName = artistName,
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Albums
            composable(Screen.Albums.route) {
                AlbumsScreen(
                    viewModel = musicViewModel,
                    onAlbumClick = { albumTitle ->
                        navController.navigate(Screen.AlbumDetail.createRoute(albumTitle))
                    }
                )
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(navArgument("albumTitle") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("albumTitle") ?: ""
                val albumTitle = try { URLDecoder.decode(encoded, "UTF-8") } catch (e: Exception) { encoded }
                AlbumDetailScreen(
                    albumTitle = albumTitle,
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Playlist Detail
            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(
                    navArgument("playlistId") { type = NavType.LongType },
                    navArgument("playlistName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                val encoded = backStackEntry.arguments?.getString("playlistName") ?: ""
                val playlistName = try { URLDecoder.decode(encoded, "UTF-8") } catch (e: Exception) { encoded }
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    playlistName = playlistName,
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Recently Played History
            composable(Screen.RecentlyPlayed.route) {
                HistoryScreen(
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Search Screen
            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings Screen
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    musicViewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Full-screen Now Playing
            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(
                    viewModel = musicViewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}
