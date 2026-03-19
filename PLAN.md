# RehearsAll — Implementation Plan

## Phase Overview

| Phase | Name | Description | Dependencies |
|-------|------|-------------|-------------|
| 1 | Project Scaffolding | Gradle, Hilt, Room, Media3, theme, navigation shell | None |
| 2 | Audio Import & File List | SAF picker, copy to internal storage, list screen | Phase 1 |
| 3 | Core Playback | ExoPlayer wrapper, transport controls, speed control | Phase 1 |
| 4 | Waveform | Extract, cache, render, zoom, seek | Phase 3 |
| 5 | Bookmarks | Drop, display, rename, delete, persist | Phase 3, 4 |
| 6 | A-B Looping | Set points, loop, adjust, save/load | Phase 3, 4 |
| 7 | Chunked Repetition | Markers, practice engine, three modes | Phase 6 |
| 8 | Persistence & Polish | Resume position, audio focus, edge cases | Phase 2–7 |

---

## Phase 1 — Project Scaffolding

**Goal:** Bootable app with all dependencies wired, blank screens navigable.

### Tasks
- [ ] Create Android project via Android Studio (or manually):
  - Package: `com.rehearsall`
  - Min SDK 26, Target SDK 35
  - Kotlin DSL build files
- [ ] Configure `build.gradle.kts` (app) with dependencies:
  - Jetpack Compose BOM (latest stable)
  - Material 3
  - Compose Navigation
  - Media3 ExoPlayer + Media3 Common
  - Room (runtime, compiler/ksp, ktx)
  - Hilt (android, compiler, navigation-compose)
  - Coroutines (core, android)
- [ ] Create `RehearsAllApp` Application class with `@HiltAndroidApp`
- [ ] Create `MainActivity` with `@AndroidEntryPoint`, set Compose content
- [ ] Set up Material 3 theme (`Theme.kt`, `Color.kt`, `Type.kt`)
- [ ] Set up Compose Navigation with two routes: `FileList`, `Playback/{audioFileId}`
- [ ] Create placeholder `FileListScreen` and `PlaybackScreen` composables
- [ ] Create empty Hilt modules: `DatabaseModule`, `AudioModule`, `RepositoryModule`
- [ ] Verify: app builds, launches, navigates between blank screens

### Key Files Created
```
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/rehearsall/RehearsAllApp.kt
app/src/main/java/com/rehearsall/MainActivity.kt
app/src/main/java/com/rehearsall/di/DatabaseModule.kt
app/src/main/java/com/rehearsall/di/AudioModule.kt
app/src/main/java/com/rehearsall/di/RepositoryModule.kt
app/src/main/java/com/rehearsall/ui/theme/Theme.kt
app/src/main/java/com/rehearsall/ui/theme/Color.kt
app/src/main/java/com/rehearsall/ui/theme/Type.kt
app/src/main/java/com/rehearsall/ui/navigation/NavGraph.kt
app/src/main/java/com/rehearsall/ui/navigation/Screen.kt
app/src/main/java/com/rehearsall/ui/filelist/FileListScreen.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackScreen.kt
```

---

## Phase 2 — Audio Import & File List

**Goal:** Users can import audio files and see them in a list. Files are copied to internal storage and metadata is persisted in Room.

### Tasks
- [ ] Define Room entities: `AudioFileEntity` (id, fileName, displayName, internalPath, format, durationMs, fileSizeBytes, importedAt, lastPlayedAt, lastPositionMs, lastSpeed)
- [ ] Create `AudioFileDao` (insert, getAll as Flow, getById, delete, updateLastPlayed, updateLastPosition, updateLastSpeed)
- [ ] Create `RehearsAllDatabase` with `AudioFileEntity`
- [ ] Wire database in `DatabaseModule`
- [ ] Create `AudioFileRepository` (interface + impl)
- [ ] Create `AudioImporter` utility:
  - Accept URI from SAF
  - Copy file to `{context.filesDir}/audio/{uuid}.{ext}`
  - Extract metadata: file name, format, duration (via `MediaMetadataRetriever`), file size
  - Insert `AudioFileEntity` into Room
  - Return `Result<AudioFile>`
