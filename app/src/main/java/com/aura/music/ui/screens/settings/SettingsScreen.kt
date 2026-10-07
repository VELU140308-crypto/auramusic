package com.aura.music.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.music.data.repository.ThemeMode
import com.aura.music.ui.components.SortMenuBottomSheet
import com.aura.music.ui.viewmodel.MusicViewModel
import com.aura.music.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    musicViewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val settingsMessage by settingsViewModel.message.collectAsState()
    val sortOption by musicViewModel.sortOption.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(settingsMessage) {
        settingsMessage?.let {
            snackbarHostState.showSnackbar(it)
            settingsViewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Appearance Header
            SettingsCategoryHeader("Appearance")
            SettingsItem(
                icon = Icons.Rounded.Palette,
                title = "App Theme",
                subtitle = when (themeMode) {
                    ThemeMode.DARK -> "Dark (Default)"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.SYSTEM -> "Follow System"
                },
                onClick = { showThemeDialog = true }
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // Library Management Header
            SettingsCategoryHeader("Library & Playback")
            SettingsItem(
                icon = Icons.Rounded.Refresh,
                title = "Rescan Music Library",
                subtitle = "Scan device storage for newly added audio files",
                onClick = { musicViewModel.rescanLibrary() }
            )

            SettingsItem(
                icon = Icons.Rounded.Sort,
                title = "Default Sorting",
                subtitle = "${sortOption.sortBy.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} (${sortOption.sortOrder.name.lowercase()})",
                onClick = { showSortSheet = true }
            )

            SettingsItem(
                icon = Icons.Rounded.Headphones,
                title = "Audio Focus & Hardware",
                subtitle = "Automatic pause on headphone unplug and audio interruptions enabled",
                onClick = { /* Informational */ }
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // Data & Storage Header
            SettingsCategoryHeader("Data & Storage")
            SettingsItem(
                icon = Icons.Rounded.DeleteSweep,
                title = "Clear Playback History",
                subtitle = "Reset your recently played songs record",
                onClick = { showClearHistoryDialog = true }
            )

            SettingsItem(
                icon = Icons.Rounded.CleaningServices,
                title = "Clear Cache",
                subtitle = "Delete temporary image and thumbnail cache",
                onClick = { showClearCacheDialog = true }
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))

            // About & Privacy
            SettingsCategoryHeader("About")
            SettingsItem(
                icon = Icons.Rounded.PrivacyTip,
                title = "Privacy Statement",
                subtitle = "100% offline. Zero tracking. No internet permissions.",
                onClick = { showPrivacyDialog = true }
            )

            SettingsItem(
                icon = Icons.Rounded.Info,
                title = "About Aura Music",
                subtitle = "Version 1.0.0 (Pure Offline)",
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Dialogs
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme") },
                text = {
                    Column {
                        ThemeOptionRow(
                            label = "Dark (Default)",
                            selected = themeMode == ThemeMode.DARK,
                            onSelect = {
                                settingsViewModel.setThemeMode(ThemeMode.DARK)
                                showThemeDialog = false
                            }
                        )
                        ThemeOptionRow(
                            label = "Light",
                            selected = themeMode == ThemeMode.LIGHT,
                            onSelect = {
                                settingsViewModel.setThemeMode(ThemeMode.LIGHT)
                                showThemeDialog = false
                            }
                        )
                        ThemeOptionRow(
                            label = "Follow System",
                            selected = themeMode == ThemeMode.SYSTEM,
                            onSelect = {
                                settingsViewModel.setThemeMode(ThemeMode.SYSTEM)
                                showThemeDialog = false
                            }
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSortSheet) {
            SortMenuBottomSheet(
                currentSortOption = sortOption,
                onSortSelected = { musicViewModel.setSortOption(it) },
                onDismiss = { showSortSheet = false }
            )
        }

        if (showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearHistoryDialog = false },
                title = { Text("Clear Playback History") },
                text = { Text("Are you sure you want to delete your listening history?") },
                confirmButton = {
                    TextButton(onClick = {
                        settingsViewModel.clearPlaybackHistory()
                        showClearHistoryDialog = false
                    }) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text("Clear Cache") },
                text = { Text("This will remove cached album artwork thumbnails to free up device space.") },
                confirmButton = {
                    TextButton(onClick = {
                        settingsViewModel.clearAppCache()
                        showClearCacheDialog = false
                    }) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPrivacyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyDialog = false },
                title = { Text("Offline Privacy Guarantee") },
                text = {
                    Text(
                        "Aura Music is strictly designed for local, offline listening.\n\n" +
                        "• No Internet permission: The app cannot connect to external servers.\n" +
                        "• MediaStore API: Only queries audio files stored on this device.\n" +
                        "• Safe Storage: Playlists and favorites are stored purely on your local device with Room.\n" +
                        "• Zero telemetry or third-party tracking."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { Text("Aura Music v1.0.0") },
                text = {
                    Text(
                        "A modern offline Android music player built with Jetpack Compose, Material 3, Android Media3 / ExoPlayer, and Room Database.\n\n" +
                        "Engineered for speed, smooth background playback, and complete privacy."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
