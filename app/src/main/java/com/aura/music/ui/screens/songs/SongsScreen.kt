package com.aura.music.ui.screens.songs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aura.music.data.model.Song
import com.aura.music.data.model.SortBy
import com.aura.music.data.model.SortOption
import com.aura.music.data.model.SortOrder
import com.aura.music.ui.components.AddToPlaylistBottomSheet
import com.aura.music.ui.components.CreatePlaylistDialog
import com.aura.music.ui.components.FastScrollAlphabetBar
import com.aura.music.ui.components.FastScrollPopupBubble
import com.aura.music.ui.components.SongItem
import com.aura.music.ui.components.SortMenuBottomSheet
import com.aura.music.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit
) {
    val songs by viewModel.allSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()

    // Isolated lightweight states to prevent entire list from recomposing every 300ms!
    val currentPlayingSongId by viewModel.currentPlayingSongId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // A-Z index lookup map
    val letterIndexMap = remember(songs) {
        val map = mutableMapOf<Char, Int>()
        songs.forEachIndexed { index, song ->
            val firstChar = song.title.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }
    val availableLetters = remember(letterIndexMap) { letterIndexMap.keys }

    var activeLetterPopup by remember { mutableStateOf<Char?>(null) }
    var hideLetterJob by remember { mutableStateOf<Job?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importSafUris(uris)
        }
    }

    LaunchedEffect(scanMessage) {
        scanMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearScanMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "All Songs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${songs.size} songs • ${if (sortOption.sortBy == SortBy.TITLE) "A-Z" else sortOption.sortBy.name.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick A-Z sort toggle button
                    IconButton(onClick = { viewModel.toggleAlphabeticalSort() }) {
                        Icon(
                            imageVector = Icons.Rounded.SortByAlpha,
                            contentDescription = "A-Z Quick Sort",
                            tint = if (sortOption.sortBy == SortBy.TITLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }

                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort Options")
                    }

                    IconButton(
                        onClick = { viewModel.rescanLibrary() },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Rescan")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (songs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.shuffleAll(songs) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle all")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (songs.isEmpty() && !isScanning) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No songs found on device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan your local storage or import audio files directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.rescanLibrary() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Device")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { safLauncher.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Files via SAF")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Visible Sort Options bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = sortOption.sortBy == SortBy.TITLE && sortOption.sortOrder == SortOrder.ASCENDING,
                            onClick = {
                                viewModel.setSortOption(SortOption(SortBy.TITLE, SortOrder.ASCENDING))
                            },
                            label = { Text("Title A–Z") },
                            leadingIcon = if (sortOption.sortBy == SortBy.TITLE && sortOption.sortOrder == SortOrder.ASCENDING) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )

                        FilterChip(
                            selected = sortOption.sortBy == SortBy.TITLE && sortOption.sortOrder == SortOrder.DESCENDING,
                            onClick = {
                                viewModel.setSortOption(SortOption(SortBy.TITLE, SortOrder.DESCENDING))
                            },
                            label = { Text("Title Z–A") },
                            leadingIcon = if (sortOption.sortBy == SortBy.TITLE && sortOption.sortOrder == SortOrder.DESCENDING) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )

                        FilterChip(
                            selected = sortOption.sortBy != SortBy.TITLE,
                            onClick = { showSortSheet = true },
                            label = {
                                Text(
                                    if (sortOption.sortBy != SortBy.TITLE)
                                        sortOption.sortBy.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                                    else "More ▾"
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Song list (with padding for alphabet strip)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(
                                items = songs,
                                key = { it.id }
                            ) { song ->
                                val isThisSongPlaying = isPlaying && currentPlayingSongId == song.id
                                SongItem(
                                    song = song,
                                    isPlaying = isThisSongPlaying,
                                    onClick = { viewModel.playSong(song, songs) },
                                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                                    onAddToPlaylist = { selectedSongForPlaylist = song }
                                )
                            }
                        }

                        // A-Z Fast Scroll Alphabet Bar on the right
                        if (songs.size > 8) {
                            FastScrollAlphabetBar(
                                availableLetters = availableLetters,
                                onLetterSelected = { letter ->
                                    activeLetterPopup = letter
                                    hideLetterJob?.cancel()
                                    hideLetterJob = scope.launch {
                                        delay(900)
                                        activeLetterPopup = null
                                    }

                                    val targetIndex = letterIndexMap[letter] ?: run {
                                        // Find next closest letter in alphabet
                                        if (letter in 'A'..'Z') {
                                            letterIndexMap.filterKeys { it >= letter }.minByOrNull { it.key }?.value
                                        } else {
                                            letterIndexMap['#']
                                        }
                                    }

                                    if (targetIndex != null) {
                                        scope.launch {
                                            listState.scrollToItem(targetIndex)
                                        }
                                    }
                                },
                                modifier = Modifier.padding(end = 4.dp, top = 8.dp, bottom = 96.dp)
                            )
                        }
                    }
                }

                // Floating bubble showing current letter while scrolling
                FastScrollPopupBubble(
                    letter = activeLetterPopup,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        // Bottom Sheets and Dialogs
        if (showSortSheet) {
            SortMenuBottomSheet(
                currentSortOption = sortOption,
                onSortSelected = { viewModel.setSortOption(it) },
                onDismiss = { showSortSheet = false }
            )
        }

        selectedSongForPlaylist?.let { song ->
            AddToPlaylistBottomSheet(
                song = song,
                playlists = playlists,
                onPlaylistSelected = { playlist ->
                    viewModel.addSongToPlaylist(playlist.id, song.id)
                },
                onCreateNewClicked = {
                    showCreatePlaylistDialog = true
                },
                onDismiss = { selectedSongForPlaylist = null }
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { name ->
                    viewModel.createPlaylist(name)
                    showCreatePlaylistDialog = false
                }
            )
        }
    }
}
