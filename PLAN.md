# RehearsAll — Implementation Plan

## Phase Overview

| Phase | Name | Description | Dependencies |
|-------|------|-------------|-------------|
| 1 | Project Scaffolding | Gradle, Hilt, Room, Media3, Timber, theme, navigation, adaptive layout shell | None |
| 2 | Audio Import & File List | SAF picker, copy to internal storage, list screen, adaptive layout | Phase 1 |
| 3 | Core Playback & MediaSession | MediaLibraryService, ExoPlayer, transport controls, speed control, notification/lock screen, headphone buttons, repeat modes | Phase 1, 2 |
| 4 | Playlists & Queue | Playlist CRUD, play queue, next/previous, queue bottom sheet | Phase 2, 3 |
| 5 | Waveform | Extract, cache, render, zoom, seek | Phase 2, 3 |
| 6 | Bookmarks | Drop, display, rename, delete, persist, bottom sheet | Phase 3, 5 |
| 7 | A-B Looping | Set points, loop, adjust, save/load, bottom sheet | Phase 3, 5 |
| 8 | Chunked Repetition | Markers, practice engine, three modes, bottom sheet | Phase 7 |
| 9 | Android Auto | Content tree, media browsing, voice actions | Phase 3, 4, 7 |
| 10 | Settings & Preferences | Theme picker, skip increment, user preferences | Phase 1 |
| 11 | Production Polish | Accessibility, edge cases, performance, error handling | Phase 2–10 |
| 12 | CI/CD & Release | GitHub Actions, signing, ProGuard, release APK, README, license | Phase 11 |
| 13 | App Icon | Adaptive icon from selected concept (Waveform Loop v5) | Phase 1 |
| 14 | End-User Documentation | In-app help, user guide with screenshots, feature walkthrough | Phase 11 |

---

## Phase 1 — Project Scaffolding

**Goal:** Bootable app with all dependencies wired, Material You theme working (light/dark/system), adaptive layout shell, Timber logging, blank screens navigable.

### Tasks
- [ ] Create Android project:
  - Package: `com.rehearsall`
  - Min SDK 26, Target SDK 36
  - Kotlin DSL build files
- [ ] Create `gradle/libs.versions.toml` (Gradle version catalog):
  - Centralize all dependency versions (Compose BOM, Media3, Room, Hilt, Timber, etc.)
  - Define version aliases, library aliases, and plugin aliases
- [ ] Configure `build.gradle.kts` (project level):
  - Kotlin plugin
  - Hilt plugin
  - KSP plugin (for Room + Hilt annotation processing)
- [ ] Configure `build.gradle.kts` (app level) with dependencies:
  - **Compose:** BOM (latest stable), UI, Material 3, Navigation, Activity
  - **Material 3 extensions:** `material3-window-size-class`, `material3-adaptive-navigation-suite`
  - **Media3:** ExoPlayer, Session, Common, UI
  - **Room:** runtime, KSP compiler, ktx
  - **Hilt:** android, compiler, navigation-compose
  - **DataStore:** preferences
  - **Timber:** logging
  - **Coroutines:** core, android
  - **Testing:** JUnit 4 (AndroidX Test), MockK, Turbine, Compose UI testing, Room testing, Hilt testing
- [ ] Configure release build type: minification enabled, R8, proguard rules
- [ ] Create `RehearsAllApp` Application class with `@HiltAndroidApp`
- [ ] Create `MainActivity` with `@AndroidEntryPoint`:
  - Calculate `WindowSizeClass`
  - Set edge-to-edge display
  - Set Compose content with theme
- [ ] Set up Material 3 theme:
  - `Theme.kt` — `RehearsAllTheme` composable with dynamic color + light/dark/system support
  - `Color.kt` — Custom `LightColorScheme` and `DarkColorScheme` for pre-Android 12 fallback
  - `Type.kt` — Typography (Material 3 defaults are fine initially, customize later)
- [ ] Set up Compose Navigation with routes: `FileList`, `Playback/{audioFileId}`, `Playlist/{playlistId}`, `Settings`
- [ ] Create placeholder screens: `FileListScreen`, `PlaybackScreen`, `PlaylistScreen`, `SettingsScreen`
- [ ] Set up adaptive layout shell:
  - Compact: standard navigation
  - Expanded: `ListDetailPaneScaffold` placeholder
- [ ] Create empty Hilt modules: `DatabaseModule`, `AudioModule`, `RepositoryModule`, `PreferencesModule`
- [ ] Configure backup exclusion rules (`backup_rules.xml` / `data_extraction_rules.xml`):
  - Exclude `databases/` (Room DB — re-importable, not worth backup size)
  - Exclude `files/audio/` (large audio files — user has originals)
  - Exclude `files/waveforms/` (regeneratable cache)
  - Exclude `files/logs/` (transient diagnostics)
  - Allow DataStore preferences (small, user settings worth preserving)
- [ ] Add Timber logging:
  - Add Timber dependency
  - Initialize in `RehearsAllApp.onCreate()`: `DebugTree` for debug, `FileLoggingTree` for release
  - Create `FileLoggingTree` that writes to `{filesDir}/logs/rehearsall.log`
- [ ] Verify: app builds, launches, theme switches between light/dark/system, navigates between blank screens, Timber logs appear in Logcat

### Tests (Phase 1)
- [ ] Verify theme composable renders without crash in light, dark, and system modes (Compose UI test)
- [ ] Verify navigation routes resolve correctly (unit test)

### Key Files Created
```
build.gradle.kts (project)
settings.gradle.kts
gradle.properties
gradle/libs.versions.toml
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/rehearsall/RehearsAllApp.kt
app/src/main/java/com/rehearsall/MainActivity.kt
app/src/main/java/com/rehearsall/logging/FileLoggingTree.kt
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/java/com/rehearsall/di/DatabaseModule.kt
app/src/main/java/com/rehearsall/di/AudioModule.kt
app/src/main/java/com/rehearsall/di/RepositoryModule.kt
app/src/main/java/com/rehearsall/di/PreferencesModule.kt
app/src/main/java/com/rehearsall/ui/theme/Theme.kt
app/src/main/java/com/rehearsall/ui/theme/Color.kt
app/src/main/java/com/rehearsall/ui/theme/Type.kt
app/src/main/java/com/rehearsall/ui/navigation/NavGraph.kt
app/src/main/java/com/rehearsall/ui/navigation/Screen.kt
app/src/main/java/com/rehearsall/ui/filelist/FileListScreen.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackScreen.kt
app/src/main/java/com/rehearsall/ui/playlist/PlaylistScreen.kt
app/src/main/java/com/rehearsall/ui/settings/SettingsScreen.kt
```

---

## Phase 2 — Audio Import & File List

**Goal:** Users can import audio files via SAF, see them in a list, delete them. Files are copied to internal storage, metadata persisted in Room. Adaptive layout for phone/tablet.

### Tasks
- [ ] Define Room entities: `AudioFileEntity` (id, fileName, displayName, internalPath, format, durationMs, fileSizeBytes, artist, title, importedAt, lastPlayedAt, lastPositionMs, lastSpeed)
- [ ] Create `AudioFileDao` (insert, getAll as Flow, getById, getRecent, delete, updateDisplayName, updateLastPlayed, updateLastPosition, updateLastSpeed)
- [ ] Create `RehearsAllDatabase` (version 1) with `AudioFileEntity`
- [ ] Wire database in `DatabaseModule` (singleton, fallbackToDestructiveMigration for dev)
- [ ] Create `AudioFileRepository` (interface + impl):
  - Maps entities ↔ domain models
  - Wraps DB calls in `withContext(Dispatchers.IO)`
