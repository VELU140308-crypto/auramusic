package com.aura.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String = "",
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    object Splash : Screen("splash")
    object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    object Songs : Screen(
        route = "songs",
        title = "Songs",
        selectedIcon = Icons.Filled.MusicNote,
        unselectedIcon = Icons.Outlined.MusicNote
    )
    object Playlists : Screen(
        route = "playlists",
        title = "Playlists",
        selectedIcon = Icons.Filled.QueueMusic,
        unselectedIcon = Icons.Outlined.QueueMusic
    )
    object Favorites : Screen(
        route = "favorites",
        title = "Favorites",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    object Artists : Screen("artists", "Artists")
    object ArtistDetail : Screen("artist_detail/{artistName}", "Artist") {
        fun createRoute(artistName: String): String = "artist_detail/${java.net.URLEncoder.encode(artistName, "UTF-8")}"
    }

    object Albums : Screen("albums", "Albums")
    object AlbumDetail : Screen("album_detail/{albumTitle}", "Album") {
        fun createRoute(albumTitle: String): String = "album_detail/${java.net.URLEncoder.encode(albumTitle, "UTF-8")}"
    }

    object PlaylistDetail : Screen("playlist_detail/{playlistId}/{playlistName}", "Playlist") {
        fun createRoute(id: Long, name: String): String = "playlist_detail/$id/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }

    object RecentlyPlayed : Screen("recently_played", "Recently Played")
    object Search : Screen("search", "Search")
    object Settings : Screen("settings", "Settings")
    object NowPlaying : Screen("now_playing", "Now Playing")
}

val bottomNavigationItems = listOf(
    Screen.Home,
    Screen.Songs,
    Screen.Playlists,
    Screen.Favorites
)
