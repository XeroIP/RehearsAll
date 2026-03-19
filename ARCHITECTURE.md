# RehearsAll — Architecture & Technical Decisions

## 1. Why Jetpack Compose (Not XML)

| Factor | Compose | XML |
|--------|---------|-----|
| Waveform rendering | Custom `Canvas` composable with built-in gesture support (pinch, drag, tap) | Custom `View` with `onDraw` + `GestureDetector` — more boilerplate |
| State management | Natural fit with `StateFlow` → `collectAsState()` | Requires `LiveData` observers or data binding |
| Theming | Material 3 out of the box | Material 3 available but more verbose |
| Maintainability | Single language (Kotlin), declarative | XML + Kotlin split, imperative updates |
| Custom UI | Compose modifiers + Canvas = flexible custom drawing | More powerful low-level control but more code |

**Decision:** Compose. The waveform is the most complex UI element, and Compose Canvas with gesture modifiers is cleaner than a custom `View`. State flows naturally from ViewModel → Compose.

## 2. Dependency Injection — Hilt

- Standard Android DI with first-class Compose support (`hiltViewModel()`)
- `@Singleton` for PlaybackManager, Database, Repositories
- `@ViewModelScoped` for ViewModel dependencies
- Modules: `DatabaseModule`, `AudioModule`, `RepositoryModule`

## 3. Audio Engine — Media3 ExoPlayer

### Why Media3
- Modern replacement for legacy ExoPlayer
- Built-in `PlaybackParameters(speed)` — pitch-preserved time stretching via Sonic
- Supports MP3, WAV, OGG, FLAC, M4A/AAC natively
- Handles audio focus via `AudioAttributes`

### PlaybackManager (Singleton)
Wraps `ExoPlayer` and exposes a clean API to the rest of the app:

```kotlin
interface PlaybackManager {
    // State
    val playbackState: StateFlow<PlaybackState>  // position, duration, isPlaying, speed
    val currentFileId: StateFlow<Long?>

    // Transport
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipForward(ms: Long = 5000)
    fun skipBackward(ms: Long = 5000)

    // Speed
    fun setSpeed(speed: Float)  // 0.25f..3.0f

    // File
    fun loadFile(filePath: String, startPositionMs: Long = 0)
    fun release()

    // A-B Looping
    fun setLoopRegion(startMs: Long, endMs: Long)
    fun clearLoopRegion()
}
```

### A-B Loop Implementation
- **Do NOT use `ClippingMediaSource`** — it requires rebuilding the source to adjust boundaries
- Instead: poll playback position every ~16ms via coroutine; when `position >= loopEndMs`, seek to `loopStartMs`
- The 16ms polling aligns with frame rate and keeps the overshoot minimal (~1 frame)
- Expose loop region as `StateFlow<LoopRegion?>` for UI to render the highlighted waveform region

### Audio Focus & Interruptions
- Set `AudioAttributes` with `USAGE_MEDIA` + `CONTENT_TYPE_MUSIC`
- Request audio focus via `AudioManager`
- Pause on focus loss (phone call, other app plays audio)
- Duck volume on transient focus loss
- Pause on headphone disconnect via `ACTION_AUDIO_BECOMING_NOISY`

## 4. Waveform Rendering

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
├── Bookmark markers (colored ticks above waveform)
├── Chunk markers (colored ticks below waveform, different color)
└── A-B region overlay (semi-transparent highlight)
```

- Zoom level determines how many amplitude samples map to one pixel
- At minimum zoom: entire file visible. At maximum zoom: ~2 seconds visible
- Auto-scroll to keep the playback cursor visible during playback

## 5. Chunked Practice Engine

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

**Sequential Play** — play each chunk once with a configurable pause between them.

### Integration with PlaybackManager
- The `ChunkedPracticeEngine` uses `PlaybackManager.setLoopRegion()` for each step
- Tracks repetition count by observing when playback loops back to start
- Manages inter-step pauses with `delay()` coroutines
- Exposed as `StateFlow<PracticeState>` for the UI

## 6. Navigation

Two screens with Compose Navigation:
1. **FileListScreen** — the entry point; shows imported files
2. **PlaybackScreen** — opened when a file is tapped; includes all practice features

Navigation argument: `audioFileId: Long`

## 7. Error Handling Strategy

- **File import failures:** Show snackbar with error message, don't crash
- **Unsupported format:** Detect on import, show clear error, don't add to list
- **Waveform extraction failure:** Show playback screen without waveform, display a retry option
- **Database errors:** Wrap in `Result<T>`, surface errors via UI state
- **Audio focus denied:** Show a toast, don't force playback

## 8. Testing Strategy (Post-MVP)

- **Unit tests:** Domain models, use cases, step sequence generation, practice engine state transitions
- **Integration tests:** Room DAOs, repository with in-memory database
- **UI tests:** Compose testing for critical flows (import, play, set markers)
- Not implementing tests in MVP but architecture supports easy addition