- [ ] Create `AudioImporter` utility:
  - Accept URI from SAF
  - Copy file to `{context.filesDir}/audio/{uuid}.{ext}`
  - Extract metadata: file name, format, duration, file size (via `MediaMetadataRetriever`)
  - Extract embedded metadata: artist (`METADATA_KEY_ARTIST`), title (`METADATA_KEY_TITLE`)
  - Set `displayName` to embedded title if present, otherwise file name without extension
  - Validate format (reject unsupported types with clear error)
  - Validate file size (reject files >500MB with clear error — prevents storage exhaustion)
  - Insert `AudioFileEntity` into Room
  - Return `Result<AudioFile>`
  - Log import events via Timber
- [ ] Create domain model: `AudioFile`
- [ ] Create `FileListViewModel`:
  - Expose `StateFlow<FileListUiState>` (loading, files list, empty, error)
  - `importFile(uri: Uri)` — calls importer, shows snackbar on success/failure
  - `deleteFile(id: Long)` — removes from DB + deletes internal file + waveform cache
- [ ] Build `FileListScreen`:
  - Large top app bar with title "RehearsAll", settings icon
  - FAB for import (launches SAF picker)
  - SAF picker: `ACTION_OPEN_DOCUMENT`, MIME types for MP3, WAV, OGG, FLAC, M4A/AAC
  - `LazyColumn` of file cards: display name, artist subtitle (if available), duration (formatted mm:ss), format chip
  - Swipe-to-delete with undo snackbar
  - Long-press → file details bottom sheet (format, size, duration, artist, import date, **rename** button, delete button)
  - Tap a file → navigate to `Playback/{audioFileId}`
  - Empty state composable when no files imported
  - Loading indicator during import
- [ ] Adaptive layout:
  - Compact: full-screen file list
  - Expanded: list-detail pane with `ListDetailPaneScaffold`
- [ ] Accessibility: content descriptions on FAB, file cards, swipe actions

