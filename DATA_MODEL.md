# RehearsAll — Data Model & Persistence

## Room Database: `RehearsAllDatabase`

Version progression: one version bump per phase that adds entities.

---

## Entity: `audio_files`

Stores metadata for each imported audio file.

```kotlin
@Entity(tableName = "audio_files")
data class AudioFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,           // original file name from SAF
    val displayName: String,        // user-facing name (initially = fileName without extension)
    val internalPath: String,       // path in app-internal storage
    val format: String,             // "mp3", "wav", "ogg", "flac", "m4a"
    val durationMs: Long,           // total duration in milliseconds
    val fileSizeBytes: Long,        // file size for display
    val importedAt: Long,           // epoch millis
    val lastPlayedAt: Long?,        // epoch millis, null if never played
    val lastPositionMs: Long = 0,   // resume position
    val lastSpeed: Float = 1.0f     // last used playback speed
)
```

**DAO operations:**
- `insert(entity): Long`
- `getAll(): Flow<List<AudioFileEntity>>` — ordered by `importedAt DESC`
- `getById(id: Long): AudioFileEntity?`
- `delete(id: Long)`
- `updateLastPlayed(id: Long, lastPlayedAt: Long, lastPositionMs: Long)`
- `updateLastPosition(id: Long, positionMs: Long)`
- `updateLastSpeed(id: Long, speed: Float)`

---

## Entity: `bookmarks`

User-placed position markers for quick navigation.

```kotlin
@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = AudioFileEntity::class,
        parentColumns = ["id"],
        childColumns = ["audioFileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("audioFileId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioFileId: Long,
    val positionMs: Long,
    val name: String,             // e.g., "Bookmark 1", "Chorus start"
    val createdAt: Long           // epoch millis
)
```

**DAO operations:**
- `insert(entity): Long`
- `getAllForFile(audioFileId: Long): Flow<List<BookmarkEntity>>` — ordered by `positionMs ASC`
- `update(entity)`
- `delete(id: Long)`
- `deleteAllForFile(audioFileId: Long)`

---

## Entity: `loops`

Saved A-B loop regions for reuse.

```kotlin
@Entity(
    tableName = "loops",
    foreignKeys = [ForeignKey(
        entity = AudioFileEntity::class,
        parentColumns = ["id"],
        childColumns = ["audioFileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("audioFileId")]
)
data class LoopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioFileId: Long,
    val name: String,             // e.g., "Verse 1", "Bridge"
    val startMs: Long,
    val endMs: Long,
    val createdAt: Long           // epoch millis
)
```

**DAO operations:**
- `insert(entity): Long`
- `getAllForFile(audioFileId: Long): Flow<List<LoopEntity>>` — ordered by `startMs ASC`
- `update(entity)`
- `delete(id: Long)`
- `deleteAllForFile(audioFileId: Long)`

---

## Entity: `chunk_markers`

Position markers that divide audio into sequential sections for practice.

```kotlin
@Entity(
    tableName = "chunk_markers",
    foreignKeys = [ForeignKey(
        entity = AudioFileEntity::class,
        parentColumns = ["id"],
        childColumns = ["audioFileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("audioFileId")]
)
data class ChunkMarkerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioFileId: Long,
    val positionMs: Long,
    val label: String,            // e.g., "1", "Verse 1", "Line 3"
    val orderIndex: Int,          // 0-based order for sequencing
    val createdAt: Long           // epoch millis
)
```

**DAO operations:**
- `insert(entity): Long`
- `getAllForFile(audioFileId: Long): Flow<List<ChunkMarkerEntity>>` — ordered by `orderIndex ASC`
- `update(entity)`
- `delete(id: Long)`
- `deleteAllForFile(audioFileId: Long)`
- `updateOrder(id: Long, newOrderIndex: Int)` — for reordering

**Note on chunk boundaries:** Chunks are defined by the gaps between markers. Given markers at positions [M0, M1, M2, M3] and a file of duration D:
- Chunk 0: 0 → M0
- Chunk 1: M0 → M1
- Chunk 2: M1 → M2
- Chunk 3: M2 → M3
- Chunk 4: M3 → D

Actually, simpler design: markers represent chunk **start** positions. First chunk starts at 0 (implicit). Each marker starts a new chunk. Last chunk ends at file duration.
- Markers at [M0, M1, M2]:
  - Chunk 1: 0 → M0
  - Chunk 2: M0 → M1
  - Chunk 3: M1 → M2
  - Chunk 4: M2 → duration

Wait — even simpler: marker positions define **boundaries** between chunks. With N markers you get N+1 chunks:
- Markers = [M1, M2, M3]
- Chunk 1: 0 → M1
- Chunk 2: M1 → M2
- Chunk 3: M2 → M3
- Chunk 4: M3 → end

**This is the approach we use.** Markers are chunk dividers placed at transition points.

---

## Entity: `practice_settings`

Per-file settings for chunked repetition practice.

```kotlin
@Entity(tableName = "practice_settings")
data class PracticeSettingsEntity(
    @PrimaryKey val audioFileId: Long,  // 1:1 with audio file
    val repeatCount: Int = 3,           // 1–20
    val pauseBetweenRepsMs: Long = 0,   // 0–5000
    val pauseBetweenChunksMs: Long = 0, // 0–10000
    val selectedMode: String = "CUMULATIVE_BUILD_UP"  // enum name
)
```

**DAO operations:**
- `insertOrUpdate(entity)` — `@Insert(onConflict = REPLACE)`
- `getForFile(audioFileId: Long): PracticeSettingsEntity?`
- `getForFileFlow(audioFileId: Long): Flow<PracticeSettingsEntity?>`

---

## Domain Models

Clean Kotlin data classes without Room annotations. Mapped at the repository layer.

```kotlin
data class AudioFile(
    val id: Long,
    val displayName: String,
    val format: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val importedAt: Instant,
    val lastPositionMs: Long,
    val lastSpeed: Float
)

data class Bookmark(
    val id: Long,
    val positionMs: Long,
    val name: String
)

data class Loop(
    val id: Long,
    val name: String,
    val startMs: Long,
    val endMs: Long
)

data class ChunkMarker(
    val id: Long,
    val positionMs: Long,
    val label: String,
    val orderIndex: Int
)

data class PracticeSettings(
    val repeatCount: Int,
    val pauseBetweenRepsMs: Long,
    val pauseBetweenChunksMs: Long,
    val mode: PracticeMode
)

enum class PracticeMode {
    SINGLE_CHUNK_LOOP,
    CUMULATIVE_BUILD_UP,
    SEQUENTIAL_PLAY
}
```

---

## File Storage Layout

```
context.filesDir/
├── audio/
│   ├── {uuid1}.mp3        # Copied audio files (UUID names to avoid conflicts)
│   ├── {uuid2}.wav
│   └── ...
└── waveforms/
    ├── {audioFileId1}.waveform   # Binary amplitude data cache
    ├── {audioFileId2}.waveform
    └── ...
```

When an audio file is deleted from the app:
1. Delete the `AudioFileEntity` from Room (cascades to bookmarks, loops, chunk markers)
2. Delete the audio file from `audio/`
3. Delete the waveform cache from `waveforms/`

---

## Migration Strategy

- **Version 1:** `audio_files` (Phase 2)
- **Version 2:** Add `bookmarks`, `loops` (Phases 5–6)
- **Version 3:** Add `chunk_markers`, `practice_settings` (Phase 7)

Since this is a new app with no existing users, destructive migrations (`fallbackToDestructiveMigration()`) are acceptable during development. Write proper migrations before any release.
