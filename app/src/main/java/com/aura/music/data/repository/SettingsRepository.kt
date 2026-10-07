package com.aura.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aura.music.data.model.SortBy
import com.aura.music.data.model.SortOption
import com.aura.music.data.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_settings_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _sortOption = MutableStateFlow(loadSortOption())
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString("key_theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("key_theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadSortOption(): SortOption {
        val sortByStr = prefs.getString("key_sort_by", SortBy.TITLE.name) ?: SortBy.TITLE.name
        val sortOrderStr = prefs.getString("key_sort_order", SortOrder.ASCENDING.name) ?: SortOrder.ASCENDING.name
        val sortBy = try { SortBy.valueOf(sortByStr) } catch (e: Exception) { SortBy.TITLE }
        val sortOrder = try { SortOrder.valueOf(sortOrderStr) } catch (e: Exception) { SortOrder.ASCENDING }
        return SortOption(sortBy, sortOrder)
    }

    fun setSortOption(sortOption: SortOption) {
        prefs.edit()
            .putString("key_sort_by", sortOption.sortBy.name)
            .putString("key_sort_order", sortOption.sortOrder.name)
            .apply()
        _sortOption.value = sortOption
    }

    fun setLastScanTime(time: Long) {
        prefs.edit().putLong("key_last_scan_time", time).apply()
    }

    fun getLastScanTime(): Long {
        return prefs.getLong("key_last_scan_time", 0L)
    }
}