### Tests (Phase 2)
- [ ] `AudioFileDao` — insert, getAll, getRecent, delete, cascading (Room in-memory DB)
- [ ] `AudioFileRepository` — entity ↔ domain mapping, error wrapping
- [ ] `AudioImporter` — validate supported/unsupported formats, metadata extraction (instrumented, with test audio files)
- [ ] `FileListViewModel` — state transitions: loading → loaded, import success/failure, delete
- [ ] `FileListScreen` — UI test: empty state shown, file card renders, tap navigates

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/RehearsAllDatabase.kt
app/src/main/java/com/rehearsall/data/db/entity/AudioFileEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/AudioFileDao.kt
app/src/main/java/com/rehearsall/data/repository/AudioFileRepository.kt
app/src/main/java/com/rehearsall/data/audio/AudioImporter.kt
app/src/main/java/com/rehearsall/domain/model/AudioFile.kt
app/src/main/java/com/rehearsall/ui/filelist/FileListViewModel.kt
app/src/main/java/com/rehearsall/ui/filelist/FileListScreen.kt
app/src/main/java/com/rehearsall/ui/filelist/FileListUiState.kt
app/src/main/java/com/rehearsall/ui/common/FileDetailsBottomSheet.kt
app/src/test/java/com/rehearsall/data/repository/AudioFileRepositoryTest.kt
app/src/test/java/com/rehearsall/ui/filelist/FileListViewModelTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/AudioFileDaoTest.kt
app/src/androidTest/java/com/rehearsall/data/audio/AudioImporterTest.kt
app/src/androidTest/java/com/rehearsall/ui/filelist/FileListScreenTest.kt
```

---

## Phase 3 — Core Playback & MediaSession

**Goal:** Full playback engine as a `MediaLibraryService` with notification controls, lock screen controls, headphone/Bluetooth button support, and repeat modes. Speed control via bottom sheet. This phase lays the foundation for Android Auto (Phase 9) and playlists (Phase 4).

### Why MediaLibraryService Instead of a Plain Singleton
Media3's `MediaLibraryService` gives us all platform integrations from a single component:
- Foreground service → plays when app is backgrounded
- Notification with media controls → automatic
- Lock screen controls → automatic
- Headphone/Bluetooth media buttons → automatic via MediaSession
- Android Auto browsing → built-in (content tree added in Phase 9)
- Audio focus → handled by MediaSession automatically
- Repeat modes → built-in (OFF / ONE / ALL)

### Tasks
- [ ] Create `RehearsAllPlaybackService` extending `MediaLibraryService`:
  - Initialize `ExoPlayer` with `AudioAttributes` (USAGE_MEDIA, CONTENT_TYPE_MUSIC)
  - Create `MediaLibrarySession` with the player
  - Configure `MediaSession.Callback` for custom commands (speed control)
  - Set up foreground notification with `MediaStyleNotificationHelper`
  - Handle `onGetLibraryRoot()` and `onGetChildren()` — return empty stubs for now (populated in Phase 9)
  - Validate `MediaBrowser` callers in `onGetLibraryRoot()`: allow own package + Android Auto system packages, reject unknown callers
- [ ] Register service in `AndroidManifest.xml`:
  - `<service>` with `foregroundServiceType="mediaPlayback"`
  - Intent filter for `MediaLibraryService`
  - Intent filter for `android.media.browse.MediaBrowserService` (Android Auto compatibility)
- [ ] Create `PlaybackManager` interface (clean API for the app UI):
  - State: `playbackState: StateFlow<PlaybackState>`, `currentFileId: StateFlow<Long?>`, `repeatMode: StateFlow<RepeatMode>`, `shuffleEnabled: StateFlow<Boolean>`
  - Transport: `play()`, `pause()`, `seekTo()`, `skipForward()`, `skipBackward()`, `skipToNext()`, `skipToPrevious()`
  - Speed: `setSpeed(speed: Float)` — 0.25x to 3.0x, pitch-preserved
  - Queue: `playFile()`, `setQueue()`, `setRepeatMode()`, `setShuffleEnabled()`
  - A-B loop: `setLoopRegion()`, `clearLoopRegion()` (used in later phases)
  - Lifecycle: `release()`
- [ ] Implement `PlaybackManagerImpl`:
  - Binds to `RehearsAllPlaybackService` via `MediaBrowser`
  - Wraps `MediaController` commands
  - Position polling via coroutine (~16ms) while playing
  - Speed control via `player.playbackParameters = PlaybackParameters(speed)`
  - A-B loop check in polling coroutine (position >= endMs → seek to startMs)
  - Expose all state as `StateFlow`
- [ ] Implement repeat modes:
  - `RepeatMode.OFF` → `Player.REPEAT_MODE_OFF`
  - `RepeatMode.ONE` → `Player.REPEAT_MODE_ONE`
  - `RepeatMode.ALL` → `Player.REPEAT_MODE_ALL`
  - Cycle through modes via button in transport bar
- [ ] Wire `PlaybackManager` in `AudioModule`
- [ ] Create `PlaybackViewModel`:
  - Receive `audioFileId` from nav args
  - Load file metadata from repository
  - Load file into `PlaybackManager`, restore last position and speed
  - Expose combined UI state: playback state + file metadata + repeat mode
  - On `onCleared()`: save last position and speed to repository
- [ ] Build `PlaybackScreen` (initial version — no waveform):
  - Top app bar with file name and back navigation
  - Simple horizontal slider/scrubber showing position
  - Time display: current position / total duration (mm:ss format)
  - Transport bar:
    - Skip back button
    - Previous track button
    - Play/Pause (large FAB)
    - Next track button
    - Skip forward button
  - Repeat mode button (cycles OFF → ONE → ALL, icon changes)
  - Shuffle toggle button (highlighted when active)
  - Speed badge in transport bar — shows current speed, tap opens speed bottom sheet
- [ ] Build `MiniPlayer` composable:
  - Persistent bar at bottom of FileList, Playlist, and Settings screens during active playback
  - Shows: track name (marquee if truncated), play/pause button, thin progress bar
  - Tap bar → navigate to PlaybackScreen
  - Hidden when no track is loaded (`currentFileId == null`)
- [ ] Build `SpeedControlBottomSheet`:
  - Current speed display (e.g., "1.00x") — large text
  - Preset chips in a row: 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x
  - Fine-tune: − 0.05x / + 0.05x buttons flanking the display
  - Slider from 0.25x to 3.0x
  - "Reset to 1.0x" text button
- [ ] Verify headphone/Bluetooth controls:
  - Play/pause via headphone button
  - Next/previous via Bluetooth controls
  - Controls work when app is backgrounded
- [ ] Verify notification controls:
  - Play/pause, next, previous visible in notification
  - Notification persists during playback, dismissed when stopped
  - Lock screen shows controls
- [ ] Accessibility: content descriptions on all transport buttons, speed controls

### Tests (Phase 3)
- [ ] `PlaybackManagerImpl` — state transitions: load → playing → paused → seek, speed changes (mock ExoPlayer)
- [ ] `PlaybackManagerImpl` — repeat mode cycling: OFF → ONE → ALL → OFF
- [ ] `PlaybackManagerImpl` — shuffle toggle: enabled/disabled state propagated correctly
- [ ] `PlaybackViewModel` — state emission, position/speed persistence on clear
- [ ] `SpeedControlBottomSheet` — UI test: presets change speed, slider works, fine-tune buttons increment correctly
- [ ] Service binding: verify `MediaBrowser` connects and `MediaController` receives commands

### Key Files Created
```
app/src/main/java/com/rehearsall/playback/RehearsAllPlaybackService.kt
app/src/main/java/com/rehearsall/playback/PlaybackManager.kt
app/src/main/java/com/rehearsall/playback/PlaybackManagerImpl.kt
app/src/main/java/com/rehearsall/playback/PlaybackState.kt
app/src/main/java/com/rehearsall/playback/RepeatMode.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackViewModel.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackScreen.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackUiState.kt
app/src/main/java/com/rehearsall/ui/playback/components/TransportBar.kt
app/src/main/java/com/rehearsall/ui/playback/components/SpeedControlBottomSheet.kt
app/src/main/java/com/rehearsall/ui/common/MiniPlayer.kt
app/src/test/java/com/rehearsall/playback/PlaybackManagerImplTest.kt
app/src/test/java/com/rehearsall/ui/playback/PlaybackViewModelTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/SpeedControlBottomSheetTest.kt
```

---

## Phase 4 — Playlists & Queue

**Goal:** Users can create playlists, manage play queues, and navigate between tracks. Queue visible in a bottom sheet.

### Tasks
- [ ] Define Room entities:
  - `PlaylistEntity` (id, name, createdAt, updatedAt)
  - `PlaylistItemEntity` (id, playlistId FK, audioFileId FK, orderIndex)
- [ ] Create `PlaylistDao` (insert, getAll as Flow, getById, update, delete)
- [ ] Create `PlaylistItemDao` (insert, getAllForPlaylist as Flow, delete, updateOrder, deleteAllForPlaylist)
- [ ] Add entities to database, bump version
- [ ] Create domain models: `Playlist`, `PlaylistItem`, `QueueItem`
- [ ] Create `PlaylistRepository` (interface + impl)
- [ ] Update `PlaybackManager`:
  - `playPlaylist(playlistId, startIndex)` — load playlist items into ExoPlayer as queue
  - `setQueue(items: List<QueueItem>)` — set arbitrary queue
  - `currentQueue: StateFlow<List<QueueItem>>` — expose current queue with now-playing indicator
  - `skipToNext()` / `skipToPrevious()` — navigate queue
- [ ] Create `PlaylistViewModel`:
  - CRUD operations for playlists
  - Add/remove files from playlist
  - Reorder items within playlist
- [ ] Build `PlaylistScreen`:
  - Large top app bar with playlist name (editable)
  - `LazyColumn` of playlist items: track name, duration, drag-to-reorder handles
  - Swipe-to-remove from playlist
  - "Add Tracks" button → opens file picker (from imported files)
  - "Play" FAB — starts playlist playback
  - Delete playlist option in overflow menu
- [ ] Update `FileListScreen`:
  - Add "Playlists" section/tab alongside "All Files"
  - "New Playlist" button
  - Playlist cards: name, track count, total duration
  - Tap playlist → navigate to `Playlist/{playlistId}`
  - Long-press file → "Add to Playlist" option in bottom sheet
- [ ] Build `QueueBottomSheet`:
  - Shows current play queue
  - Highlight currently playing track
  - Tap item → skip to that track
  - Drag-to-reorder (modifies queue on the fly)
  - Swipe-to-remove from queue
  - "Clear Queue" option
- [ ] Add queue icon to playback transport bar (opens `QueueBottomSheet`)
- [ ] "Play All" option on file list — queues all files

### Tests (Phase 4)
- [ ] `PlaylistDao` — CRUD, cascade delete
- [ ] `PlaylistItemDao` — insert, ordering, cascade delete with playlist
- [ ] `PlaylistRepository` — entity ↔ domain mapping, playlist with items
- [ ] `PlaybackManager` — queue: play playlist sets correct queue, skipToNext/Previous, repeat mode with queue, shuffle mode randomizes order
- [ ] `PlaylistViewModel` — create, add items, reorder, delete
- [ ] `QueueBottomSheet` — UI test: current track highlighted, tap skips, reorder works

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/entity/PlaylistEntity.kt
app/src/main/java/com/rehearsall/data/db/entity/PlaylistItemEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/PlaylistDao.kt
app/src/main/java/com/rehearsall/data/db/dao/PlaylistItemDao.kt
app/src/main/java/com/rehearsall/domain/model/Playlist.kt
app/src/main/java/com/rehearsall/domain/model/PlaylistItem.kt
app/src/main/java/com/rehearsall/domain/model/QueueItem.kt
app/src/main/java/com/rehearsall/data/repository/PlaylistRepository.kt
app/src/main/java/com/rehearsall/ui/playlist/PlaylistViewModel.kt
app/src/main/java/com/rehearsall/ui/playlist/PlaylistScreen.kt
app/src/main/java/com/rehearsall/ui/playlist/PlaylistUiState.kt
app/src/main/java/com/rehearsall/ui/playback/components/QueueBottomSheet.kt
app/src/test/java/com/rehearsall/data/repository/PlaylistRepositoryTest.kt
app/src/test/java/com/rehearsall/ui/playlist/PlaylistViewModelTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/PlaylistDaoTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/PlaylistItemDaoTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/QueueBottomSheetTest.kt
```

