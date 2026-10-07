package com.aura.music.data.saf

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.aura.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafImporter(private val context: Context) {

    suspend fun importSongsFromUris(uris: List<Uri>): List<Song> = withContext(Dispatchers.IO) {
        val importedSongs = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        for (uri in uris) {
            try {
                // Persist read permissions for offline playback across restarts
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.w("SafImporter", "Could not take persistable permission for $uri", e)
                }

                var displayName = "Imported Track"
                var size = 0L

                // Extract filename and size from document query
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex) ?: displayName
                        }
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }

                // Extract metadata using MediaMetadataRetriever
                val retriever = MediaMetadataRetriever()
                var title = displayName.substringBeforeLast(".")
                var artist = "Unknown Artist"
                var album = "Unknown Album"
                var duration = 0L
                var mimeType = contentResolver.getType(uri) ?: "audio/mpeg"

                try {
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                        if (it.isNotBlank()) title = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                        if (it.isNotBlank()) artist = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                        if (it.isNotBlank()) album = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                        duration = it.toLongOrNull() ?: 0L
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let {
                        mimeType = it
                    }
                } catch (e: Exception) {
                    Log.w("SafImporter", "Could not retrieve metadata for $uri", e)
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        // ignored
                    }
                }

                val generatedId = uri.hashCode().toLong()

                importedSongs.add(
                    Song(
                        id = generatedId,
                        title = title.trim(),
                        artist = artist.trim(),
                        album = album.trim(),
                        albumId = 0L,
                        duration = duration,
                        contentUri = uri.toString(),
                        albumArtUri = null,
                        dateAdded = System.currentTimeMillis() / 1000,
                        size = size,
                        mimeType = mimeType,
                        isAvailable = true
                    )
                )
            } catch (e: Exception) {
                Log.e("SafImporter", "Failed to import URI $uri", e)
            }
        }

        importedSongs
    }
}
