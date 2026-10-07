# Aura Music — Modern Offline Android Music Player

A sleek, 100% offline local music player app for Android, built with **Jetpack Compose**, **Material 3**, **Android Media3 (ExoPlayer + MediaSession)**, and **Room Database**.

---

## 🌟 Key Features

- **100% Offline & Private**: Zero network dependencies, zero tracking, and no `android.permission.INTERNET` requested. Fully functional in airplane mode.
- **MediaStore Music Scanner**: Automatically scans device storage for `MP3`, `M4A`, `AAC`, `WAV`, `FLAC`, and `OGG` files, capturing titles, artists, albums, durations, and high-resolution album artwork.
- **Storage Access Framework (SAF) Import**: Import songs directly from custom directories (Downloads, SD card) with persistable URI access.
- **Media3 Background Playback Engine**:
  - `PlaybackService` (`MediaSessionService`) with foreground notification and lockscreen playback controls.
  - Automatic **Audio Focus** handling (pauses during phone calls/navigation alerts).
  - **Becoming Noisy** event handling (automatically pauses when headphones are unplugged or Bluetooth disconnects).
  - Shuffle and Repeat modes (`OFF`, `REPEAT_ALL`, `REPEAT_ONE`).
  - Seamless seeking and queue management.
- **Error Resilient**: Inaccessible or deleted files are marked gracefully without crashing, skipping to the next available track and alerting the user.
- **Room Database Persistence**:
  - Playlists (Create, Rename, Delete, Add/Remove songs).
  - Liked / Favorite songs.
  - Recently Played history (with one-tap clear option).
  - Play counts and sorting preferences.
- **Rich User Interface**:
  - **Material 3** Dark Theme by default (with Light and System theme toggles).
  - Animated **Mini-Player** persisting above bottom navigation.
  - Immersive full-screen **Now Playing** screen with large artwork and scrubber.
  - Flexible multi-field sorting (Song Title, Artist, Album, Date Added, Duration; Ascending/Descending).
  - Full-text search across Title, Artist, and Album.
  - Dedicated Artists and Albums explorer screens.
- **Modern Permissions**: Granular `READ_MEDIA_AUDIO` on Android 13+ (API 33+), legacy storage fallback for Android 7.0–12, and `POST_NOTIFICATIONS`.

---

## 🏗️ Architecture

```
com.aura.music
├── AuraMusicApp.kt                  # Application initialization (DB, Repositories, Controller)
├── MainActivity.kt                  # Single Activity hosting Jetpack Compose & ViewModels
├── data/
│   ├── model/                       # Domain models (Song, Playlist, Artist, Album, PlaybackState, SortOption)
│   ├── local/                       # Room Database, DAOs, and Entities (SongEntity, PlaylistEntity, etc.)
│   ├── mediastore/                  # MediaStoreScanner querying local device storage
│   ├── saf/                         # SafImporter importing audio files via Storage Access Framework
│   └── repository/                  # MusicRepository & SettingsRepository
├── player/
│   ├── PlaybackService.kt           # Media3 MediaSessionService foreground playback service
│   ├── BecomingNoisyReceiver.kt     # Broadcast receiver for headphone unplugging
│   └── MusicController.kt           # MediaController bridge managing playback state & ExoPlayer
└── ui/
    ├── theme/                       # Material 3 Color palette, Typography, and Theme
    ├── components/                  # MiniPlayer, SongItem, AlbumArt, Dialogs, BottomSheets, PermissionHandler
    ├── navigation/                  # Screen routes & AuraNavGraph
    ├── screens/
    │   ├── splash/                  # Animated splash screen
    │   ├── home/                    # Home with quick cards, carousels, and All Songs highlight
    │   ├── songs/                   # All Songs screen with sorting and search
    │   ├── artists/                 # Artists list & ArtistDetailScreen
    │   ├── albums/                  # Albums grid & AlbumDetailScreen
    │   ├── playlists/               # Playlists list & PlaylistDetailScreen
    │   ├── favorites/               # Liked tracks screen
    │   ├── history/                 # Chronological Recently Played history screen
    │   ├── nowplaying/              # Full-screen Now Playing screen with scrubber
    │   ├── search/                  # Real-time search screen
    │   └── settings/                # Settings screen (Theme, rescan, cache, privacy)
    └── viewmodel/                   # MusicViewModel & SettingsViewModel
```

---

## 🚀 How to Build and Run in Android Studio

### Prerequisites
1. **Android Studio**: Android Studio Jellyfish (2024.1+) or Ladybug / Koala / Hedgehog.
2. **JDK**: Java 17 or Java 21 (bundled with modern Android Studio).
3. **Android SDK**: Android SDK 35 (compileSdk 35, minSdk 24).

### Step-by-Step Instructions

1. **Open the Project**:
   - Launch Android Studio.
   - Click **Open** (or `File -> Open`).
   - Select the project folder:
     ```
     C:\Users\HP\.gemini\antigravity\scratch\AuraMusic
     ```
   - Click **OK**.

2. **Gradle Sync**:
   - Android Studio will automatically recognize the Gradle build files (`settings.gradle.kts` and `app/build.gradle.kts`).
   - Wait for Gradle sync to complete and download the dependencies (Media3, Compose BOM, Room, Coil).

3. **Run on Emulator or Physical Device**:
   - Connect an Android device (via USB with USB debugging enabled) or start an Android Virtual Device (AVD with Android 7.0 to Android 15).
   - Select the `app` run configuration in the top toolbar.
   - Click the green **Run** button (or press `Shift + F10`).

4. **Grant Permission & Enjoy**:
   - On first launch, tap **Grant Permission** on the permission card or Home screen.
   - Aura will scan the device for offline audio tracks and populate your library immediately!