---

## Phase 5 — Waveform Display

**Goal:** Interactive waveform replaces the simple slider. Supports zoom, scroll, tap-to-seek. Theme-aware colors.

### Tasks
- [ ] Create `WaveformExtractor`:
  - Input: internal file path → Output: `FloatArray` of normalized amplitudes (0.0–1.0)
  - Use `MediaExtractor` + `MediaCodec` to decode audio to PCM
  - For each ~10ms window, compute RMS amplitude
  - Normalize to 0.0–1.0 range
  - Run on `Dispatchers.IO`, report progress via `Flow<Float>`
- [ ] Create `WaveformCache`:
  - Save: serialize `FloatArray` → binary file at `{filesDir}/waveforms/{audioFileId}.waveform`
  - Load: deserialize binary file → `FloatArray` (validate magic bytes + version; reject corrupted cache and re-extract)
  - Delete: remove cache file (called when audio file is deleted)
  - Binary format: 4-byte magic + 4-byte version + 4-byte count + float array (little-endian, native ARM byte order)
- [ ] Create `WaveformRepository`:
  - `getWaveform(audioFileId): Flow<WaveformState>` — emits Loading(progress), Ready(amplitudes), Error
  - Check cache first, extract if not cached
- [ ] Build `WaveformView` composable:
  - Accept: amplitude data, playback position, zoom level, markers, loop region
  - **Drawing (Canvas):**
    - Amplitude bars (vertical lines, height proportional to amplitude, uses `MaterialTheme.colorScheme.primary`)
    - Center line (subtle, `outline` color)
    - Playback cursor (bright vertical line, `primary` with high contrast)
  - **Gestures:**
    - Horizontal scroll via `scrollable` or `transformable`
    - Pinch-to-zoom: change X scale factor (min = full file, max = ~2 sec visible)
    - Tap: map X → positionMs, call `onSeek(positionMs)`
  - **Auto-scroll:** keep cursor visible during playback (centered or 30% from left)
  - **Performance:** only draw visible amplitude bars (skip off-screen)
- [ ] Build `WaveformOverviewBar` composable:
  - Thin minimap showing full file amplitude at fixed small scale
  - Highlighted viewport rectangle showing currently visible region
  - Tap to jump to position, drag viewport to scroll main waveform
  - Only visible when zoomed in (hidden at minimum zoom since the full waveform is already visible)
- [ ] Integrate `WaveformView` into `PlaybackScreen`:
  - Replace simple slider with waveform
  - Keep time display and transport bar below
  - Show `LinearProgressIndicator` while waveform extracts
  - Fallback to simple slider if extraction fails (with retry button)
- [ ] Trigger waveform extraction on file import (background) so it's ready when user opens the file
- [ ] Adaptive layout:
  - Compact: waveform fills width, fixed height
  - Medium/Expanded: waveform can be taller, more detail visible

### Tests (Phase 5)
- [ ] `WaveformExtractor` — extraction produces valid amplitude data for test audio files (instrumented)
- [ ] `WaveformCache` — round-trip: save → load returns identical data
- [ ] `WaveformRepository` — emits Loading → Ready for uncached, Ready immediately for cached
- [ ] `WaveformView` — UI test: renders without crash, tap-to-seek callback fires

### Key Files Created
```
app/src/main/java/com/rehearsall/data/audio/WaveformExtractor.kt
app/src/main/java/com/rehearsall/data/audio/WaveformCache.kt
app/src/main/java/com/rehearsall/data/repository/WaveformRepository.kt
app/src/main/java/com/rehearsall/ui/playback/components/WaveformView.kt
app/src/main/java/com/rehearsall/ui/playback/components/WaveformOverviewBar.kt
app/src/test/java/com/rehearsall/data/audio/WaveformCacheTest.kt
app/src/test/java/com/rehearsall/data/repository/WaveformRepositoryTest.kt
app/src/androidTest/java/com/rehearsall/data/audio/WaveformExtractorTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/WaveformViewTest.kt
```

---

## Phase 6 — Bookmarks

**Goal:** Users can drop, view, rename, delete, and tap bookmarks. Managed via bottom sheet. Visible on waveform.

### Tasks
- [ ] Define Room entity: `BookmarkEntity` (id, audioFileId FK, positionMs, name, createdAt)
- [ ] Create `BookmarkDao` (insert, getAllForFile as Flow, update, delete)
- [ ] Add `BookmarkEntity` to database, bump version
- [ ] Create domain model: `Bookmark`
- [ ] Create `BookmarkRepository` (interface + impl)
- [ ] Update `PlaybackViewModel`:
  - Expose `StateFlow<List<Bookmark>>` for current file
  - `addBookmark()` — creates at current position with default name "Bookmark N"
  - `renameBookmark(id, newName)`, `deleteBookmark(id)`
  - `seekToBookmark(id)`
- [ ] Update `WaveformView`:
  - Render bookmark markers as small triangles above the waveform (`tertiary` color)
  - Tap tolerance zone around markers for easy selection
- [ ] Build Bookmarks tab content (for consolidated Markers bottom sheet):
  - "Add Bookmark" button (prominent, top of tab)
  - `LazyColumn` of bookmarks: name, position (mm:ss), tap-to-seek
  - Swipe-to-delete with undo snackbar
  - Tap bookmark name → inline rename (text field)
  - Empty state when no bookmarks
- [ ] Build `MarkersBottomSheet` with `TabRow` — Bookmarks tab first, Loops and Chunks tabs added in Phases 7 and 8
- [ ] Add markers icon to playback toolbar (opens consolidated Markers bottom sheet)
- [ ] Accessibility: bookmark markers have content descriptions with name and position

