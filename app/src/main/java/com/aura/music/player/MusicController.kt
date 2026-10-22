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

    // Base playback state (updates only on track change, play/pause, shuffle, repeat)
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Isolated progress streams to prevent whole-screen recomposition lag
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Volume stream (0.0f to 1.0f)
    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()
    private var lastNonZeroVolume: Float = 1.0f

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
                val dur = controller.duration.coerceAtLeast(0L)
                if (dur != androidx.media3.common.C.TIME_UNSET) {
                    _duration.value = dur
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId?.toLongOrNull()
                val currentSong = currentSongList.find { it.id == songId }
                val currentIndex = currentSongList.indexOfFirst { it.id == songId }

                _playbackState.update {
                    it.copy(
                        currentSong = currentSong,
                        currentIndex = currentIndex
                    )
                }

                _currentPosition.value = 0L
                val dur = controller.duration.coerceAtLeast(0L)
                if (dur != androidx.media3.common.C.TIME_UNSET) {
                    _duration.value = dur
                } else if (currentSong != null && currentSong.duration > 0) {
                    _duration.value = currentSong.duration
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

                // Skip to next song if queue exists
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
                repeatMode = controller.repeatMode
            )
        }

        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
        val dur = controller.duration.coerceAtLeast(0L)
        if (dur != androidx.media3.common.C.TIME_UNSET) {
            _duration.value = dur
        } else if (currentSong != null && currentSong.duration > 0) {
            _duration.value = currentSong.duration
        }

        _volume.value = controller.volume

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
                        _currentPosition.value = pos
                        if (dur != androidx.media3.common.C.TIME_UNSET && dur > 0) {
                            _duration.value = dur
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
        _currentPosition.value = 0L
        _duration.value = if (song.duration > 0) song.duration else 0L
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
        _currentPosition.value = positionMs
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

    // Volume controls
    fun setVolume(level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        mediaController?.volume = clamped
        _volume.value = clamped
        if (clamped > 0f) {
            lastNonZeroVolume = clamped
        }
    }

    fun toggleMute() {
        if (_volume.value > 0.01f) {
            lastNonZeroVolume = _volume.value
            setVolume(0f)
        } else {
            setVolume(if (lastNonZeroVolume > 0.05f) lastNonZeroVolume else 1.0f)
        }
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
