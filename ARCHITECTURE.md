# RehearsAll — Architecture & Technical Decisions

## 1. Why Jetpack Compose (Not XML)

| Factor | Compose | XML |
|--------|---------|-----|
| Waveform rendering | Custom `Canvas` composable with built-in gesture support (pinch, drag, tap) | Custom `View` with `onDraw` + `GestureDetector` — more boilerplate |
| State management | Natural fit with `StateFlow` → `collectAsState()` | Requires `LiveData` observers or data binding |
| Theming | Material 3 + Dynamic Color out of the box | Material 3 available but more verbose |
| Bottom sheets | `ModalBottomSheet` composable — simple, state-driven | `BottomSheetDialogFragment` — lifecycle-heavy |
| Adaptive layout | `WindowSizeClass` integrates cleanly | Requires resource qualifiers or manual checks |
| Maintainability | Single language (Kotlin), declarative | XML + Kotlin split, imperative updates |
| Custom UI | Compose modifiers + Canvas = flexible custom drawing | More powerful low-level control but more code |
| Testing | Compose testing APIs with semantics-based assertions | Espresso with view matchers — more brittle |

**Decision:** Compose. The waveform is the most complex UI element, and Compose Canvas with gesture modifiers is cleaner than a custom `View`. Bottom sheets, adaptive layouts, and dynamic theming are all first-class in Compose.

## 2. Theming — Material You / Dynamic Color

### How It Works
- **Android 12+ (API 31+):** `dynamicDarkColorScheme()` / `dynamicLightColorScheme()` — derives the entire color palette from the user's wallpaper. The app automatically harmonizes with the system.
- **Android 8–11 (API 26–30):** Falls back to a custom-defined Material 3 color scheme (a handpicked palette that looks good in both light and dark).
- **Three theme modes:** Light / Dark / Follow System (default)
- **Preference stored in DataStore** — lightweight, coroutine-native replacement for SharedPreferences

### Implementation
```kotlin
@Composable
fun RehearsAllTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,  // from DataStore
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        // Dynamic color available (Android 12+)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        // Fallback for older devices
        darkTheme -> DarkColorScheme   // custom palette
        else -> LightColorScheme       // custom palette
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RehearsAllTypography,
        content = content
    )
}
```

### Color Usage Conventions
- **Primary:** Waveform bars, play button, active states
- **Secondary:** Chunk markers, practice mode indicators
- **Tertiary:** Bookmark markers
- **Error:** Destructive actions (delete)
- **Surface variants:** Bottom sheet backgrounds, card backgrounds
- **Outline:** Waveform grid lines, dividers

## 3. Adaptive Layout — Phone & Tablet

### WindowSizeClass
```
Compact  (<600dp)  → Phone portrait  → Single-column, full-width bottom sheets
Medium   (600–840dp) → Phone landscape / small tablet → Side panel for controls
Expanded (>840dp)  → Tablet → Two-pane layout (file list + playback side by side)
```

### Layout Strategy
- **FileListScreen:**
  - Compact: Full-screen list
  - Expanded: List on left (1/3), playback on right (2/3) — list-detail pattern
- **PlaybackScreen:**
  - Compact: Waveform top, controls bottom, consolidated markers sheet (bookmarks/loops/chunks tabs)
  - Medium: Waveform top, controls right of waveform
  - Expanded: Waveform spans full width, control panels as persistent side sheets

### Implementation
Use `calculateWindowSizeClass()` from `material3-window-size-class` artifact. Pass the size class down through composition locals or as parameters.

## 4. Dependency Injection — Hilt

- Standard Android DI with first-class Compose support (`hiltViewModel()`)
- `@Singleton` for PlaybackService binder, Database, Repositories, DataStore
- `@ViewModelScoped` for ViewModel dependencies
- Modules: `DatabaseModule`, `AudioModule`, `RepositoryModule`, `PreferencesModule`
- **Testing:** Hilt test modules for swapping real implementations with fakes/mocks

## 5. Audio Engine — Media3 MediaLibraryService

### Why MediaLibraryService (Not a Plain Singleton)

The original design had `PlaybackManager` as a `@Singleton`. We're upgrading to a `MediaLibraryService` because it provides **all** of the following from a single foundation:

| Feature | Plain Singleton | MediaLibraryService |
|---------|----------------|-------------------|
| Basic playback | Yes | Yes |
| Foreground service (plays when app backgrounded) | Manual | Built-in |
| Notification media controls | Manual | Automatic |
| Lock screen controls | Manual | Automatic |
| Headphone/Bluetooth button controls | Manual BroadcastReceiver | Automatic via MediaSession |
| Android Auto media browsing | Not possible | Built-in via MediaLibrarySession |
| Standard repeat modes | Manual | Built-in (repeat one/all/off) |
| Play queue / next / previous | Manual | Built-in via playlist API |
| Audio focus management | Manual | Automatic |

**One service, all the platform integrations for free.** The trade-off is more setup in Phase 3, but it eliminates entire phases of manual integration work later.

### Architecture

```
┌───────────────────────────────────────────────┐
│ RehearsAllPlaybackService                     │
│ (extends MediaLibraryService)                 │
│                                               │
│  ┌─────────────┐  ┌────────────────────────┐  │
│  │  ExoPlayer   │  │  MediaLibrarySession   │  │
│  │  (playback)  │  │  (media session +      │  │
│  │              │  │   content tree for      │  │
│  │              │  │   Auto/notification)    │  │
│  └─────────────┘  └────────────────────────┘  │
│                                               │
│  ┌──────────────────────────────────────────┐ │
│  │ PlaybackManager (interface)              │ │
│  │ Wraps ExoPlayer + adds custom logic:     │ │
│  │ - A-B looping (position polling)         │ │
│  │ - Speed control (pitch-preserved)        │ │
│  │ - Chunked practice engine                │ │
│  │ - Position/state broadcasting            │ │
│  └──────────────────────────────────────────┘ │
└───────────────────────────────────────────────┘
         ▲                    ▲
         │ bind               │ MediaBrowser
         │                    │
┌────────┴────────┐  ┌───────┴──────────┐
│   App UI        │  │  Android Auto    │
│   (Compose)     │  │  (car head unit) │
└─────────────────┘  └──────────────────┘
```

### PlaybackManager Interface
Still the same clean API surface for the app UI, but now backed by the service:

```kotlin
interface PlaybackManager {
    // State
    val playbackState: StateFlow<PlaybackState>
    val currentFileId: StateFlow<Long?>
    val currentQueue: StateFlow<List<QueueItem>>
    val repeatMode: StateFlow<RepeatMode>  // OFF, ONE, ALL
    val shuffleEnabled: StateFlow<Boolean>

    // Transport
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipForward(ms: Long)
    fun skipBackward(ms: Long)
    fun skipToNext()
    fun skipToPrevious()

    // Speed
    fun setSpeed(speed: Float)  // 0.25f..3.0f

    // Queue
    fun playFile(audioFileId: Long, startPositionMs: Long = 0)
    fun playPlaylist(playlistId: Long, startIndex: Int = 0)
    fun setQueue(items: List<QueueItem>)
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleEnabled(enabled: Boolean)

    // A-B Looping
    fun setLoopRegion(startMs: Long, endMs: Long)
    fun clearLoopRegion()

    // Lifecycle
    fun release()
}

enum class RepeatMode { OFF, ONE, ALL }
```

### MediaSession Command Mapping
Media buttons (headphones, Bluetooth, car) map to standard MediaSession actions:

| Button | Action |
|--------|--------|
| Play/Pause | `play()` / `pause()` |
| Next | `skipToNext()` (next track in queue/playlist) |
| Previous | `skipToPrevious()` (previous track, or seek to start if >3s in) |
| Fast Forward | `skipForward(configuredMs)` |
| Rewind | `skipBackward(configuredMs)` |

### A-B Loop Implementation
- **Do NOT use `ClippingMediaSource`** — it requires rebuilding the source to adjust boundaries
- Instead: poll playback position every ~16ms via coroutine; when `position >= loopEndMs`, seek to `loopStartMs`
- The 16ms polling aligns with frame rate and keeps the overshoot minimal (~1 frame)
- Expose loop region as `StateFlow<LoopRegion?>` for UI to render the highlighted waveform region

### Loop Crossfade
To eliminate the audible "click" when looping back from B to A, the engine applies a brief volume crossfade:
- **Fade-out:** ~50ms before reaching point B, ramp volume from 1.0 → 0.0
- **Seek:** jump to point A
- **Fade-in:** ramp volume from 0.0 → 1.0 over ~50ms