### Tests (Phase 6)
- [ ] `BookmarkDao` — CRUD, cascade delete when audio file deleted (Room in-memory)
- [ ] `BookmarkRepository` — entity ↔ domain mapping
- [ ] `PlaybackViewModel` — bookmark add/rename/delete state changes
- [ ] `MarkersBottomSheet` Bookmarks tab — UI test: add shows in list, swipe deletes, tap seeks

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/entity/BookmarkEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/BookmarkDao.kt
app/src/main/java/com/rehearsall/domain/model/Bookmark.kt
app/src/main/java/com/rehearsall/data/repository/BookmarkRepository.kt
app/src/main/java/com/rehearsall/ui/playback/components/MarkersBottomSheet.kt
app/src/main/java/com/rehearsall/ui/playback/components/BookmarkTabContent.kt
app/src/test/java/com/rehearsall/data/repository/BookmarkRepositoryTest.kt
app/src/test/java/com/rehearsall/ui/playback/PlaybackViewModelBookmarkTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/BookmarkDaoTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/MarkersBottomSheetBookmarkTabTest.kt
```

---

## Phase 7 — A-B Looping

**Goal:** Set, adjust, save, and recall loop regions. Visual feedback on waveform. Managed via bottom sheet.

### Tasks
- [ ] Define Room entity: `LoopEntity` (id, audioFileId FK, name, startMs, endMs, createdAt)
- [ ] Create `LoopDao` (insert, getAllForFile as Flow, update, delete)
- [ ] Add `LoopEntity` to database, bump version
- [ ] Create domain model: `Loop`
- [ ] Create `LoopRepository` (interface + impl)
- [ ] Implement A-B loop logic in `PlaybackManager`:
  - `setLoopRegion(startMs, endMs)` — stores region, enables loop checking in polling coroutine
  - When `position >= endMs`, seek to `startMs`
  - Handle `STATE_ENDED` near loop end — seek back and resume
  - `clearLoopRegion()` — disables looping
  - Enforce: `startMs < endMs`, minimum 100ms loop length
  - Expose `loopRegion` in `PlaybackState`
- [ ] Update `PlaybackViewModel`:
  - `setLoopStart()` / `setLoopEnd()` — at current position
  - `clearLoop()`, `saveLoop(name)`, `loadLoop(loopId)`, `deleteLoop(loopId)`
  - `adjustLoopBoundary(isStart: Boolean, newMs: Long)` — for drag adjustment
- [ ] Update `WaveformView`:
  - Semi-transparent overlay between A and B (`primaryContainer` color with alpha)
  - A and B edge markers as draggable handles
  - Long-press-and-drag gesture: sets A at press X, B at release X
  - Drag handles to adjust boundaries
- [ ] Implement loop crossfade in `PlaybackManager`:
  - When crossfade enabled (DataStore pref `LOOP_CROSSFADE`, default `true`):
    - ~50ms before point B: ramp `player.volume` from 1.0 → 0.0
    - After seek to A: ramp `player.volume` from 0.0 → 1.0 over ~50ms
  - When crossfade disabled: hard seek (current behavior)
- [ ] Build Loops tab content (for consolidated Markers bottom sheet):
  - "Set A" / "Set B" / "Clear Loop" buttons in a row
  - Current loop display: A time – B time, duration
  - "Save Loop" button → name input dialog
  - `LazyColumn` of saved loops: name, range (mm:ss – mm:ss), duration
  - Tap saved loop → load and activate
  - Swipe-to-delete saved loops
  - Empty state when no saved loops
- [ ] Add Loops tab to `MarkersBottomSheet` (alongside existing Bookmarks tab)
- [ ] Accessibility: loop region described, A/B handles labeled

### Tests (Phase 7)
- [ ] `LoopDao` — CRUD, cascade delete (Room in-memory)
- [ ] `LoopRepository` — entity ↔ domain mapping
- [ ] `PlaybackManager` — loop: position >= endMs triggers seek to startMs, clearLoop stops looping
- [ ] `PlaybackViewModel` — set A/B, save/load/delete loops
- [ ] `PlaybackManager` — crossfade: volume ramps when crossfade enabled, hard seek when disabled
- [ ] `MarkersBottomSheet` Loops tab — UI test: set A/B enables save, tap saved loop loads it

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/entity/LoopEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/LoopDao.kt
app/src/main/java/com/rehearsall/domain/model/Loop.kt
app/src/main/java/com/rehearsall/data/repository/LoopRepository.kt
app/src/main/java/com/rehearsall/ui/playback/components/LoopTabContent.kt
app/src/test/java/com/rehearsall/data/repository/LoopRepositoryTest.kt
app/src/test/java/com/rehearsall/playback/PlaybackManagerLoopTest.kt
app/src/test/java/com/rehearsall/ui/playback/PlaybackViewModelLoopTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/LoopDaoTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/MarkersBottomSheetLoopTabTest.kt
```

---

## Phase 8 — Chunked Repetition

**Goal:** Full memorization practice system with three modes. Chunk management via Chunks tab in the consolidated markers sheet, practice controls in a separate bottom sheet.

### Tasks
- [ ] Define Room entities:
  - `ChunkMarkerEntity` (id, audioFileId FK, positionMs, label, createdAt) — **no orderIndex**; always sorted by `positionMs`
  - `PracticeSettingsEntity` (audioFileId PK, repeatCount, gapBetweenRepsMs, gapBetweenChunksMs, selectedMode)
- [ ] Create `ChunkMarkerDao`, `PracticeSettingsDao`
- [ ] Add entities to database, bump version
- [ ] Create domain models:
  - `ChunkMarker` (id, positionMs, label) — no orderIndex; sorted by `positionMs`
  - `PracticeMode` enum: `SINGLE_CHUNK_LOOP`, `CUMULATIVE_BUILD_UP`, `SEQUENTIAL_PLAY`
  - `PracticeSettings` (repeatCount, gapBetweenRepsMs, gapBetweenChunksMs, mode)
  - `PracticeStep` (startMs, endMs, label, repeatCount, chunkRange)
- [ ] Create `ChunkMarkerRepository`, `PracticeSettingsRepository`
- [ ] Implement step generation (pure functions, highly testable):
  - `generateSingleChunkSteps(chunks, repeatCount): List<PracticeStep>`
  - `generateCumulativeBuildUpSteps(chunks, repeatCount): List<PracticeStep>`
  - `generateSequentialSteps(chunks): List<PracticeStep>`
- [ ] Implement `ChunkedPracticeEngine`:
  - `startPractice(steps, settings)` — launches coroutine iterating through steps
  - For each step: set loop region, count reps by detecting position jumps, pause between reps
  - `skipToNextStep()`, `skipToPreviousStep()`, `stopPractice()`
  - Expose `StateFlow<PracticeState>`: Idle, Playing(step, rep, total), Pausing, Complete
- [ ] Update `PlaybackViewModel` with chunk/practice controls:
  - Add/remove chunk markers, reposition by dragging on waveform
  - Start/stop practice with selected mode and settings
  - Expose chunk markers and practice state
- [ ] Update `WaveformView`:
  - Chunk markers as numbered vertical lines below waveform (`secondary` color)
  - Alternating subtle background colors between markers to visualize chunks
  - Active chunk highlight during practice (`secondaryContainer` with alpha)
  - Drag chunk markers on waveform to adjust position (updates `positionMs`)
- [ ] Build Chunks tab content (for consolidated Markers bottom sheet):
  - "Add Chunk Marker" button (at current position)
  - `LazyColumn` of chunk markers: number, label, position (mm:ss) — sorted by position, no drag-to-reorder (position determines order)
  - Swipe-to-delete
  - Tap marker → seek to position
  - "Start Practice" button at bottom → opens practice controls sheet
- [ ] Add Chunks tab to `MarkersBottomSheet` (alongside Bookmarks and Loops tabs)
- [ ] Build `PracticeControlsBottomSheet`:
  - Mode selector: segmented button row (Single Chunk / Build-Up / Sequential)
  - Brief description of selected mode
  - Repeat count stepper (1–20, default 3)
  - Gap between reps slider (0–5s)
  - Gap between chunks slider (0–10s)
  - "Start" / "Stop" button
  - During practice: current step label, rep counter ("Rep 2/3"), progress through steps, animated progress bar
  - Skip prev/next step buttons during practice
- [ ] Verify Chunks tab appears in existing `MarkersBottomSheet` (opened via markers icon added in Phase 6)
- [ ] Accessibility: chunk markers labeled, practice state announced

