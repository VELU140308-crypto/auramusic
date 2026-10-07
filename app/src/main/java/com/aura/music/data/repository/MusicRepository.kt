package com.aura.music.data.repository

import android.net.Uri
import com.aura.music.data.local.AuraDatabase
import com.aura.music.data.local.entities.PlaybackHistoryEntity
import com.aura.music.data.local.entities.PlaylistEntity
import com.aura.music.data.local.entities.PlaylistSongCrossRef
import com.aura.music.data.local.entities.SongEntity
import com.aura.music.data.mediastore.MediaStoreScanner
import com.aura.music.data.model.Album
import com.aura.music.data.model.Artist
import com.aura.music.data.model.Playlist
import com.aura.music.data.model.Song
import com.aura.music.data.saf.SafImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(
    private val database: AuraDatabase,
    private val scanner: MediaStoreScanner,
    private val safImporter: SafImporter
) {
    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val historyDao = database.historyDao()

    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { list ->
        list.map { it.toSong() }
    }

    val favoriteSongs: Flow<List<Song>> = songDao.getFavoriteSongs().map { list ->
        list.map { it.toSong() }
    }

    val mostPlayedSongs: Flow<List<Song>> = songDao.getMostPlayedSongs().map { list ->
        list.map { it.toSong() }
    }

    val recentlyAddedSongs: Flow<List<Song>> = songDao.getRecentlyAddedSongs().map { list ->
        list.map { it.toSong() }
    }

    val recentlyPlayedSongs: Flow<List<Song>> = historyDao.getRecentlyPlayed().map { list ->
        list.map { it.toSong() }
    }

    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list ->
        list.map { entity ->
            val count = playlistDao.getSongCountForPlaylist(entity.id).first()
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                songCount = count
            )
        }
    }

    val artists: Flow<List<Artist>> = allSongs.map { songs ->
        songs.groupBy { it.artist }.map { (artistName, songList) ->
            val albumCount = songList.map { it.album }.distinct().size
            Artist(
                name = artistName,
                songCount = songList.size,
                albumCount = albumCount
            )
        }.sortedBy { it.name.lowercase() }
    }

    val albums: Flow<List<Album>> = allSongs.map { songs ->
        songs.groupBy { it.album }.map { (albumTitle, songList) ->
            val firstSong = songList.first()
            Album(
                id = firstSong.albumId,
                title = albumTitle,
                artist = firstSong.artist,
                artworkUri = firstSong.albumArtUri,
                songCount = songList.size
            )
        }.sortedBy { it.title.lowercase() }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }
    }

    fun getSongsForArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artist).map { list ->
            list.map { it.toSong() }
        }
    }

    fun getSongsForAlbum(album: String): Flow<List<Song>> {
        return songDao.getSongsByAlbum(album).map { list ->
            list.map { it.toSong() }
        }
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { list ->
            list.map { it.toSong() }
        }
    }

    suspend fun rescanLibrary(): Int = withContext(Dispatchers.IO) {
        val scannedSongs = scanner.scanLocalMusic()

        // Preserve favorites and play counts from previous scans
        val existingSongs = songDao.getAllSongs().first().associateBy { it.id }

        val entitiesToSave = scannedSongs.map { song ->
            val existing = existingSongs[song.id]
            SongEntity.fromSong(
                song.copy(
                    isFavorite = existing?.isFavorite ?: false,
                    playCount = existing?.playCount ?: 0,
                    lastPlayed = existing?.lastPlayed ?: 0L,
                    isAvailable = true
                )
            )
        }

        songDao.insertSongs(entitiesToSave)
        entitiesToSave.size
    }

    suspend fun importFromSaf(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        val imported = safImporter.importSongsFromUris(uris)
        val entities = imported.map { SongEntity.fromSong(it) }
        songDao.insertSongs(entities)
        imported.size
    }

    suspend fun toggleFavorite(song: Song) = withContext(Dispatchers.IO) {
        val newStatus = !song.isFavorite
        songDao.setFavorite(song.id, newStatus)
    }

    suspend fun recordPlayback(song: Song) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        songDao.incrementPlayCount(song.id, now)
        historyDao.insertHistory(PlaybackHistoryEntity(songId = song.id, playedAt = now))
    }

    suspend fun markUnavailable(songId: Long) = withContext(Dispatchers.IO) {
        songDao.markUnavailable(songId)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(PlaylistEntity(id = playlistId, name = newName))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearHistory()
    }
}
