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
    val displayName: String,        // user-facing name (initially = embedded title or fileName without extension)
    val internalPath: String,       // path in app-internal storage
    val format: String,             // "mp3", "wav", "ogg", "flac", "m4a"
    val durationMs: Long,           // total duration in milliseconds
    val fileSizeBytes: Long,        // file size for display
    val artist: String?,            // extracted from embedded metadata (ID3, M4A, etc.), null if absent
    val title: String?,             // extracted from embedded metadata, null if absent
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
- `getRecent(limit: Int = 20): Flow<List<AudioFileEntity>>` — ordered by `lastPlayedAt DESC`, non-null only
- `delete(id: Long)`
- `updateDisplayName(id: Long, displayName: String)` — for user rename
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
    val label: String,            // e.g., "Verse 1", "Line 3" — auto-generated as "1", "2", etc. on creation
    val createdAt: Long           // epoch millis
)
```

**No `orderIndex` field.** Chunk markers are always sorted by `positionMs` — their position in the audio determines their order. Users adjust position by dragging the marker on the waveform, not by reordering a list.

**DAO operations:**
- `insert(entity): Long`
- `getAllForFile(audioFileId: Long): Flow<List<ChunkMarkerEntity>>` — ordered by `positionMs ASC`
- `update(entity)` — used when dragging a marker to a new position
- `delete(id: Long)`
- `deleteAllForFile(audioFileId: Long)`

**Chunk boundary model:** Markers define **boundaries** (dividers) between chunks. With N markers you get N+1 chunks. The first chunk implicitly starts at 0, and the last chunk implicitly ends at the file duration.

Example — markers at positions [M1, M2, M3]:
- Chunk 1: `0` → `M1`
- Chunk 2: `M1` → `M2`
- Chunk 3: `M2` → `M3`
- Chunk 4: `M3` → `duration`

---

## Entity: `practice_settings`

Per-file settings for chunked repetition practice.

```kotlin
@Entity(
    tableName = "practice_settings",
    foreignKeys = [ForeignKey(
        entity = AudioFileEntity::class,
        parentColumns = ["id"],
        childColumns = ["audioFileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PracticeSettingsEntity(
    @PrimaryKey val audioFileId: Long,  // 1:1 with audio file
    val repeatCount: Int = 3,           // 1–20
    val gapBetweenRepsMs: Long = 0,     // 0–5000, silence gap between repetitions
    val gapBetweenChunksMs: Long = 0,   // 0–10000, silence gap between steps
    val selectedMode: String = "CUMULATIVE_BUILD_UP"  // enum name
)
```

**Naming note:** `gapBetweenRepsMs` and `gapBetweenChunksMs` (not "pause") — these are discrete silence gaps inserted after playback stops, before the next repetition or step begins.

**DAO operations:**
- `insertOrUpdate(entity)` — `@Insert(onConflict = REPLACE)`
- `getForFile(audioFileId: Long): PracticeSettingsEntity?`
- `getForFileFlow(audioFileId: Long): Flow<PracticeSettingsEntity?>`

---

## Entity: `playlists`

User-created playlists for organizing and sequencing audio files.

```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,           // epoch millis
    val updatedAt: Long            // epoch millis — updated when items change
)
```

**DAO operations:**
- `insert(entity): Long`
- `getAll(): Flow<List<PlaylistEntity>>` — ordered by `updatedAt DESC`
- `getById(id: Long): PlaylistEntity?`
- `update(entity)`
- `delete(id: Long)`

---

## Entity: `playlist_items`

Junction table linking playlists to audio files, with ordering.

```kotlin
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["audioFileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("audioFileId")]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val audioFileId: Long,
    val orderIndex: Int              // 0-based position in playlist
)
```

**DAO operations:**
- `insert(entity): Long`
- `insertAll(entities: List<PlaylistItemEntity>)`
- `getAllForPlaylist(playlistId: Long): Flow<List<PlaylistItemEntity>>` — ordered by `orderIndex ASC`
- `getPlaylistWithFiles(playlistId: Long): Flow<List<PlaylistItemWithFile>>` — JOIN with audio_files for display
- `delete(id: Long)`
- `deleteAllForPlaylist(playlistId: Long)`
- `updateOrder(id: Long, newOrderIndex: Int)` — for drag-to-reorder

**`PlaylistItemWithFile`** — JOIN result class used by `getPlaylistWithFiles()`:

```kotlin
data class PlaylistItemWithFile(
    @Embedded val item: PlaylistItemEntity,
    @Relation(
        parentColumn = "audioFileId",
        entityColumn = "id"
    )
    val audioFile: AudioFileEntity
)
```

**Note on cascading:** If an audio file is deleted, its `playlist_items` entries are automatically removed (CASCADE). If a playlist is deleted, all its items are removed (CASCADE). The audio files themselves are NOT deleted when a playlist is deleted.

---

## Domain Models

Clean Kotlin data classes without Room annotations. Mapped at the repository layer.

```kotlin
data class AudioFile(
    val id: Long,
    val displayName: String,
    val artist: String?,       // from embedded metadata
    val title: String?,        // from embedded metadata
    val format: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val importedAt: Instant,
    val lastPlayedAt: Instant?, // null if never played
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
    val label: String
)

data class Chunk(
    val index: Int,        // 0-based chunk number
    val startMs: Long,
    val endMs: Long,
    val label: String
)
// Derived at runtime from chunk markers + file duration — not persisted.
// With N markers you get N+1 chunks (see chunk boundary model above).

data class PracticeSettings(
    val repeatCount: Int,
    val gapBetweenRepsMs: Long,
    val gapBetweenChunksMs: Long,
    val mode: PracticeMode
)

enum class PracticeMode {
    SINGLE_CHUNK_LOOP,
    CUMULATIVE_BUILD_UP,
    SEQUENTIAL_PLAY
}

data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val totalDurationMs: Long
)

data class PlaylistItem(
    val id: Long,
    val audioFile: AudioFile,
    val orderIndex: Int
)

data class QueueItem(
    val audioFileId: Long,
    val displayName: String,
    val durationMs: Long,
    val isCurrentTrack: Boolean
)

enum class RepeatMode { OFF, ONE, ALL }
```

---

## File Storage Layout

```
context.filesDir/
├── audio/
│   ├── {uuid1}.mp3        # Copied audio files (UUID names to avoid conflicts)
│   ├── {uuid2}.wav
│   └── ...
├── waveforms/
│   ├── {audioFileId1}.waveform   # Binary amplitude data cache
│   ├── {audioFileId2}.waveform
│   └── ...
└── logs/
    └── rehearsall.log     # Timber file log for release builds
```

When an audio file is deleted from the app:
1. Delete the `AudioFileEntity` from Room (cascades to bookmarks, loops, chunk markers, playlist items)
2. Delete the audio file from `audio/`
3. Delete the waveform cache from `waveforms/`

---

## DataStore Preferences (Not Room)

Lightweight user settings that don't need relational storage. Uses Jetpack DataStore (Preferences).

```kotlin
object PreferenceKeys {
    val THEME_MODE = stringPreferencesKey("theme_mode")              // "LIGHT", "DARK", "SYSTEM"
    val SKIP_INCREMENT_MS = longPreferencesKey("skip_increment_ms")  // default 5000
    val LOOP_CROSSFADE = booleanPreferencesKey("loop_crossfade")     // default true
    val WAVEFORM_OVERLAY = stringPreferencesKey("waveform_overlay")  // "NONE", "LOOPS", "CHUNKS" — default NONE
}
```

**Why DataStore instead of Room for these?**
DataStore is the modern replacement for SharedPreferences — it's coroutine-native, type-safe, and doesn't require schema migrations. It's the right tool for simple key-value settings. Room is for structured, relational data.

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class OverlayMode { NONE, LOOPS, CHUNKS }
```

---

## Migration Strategy

- **Version 1:** `audio_files` (Phase 2)
- **Version 2:** Add `playlists`, `playlist_items` (Phase 4)
- **Version 3:** Add `bookmarks`, `loops` (Phases 6–7)
- **Version 4:** Add `chunk_markers`, `practice_settings` (Phase 8)

Since this is a new app with no existing users, destructive migrations (`fallbackToDestructiveMigration()`) are acceptable during development. Write proper migrations before any release.