### Tests (Phase 8)
- [ ] Step generation — single chunk: correct steps for N chunks
- [ ] Step generation — cumulative build-up: correct step count (2N−1), correct ranges
- [ ] Step generation — sequential: correct step count, one rep each
- [ ] `ChunkedPracticeEngine` — full state machine: Idle → Playing → Pausing → Playing → Complete
- [ ] `ChunkedPracticeEngine` — skip forward/backward during practice
- [ ] `ChunkMarkerDao` — CRUD, ordering by positionMs, cascade delete
- [ ] `PracticeSettingsDao` — insert-or-update, getForFile
- [ ] `PlaybackViewModel` — chunk add/remove, practice start/stop
- [ ] `MarkersBottomSheet` Chunks tab — UI test: add marker, list sorted by position
- [ ] `PracticeControlsBottomSheet` — UI test: mode selection, start/stop visibility

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/entity/ChunkMarkerEntity.kt
app/src/main/java/com/rehearsall/data/db/entity/PracticeSettingsEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/ChunkMarkerDao.kt
app/src/main/java/com/rehearsall/data/db/dao/PracticeSettingsDao.kt
app/src/main/java/com/rehearsall/domain/model/ChunkMarker.kt
app/src/main/java/com/rehearsall/domain/model/PracticeMode.kt
app/src/main/java/com/rehearsall/domain/model/PracticeStep.kt
app/src/main/java/com/rehearsall/domain/model/PracticeSettings.kt
app/src/main/java/com/rehearsall/domain/usecase/GeneratePracticeStepsUseCase.kt
app/src/main/java/com/rehearsall/data/repository/ChunkMarkerRepository.kt
app/src/main/java/com/rehearsall/data/repository/PracticeSettingsRepository.kt
app/src/main/java/com/rehearsall/playback/ChunkedPracticeEngine.kt
app/src/main/java/com/rehearsall/playback/PracticeState.kt
app/src/main/java/com/rehearsall/ui/playback/components/ChunkTabContent.kt
app/src/main/java/com/rehearsall/ui/playback/components/PracticeControlsBottomSheet.kt
app/src/test/java/com/rehearsall/domain/usecase/GeneratePracticeStepsUseCaseTest.kt
app/src/test/java/com/rehearsall/playback/ChunkedPracticeEngineTest.kt
app/src/test/java/com/rehearsall/data/repository/ChunkMarkerRepositoryTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/ChunkMarkerDaoTest.kt
app/src/androidTest/java/com/rehearsall/data/db/dao/PracticeSettingsDaoTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/MarkersBottomSheetChunkTabTest.kt
app/src/androidTest/java/com/rehearsall/ui/playback/PracticeControlsBottomSheetTest.kt
```

---

## Phase 9 — Android Auto

**Goal:** Full Android Auto media browsing — browse files and playlists from the car head unit, play audio, navigate queue, play saved loops. Voice search support.

**Dependencies:** Phase 3 (playback service), Phase 4 (playlists/queue), Phase 7 (A-B looping — needed for loop-aware browsing).

### How Android Auto Works
Android Auto does NOT display your Compose UI. It renders its own standard media player UI. Your app provides:
1. A **browsable content tree** via `MediaLibrarySession.Callback`
2. **Playback controls** via `MediaSession`
3. **Custom actions** on the Now Playing screen (e.g., loop toggle)
4. **Search results** via `onSearch()` callback

### Tasks
- [ ] Implement content tree in `RehearsAllPlaybackService`:
  - `onGetLibraryRoot()` — return root `MediaItem` with children IDs
  - `onGetChildren()` — return children based on parent ID:
    - `"root"` → [Recent, All Files, Playlists]
    - `"recent"` → last 20 played files as playable `MediaItem`s
    - `"all_files"` → all imported files:
      - Files **without** saved loops → directly **playable**
      - Files **with** saved loops → **browsable** (tap to expand)
    - `"file:{id}"` (for files with loops) → children:
      - `"file:{id}:full"` → "Full Track" (playable, plays entire file)
      - `"file:{id}:loop:{loopId}"` → loop name (playable, activates A-B loop)
    - `"playlists"` → all playlists as browsable `MediaItem`s
    - `"playlist:{id}"` → files in that playlist as playable `MediaItem`s
  - Each `MediaItem` includes `MediaMetadata` (title, duration, artwork)
- [ ] Implement loop playback from Auto:
  - When a loop `MediaItem` is selected (e.g., `"file:123:loop:7"`):
    - Load the file into ExoPlayer
    - Seek to loop start position
    - Activate A-B loop region via `PlaybackManager.setLoopRegion()`
    - Playback continuously loops within the region
  - When "Full Track" is selected (e.g., `"file:123:full"`):
    - Load the file normally, no loop region
- [ ] Implement custom "Loop On/Off" action on Now Playing screen:
  - Register `SessionCommand("ACTION_TOGGLE_LOOP")` as a custom action
  - Add to `MediaSession.setCustomLayout()` — only shown when current file has saved loops or an active loop
  - Icon toggles between loop-on and loop-off states
  - Handle in `MediaSession.Callback.onCustomCommand()`:
    - If loop is active → `clearLoopRegion()`, update icon to loop-off
    - If loop is inactive and file has a default/last-used loop → `setLoopRegion()`, update icon to loop-on
- [ ] Implement `onSearch()` callback:
  - Search by file name / playlist name
  - Return matching `MediaItem`s as playable results
  - Enables voice: "Hey Google, play [name] on RehearsAll"
- [ ] Implement `onGetItem()` — return a single `MediaItem` by ID
- [ ] Generate artwork for `MediaItem`s:
  - Use a solid-color thumbnail with the waveform silhouette (or just app icon)
  - Cache generated artwork bitmaps
- [ ] Add `<automotiveApp>` declaration in `automotive_app_desc.xml`:
  - `<uses name="media"/>`
- [ ] Reference in `AndroidManifest.xml`:
  - `<meta-data android:name="com.google.android.gms.car.application" android:resource="@xml/automotive_app_desc"/>`
- [ ] Handle item selection:
  - File tapped → set as current playback (no loop)
  - Loop tapped → set as current playback with loop region active
  - Playlist tapped → load playlist into queue, start playback
- [ ] Test with Android Auto Desktop Head Unit (DHU) or real car:
  - Browse content tree: Recent, All Files, Playlists
  - Tap file without loops → plays normally
  - Tap file with loops → expands to show "Full Track" + saved loops
  - Tap a saved loop → plays looped region
  - Toggle loop on/off via custom action on Now Playing
  - Tap to play a playlist
  - Next/previous track in queue
  - Voice search: "Play [file name]"
  - Verify playback continues when navigating in Auto

### Tests (Phase 9)
- [ ] Content tree — root returns correct children IDs
- [ ] Content tree — "all_files" returns files; files with loops are browsable, without are playable
- [ ] Content tree — "file:{id}" returns "Full Track" + saved loops with correct metadata
- [ ] Content tree — "playlist:{id}" returns correct files in order
- [ ] Loop playback — selecting loop MediaItem activates correct loop region
- [ ] Loop playback — selecting "Full Track" plays without loop
- [ ] Custom action — toggle loop on/off changes loop state
- [ ] Search — returns matching files and playlists by name

### Key Files Created
```
app/src/main/res/xml/automotive_app_desc.xml
app/src/main/java/com/rehearsall/playback/ContentTreeBuilder.kt
app/src/main/java/com/rehearsall/playback/MediaItemMapper.kt
app/src/main/java/com/rehearsall/playback/LoopActionHandler.kt
app/src/test/java/com/rehearsall/playback/ContentTreeBuilderTest.kt
app/src/test/java/com/rehearsall/playback/MediaItemMapperTest.kt
app/src/test/java/com/rehearsall/playback/LoopActionHandlerTest.kt
```

---

## Phase 10 — Settings & Preferences

**Goal:** User preferences for theme, skip increment, and other settings. Stored in DataStore.

### Tasks
- [ ] Create `UserPreferencesRepository`:
  - Uses DataStore Preferences
  - `themeMode: Flow<ThemeMode>` (LIGHT, DARK, SYSTEM — default SYSTEM)
  - `skipIncrementMs: Flow<Long>` (default 5000, options: 2000, 5000, 10000, 15000, 30000)
  - `loopCrossfade: Flow<Boolean>` (default `true`)
  - Setter functions for each preference
- [ ] Wire in `PreferencesModule`
- [ ] Build `SettingsScreen`:
  - Large top app bar with "Settings" title
  - Theme section:
    - "Appearance" header
    - Radio buttons: Light / Dark / Follow System
    - Preview of current theme (small color swatches)
  - Playback section:
    - "Skip Increment" — dropdown or radio: 2s, 5s, 10s, 15s, 30s
    - "Loop Crossfade" — toggle switch (default on). Brief description: "Smooth volume fade when looping"
  - About section:
    - App version
    - "RehearsAll" with brief description
- [ ] Update `MainActivity` to observe `themeMode` from DataStore and pass to `RehearsAllTheme`
- [ ] Update `PlaybackManager.skipForward/skipBackward` to use configurable increment
- [ ] Accessibility: all settings labeled, radio groups properly grouped

### Tests (Phase 10)
- [ ] `UserPreferencesRepository` — read/write theme mode, skip increment (instrumented, DataStore)
- [ ] `SettingsScreen` — UI test: theme change reflected, skip increment persists
- [ ] `PlaybackManager` — skip uses configurable increment
- [ ] `UserPreferencesRepository` — loopCrossfade preference read/write and toggle behavior

### Key Files Created
```
app/src/main/java/com/rehearsall/data/preferences/UserPreferencesRepository.kt
app/src/main/java/com/rehearsall/domain/model/ThemeMode.kt
app/src/main/java/com/rehearsall/ui/settings/SettingsScreen.kt
app/src/main/java/com/rehearsall/ui/settings/SettingsViewModel.kt
app/src/test/java/com/rehearsall/ui/settings/SettingsViewModelTest.kt
app/src/androidTest/java/com/rehearsall/data/preferences/UserPreferencesRepositoryTest.kt
app/src/androidTest/java/com/rehearsall/ui/settings/SettingsScreenTest.kt
```

---

## Phase 11 — Production Polish

**Goal:** Handle all edge cases, accessibility, performance. App is reliable and production-ready.

### Tasks
- [ ] **Resume playback position:**
  - On leaving PlaybackScreen (or app backgrounded): save current position + speed
  - On opening a file: restore last position and speed
  - Verify with process death and restart
- [ ] **Configuration changes:**
  - Verify no playback interruption on rotation
  - Verify theme switch doesn't lose playback state
  - Verify bottom sheet state survives config change
- [ ] **Practice progress persistence:**
  - On process kill during active practice: save current step index and rep to Room
  - On reopening file with saved practice state: offer to resume from where they left off
  - Clear saved state when practice completes normally
- [ ] **Error states:**
  - File not found (deleted externally) — detect, show error, offer removal from list
  - Corrupt audio file — show error snackbar, log via Timber
  - Waveform extraction failure — fallback to slider, show retry
  - Database migration failure — log, show error
  - Service connection failure — retry with exponential backoff
- [ ] **Performance:**
  - Profile waveform rendering — ensure 60fps during scroll/zoom
  - Lazy loading for file list with large libraries
  - Efficient Canvas drawing (skip off-screen bars, use `drawPoints` for dense data)
- [ ] **Accessibility audit:**
  - All interactive elements have content descriptions
  - Screen reader navigation order is logical
  - Touch targets ≥ 48dp
  - Color contrast meets WCAG AA
  - Custom announcements for practice state changes
- [ ] **Edge-to-edge display:**
  - Proper insets handling for status bar, navigation bar, cutouts
  - Bottom sheets respect navigation bar
- [ ] **Predictive back:**
  - `PredictiveBackHandler` on PlaybackScreen
  - Confirm exit if practice session is active
- [ ] **Lifecycle cleanup:**
  - Service stops when all clients unbind and playback is stopped
  - Cancel waveform extraction coroutines when leaving screen
- [ ] **Timber logging audit:**
  - Log: file import, playback start/stop, loop set, practice start/stop, Auto connection
  - Log non-fatal exceptions with context
  - Verify `FileLoggingTree` rotates/caps log file size in release builds

### Tests (Phase 11)
- [ ] Process death: verify state restoration (instrumented)
- [ ] End-to-end: import file → play → set bookmarks → A-B loop → chunk markers → practice (instrumented)
- [ ] End-to-end: create playlist → play playlist → queue controls → next/previous (instrumented)
- [ ] Notification: verify controls visible during playback, dismissed when stopped (instrumented)

### Key Files Created
```
app/src/androidTest/java/com/rehearsall/EndToEndTest.kt
app/src/androidTest/java/com/rehearsall/PlaylistEndToEndTest.kt
```

---

## Phase 12 — CI/CD & Release

**Goal:** Automated build and test pipeline. Signed release APK generation.

### Tasks
- [ ] Create `.github/workflows/build-and-test.yml`:
  - Trigger: push to `main`, pull requests
  - Steps: checkout, JDK 21, Android SDK, cache Gradle, lint, unit tests, build debug APK
  - Upload test results + APK as artifacts
- [ ] Create `.github/workflows/release.yml`:
  - Trigger: tag push (`v*`)
  - Steps: full test suite, build release APK (signed), create GitHub Release with APK
- [ ] Set up signing:
  - Generate release keystore
  - Configure `signingConfigs` in `build.gradle.kts`
  - Add keystore credentials as GitHub encrypted secrets
- [ ] Configure ProGuard/R8:
  - Enable in release build type
  - Keep rules for Room, Hilt, Media3, MediaLibraryService
  - Test release build — verify no runtime crashes from minification
- [ ] Add `LICENSE` file (Apache License 2.0)
- [ ] Create `README.md`:
  - App icon and name header
  - One-paragraph description: what it does, who it's for
  - Feature highlights (bullet list with screenshots/GIFs inline)
  - Screenshots: file list, playback screen, waveform zoomed, markers sheet, practice mode, Android Auto, light/dark themes
  - Build instructions (prerequisites: Android Studio, JDK 21, SDK 36)
  - Architecture overview (brief, link to ARCHITECTURE.md for details)
  - Download / install instructions for sideloading
  - Android Auto setup instructions
  - License badge and link
- [ ] Final verification:
  - Import each supported format (MP3, WAV, OGG, FLAC, M4A)
  - Full workflow on phone: import → play → bookmarks → A-B loop → chunks → practice → playlists
  - Full workflow on tablet: verify adaptive layout
  - Headphone controls: play/pause/next/prev
  - Notification + lock screen controls
  - Android Auto: browse, play, voice search (via DHU)
  - Kill app and restart → verify all data persists
  - Rotate during playback → no interruption
  - Test all three theme modes
  - Run full test suite, all green

### Key Files Created
```
.github/workflows/build-and-test.yml
.github/workflows/release.yml
LICENSE
README.md
```

---

## Phase 13 — App Icon

**Goal:** Implement the selected icon concept (Waveform Loop v5) as an Android adaptive icon.

### Tasks
- [ ] Convert `concept1-waveform-loop-v5.svg` to Android vector drawable (foreground layer)
- [ ] Create monochrome variant (for themed icons on Android 13+)
- [ ] Set background color (Material You surface or brand color)
- [ ] Configure adaptive icon in `mipmap-anydpi-v26`:
  - `ic_launcher.xml` — foreground + background layers
  - `ic_launcher_round.xml` — same, for round icon
- [ ] Add monochrome layer for API 33+ themed icons
- [ ] Generate fallback raster icons for older launchers (mipmap-mdpi through xxxhdpi)
- [ ] Configure in `AndroidManifest.xml`

### Key Files Created
```
app/src/main/res/drawable/ic_launcher_foreground.xml
app/src/main/res/drawable/ic_launcher_monochrome.xml
app/src/main/res/values/ic_launcher_background.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
app/src/main/res/mipmap-anydpi-v33/ic_launcher.xml (monochrome)
```

---

## Phase 14 — End-User Documentation

**Goal:** Comprehensive user-facing documentation with screenshots, so anyone can understand and use the app without hand-holding.

### Tasks
- [ ] Capture screenshots for all key screens and states:
  - File list (with files, empty state)
  - Playback screen with waveform (light theme, dark theme)
  - Waveform zoomed in with overview bar visible
  - Markers bottom sheet — each tab (Bookmarks, Loops, Chunks)
  - Practice mode in progress (showing step/rep counter)
  - Speed control bottom sheet
  - Queue bottom sheet
  - Playlist screen
  - Settings screen
  - Mini player bar
  - Android Auto browse tree and Now Playing screen
  - Notification media controls
- [ ] Create `docs/` directory for user documentation
- [ ] Write `docs/USER_GUIDE.md` — full feature walkthrough:
  - **Getting Started:** importing audio files (SAF picker), supported formats
  - **Playing Audio:** transport controls, seek, skip, speed control
  - **Waveform:** zooming, scrolling, tap-to-seek, overview bar
  - **Bookmarks:** adding, renaming, deleting, navigating
  - **A-B Looping:** setting loop points (tap and long-press-drag), saving/loading loops, crossfade toggle
  - **Chunked Practice:** placing chunk markers, choosing a mode (with explanation of each), configuring reps/gaps, running practice, skip controls
  - **Playlists:** creating, adding tracks, reordering, playing
  - **Queue & Repeat:** queue management, repeat modes, shuffle
  - **Android Auto:** what works, how to browse, loop toggle action
  - **Settings:** theme, skip increment, loop crossfade
  - **Tips & Tricks:** keyboard shortcuts (if any), resume behavior, file rename
  - Each section includes inline screenshots
- [ ] Write `docs/PRACTICE_MODES.md` — deep dive on the three practice modes:
  - Single Chunk Loop: when to use, how it works
  - Cumulative Build-Up: step-by-step example with diagrams, ideal for memorization
  - Sequential Play: when to use, comparison with A-B looping
- [ ] Add screenshots to `docs/screenshots/` directory (PNG, labeled by feature)
- [ ] Write `docs/SECURITY.md` — security hardening overview mapped to OWASP Mobile Top 10 (2024):
  - **Introduction:** brief explanation of OWASP Mobile Top 10 and why it matters even for a local-only app
  - **M1 — Improper Credential Usage:** N/A — no credentials, no authentication, no API keys
  - **M2 — Inadequate Supply Chain Security:** dependencies pinned via Gradle version catalog; Dependabot or Renovate recommended for updates; no third-party SDKs with broad permissions
  - **M3 — Insecure Authentication/Authorization:** N/A — single-user local app, no auth
  - **M4 — Insufficient Input Validation:** file import validates format, size (500MB cap), and uses UUID renaming to prevent path traversal; Room parameterized queries prevent SQL injection; waveform cache validated via magic bytes + version on load
  - **M5 — Insecure Communication:** N/A — app makes zero network calls; no telemetry, analytics, or cloud sync
  - **M6 — Inadequate Privacy Controls:** no PII collected; logging in release builds uses IDs and event types only — no file paths, URIs, or user-entered names; backup rules exclude databases, audio, cache, and logs
  - **M7 — Insufficient Binary Protections:** R8 minification enabled for release builds; `isDebuggable = false` in release config
  - **M8 — Security Misconfiguration:** all files in Android app sandbox (`context.filesDir`), no world-readable storage, no exported content providers; MediaBrowser caller validation rejects unknown packages; backup exclusion rules configured
  - **M9 — Insecure Data Storage:** all user data in app-private internal storage (sandboxed); encryption at rest delegated to Android's File-Based Encryption (FBE); no external storage usage
  - **M10 — Insufficient Cryptography:** N/A — no custom cryptography; FBE handles at-rest encryption at the OS level
- [ ] Link user guide from README.md

### Key Files Created
```
docs/USER_GUIDE.md
docs/PRACTICE_MODES.md
docs/SECURITY.md
docs/screenshots/               # All captured screenshots
```

---

## Dependency Graph

```
Phase 1 (Scaffolding)
  ├── Phase 2 (Import & File List)
  │     ├── Phase 3 (Core Playback & MediaSession)
  │     │     ├── Phase 4 (Playlists & Queue)
  │     │     └── Phase 5 (Waveform)
  │           ├── Phase 6 (Bookmarks)
  │           └── Phase 7 (A-B Looping)
  │                 ├── Phase 8 (Chunked Repetition)
  │                 └── Phase 9 (Android Auto) ← also needs Phase 3, 4
  ├── Phase 10 (Settings & Preferences)
  └── Phase 13 (App Icon)
                    │
         Phases 2–10, 13 all complete
                    │
              Phase 11 (Polish)
                    │
              Phase 12 (CI/CD & Release)
                    │
              Phase 14 (End-User Documentation)
```

Phases that can proceed in parallel:
- Phase 10 and Phase 13 can start immediately after Phase 1 (independent of Phases 2–9)
- Phase 6 and Phase 7 (after Phase 5)
- Phase 9 can start after Phase 3 + 4 + 7

---

## File Count Estimate
- **~80 Kotlin source files** (main)
- **~35 test files** (unit + instrumented)
- **3 Gradle build files** + properties
- **2 GitHub Actions workflows**
- **1 AndroidManifest.xml**
- **~10 resource files** (icons, themes, automotive_app_desc, backup rules)
- **4 documentation files** (README, USER_GUIDE, PRACTICE_MODES, SECURITY) + screenshots
- **1 LICENSE file**
- **~140 files total** (excluding screenshots)