- [ ] Create domain model: `AudioFile` (id, displayName, format, durationMs, fileSizeBytes, importedAt)
- [ ] Create `FileListViewModel`:
  - Expose `StateFlow<FileListUiState>` (loading, files list, error)
  - `importFile(uri: Uri)` — calls importer
  - `deleteFile(id: Long)` — removes from DB + deletes internal file + waveform cache
- [ ] Build `FileListScreen`:
  - Top app bar with title "RehearsAll" and import FAB (or top-bar action)
  - Launch SAF picker (`ACTION_OPEN_DOCUMENT`, MIME types for supported formats)
  - `LazyColumn` of file cards: display name, duration (formatted), format badge
  - Swipe-to-delete or long-press context menu for delete
  - Tap a file → navigate to `Playback/{audioFileId}`
  - Empty state when no files imported
- [ ] Verify: can import MP3/WAV, see in list, delete, survive app restart

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
```

---

## Phase 3 — Core Playback

**Goal:** Full transport controls with speed adjustment. No waveform yet — use a simple slider for seeking.

### Tasks
- [ ] Create `PlaybackState` data class (positionMs, durationMs, isPlaying, speed, isLooping, loopRegion)
- [ ] Implement `PlaybackManager` (singleton, `@Inject`):
  - Initialize ExoPlayer with audio attributes (`USAGE_MEDIA`, `CONTENT_TYPE_MUSIC`)
  - `loadFile()` — build `MediaItem` from internal file path, prepare, optionally seek to start position
  - Position polling: launch coroutine that emits position every 16ms while playing
  - Speed control: `player.playbackParameters = PlaybackParameters(speed)`
  - Expose `playbackState: StateFlow<PlaybackState>`
  - `release()` for cleanup
- [ ] Wire `PlaybackManager` in `AudioModule`
- [ ] Create `PlaybackViewModel`:
  - Receive `audioFileId` from nav args
  - Load file metadata from repository
  - Load file into `PlaybackManager`
  - Expose combined UI state: playback state + file metadata
  - On `onCleared()`: save last position and speed to repository
- [ ] Build `PlaybackScreen` (initial version — no waveform):
  - Top app bar with file name and back navigation
  - Simple horizontal slider/scrubber showing position
  - Time display: current position / total duration (mm:ss format)
  - Play/Pause button (large, center)
  - Skip back/forward buttons (±5s default)
  - Speed control section:
    - Current speed display (e.g., "1.00x")
    - Preset chips: 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x
    - Fine-tune: − 0.05x / + 0.05x buttons
    - Slider from 0.25x to 3.0x
- [ ] Verify: play/pause, seek via slider, speed changes preserve pitch, skip buttons work

### Key Files Created
```
app/src/main/java/com/rehearsall/playback/PlaybackManager.kt
app/src/main/java/com/rehearsall/playback/PlaybackState.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackViewModel.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackScreen.kt
app/src/main/java/com/rehearsall/ui/playback/PlaybackUiState.kt
app/src/main/java/com/rehearsall/ui/playback/components/TransportControls.kt
app/src/main/java/com/rehearsall/ui/playback/components/SpeedControl.kt
```

---

## Phase 4 — Waveform Display

**Goal:** Interactive waveform replaces the simple slider. Supports zoom, scroll, tap-to-seek.

### Tasks
- [ ] Create `WaveformExtractor`:
  - Input: internal file path → Output: `FloatArray` of normalized amplitudes (0.0–1.0)
  - Use `MediaExtractor` + `MediaCodec` to decode audio to PCM
  - For each ~10ms window, compute RMS amplitude
  - Normalize to 0.0–1.0 range
  - Run on `Dispatchers.IO`, report progress via callback/Flow
- [ ] Create `WaveformCache`:
  - Save: serialize `FloatArray` → binary file at `{filesDir}/waveforms/{audioFileId}.waveform`
  - Load: deserialize binary file → `FloatArray`
  - Delete: remove cache file (called when audio file is deleted)
- [ ] Create `WaveformRepository` (or extend `AudioFileRepository`):
  - `getWaveform(audioFileId): Flow<WaveformState>` — emits Loading(progress), Ready(amplitudes), Error
- [ ] Build `WaveformView` composable:
  - Accept: amplitude data, playback position, zoom level, markers, loop region
  - **Drawing (Canvas):**
    - Draw amplitude bars (vertical lines, height proportional to amplitude)
    - Color: use MaterialTheme primary color
    - Playback cursor: bright vertical line at current position
  - **Gestures:**
    - Horizontal scroll via `scrollable` modifier (or `transformable`)
    - Pinch-to-zoom: change X scale factor (min zoom = full file visible, max zoom = ~2 sec visible)
    - Tap: map X coordinate to audio position, call `onSeek(positionMs)`
  - **Auto-scroll:** During playback, keep cursor visible (centered or at ~30% from left)
  - **Performance:** Only draw visible amplitude bars (skip off-screen data)
- [ ] Integrate `WaveformView` into `PlaybackScreen`:
  - Replace the simple slider with the waveform
  - Keep time display and transport controls below the waveform
  - Show loading indicator while waveform is being extracted
- [ ] Trigger waveform extraction on file import (background) so it's ready when user opens the file
- [ ] Verify: waveform renders, zoom in/out works, tap-to-seek works, cursor tracks playback

### Key Files Created
```
app/src/main/java/com/rehearsall/data/audio/WaveformExtractor.kt
app/src/main/java/com/rehearsall/data/audio/WaveformCache.kt
app/src/main/java/com/rehearsall/data/repository/WaveformRepository.kt
app/src/main/java/com/rehearsall/ui/playback/components/WaveformView.kt
```

---

## Phase 5 — Bookmarks

**Goal:** Users can drop, view, rename, delete, and tap bookmarks on the waveform.

### Tasks
- [ ] Define Room entity: `BookmarkEntity` (id, audioFileId, positionMs, name, createdAt)
- [ ] Create `BookmarkDao` (insert, getAllForFile as Flow, update, delete)
- [ ] Add `BookmarkEntity` to database, increment version with migration
- [ ] Create domain model: `Bookmark` (id, positionMs, name, createdAt)
- [ ] Create `BookmarkRepository`
- [ ] Update `PlaybackViewModel`:
  - Expose `StateFlow<List<Bookmark>>` for current file
  - `addBookmark()` — creates bookmark at current playback position with default name "Bookmark N"
  - `renameBookmark(id, newName)`
  - `deleteBookmark(id)`
  - `seekToBookmark(id)` — calls `playbackManager.seekTo(bookmark.positionMs)`
- [ ] Update `WaveformView`:
  - Render bookmark markers as small triangles/diamonds above the waveform at their positions
  - Different color from chunk markers (use tertiary color)
- [ ] Build `BookmarkPanel` composable (below waveform or in a bottom sheet):
  - "Add Bookmark" button
  - List of bookmarks with: name, position (formatted as mm:ss), tap-to-seek, edit name, delete
  - Rename via inline text field or dialog
- [ ] Verify: add bookmark, see on waveform, tap to seek, rename, delete, survives restart

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/entity/BookmarkEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/BookmarkDao.kt
app/src/main/java/com/rehearsall/domain/model/Bookmark.kt
app/src/main/java/com/rehearsall/data/repository/BookmarkRepository.kt
app/src/main/java/com/rehearsall/ui/playback/components/BookmarkPanel.kt
```

