# RehearsAll — Playback Engine & Practice Mode Design

## Service Architecture

### RehearsAllPlaybackService (MediaLibraryService)

The playback engine runs as a `MediaLibraryService` — a foreground service that owns the `ExoPlayer` instance. This provides notification controls, lock screen controls, headphone/Bluetooth button handling, and Android Auto browsing from a single component.

```
App UI ──MediaBrowser──→ RehearsAllPlaybackService
                              ├── ExoPlayer (audio playback)
                              ├── MediaLibrarySession (media session + Auto content tree)
                              └── Foreground notification (media controls)
```

### Lifecycle
- **Service starts** when the first `MediaBrowser` connects (app UI or Android Auto)
- **ExoPlayer** is created in `onCreate()`, lives for the service lifetime
- **Service stops** when all clients disconnect AND playback is stopped
- **Foreground notification** is active whenever playback is in progress

### Two-Layer Design: Service-Side Engine + App-Side Wrapper

**Service-side:** The `RehearsAllPlaybackService` owns the `ExoPlayer` instance and all low-level playback logic that requires direct player access — position polling, A-B loop enforcement (seek-back), and loop crossfade (volume ramping). These run service-side because they need `player.currentPosition`, `player.seekTo()`, and `player.volume`.

**App-side:** `PlaybackManagerImpl` lives in the app process and communicates with the service via `MediaBrowser`/`MediaController`. It provides a clean Kotlin API so the rest of the app (ViewModels, ChunkedPracticeEngine) doesn't deal with MediaSession commands directly. It receives state updates from the service via `MediaController.Listener`.

```
RehearsAllPlaybackService (service process)
  ├── ExoPlayer (audio playback)
  ├── Position polling coroutine (~16ms) — A-B loop enforcement + crossfade
  └── MediaLibrarySession (exposes state + commands)

PlaybackManagerImpl (app process, @Singleton)
  └── MediaBrowser → connects to RehearsAllPlaybackService
        └── MediaController → sends commands, receives state updates
```

### Data Classes

```kotlin
data class PlaybackState(
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val speed: Float
) {
    companion object {
        val IDLE = PlaybackState(0L, 0L, false, 1.0f)
    }
}

data class LoopRegion(val startMs: Long, val endMs: Long)
```

### Position Polling
```kotlin
private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

// Launched when playback starts, cancelled when paused/stopped
private var pollingJob: Job? = null

private fun startPolling() {
    pollingJob?.cancel()
    pollingJob = scope.launch {
        while (isActive) {
            val position = player.currentPosition
            val state = PlaybackState(
                positionMs = position,
                durationMs = player.duration,
                isPlaying = player.isPlaying,
                speed = player.playbackParameters.speed
            )
            _playbackState.value = state

            // A-B loop check
            loopRegion?.let { region ->
                if (position >= region.endMs) {
                    player.seekTo(region.startMs)
                }
            }

            delay(16) // ~60fps update rate
        }
    }
}
```

### Speed Control Implementation
```kotlin
fun setSpeed(speed: Float) {
    val clamped = speed.coerceIn(0.25f, 3.0f)
    // Round to nearest 0.05 to avoid floating point drift
    val rounded = (clamped * 20).roundToInt() / 20f
    player.playbackParameters = PlaybackParameters(rounded)
}
```

Media3's ExoPlayer uses the Sonic library internally for time-stretching, which preserves pitch. No additional configuration needed — `PlaybackParameters(speed)` without a pitch parameter defaults to pitch = 1.0 (unchanged).

### A-B Loop — Edge Cases

**Overshoot handling:** At 3.0x speed, in 16ms the playback advances ~48ms. The seek-back will cause a brief moment of audio past point B. This is acceptable — the overshoot is imperceptible.

**Loop region near end of file:** If B is set close to the file end, ExoPlayer may report `STATE_ENDED`. The `Player.Listener.onPlaybackStateChanged` handler should seek to A and continue if loop is active:

```kotlin
override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState == Player.STATE_ENDED && loopRegion != null) {
        player.seekTo(loopRegion!!.startMs)
        player.play()
    }
}
```

**A/B ordering:** Always enforce `startMs < endMs`. If user sets B before A, swap them. Minimum loop length: 100ms (anything shorter is not useful and risks rapid seeking).

