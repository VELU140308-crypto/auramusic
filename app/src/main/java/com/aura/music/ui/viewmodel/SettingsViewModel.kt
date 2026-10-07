package com.aura.music.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.music.data.repository.MusicRepository
import com.aura.music.data.repository.SettingsRepository
import com.aura.music.data.repository.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            musicRepository.clearHistory()
            _message.value = "Playback history cleared."
        }
    }

    fun clearAppCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.cacheDir.deleteRecursively()
                withContext(Dispatchers.Main) {
                    _message.value = "Cache cleared successfully."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _message.value = "Failed to clear cache: ${e.message}"
                }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