Implementation uses `player.volume` adjustments timed by the polling coroutine. The 50ms duration is imperceptible as a fade but eliminates the discontinuity.

**User-toggleable:** Stored as `LOOP_CROSSFADE` boolean in DataStore Preferences (default: `true`). Some users may prefer the hard cut for precise timing practice. Toggle is in the Settings screen under "Playback".

### Audio Focus & Interruptions
Handled automatically by Media3 `MediaSession` when `AudioAttributes` are set:
- Pause on focus loss (phone call, other app plays audio)
- Duck volume on transient focus loss
- Pause on headphone disconnect (automatic via `MediaSession`)

## 6. Android Auto

### How Android Auto Media Apps Work
Android Auto does NOT display your app's Compose UI on the car screen. Instead:
1. Your app provides a **browsable content tree** via `MediaLibrarySession.Callback`
2. Android Auto renders its own standard media UI using your content tree
3. Playback commands flow through the `MediaSession`

Your app is a content + playback provider, not a UI provider for Auto.

### Content Tree Structure
```
Root
├── "Recent" (recently played files)
│   ├── File A
│   ├── File B
│   └── ...
├── "All Files" (all imported files)
│   ├── File A (playable — no saved loops)
│   ├── File B (browsable — has saved loops)
│   │   ├── "Full Track" (playable — plays entire file)
│   │   ├── "Loop: Verse 1" (playable — plays looped region)
│   │   ├── "Loop: Bridge" (playable — plays looped region)
│   │   └── ...
│   └── ...
└── "Playlists"
    ├── Playlist 1
    │   ├── File X
    │   ├── File Y
    │   └── ...
    ├── Playlist 2
    └── ...
```

**Loop-aware browsing:** Files with saved A-B loops become **browsable** (tapping opens sub-items). The first child is always "Full Track" (plays normally), followed by each saved loop by name. Files without saved loops are directly **playable** (tap to play).

When a loop is selected from Auto:
- Playback starts at the loop's start position
- The A-B loop region is activated (same mechanism as the phone UI)
- Playback continuously loops within the region
- The user can disable the loop via the custom "Loop On/Off" action button (see below)

Each node is a `MediaItem` with:
- `mediaId` — unique identifier (e.g., `"file:123"`, `"playlist:5"`, `"file:123:loop:7"`)
- `MediaMetadata` — title, artist (optional), duration, artwork (album art or generated waveform thumbnail)
- `browsable` flag (for folders/playlists/files-with-loops) or `playable` flag (for files/loops)

### Callback Implementation
```kotlin
override fun onGetLibraryRoot(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    params: LibraryParams?
): ListenableFuture<LibraryResult<MediaItem>> {
    // Return root node with children: Recent, All Files, Playlists
}

override fun onGetChildren(
    session: MediaLibrarySession,
    browser: MediaSession.ControllerInfo,
    parentId: String,
    page: Int,
    pageSize: Int,
    params: LibraryParams?
): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
    // Return children based on parentId
    // "root" → [Recent, All Files, Playlists]
    // "all_files" → list of audio files as playable MediaItems
    // "playlist:5" → files in that playlist as playable MediaItems
}
```

### Custom Actions on Now Playing Screen
Android Auto supports custom action buttons on the Now Playing screen via `MediaSession.setCustomLayout()`:

| Action | Icon | Behavior |
|--------|------|----------|
| **Loop On/Off** | Loop icon (toggles filled/outline) | Toggles the active A-B loop region on/off. When "on", playback loops within the saved region. When "off", plays the full track normally. Only shown when the current file has a loop active or saved loops available. |

Implementation: Use `SessionCommand` with a custom action ID (e.g., `"ACTION_TOGGLE_LOOP"`). Handle in `MediaSession.Callback.onCustomCommand()`.

### What Works on Auto vs. What Doesn't
| Feature | On Auto? | Notes |
|---------|----------|-------|
| Browse files and playlists | Yes | Content tree |
| Browse saved loops per file | Yes | Files with loops expand to show loop sub-items |
| Play a saved loop (looping) | Yes | Select loop from browse tree, or toggle via custom action |
| Toggle loop on/off | Yes | Custom action button on Now Playing screen |
| Play/pause/skip | Yes | MediaSession |
| Next/previous track | Yes | Queue / playlist |
| Speed control | No | Not a standard Auto action — phone only |
| Waveform display | No | Auto renders its own UI |
| Bookmarks | No | Navigation aid, not useful without waveform |
| Chunked practice | No | Phone/tablet only |
| Voice: "Play [playlist name]" | Yes | `onSearch()` callback |

