package com.aura.music.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.music.data.model.Album
import com.aura.music.data.model.Artist
import com.aura.music.data.model.PlaybackState
import com.aura.music.data.model.Playlist
import com.aura.music.data.model.Song
import com.aura.music.data.model.SortBy
import com.aura.music.data.model.SortOption
import com.aura.music.data.model.SortOrder
import com.aura.music.data.repository.MusicRepository
import com.aura.music.data.repository.SettingsRepository
import com.aura.music.player.MusicController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository,
    private val musicController: MusicController,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val sortOption: StateFlow<SortOption> = settingsRepository.sortOption

    val allSongs: StateFlow<List<Song>> = combine(
        repository.allSongs,
        sortOption
    ) { songs, sort ->
        sortSongs(songs, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> = repository.mostPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedSongs: StateFlow<List<Song>> = repository.recentlyAddedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedSongs: StateFlow<List<Song>> = repository.recentlyPlayedSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = repository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackState> = musicController.playbackState

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val searchResults: StateFlow<List<Song>> = combine(
        searchQuery,
        repository.allSongs
    ) { query, songs ->
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            songs.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun rescanLibrary() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = repository.rescanLibrary()
                settingsRepository.setLastScanTime(System.currentTimeMillis())
                _scanMessage.value = "Library scanned: $count songs found"
            } catch (e: Exception) {
                _scanMessage.value = "Failed to scan library: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun importSafUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val count = repository.importFromSaf(uris)
                _scanMessage.value = "Imported $count audio files successfully"
            } catch (e: Exception) {
                _scanMessage.value = "Import error: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val targetQueue = if (queue.isEmpty()) allSongs.value else queue
        musicController.playSong(song, targetQueue)
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        musicController.playQueue(queue, startIndex)
    }

    fun shuffleAll(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            val shuffled = songs.shuffled()
            musicController.playSong(shuffled.first(), shuffled)
        }
    }

    fun togglePlayPause() = musicController.togglePlayPause()

    fun seekTo(positionMs: Long) = musicController.seekTo(positionMs)

    fun skipToNext() = musicController.skipToNext()

    fun skipToPrevious() = musicController.skipToPrevious()

    fun toggleShuffle() = musicController.toggleShuffle()

    fun toggleRepeat() = musicController.toggleRepeat()

    fun clearError() = musicController.clearError()

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun setSortOption(sortOption: SortOption) {
        settingsRepository.setSortOption(sortOption)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = repository.getSongsForPlaylist(playlistId)

    fun getSongsForArtist(artist: String): Flow<List<Song>> = repository.getSongsForArtist(artist)

    fun getSongsForAlbum(album: String): Flow<List<Song>> = repository.getSongsForAlbum(album)

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun sortSongs(songs: List<Song>, sort: SortOption): List<Song> {
        val comparator = when (sort.sortBy) {
            SortBy.TITLE -> compareBy<Song> { it.title.lowercase() }
            SortBy.ARTIST -> compareBy<Song> { it.artist.lowercase() }
            SortBy.ALBUM -> compareBy<Song> { it.album.lowercase() }
            SortBy.DATE_ADDED -> compareBy<Song> { it.dateAdded }
            SortBy.DURATION -> compareBy<Song> { it.duration }
        }

        return if (sort.sortOrder == SortOrder.ASCENDING) {
            songs.sortedWith(comparator)
        } else {
            songs.sortedWith(comparator.reversed())
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
