package com.aura.music.data.model

import android.net.Uri
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Song(
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
    val formattedDuration: String
        get() {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes)
            return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }

    val contentUriParsed: Uri
        get() = Uri.parse(contentUri)
}