**Android Auto is for listening and loop practice, not full practice mode.** Waveform, markers, and chunked repetition require a touchscreen. But listening to a looped section while driving (e.g., memorizing a verse) is a valid use case.

**Dependency note:** Android Auto (Phase 9) depends on Phase 7 (A-B Looping) for loop-aware browsing, in addition to Phases 3 and 4.

### Automotive OS vs. Android Auto
- **Android Auto:** Phone app projected to car screen. Our `MediaLibraryService` handles this automatically.
- **Android Automotive OS:** Runs natively on the car. Would require a separate module. **Out of scope for now** — Auto is sufficient.

---

## 7. Playlists & Queue

### Data Model
```kotlin
// Room entities — see DATA_MODEL.md for full schema
PlaylistEntity(id, name, createdAt, updatedAt)
PlaylistItemEntity(id, playlistId, audioFileId, orderIndex)
```

### Queue Management
The play queue is ExoPlayer's native playlist. When the user:
- **Taps a file:** Queue = just that file. Repeat mode applies.
- **Plays a playlist:** Queue = all files in the playlist, starting from tapped item.
- **Taps "Play All":** Queue = all imported files.

Queue state is exposed as `StateFlow<List<QueueItem>>` so the UI can show current/next track.

### Repeat Modes
Standard three-mode cycle, exposed via `MediaSession`:
- **Off** — play queue once, then stop
- **Repeat One** — loop current track (uses ExoPlayer's `REPEAT_MODE_ONE`)
- **Repeat All** — loop entire queue (uses ExoPlayer's `REPEAT_MODE_ALL`)

**Note:** "Repeat One" is distinct from A-B looping. Repeat One loops the entire track. A-B loops a specific region within a track.

### Shuffle Mode
ExoPlayer has built-in shuffle support via `player.shuffleModeEnabled = true`. This randomizes queue order without modifying the underlying playlist. Toggled via a shuffle button in the transport bar, exposed through `MediaSession` for notification/Auto controls.

`PlaybackManager` exposes `shuffleEnabled: StateFlow<Boolean>` alongside `repeatMode`.

---

## 8. Embedded Metadata Extraction

### What Gets Extracted
On import, `MediaMetadataRetriever` reads embedded tags from the audio file:
- **Artist** (ID3 `TPE1`, M4A `©ART`, Vorbis `ARTIST`)
- **Title** (ID3 `TIT2`, M4A `©nam`, Vorbis `TITLE`)

These are stored in `AudioFileEntity.artist` and `AudioFileEntity.title` (both nullable — not all files have metadata).

### Display Logic
- **`displayName`** defaults to the embedded title if present, otherwise the file name without extension
- Artist is shown as a subtitle on file list cards and the playback screen (when available)
- User can rename `displayName` via the file details bottom sheet — this overrides the default but doesn't modify the original file

### Why Not Album Art?
Album art extraction (`METADATA_KEY_ALBUMART`) returns a `Bitmap` that would need to be cached to disk. For MVP, the waveform thumbnail serves as the visual identity of each track. Album art can be added later without schema changes.

---

## 9. Waveform Rendering

### Extraction Pipeline
```
Audio File → MediaExtractor → MediaCodec (decode to PCM)
    → Downsample to amplitude array (one value per ~10ms window)
    → Serialize to binary file in internal storage
```

- Run extraction on `Dispatchers.IO` with a progress callback
- Cache file: `{internalDir}/waveforms/{audioFileId}.waveform`
- On subsequent loads, read from cache (fast — just deserialize the amplitude array)
- Typical result: a 5-minute audio file ≈ 30,000 amplitude samples ≈ ~120KB cache file

### Compose Canvas Rendering
```
WaveformView composable:
├── Horizontal scroll (transformable modifier)
├── Pinch-to-zoom (scales the X axis, revealing more/less detail)
├── Tap-to-seek (maps tap X coordinate → audio position)
├── Long-press-drag (sets A-B region in one gesture)
├── Playback cursor (vertical line at current position)
├── Bookmark markers (colored ticks above waveform — tertiary color)
├── Chunk markers (colored ticks below waveform — secondary color)
└── A-B region overlay (semi-transparent highlight)
```

- Zoom level determines how many amplitude samples map to one pixel
- At minimum zoom: entire file visible. At maximum zoom: ~2 seconds visible
- Auto-scroll to keep the playback cursor visible during playback
- **Theme-aware colors:** waveform, markers, and overlays all use MaterialTheme colors — automatically adapt to light/dark/dynamic

### Waveform Overview Bar
When zoomed in, a thin minimap bar appears above or below the main waveform showing the full file:
- Renders the complete amplitude data at a fixed small scale
- Shows a highlighted viewport rectangle indicating the currently visible region
- Tap the overview bar → jump to that position
- Drag the viewport rectangle → scroll the main waveform

This gives spatial context when zoomed in deep, so users always know where they are in the file.

---

## 10. Chunked Practice Engine

### State Machine
```
┌─────────┐
│  Idle   │──── startPractice() ────┐
└─────────┘                         ▼
     ▲                     ┌──────────────┐
     │                     │ PlayingChunk  │◄──── loopBack (rep < N)
     │                     │ (chunk, rep)  │
     │                     └──────┬───────┘
     │                            │ rep == N
     │                            ▼
     │                     ┌──────────────┐
     │                     │   Pausing    │ (configurable gap)
     │                     └──────┬───────┘
     │                            │ timer done
     │                            ▼
     │                     ┌──────────────┐
     │                     │ NextStep     │──── more steps? ──→ PlayingChunk
     │                     └──────┬───────┘
     │                            │ no more steps
     │                            ▼
     └──────────────────── PracticeComplete
```

### Step Sequence Generation

**Single Chunk Loop** — trivial: one step per selected chunk.

**Cumulative Build-Up** — for chunks [1, 2, 3, 4]:
```
Step 1: Play chunk 1 (repeat N)
Step 2: Play chunk 2 (repeat N)
Step 3: Play chunks 1–2 combined (repeat N)
Step 4: Play chunk 3 (repeat N)
Step 5: Play chunks 1–3 combined (repeat N)
Step 6: Play chunk 4 (repeat N)
Step 7: Play chunks 1–4 combined (repeat N)
```

Each "step" maps to a `startMs` and `endMs` derived from chunk marker positions.

**Sequential Play** — play each chunk once. Inter-step gaps are controlled by `PracticeSettings.gapBetweenChunksMs` in the execution loop, not in the step generator.

### Integration with PlaybackManager
- The `ChunkedPracticeEngine` uses `PlaybackManager.setLoopRegion()` for each step
- Tracks repetition count by observing when playback loops back to start
- Manages inter-step pauses with `delay()` coroutines
- Exposed as `StateFlow<PracticeState>` for the UI

## 11. Modern UI Patterns

### Bottom Sheets
Used throughout the app for secondary content that doesn't need its own screen:

| Bottom Sheet | Content | Trigger |
|-------------|---------|---------|
| Speed Control | Presets, slider, fine-tune buttons | Tap speed badge in transport bar |
| **Markers** (3 tabs) | **Bookmarks tab:** list, add/rename/delete. **Loops tab:** saved loops, A-B controls. **Chunks tab:** chunk markers, practice settings. | Markers icon in toolbar |
| Practice Controls | Mode selector, repeat count, gap settings, start/stop | "Practice" button in Chunks tab |
| File Details | Format, size, duration, import date, **rename**, delete | Long-press file in list |
| Queue / Now Playing | Current queue, drag to reorder, remove | Queue icon in transport bar |
| Theme Picker | Light / Dark / System radio buttons | Settings icon |

**Why consolidate bookmarks, loops, and chunks into one "Markers" bottom sheet?** All three are position markers on the same waveform. Three separate bottom sheets means three separate toolbar icons and three separate mental models. A single sheet with tabs is discoverable, reduces toolbar clutter, and lets users switch between marker types without closing and reopening sheets.

Implementation: Material 3 `ModalBottomSheet` with `rememberModalBottomSheetState()`. Each tab is a composable within a `TabRow`. Sheet visibility controlled by ViewModel state.

### Mini Player
A persistent bar at the bottom of all screens (file list, playlists, settings) whenever audio is playing or paused:
- Shows: track name, play/pause button, progress bar
- Tap the bar → navigate to full PlaybackScreen
- Swipe up → expand to full PlaybackScreen (optional gesture)
- Disappears when playback is stopped (no active track)

Implementation: A composable in the root `Scaffold`, above the navigation bar. Observes `PlaybackManager.playbackState` and `currentFileId`. Only rendered when `currentFileId != null`.

### Navigation
- **Compact (phone):** Three screens — FileList, Playback, Settings — standard Compose Navigation with animated transitions
- **Expanded (tablet):** List-detail layout — both visible simultaneously, no navigation transition needed
- Navigation argument: `audioFileId: Long`
- Predictive back gesture: use `PredictiveBackHandler` for smooth back animations

### Transitions
- Shared element transition: file card in list → playback screen header
- Bottom sheet: spring animation for open/close
- Waveform zoom: animated scale transitions

## 12. Logging — Timber

### Why Timber (Not Firebase Crashlytics)
For a sideloaded app with no Play Store distribution yet, Firebase Crashlytics adds unnecessary weight:
- Requires a Firebase project, `google-services.json`, and the Google Services Gradle plugin
- Crash reports go to a cloud console — overkill when the only user is the developer
- Adds ~1MB to APK size

**Timber** is a lightweight logging facade by Jake Wharton that wraps Android's `Log` class:
- Debug builds: logs to Logcat (automatic via `DebugTree`)
- Release builds: logs to a local file at `{filesDir}/logs/rehearsall.log` (custom `FileLoggingTree`)
- Zero setup — no cloud project, no API keys, no configuration files
- When/if the app moves to Play Store, Crashlytics can be added alongside Timber without changes

### Implementation
```kotlin
// In Application.onCreate()
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(FileLoggingTree(context.filesDir))
}
```

### What Gets Logged
- File import events (success/failure, format, size)
- Playback start/stop, speed changes
- Loop set/clear, practice start/stop
- Android Auto connection events
- Non-fatal exceptions with context

## 13. Navigation Architecture

```
NavHost
├── FileListScreen (startDestination)
│   ├── → PlaybackScreen/{audioFileId}
│   ├── → PlaylistScreen/{playlistId}
│   └── → SettingsScreen (theme, skip increment)
├── PlaybackScreen/{audioFileId}
│   ├── Bottom sheets (speed, markers [3 tabs], practice, queue)
│   └── ← Back to FileListScreen
└── PlaylistScreen/{playlistId}
    ├── Manage playlist contents
    └── ← Back to FileListScreen
```

For tablet (expanded layout):
```
ListDetailPaneScaffold
├── List pane: FileListScreen (with playlist tabs/sections)
└── Detail pane: PlaybackScreen (or empty state)
```

## 14. Error Handling Strategy

- **File import failures:** Snackbar with error message, log via Timber
- **Unsupported format:** Detect on import, show clear error, don't add to list
- **Waveform extraction failure:** Show playback screen without waveform, display retry option
- **Database errors:** Wrap in `Result<T>`, surface errors via UI state, log via Timber
- **Audio focus denied:** Snackbar, don't force playback
- **File not found (deleted externally):** Detect on load, show error state, offer to remove from list
- **Service connection failure:** Retry with exponential backoff, show error after 3 attempts
- All exceptions logged via Timber with context

## 15. Testing Strategy

Testing is integrated into every phase — not deferred.

### Test Pyramid
```
         ┌─────────┐
         │  E2E /  │  ← Few: critical user flows (import → play → loop)
         │   UI    │
        ┌┴─────────┴┐
        │ Integration│  ← Medium: Room DAOs, repositories with in-memory DB
       ┌┴────────────┴┐
       │    Unit      │  ← Many: domain models, use cases, engine logic, ViewModels
       └──────────────┘
```

### What Gets Tested

| Layer | What | Tools |
|-------|------|-------|
| Domain models | `PracticeStep` generation, chunk boundary calculation, queue logic | JUnit 4 |
| Use cases | Import validation, bookmark CRUD, playlist management | JUnit 4, MockK |
| ViewModels | State emissions, user action handling | JUnit 4, Turbine, MockK |
| DAOs | Queries, cascading deletes, Flow emissions, playlist ordering | Room in-memory DB, JUnit 4 |
| Repositories | Entity ↔ domain mapping, error wrapping | JUnit 4, MockK |
| PlaybackManager | State transitions (not actual audio — mock ExoPlayer) | JUnit 4, MockK |
| ChunkedPracticeEngine | Full state machine: step progression, rep counting, pausing | JUnit 4, Turbine |
| Content tree | Android Auto browse tree returns correct structure | JUnit 4 |
| UI Screens | Critical flows, accessibility checks | Compose testing, Espresso |
| Waveform | Extraction correctness (small test audio files) | Instrumented test |

### Test Conventions
- Test files mirror source structure: `src/test/.../PlaybackManagerTest.kt`
- Use descriptive function names: `fun setLoopRegion_seeksBackWhenPositionReachesEnd()`
- One assertion per test where practical
- Fake implementations for interfaces (prefer fakes over mocks for repositories)
- `TestDispatcher` for coroutine tests
- `Turbine` for `Flow` assertions

## 16. CI/CD — GitHub Actions

### Workflow: `build-and-test.yml`
Triggered on: push to `main`, pull requests

```yaml
Steps:
1. Checkout code
2. Set up JDK 21
3. Set up Android SDK (API 36, build-tools 36.1.0)
4. Cache Gradle dependencies
5. Run lint (./gradlew lint)
6. Run unit tests (./gradlew testDebugUnitTest)
7. Build debug APK (./gradlew assembleDebug)
8. Upload test results as artifact
9. Upload APK as artifact
```

### Workflow: `release.yml`
Triggered on: tag push (`v*`)

```yaml
Steps:
1–4. Same as above
5. Run full test suite
6. Build release APK (./gradlew assembleRelease) — signed with keystore
7. Upload release APK as GitHub Release asset
```

### Signing
- Debug: auto-generated debug keystore
- Release: keystore stored as GitHub encrypted secret, referenced in `build.gradle.kts`

## 17. Security

### Threat Model
RehearsAll is a local-only app with no network calls, no authentication, and no cloud storage. The attack surface is small, but mitigations are mapped to the [OWASP Mobile Top 10 (2024)](https://owasp.org/www-project-mobile-top-10/) where applicable:

| Area | Mitigation |
|------|-----------|
| **File import** (M4) | Files from SAF are copied to app-private storage with UUID names. Original file names are metadata only — never used in paths (prevents path traversal). File size capped at 500MB on import to prevent storage exhaustion. Format validated before copy. |
| **MediaBrowser access** (M8) | `onGetLibraryRoot()` validates `ControllerInfo.packageName` — only allows own package and Android Auto system packages. Rejects unknown callers. |
| **Internal storage** (M9) | All data in `context.filesDir` — Android sandbox prevents access by other apps. No `MODE_WORLD_READABLE` or content providers exposing files. Encryption at rest via Android FBE. |
| **Room database** (M4) | Parameterized queries only (Room enforces this). No raw SQL with user input. |
| **Waveform cache** (M4) | Magic bytes + version validated on load. Corrupted or tampered cache files are rejected and re-extracted from source audio. |
| **Logging** (M6) | Release `FileLoggingTree` logs only IDs, durations, and event types. Never logs file paths, URIs, or user-entered names. |
| **Backup** (M8) | `backup_rules.xml` excludes databases, audio files, waveform cache, and logs. Only DataStore preferences (theme, skip increment) are backed up. |
| **R8/ProGuard** (M7) | Minification enabled for release builds. `isDebuggable = false` in release config. |

### OWASP Categories Not Applicable
- **M1 (Improper Credential Usage):** No credentials or API keys
- **M3 (Insecure Authentication/Authorization):** Single-user local app, no auth
- **M5 (Insecure Communication):** Zero network calls — no telemetry, analytics, or cloud sync
- **M10 (Insufficient Cryptography):** No custom crypto; FBE handles at-rest encryption at the OS level

Full OWASP mapping documented in `docs/SECURITY.md`.

## 18. ProGuard / R8 (Release Builds)

- Enable minification and resource shrinking in release build type
- Keep rules for: Room entities, Hilt-generated code, Media3, MediaLibraryService
- Test with release build to catch proguard issues early
