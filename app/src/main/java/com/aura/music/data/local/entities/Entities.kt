package com.aura.music.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aura.music.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val dateAdded: Long = 0L,
    val size: Long = 0L,
    val mimeType: String = "audio/*",
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0L,
    val isAvailable: Boolean = true
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        dateAdded = dateAdded,
        size = size,
        mimeType = mimeType,
        isFavorite = isFavorite,
        playCount = playCount,
        lastPlayed = lastPlayed,
        isAvailable = isAvailable
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumId = song.albumId,
            duration = song.duration,
            contentUri = song.contentUri,
            albumArtUri = song.albumArtUri,
            dateAdded = song.dateAdded,
            size = song.size,
            mimeType = song.mimeType,
            isFavorite = song.isFavorite,
            playCount = song.playCount,
            lastPlayed = song.lastPlayed,
            isAvailable = song.isAvailable
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playback_history",
    indices = [Index("songId")]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val songId: Long,
    val playedAt: Long = System.currentTimeMillis()
)
