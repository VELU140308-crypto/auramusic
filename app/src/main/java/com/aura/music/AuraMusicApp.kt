package com.aura.music

import android.app.Application
import com.aura.music.data.local.AuraDatabase
import com.aura.music.data.mediastore.MediaStoreScanner
import com.aura.music.data.repository.MusicRepository
import com.aura.music.data.repository.SettingsRepository
import com.aura.music.data.saf.SafImporter
import com.aura.music.player.MusicController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuraMusicApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AuraDatabase
        private set

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var musicController: MusicController
        private set

    override fun onCreate() {
        super.onCreate()

        database = AuraDatabase.getInstance(this)
        val scanner = MediaStoreScanner(this)
        val safImporter = SafImporter(this)

        musicRepository = MusicRepository(database, scanner, safImporter)
        settingsRepository = SettingsRepository(this)

        musicController = MusicController(
            context = this,
            onSongPlayed = { song ->
                applicationScope.launch {
                    musicRepository.recordPlayback(song)
                }
            },
            onSongUnavailable = { songId ->
                applicationScope.launch {
                    musicRepository.markUnavailable(songId)
                }
            }
        )
    }
}
