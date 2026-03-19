# RehearsAll — Claude Code Instructions

## What Is This Project?
RehearsAll is an Android audio practice tool for two use cases:
1. **Musical rehearsal** — learning to sing or play a song by looping sections at variable speed
2. **Spoken word memorization** — memorizing speeches, scripture, monologues via chunked repetition

This is a **functional MVP for personal use**. Prioritize working features and usability over visual polish.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3 (Material You)
- **Audio:** Media3 ExoPlayer (gapless looping, pitch-preserved speed control)
- **Database:** Room (all user data)
- **DI:** Hilt
- **Async:** Coroutines + Flow
- **Min SDK:** 26 (Android 8.0) / **Target SDK:** Latest stable
- **Build:** Gradle with Kotlin DSL

## Architecture
Clean architecture with three layers:
- **UI layer** — Compose screens, ViewModels, UI state classes
- **Domain layer** — Use cases, domain models (plain Kotlin, no Android deps)
- **Data layer** — Room DAOs/entities, repositories, audio engine

Package root: `com.rehearsall`

## Key Conventions
- ViewModels expose `StateFlow<UiState>` — screens collect and render
- Repository pattern for all data access; DAOs are never called from ViewModels directly
- Entities (Room) are separate from domain models; map at the repository boundary
- Use `sealed class`/`sealed interface` for UI states and navigation events
- Coroutines for all async work; no callbacks
- Audio focus, headphone disconnect, and config changes must be handled properly

## File Organization
```
app/src/main/java/com/rehearsall/
├── di/              # Hilt modules
├── data/
│   ├── db/          # Room database, DAOs, entities
│   ├── repository/  # Repository implementations
│   └── audio/       # Waveform extraction, file import
├── domain/
│   ├── model/       # Domain models (Bookmark, Loop, ChunkMarker, etc.)
│   └── usecase/     # Use cases
├── playback/        # PlaybackManager, ChunkedPracticeEngine
└── ui/
    ├── theme/       # Material 3 theme
    ├── navigation/  # Nav graph
    ├── filelist/    # File list screen + ViewModel
    └── playback/    # Playback screen + ViewModel + components
```

## What NOT To Build (Out of Scope)
- Lyrics/transcript display, speech-to-text, in-app recording
- Pitch shifting, folders/categories/tags, export/sharing
- Cloud sync, notification/lock-screen media controls

## Priority Order
1. Rock-solid audio playback (gapless looping, accurate speed, responsive seek)
2. Waveform display (clear, zoomable, interactive)
3. A-B looping (set, adjust, save, recall)
4. Chunked repetition (cumulative build-up is highest value)
5. Persistence (everything survives restarts)
6. Usability (clear and intuitive, not necessarily beautiful)
