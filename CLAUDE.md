# RehearsAll — Claude Code Instructions

## What Is This Project?
RehearsAll is an Android audio practice tool for two use cases:
1. **Musical rehearsal** — learning to sing or play a song by looping sections at variable speed
2. **Spoken word memorization** — memorizing speeches, scripture, monologues via chunked repetition

This is a **production-quality app** targeting sideload distribution initially, with future Play Store release planned. All features must be fully tested, polished, and reliable.

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3 (Material You / Dynamic Color)
- **Audio:** Media3 ExoPlayer + MediaLibraryService (gapless looping, pitch-preserved speed control, notification/lock screen/headphone/Bluetooth controls, Android Auto)
- **Database:** Room (structured user data)
- **Preferences:** DataStore (theme, skip increment, lightweight settings)
- **DI:** Hilt
- **Async:** Coroutines + Flow
- **Logging:** Timber (debug: Logcat, release: local file log)
- **CI/CD:** GitHub Actions
- **Testing:** JUnit 4 (AndroidX Test), Turbine (Flow testing), MockK, Compose UI testing, Espresso
- **Min SDK:** 26 (Android 8.0) / **Target SDK:** 36
- **Build:** Gradle with Kotlin DSL

## Architecture
Clean architecture with three layers:
- **UI layer** — Compose screens, ViewModels, UI state classes
- **Domain layer** — Use cases, domain models (plain Kotlin, no Android deps)
- **Data layer** — Room DAOs/entities, repositories, audio engine, DataStore

Package root: `com.rehearsall`

## Key Conventions
- ViewModels expose `StateFlow<UiState>` — screens collect and render
- Repository pattern for all data access; DAOs are never called from ViewModels directly
- Entities (Room) are separate from domain models; map at the repository boundary
- Use `sealed class`/`sealed interface` for UI states and navigation events
- Coroutines for all async work; no callbacks
- Audio focus, headphone disconnect, and config changes must be handled properly
- **Tests accompany every feature** — unit tests for domain/data, UI tests for screens
- **Bottom sheets** for secondary panels (consolidated markers [bookmarks/loops/chunks tabs], settings, speed control, queue)
- **Mini player** bar persistent at bottom of all screens during active playback
- **Shuffle mode** via ExoPlayer's built-in shuffle support
- **Loop crossfade** (~50ms volume ramp) — user-toggleable via DataStore preference
- **Embedded metadata** extraction (artist, title) on import via `MediaMetadataRetriever`
- **File rename** via file details bottom sheet
- **Adaptive layouts** using WindowSizeClass for phone + tablet support

## Theming
- **Dynamic Color** (Material You): on Android 12+, derive palette from wallpaper
- **Fallback palette**: custom Material 3 color scheme for Android 8–11
- **Three modes**: Light / Dark / Follow System (default: Follow System)
- **Theme preference** stored in DataStore
- Edge-to-edge display with proper system bar handling

## Modern UI Patterns
- Modal and standard bottom sheets (Material 3 `ModalBottomSheet`)
- Large collapsing top app bars where appropriate
- Predictive back gesture support
- Animated shared element transitions between screens
- Scaffold with snackbar host for transient messages
- Swipe-to-dismiss on list items
- Pull-to-refresh where applicable

## File Organization
```
app/src/main/java/com/rehearsall/
├── di/              # Hilt modules
├── data/
│   ├── db/          # Room database, DAOs, entities
│   ├── repository/  # Repository implementations
│   ├── preferences/ # DataStore (theme, settings)
│   └── audio/       # Waveform extraction, file import
├── domain/
│   ├── model/       # Domain models (Bookmark, Loop, ChunkMarker, etc.)
│   └── usecase/     # Use cases
├── playback/        # MediaLibraryService, PlaybackManager, ChunkedPracticeEngine, ContentTreeBuilder
└── ui/
    ├── theme/       # Material 3 theme, dynamic color, color schemes
    ├── navigation/  # Nav graph
    ├── common/      # Shared composables (bottom sheets, dialogs)
    ├── filelist/    # File list screen + ViewModel
    ├── playback/    # Playback screen + ViewModel + components
    ├── playlist/    # Playlist screen + ViewModel
    └── settings/    # Settings screen (theme, skip increment)

app/src/main/java/com/rehearsall/logging/  # Timber FileLoggingTree

app/src/test/          # Unit tests (JUnit 4, MockK, Turbine)
app/src/androidTest/   # Instrumented tests (Compose UI, Espresso)
```

## License
Apache License 2.0 — chosen for patent grant, Android ecosystem compatibility, and permissive terms.

## Documentation
- `README.md` — project overview, screenshots, build instructions, sideload/install guide
- `docs/USER_GUIDE.md` — full feature walkthrough with inline screenshots
- `docs/PRACTICE_MODES.md` — deep dive on the three chunked practice modes
- `docs/SECURITY.md` — security hardening overview (storage, import validation, access control, logging)
- `docs/screenshots/` — labeled PNGs for all key screens and states

## What NOT To Build (Out of Scope)
- Lyrics/transcript display, speech-to-text, in-app recording
- Pitch shifting, folders/categories/tags, export/sharing
- Cloud sync
- Android Automotive OS (native car app — different from Android Auto which IS in scope)
- Play Store listing assets (deferred to later)

## Quality Bar
- All features have unit tests (domain, data, ViewModel)
- Critical user flows have UI tests
- No crashes — Timber logging for diagnostics (local file log in release builds)
- Accessibility: content descriptions on all interactive elements
- Tablet layouts work correctly
- Theme switching is seamless (no flicker, no state loss)
- ProGuard/R8 enabled for release builds

## Priority Order
1. Rock-solid audio playback (gapless looping, accurate speed, responsive seek)
2. Full media integration (notification, lock screen, headphone buttons, Android Auto)
3. Waveform display (clear, zoomable, interactive)
4. Playlists and queue management
5. A-B looping (set, adjust, save, recall)
6. Chunked repetition (cumulative build-up is highest value)
7. Persistence (everything survives restarts)
8. Modern, polished UI (Material You, bottom sheets, adaptive layout)
9. Test coverage and production readiness

<!-- shift-change:start v2 -->
## Project status

The handoff for this repo lives in STATUS.md, imported below. Read it before starting work and treat its "Next step" as the default starting point unless I say otherwise.

@STATUS.md

Keeping it current:
- Before any `git push`, update STATUS.md (sections, `updated` timestamp, `source: manual`) and include it in the commit, following .claude/commands/wrapup.md.
- When I say "wrap up", or a task is finished, follow .claude/commands/wrapup.md.
- Commits that change only STATUS.md are handoff notes, not code: make them on the current branch, including main, even where other instructions require a feature branch.
- If STATUS.md says `source: auto`, a script reconstructed it from a session transcript. Summarize it for me and confirm it's right before relying on it.
- If STATUS.md has a merge conflict, don't hand-merge it. Write a fresh STATUS.md from both versions and the merged code, then continue the merge.
- STATUS.md is committed and may be public. Never put credentials, tokens, IP addresses, hostnames, internal URLs, or personal details in it; describe them generically ("the home server", "the API key").
<!-- shift-change:end -->

## Repo-specific

- `main` is protected (required `build` check). Do work and STATUS.md handoffs on `dev`; never commit to `main` directly. This overrides the "including main" line in the status block above.
