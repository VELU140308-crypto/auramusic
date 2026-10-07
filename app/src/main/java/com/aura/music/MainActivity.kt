package com.aura.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.aura.music.ui.navigation.AuraNavGraph
import com.aura.music.ui.theme.AuraMusicTheme
import com.aura.music.ui.viewmodel.MusicViewModel
import com.aura.music.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as AuraMusicApp }

    private val musicViewModel: MusicViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MusicViewModel(
                    repository = app.musicRepository,
                    musicController = app.musicController,
                    settingsRepository = app.settingsRepository
                ) as T
            }
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    context = applicationContext,
                    settingsRepository = app.settingsRepository,
                    musicRepository = app.musicRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState()

            AuraMusicTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AuraNavGraph(
                        navController = navController,
                        musicViewModel = musicViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
