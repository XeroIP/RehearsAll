# RehearsAll — Playback Engine & Practice Mode Design

## PlaybackManager — Detailed Design

### Lifecycle
- **Created** as a `@Singleton` via Hilt, lives for the app's process lifetime
- **ExoPlayer instance** is created lazily on first `loadFile()` call
- **Released** in `Application.onTerminate()` or when the last activity is destroyed (use `ProcessLifecycleOwner`)

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

**Overshoot handling:** At 3.0x speed, in 16ms the playback advances ~48ms. The seek-back will cause a brief moment of audio past point B. This is acceptable for MVP — the overshoot is imperceptible.

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

            for (rep in 1..step.repeatCount) {
                // Update state
                _practiceState.value = PracticeState.Playing(
                    stepIndex = index,
                    totalSteps = steps.size,
                    currentRep = rep,
                    totalReps = step.repeatCount,
                    stepLabel = step.label,
                    chunkRange = step.chunkRange
                )

                // Play the region once
                playbackManager.seekTo(step.startMs)
                playbackManager.setLoopRegion(step.startMs, step.endMs)
                playbackManager.play()

                // Wait for playback to reach endMs
                playbackManager.playbackState
                    .first { it.positionMs >= step.endMs - 50 }
                // (The -50ms tolerance prevents missing the end due to polling)

                playbackManager.pause()

                // Pause between reps (if not the last rep)
                if (rep < step.repeatCount && settings.pauseBetweenRepsMs > 0) {
                    _practiceState.value = PracticeState.PauseBetweenReps(
                        index, rep + 1, settings.pauseBetweenRepsMs
                    )
                    delay(settings.pauseBetweenRepsMs)
                }
            }

            // Pause between steps (if not the last step)
            if (index < steps.lastIndex && settings.pauseBetweenChunksMs > 0) {
                _practiceState.value = PracticeState.PauseBetweenSteps(
                    index, steps[index + 1].label, settings.pauseBetweenChunksMs
                )
                delay(settings.pauseBetweenChunksMs)
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

### Repetition Detection — Alternative Approach

Instead of waiting for position to reach endMs (which may be unreliable with the loop mechanism), a cleaner approach:

1. Set loop region via PlaybackManager
2. Let PlaybackManager's built-in loop handle the seek-back
3. Count loop iterations by detecting when position jumps backward (from near endMs to startMs)
4. After N iterations, advance to next step

```kotlin
// In the practice loop, count reps by detecting seek-backs:
var repCount = 0
playbackManager.playbackState.collect { state ->
    if (previousPosition > state.positionMs + 500) {
        // Position jumped backward significantly → a loop iteration completed
        repCount++
        if (repCount >= targetReps) {
            // Move on
        }
    }
    previousPosition = state.positionMs
}
```

This is more robust than the `first {}` approach since it doesn't fight against the loop mechanism. **Use this approach.**

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
Simple format for fast read/write:
```
[4 bytes] magic: "WAVE"
[4 bytes] version: 1 (int)
[4 bytes] sample count (int)
[4 bytes × N] amplitude values (float array)
```

### Performance Expectations
- A 5-minute MP3 at 44.1kHz → ~30,000 amplitude samples
- Extraction time: 2–5 seconds on modern hardware
- Cache file size: ~120KB
- Cache load time: <10ms

### Progress Reporting
Track extraction progress as `(bytesProcessed / totalBytes)` and emit via `Flow<Float>` so the UI can show a progress indicator.

---

## Audio Focus — Detailed Implementation

```kotlin
private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
    .setAudioAttributes(audioAttributes)
    .setOnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus — resume if we were playing before
                if (wasPlayingBeforeFocusLoss) {
                    player.play()
                    player.volume = 1.0f
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss — pause and give up
                wasPlayingBeforeFocusLoss = player.isPlaying
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (phone call) — pause
                wasPlayingBeforeFocusLoss = player.isPlaying
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Brief interruption (notification) — lower volume
                player.volume = 0.2f
            }
        }
    }
    .build()
```

Request focus in `play()`, abandon in `pause()`/`stop()`.

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
