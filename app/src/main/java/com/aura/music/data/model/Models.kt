package com.aura.music.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Playlist(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

@Immutable
data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

@Immutable
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val year: Int = 0
)

@Immutable
data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = 0, // 0 = OFF, 1 = ONE, 2 = ALL
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val errorMessage: String? = null
)

enum class SortBy {
    TITLE,
    ARTIST,
    ALBUM,
    DATE_ADDED,
    DURATION
}

enum class SortOrder {
    ASCENDING,
    DESCENDING
}

@Immutable
data class SortOption(
    val sortBy: SortBy = SortBy.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING
)