---

## Phase 6 — A-B Looping

**Goal:** Set, adjust, save, and recall loop regions. Visual feedback on waveform.

### Tasks
- [ ] Define Room entity: `LoopEntity` (id, audioFileId, name, startMs, endMs, createdAt)
- [ ] Create `LoopDao` (insert, getAllForFile as Flow, update, delete)
- [ ] Add `LoopEntity` to database with migration
- [ ] Create domain model: `Loop` (id, name, startMs, endMs)
- [ ] Create `LoopRepository`
- [ ] Implement A-B loop logic in `PlaybackManager`:
  - `setLoopRegion(startMs, endMs)` — stores region, enables loop checking
  - In the position polling coroutine: if `loopRegion != null && position >= endMs`, seek to `startMs`
  - `clearLoopRegion()` — disables looping, resumes normal playback
  - Expose `loopRegion` in `PlaybackState`
- [ ] Update `PlaybackViewModel`:
  - `setLoopStart()` — set A point at current position
  - `setLoopEnd()` — set B point at current position, activates loop
  - `clearLoop()` — deactivate loop
  - `saveLoop(name)` — persist current A-B region
  - `loadLoop(loopId)` — recall saved loop and activate it
  - `deleteLoop(loopId)`
  - `adjustLoopStart(newMs)` / `adjustLoopEnd(newMs)` — for drag adjustment