**Short loop + crossfade edge case:** When a loop region is shorter than 150ms, crossfade is automatically disabled for that loop (hard seek-back used instead). A 100ms loop with a 50ms fade-out would leave no un-faded audio.

### Loop Crossfade

When enabled (DataStore pref `LOOP_CROSSFADE`, default `true`), the engine applies a brief volume crossfade to eliminate the audible discontinuity at the loop boundary:

```kotlin
// Inside the polling coroutine, ~50ms before reaching endMs:
loopRegion?.let { region ->
    val fadeStartMs = region.endMs - 50
    if (position >= fadeStartMs && position < region.endMs) {
        // Fade out: linear ramp 1.0 → 0.0 over 50ms
        val fadeProgress = (position - fadeStartMs).toFloat() / 50f
        player.volume = (1f - fadeProgress).coerceIn(0f, 1f)
    }
    if (position >= region.endMs) {
        player.volume = 0f
        player.seekTo(region.startMs)
        // Fade in over next ~50ms (tracked by subsequent poll cycles)
        fadeInRemainingMs = 50
    }
}

// Fade-in logic (runs each poll cycle while fadeInRemainingMs > 0):
if (fadeInRemainingMs > 0) {
    fadeInRemainingMs -= 16  // approximate poll interval
    val fadeProgress = 1f - (fadeInRemainingMs.toFloat() / 50f)
    player.volume = fadeProgress.coerceIn(0f, 1f)
}
```

When crossfade is disabled, the hard seek-back behavior is used (current behavior — no volume changes).

**Why 50ms?** Short enough to be imperceptible as a "fade" but long enough to eliminate the click/pop at the discontinuity. At 16ms polling, that's ~3 poll cycles — enough granularity for a smooth ramp.

---

## ChunkedPracticeEngine — Detailed Design

### Class Structure
```kotlin
class ChunkedPracticeEngine @Inject constructor(
    private val playbackManager: PlaybackManager
) {
    private val _practiceState = MutableStateFlow<PracticeState>(PracticeState.Idle)
    val practiceState: StateFlow<PracticeState> = _practiceState.asStateFlow()

    private var practiceJob: Job? = null
    private var steps: List<PracticeStep> = emptyList()
    private var currentStepIndex: Int = 0
}
```

### PracticeState
```kotlin
sealed interface PracticeState {
    data object Idle : PracticeState

    data class Playing(
        val stepIndex: Int,           // current step (0-based)
        val totalSteps: Int,
        val currentRep: Int,          // current repetition (1-based)
        val totalReps: Int,
        val stepLabel: String,        // e.g., "Chunk 2" or "Chunks 1–3"
        val chunkRange: IntRange      // which chunks are being played
    ) : PracticeState

    data class PauseBetweenReps(
        val stepIndex: Int,
        val nextRep: Int,
        val remainingMs: Long
    ) : PracticeState

    data class PauseBetweenSteps(
        val completedStepIndex: Int,
        val nextStepLabel: String,
        val remainingMs: Long
    ) : PracticeState

    data object Complete : PracticeState
}
```

### Step Generation — Cumulative Build-Up (The Core Algorithm)

Given chunk markers that define boundaries, chunks are numbered 1..N:

```kotlin
fun generateCumulativeBuildUpSteps(
    chunks: List<Chunk>,   // Chunk(index, startMs, endMs, label)
    repeatCount: Int
): List<PracticeStep> {
    val steps = mutableListOf<PracticeStep>()

    for (i in chunks.indices) {
        // Step A: Practice the new chunk alone
        steps.add(PracticeStep(
            startMs = chunks[i].startMs,
            endMs = chunks[i].endMs,
            label = "Chunk ${i + 1}",
            repeatCount = repeatCount,
            chunkRange = i..i
        ))

        // Step B: Practice all chunks from beginning through current (if > 1 chunk)
        if (i > 0) {
            steps.add(PracticeStep(
                startMs = chunks[0].startMs,
                endMs = chunks[i].endMs,
                label = "Chunks 1–${i + 1}",
                repeatCount = repeatCount,
                chunkRange = 0..i
            ))
        }
    }

    return steps
}
```

