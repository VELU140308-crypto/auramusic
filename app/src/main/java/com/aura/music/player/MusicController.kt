package com.aura.music.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aura.music.data.model.PlaybackState
import com.aura.music.data.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicController(
    private val context: Context,
    private val onSongPlayed: (Song) -> Unit = {},
    private val onSongUnavailable: (Long) -> Unit = {}
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var currentSongList: List<Song> = emptyList()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                updateCurrentStateFromPlayer()
            } catch (e: Exception) {
                Log.e("MusicController", "Failed to connect to MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val duration = controller.duration.coerceAtLeast(0L)
                _playbackState.update {
                    it.copy(
                        duration = if (duration == androidx.media3.common.C.TIME_UNSET) 0L else duration
                    )
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId?.toLongOrNull()
                val currentSong = currentSongList.find { it.id == songId }
                val currentIndex = currentSongList.indexOfFirst { it.id == songId }

                _playbackState.update {
                    it.copy(
                        currentSong = currentSong,
                        currentIndex = currentIndex,
                        currentPosition = 0L,
                        duration = controller.duration.coerceAtLeast(0L)
                    )
                }

                if (currentSong != null) {
                    onSongPlayed(currentSong)
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playbackState.update { it.copy(repeatMode = repeatMode) }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MusicController", "Player error encountered: ${error.errorCodeName}", error)
                val failedSong = _playbackState.value.currentSong
                if (failedSong != null) {
                    onSongUnavailable(failedSong.id)
                }

                _playbackState.update {
                    it.copy(
                        errorMessage = "Cannot play \"${failedSong?.title ?: "track"}\". The file is missing or inaccessible.",
                        isPlaying = false
                    )
                }

                // If error occurs, attempt to skip to next song if queue exists
                if (controller.hasNextMediaItem()) {
                    controller.seekToNextMediaItem()
                    controller.prepare()
                    controller.play()
                }
            }
        })
    }

    private fun updateCurrentStateFromPlayer() {
        val controller = mediaController ?: return
        val currentMediaId = controller.currentMediaItem?.mediaId?.toLongOrNull()
        val currentSong = currentSongList.find { it.id == currentMediaId }

        _playbackState.update {
            it.copy(
                isPlaying = controller.isPlaying,
                currentSong = currentSong,
                currentIndex = controller.currentMediaItemIndex,
                isShuffleEnabled = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode,
                currentPosition = controller.currentPosition.coerceAtLeast(0L),
                duration = controller.duration.coerceAtLeast(0L)
            )
        }

        if (controller.isPlaying) {
            startProgressTracker()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val pos = controller.currentPosition.coerceAtLeast(0L)
                        val dur = controller.duration.coerceAtLeast(0L)
                        _playbackState.update {
                            it.copy(
                                currentPosition = pos,
                                duration = if (dur == androidx.media3.common.C.TIME_UNSET) it.duration else dur
                            )
                        }
                    }
                }
                delay(300L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playSong(song: Song, queue: List<Song>) {
        val controller = mediaController ?: return
        currentSongList = queue
        val mediaItems = queue.map { it.toMediaItem() }
        val targetIndex = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        controller.setMediaItems(mediaItems, targetIndex, 0L)
        controller.prepare()
        controller.play()

        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = queue,
                currentIndex = targetIndex,
                isPlaying = true,
                errorMessage = null
            )
        }
    }

    fun playQueue(queue: List<Song>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        val targetSong = queue.getOrNull(startIndex) ?: queue[0]
        playSong(targetSong, queue)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        controller.seekTo(positionMs)
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3000L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else {
            controller.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newState = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newState
        _playbackState.update { it.copy(isShuffleEnabled = newState) }
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
        _playbackState.update { it.copy(repeatMode = newMode) }
    }

    fun clearError() {
        _playbackState.update { it.copy(errorMessage = null) }
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }
}