- [ ] Update `WaveformView`:
  - Render loop region as semi-transparent overlay between A and B
  - Render A and B markers as draggable handles on the waveform edges
  - Long-press-and-drag gesture: sets A at press position, B at release position
  - Drag A/B handles to adjust loop boundaries
- [ ] Build `LoopControls` composable:
  - "Set A" / "Set B" / "Clear" buttons
  - Save current loop (with name input dialog)
  - List of saved loops: name, range (formatted), tap to load, delete
- [ ] Verify: set A-B, hear gapless loop, adjust by drag, save/load, long-press-drag gesture

### Key Files Created
```
app/src/main/java/com/rehearsall/data/db/entity/LoopEntity.kt
app/src/main/java/com/rehearsall/data/db/dao/LoopDao.kt
app/src/main/java/com/rehearsall/domain/model/Loop.kt
app/src/main/java/com/rehearsall/data/repository/LoopRepository.kt
app/src/main/java/com/rehearsall/ui/playback/components/LoopControls.kt
```

---

## Phase 7 — Chunked Repetition

**Goal:** Full memorization practice system with three modes.

### Tasks
- [ ] Define Room entities:
  - `ChunkMarkerEntity` (id, audioFileId, positionMs, label, orderIndex, createdAt)
  - `PracticeSettingsEntity` (audioFileId PK, repeatCount, pauseBetweenRepsMs, pauseBetweenChunksMs, selectedMode)
- [ ] Create `ChunkMarkerDao`, `PracticeSettingsDao`
- [ ] Add entities to database with migration
- [ ] Create domain models:
  - `ChunkMarker` (id, positionMs, label, orderIndex)
  - `PracticeMode` enum: `SINGLE_CHUNK_LOOP`, `CUMULATIVE_BUILD_UP`, `SEQUENTIAL_PLAY`
  - `PracticeSettings` (repeatCount, pauseBetweenRepsMs, pauseBetweenChunksMs, mode)
  - `PracticeStep` (startMs, endMs, label, repeatCount)
- [ ] Create `ChunkMarkerRepository`, `PracticeSettingsRepository`
- [ ] Implement `ChunkedPracticeEngine`:
  - `generateSteps(markers, mode, repeatCount): List<PracticeStep>` — pure function, generates the sequence
  - `startPractice(steps)` — launches coroutine that iterates through steps
  - For each step: call `playbackManager.setLoopRegion(start, end)`, count repetitions, pause between reps
  - After N reps: pause (configurable), advance to next step
  - `skipToNextStep()`, `skipToPreviousStep()`
  - `stopPractice()`
  - Expose `StateFlow<PracticeState>`: Idle, Playing(stepIndex, repNumber, totalSteps), Pausing, Complete
- [ ] Update `PlaybackViewModel` with practice controls:
  - Add/remove/reorder chunk markers
  - Start/stop practice with selected mode and settings
  - Expose practice state for UI