**Example with 4 chunks:**
| Step | What plays | Reps |
|------|-----------|------|
| 1 | Chunk 1 | ×N |
| 2 | Chunk 2 | ×N |
| 3 | Chunks 1–2 | ×N |
| 4 | Chunk 3 | ×N |
| 5 | Chunks 1–3 | ×N |
| 6 | Chunk 4 | ×N |
| 7 | Chunks 1–4 | ×N |

Total steps = 2N − 1 (for N chunks).

### Practice Execution Loop

```kotlin
fun startPractice(
    chunks: List<Chunk>,
    mode: PracticeMode,
    settings: PracticeSettings
) {
    steps = when (mode) {
        SINGLE_CHUNK_LOOP -> chunks.map { chunk ->
            PracticeStep(chunk.startMs, chunk.endMs, chunk.label,
                         settings.repeatCount, chunk.index..chunk.index)
        }
        CUMULATIVE_BUILD_UP -> generateCumulativeBuildUpSteps(chunks, settings.repeatCount)
        SEQUENTIAL_PLAY -> chunks.map { chunk ->
            PracticeStep(chunk.startMs, chunk.endMs, chunk.label,
                         1, chunk.index..chunk.index)  // 1 rep each
        }
    }

    practiceJob = scope.launch {
        for ((index, step) in steps.withIndex()) {
            currentStepIndex = index

            // Set up loop region — PlaybackManager's polling coroutine handles the seek-back
            playbackManager.seekTo(step.startMs)
            playbackManager.setLoopRegion(step.startMs, step.endMs)
            playbackManager.play()

            // Count reps by detecting backward position jumps (see section below)
            var repCount = 0
            var previousPosition = step.startMs

            _practiceState.value = PracticeState.Playing(
                stepIndex = index,
                totalSteps = steps.size,
                currentRep = 1,
                totalReps = step.repeatCount,
                stepLabel = step.label,
                chunkRange = step.chunkRange
            )

            playbackManager.playbackState.collect { state ->
                if (previousPosition > state.positionMs + 500) {
                    // Position jumped backward significantly → a loop iteration completed
                    repCount++

                    if (repCount >= step.repeatCount) {
                        playbackManager.pause()

                        // Gap between steps (if not the last step)
                        if (index < steps.lastIndex && settings.gapBetweenChunksMs > 0) {
                            _practiceState.value = PracticeState.PauseBetweenSteps(
                                index, steps[index + 1].label, settings.gapBetweenChunksMs
                            )
                            delay(settings.gapBetweenChunksMs)
                        }
                        return@collect // Move to next step
                    }

                    // Gap between reps (if not the last rep)
                    if (settings.gapBetweenRepsMs > 0) {
                        playbackManager.pause()
                        _practiceState.value = PracticeState.PauseBetweenReps(
                            index, repCount + 1, settings.gapBetweenRepsMs
                        )
                        delay(settings.gapBetweenRepsMs)
                        playbackManager.play()
                    }

                    // Update rep counter
                    _practiceState.value = PracticeState.Playing(
                        stepIndex = index,
                        totalSteps = steps.size,
                        currentRep = repCount + 1,
                        totalReps = step.repeatCount,
                        stepLabel = step.label,
                        chunkRange = step.chunkRange
                    )
                }
                previousPosition = state.positionMs
            }
        }

        playbackManager.clearLoopRegion()
        _practiceState.value = PracticeState.Complete
    }
}
```

### Skip Controls During Practice

```kotlin
fun skipToNextStep() {
    if (currentStepIndex < steps.lastIndex) {
        practiceJob?.cancel()
        currentStepIndex++
        // Restart practice from new step
        restartFromStep(currentStepIndex)
    }
}

fun skipToPreviousStep() {
    if (currentStepIndex > 0) {
        practiceJob?.cancel()
        currentStepIndex--
        restartFromStep(currentStepIndex)
    }
}
```

### How Looping and Repetition Counting Work Together

There are two distinct concerns — **mechanism** (who does the seek-back) and **observation** (who counts reps):

**1. Loop mechanism (PlaybackManager — polling coroutine):**
The polling coroutine at ~16ms checks `position >= endMs` and seeks back to `startMs`. This is the A-B loop mechanism — it keeps audio looping within the region. It optionally applies crossfade (see above).

**2. Repetition counting (ChunkedPracticeEngine — observer):**
The practice engine doesn't control the loop directly. Instead, it observes `playbackState` and detects when position jumps backward (from near `endMs` to `startMs`), which means one loop iteration completed. After N iterations, it advances to the next step.

```kotlin
// ChunkedPracticeEngine counts reps by detecting seek-backs:
var repCount = 0
var previousPosition = 0L
playbackManager.playbackState.collect { state ->
    if (previousPosition > state.positionMs + 500) {
        // Position jumped backward significantly → a loop iteration completed
        repCount++
        if (repCount >= targetReps) {
            // Pause playback, advance to next step
        }
    }
    previousPosition = state.positionMs
}
```

**Why this split?** The practice engine doesn't fight with the loop mechanism. PlaybackManager handles the low-level loop; the practice engine just watches and counts. Using `first { position >= endMs }` would race against the seek-back and may miss the trigger — backward-jump detection is reliable regardless of timing.

---

## Waveform Extraction — Detailed Design

### Algorithm

```
1. Open file with MediaExtractor, select first audio track
2. Create MediaCodec decoder for the track's MIME type
3. Feed encoded data → decoder → PCM output buffers
4. PCM processing:
   a. Read samples as 16-bit signed integers (most common PCM format)
   b. If stereo, average L+R channels
   c. Accumulate samples into windows (~10ms each = sampleRate/100 samples)
   d. For each window, compute RMS: sqrt(sum(sample²) / count)
   e. Store RMS as Float in output array
5. Normalize: divide all values by the maximum value → range 0.0–1.0
6. Serialize to binary file
```

### Binary Format
Simple format for fast read/write (little-endian — native ARM byte order):
```
[4 bytes] magic: "WAVE"
[4 bytes] version: 1 (int, little-endian)
[4 bytes] sample count (int, little-endian)
[4 bytes × N] amplitude values (float array, little-endian)
```

Use `ByteBuffer.order(ByteOrder.LITTLE_ENDIAN)` for read/write. Little-endian is native on ARM (all Android devices), so `ByteBuffer.allocateDirect()` avoids byte-swapping overhead.

### Performance Expectations
- A 5-minute MP3 at 44.1kHz → ~30,000 amplitude samples
- Extraction time: 2–5 seconds on modern hardware
- Cache file size: ~120KB
- Cache load time: <10ms

### Progress Reporting
Track extraction progress as `(bytesProcessed / totalBytes)` and emit via `Flow<Float>` so the UI can show a progress indicator.

---

## Audio Focus & Interruptions

With `MediaSession` and proper `AudioAttributes`, audio focus is handled **automatically** by Media3:
- Focus requested when `player.play()` is called
- Focus abandoned when `player.pause()` or `player.stop()` is called
- Pause on `AUDIOFOCUS_LOSS` and `AUDIOFOCUS_LOSS_TRANSIENT`
- Duck on `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`
- Resume on `AUDIOFOCUS_GAIN`
- Headphone disconnect (ACTION_AUDIO_BECOMING_NOISY) → automatic pause via MediaSession

No manual `AudioFocusRequest` or `BroadcastReceiver` needed — the `MediaSession` handles it all.

### Queue & Repeat Modes

ExoPlayer natively supports playlists and repeat:
```kotlin
// Set queue
player.setMediaItems(mediaItems)
player.prepare()
player.play()

// Repeat modes
player.repeatMode = Player.REPEAT_MODE_OFF   // play queue once
player.repeatMode = Player.REPEAT_MODE_ONE   // loop current track
player.repeatMode = Player.REPEAT_MODE_ALL   // loop entire queue

// Shuffle
player.shuffleModeEnabled = true  // randomizes playback order without modifying playlist

// Navigation
player.seekToNextMediaItem()
player.seekToPreviousMediaItem()
```

These are automatically exposed through the `MediaSession` to notification controls, lock screen, headphone buttons, and Android Auto.

---

## Threading Model

| Operation | Dispatcher | Reason |
|-----------|-----------|--------|
| ExoPlayer calls | `Main` | ExoPlayer requires main thread |
| Position polling | `Main` | Updates UI state; ExoPlayer reads must be on main |
| Waveform extraction | `IO` | Heavy CPU + disk work |
| Room queries | `IO` | Database I/O |
| File copy (import) | `IO` | Disk I/O |
| Practice engine delays | `Default` | Timer-based coroutine delays |
| UI state collection | `Main` | Compose requires main thread |