- [ ] Update `WaveformView`:
  - Render chunk markers as numbered vertical lines below the waveform
  - Different color from bookmarks (use secondary color)
  - Render chunk regions (alternating subtle background colors between markers)
  - Show current active chunk highlight during practice
- [ ] Build `ChunkedPracticePanel`:
  - Toggle to enter "practice mode" (tab or expandable section)
  - "Add Chunk Marker" button (at current position)
  - List of chunk markers: label, position, reorder handles, delete
  - Mode selector: radio buttons or segmented control for the three modes
  - Settings: repeat count (stepper 1–20), pause between reps (slider 0–5s), pause between chunks (slider 0–10s)
  - "Start Practice" / "Stop Practice" button
  - During practice: show current step, chunk name, repetition counter (e.g., "Chunk 2 — Rep 2/3"), progress bar
  - Skip prev/next chunk buttons during practice
- [ ] Verify: add chunk markers, start each mode, hear correct repetition pattern, cumulative build-up works correctly, skip controls work, settings persist

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
app/src/main/java/com/rehearsall/data/repository/ChunkMarkerRepository.kt
app/src/main/java/com/rehearsall/data/repository/PracticeSettingsRepository.kt
app/src/main/java/com/rehearsall/playback/ChunkedPracticeEngine.kt
app/src/main/java/com/rehearsall/ui/playback/components/ChunkedPracticePanel.kt
```

---

## Phase 8 — Persistence & Polish

**Goal:** Everything persists. Edge cases handled. App feels solid.

### Tasks
- [ ] **Resume playback position:**
  - On leaving PlaybackScreen (or app backgrounded): save current position + speed to Room
  - On opening a file: restore last position and speed
- [ ] **Audio focus handling** (in PlaybackManager):
  - Request focus on play, abandon on pause/stop
  - Pause on `AUDIOFOCUS_LOSS` and `AUDIOFOCUS_LOSS_TRANSIENT`
  - Duck on `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`
  - Resume on `AUDIOFOCUS_GAIN` (only if was playing before loss)
- [ ] **Headphone disconnect:**
  - Register `BroadcastReceiver` for `ACTION_AUDIO_BECOMING_NOISY`
  - Pause playback when headphones disconnected
- [ ] **Configuration changes:**
  - ViewModel + Hilt singleton PlaybackManager survives rotation
  - Verify no playback interruption on rotation
- [ ] **Skip increment setting:**
  - Allow user to configure skip forward/backward increment (default 5s)
  - Store in shared preferences or Room
- [ ] **Error states in UI:**
  - Handle file not found (deleted externally)
  - Handle corrupt audio file
  - Handle waveform extraction failure (show playback screen with fallback slider)
- [ ] **Clean up:**
  - Release ExoPlayer when app is destroyed
  - Cancel waveform extraction coroutines when leaving screen
  - Handle back navigation from practice mode (confirm if practice is active)
- [ ] **Final verification pass:**
  - Import each supported format (MP3, WAV, OGG, FLAC, M4A)
  - Full workflow: import → play → bookmark → A-B loop → chunk markers → practice
  - Kill and restart app → verify all data persisted
  - Rotate during playback → verify no interruption
  - Phone call during playback → verify pause/resume

---

## Dependency Graph

```
Phase 1 (Scaffolding)
  ├── Phase 2 (Import & File List)
  └── Phase 3 (Core Playback)
        └── Phase 4 (Waveform)
              ├── Phase 5 (Bookmarks)
              └── Phase 6 (A-B Looping)
                    └── Phase 7 (Chunked Repetition)
                          └── Phase 8 (Polish)
```

Phases 2 and 3 can proceed in parallel after Phase 1.
Phases 5 and 6 can proceed in parallel after Phase 4.

---

## Estimated File Count
- **~45 Kotlin source files** across all phases
- **3 Gradle build files** (project, app, settings)
- **1 AndroidManifest.xml**
- **~50 files total**
