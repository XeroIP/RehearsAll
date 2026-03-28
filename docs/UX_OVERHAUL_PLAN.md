# RehearsAll UX Overhaul Plan

## Context
RehearsAll is a functional Android audio practice app with solid architecture but significant UX gaps. The primary persona is **musicians** learning parts. The biggest pain point is **discoverability** — key features are hidden behind gestures (long-press, swipe) that users won't find without guidance. The app draws inspiration from **Spotify/Apple Music** patterns and needs to scale from small libraries (<20 files) to large ones (100+). This plan covers a full app UX audit across all screens.

---

## Phase 1 — Foundation & Discoverability (Small libraries, <20 files)

### 1.1 Skippable Onboarding Flow
- **3-4 screen tutorial** on first launch (stored in DataStore `hasCompletedOnboarding`)
- Screen 1: "Import your practice files" — show import button with supported formats
- Screen 2: "Control your playback" — highlight speed, looping, waveform
- Screen 3: "Organize with playlists" — show playlist creation
- Screen 4: "Practice smarter" — introduce chunked practice modes
- "Skip" button on every screen, "Get Started" on last screen
- **Files:** New `ui/onboarding/` package (OnboardingScreen.kt, OnboardingViewModel.kt), update NavGraph.kt

### 1.2 Bottom Navigation Bar
Replace the current flat single-screen layout with a **3-tab bottom navigation bar**:
- **Library** — all imported audio files (current file list minus playlist section)
- **Playlists** — dedicated playlist list screen
- **Recents** — recently played files (uses existing `AudioFileDao.getRecent()`)

**Why bottom nav over top tabs:** Thumb-reachable on phones (primary device), Spotify-familiar pattern, clear separation of concerns. Mini player sits directly above the bottom bar.

**Files:**
- New `ui/library/LibraryScreen.kt` (extract from FileListScreen.kt)
- New `ui/playlists/PlaylistListScreen.kt` (extract playlist section from FileListScreen.kt)
- New `ui/recents/RecentsScreen.kt` + RecentsViewModel.kt
- Update `ui/navigation/NavGraph.kt` — add NavigationBar, restructure routes
- Update `ui/common/MiniPlayer.kt` — position above bottom nav

### 1.3 Three-Dot Menu Per Item
Add a visible **overflow icon (⋮)** on every file and playlist row:
- **File row menu:** Play, Add to Playlist, File Details, Rename, Delete
- **Playlist row menu:** Play All, Rename, Delete
- Keep **long-press for multi-select** mode (power user pattern)
- Add a subtle tooltip/hint on first use: "Long-press to select multiple files"

**Why both:** 3-dot menus solve discoverability for single-item actions. Long-press multi-select is efficient for batch operations but needs a one-time hint.

**Files:**
- Update `ui/filelist/FileListScreen.kt` — add IconButton with DropdownMenu per AudioFileCard
- Update `ui/playlist/PlaylistScreen.kt` — add per-track overflow menu
- New shared composable `ui/common/ItemOverflowMenu.kt`

### 1.4 Contextual Empty States with CTAs
Replace passive empty state text with **actionable buttons**:

| Screen | Current | Proposed |
|--------|---------|----------|
| Library | "No audio files yet. Tap + to import" | Import button + illustration |
| Playlist (no tracks) | "Long-press a file..." | **"Add Tracks" button** (opens file picker) |
| Recents | N/A (new screen) | "Play a file to see it here" |
| Queue | "Queue is empty" | "Add files from your library" with nav button |
| Bookmarks tab | "No bookmarks yet. Tap +" | Keep (FAB is visible) |

**Files:**
- Update `ui/playlist/PlaylistScreen.kt` — PlaylistEmptyState with "Add Tracks" button
- Update `ui/filelist/FileListScreen.kt` — enhanced empty state
- New `ui/recents/RecentsScreen.kt` — empty state composable

### 1.5 Import Progress Indicator (Issue #18)
- Replace indeterminate `LinearProgressIndicator` with a **determinate** progress bar
- Show count label during import: "Importing 3 of 7 files..."
- Expose `importProgress: StateFlow` (with `current`, `total` fields) from ViewModel
- Keep summary snackbar at the end

**Why here:** The import flow is the first thing new users hit. With parallelized imports (from #22 fix), a progress count is even more valuable since completion order is less predictable.

**Files:**
- Update `ui/filelist/FileListViewModel.kt` — add progress tracking StateFlow
- Update `ui/filelist/FileListScreen.kt` — determinate progress bar + count label

### 1.6 Add Tracks from Within Playlist (Issue #25)
When user taps "Add Tracks" (from empty state, overflow menu, or a new action button):
- Open a **full-screen or bottom sheet file picker** showing all library files
- Files already in the playlist show a **checkmark** and are visually dimmed
- Multi-select with "Add Selected (N)" button at bottom
- Snackbar confirmation: "Added N tracks to [playlist name]"

**Data layer:**
- Add `AudioFileDao.getFilesNotInPlaylist(playlistId)` query
- Add `PlaylistRepository.addFilesToPlaylist(playlistId, fileIds: List<Long>)` batch method
- Add `PlaylistViewModel.addTracks()` action

**Files:**
- New `ui/playlist/AddTracksScreen.kt` or `ui/common/TrackPickerBottomSheet.kt`
- Update `data/db/dao/AudioFileDao.kt` — new query
- Update `data/repository/PlaylistRepository.kt` — batch add method
- Update `ui/playlist/PlaylistViewModel.kt` — new actions and state

---

## Phase 2 — Polish & Efficiency (Medium libraries, 20-100 files)

### 2.1 Search
- Add **search bar** to Library tab (expandable in top app bar)
- Filter files by display name and artist in real-time
- Add search to Playlists tab (filter by playlist name)
- Leverage existing `ContentTreeBuilder.search()` pattern for consistency

**Files:**
- Update Library/Playlists screens with SearchBar composable
- Update ViewModels with search query StateFlow and filtered list logic

### 2.2 Playlist Visual Identity
- **Auto-generated mosaic** from track metadata (if album art exists) or colored gradient tiles
- **User-customizable:** Pick icon (from curated set) or accent color per playlist
- Store in `PlaylistEntity` — add `iconName: String?` and `colorHex: String?` columns

**Files:**
- Update `data/db/entity/PlaylistEntity.kt` — new columns + migration
- New `ui/common/PlaylistCover.kt` composable
- Update playlist list and detail screens to display covers

### 2.3 Sorting & Filtering
- Library: Sort by name, date imported, last played, duration
- Playlists: Sort by name, date created, recently updated
- Persist sort preference in DataStore
- Use Material 3 FilterChip row or dropdown menu

**Files:**
- Update Library/Playlists ViewModels — sort enum + preference
- Update DataStore preferences

### 2.4 Playback Screen Progressive Disclosure
Address the density concern for the playback screen:
- **Default view:** Track info, time slider, transport bar, speed/markers/queue buttons — clean and focused
- **Waveform:** Shown when user taps a "Waveform" toggle or button (not always visible)
- **Practice controls:** Accessed via dedicated button, opens bottom sheet (current pattern is fine)
- Consider grouping Speed + Markers + Queue + Practice into a single **action row** with clear labels

**Files:**
- Update `ui/playback/PlaybackScreen.kt` — conditional waveform display
- Update `PlaybackContent` layout

### 2.5 Gesture Hints & Tooltips
- First-time hints using Material 3 `RichTooltip` or a coach mark overlay:
  - "Swipe left to delete" on first file list view
  - "Long-press to select multiple" on first file interaction
  - "Pinch to zoom waveform" on first playback screen visit
- Track shown hints in DataStore (map of hint keys → boolean)

**Files:**
- New `ui/common/CoachMark.kt` composable
- Update DataStore preferences for hint tracking
- Add hints to FileListScreen, PlaybackScreen, WaveformView

---

## Phase 3 — Scale & Power Users

Phase 3 is tracked separately in [UX_PHASE3_PLAN.md](UX_PHASE3_PLAN.md). It covers alphabet scrolling, batch operations, recently played intelligence, playlist power features, smart playlists, and adaptive tablet layouts (issues #35–#40).

---

## Cross-Cutting Concerns

### Animations & Transitions
- Shared element transitions between file list item and playback screen
- Predictive back gesture support (already mentioned in CLAUDE.md)
- Smooth bottom sheet animations (Material 3 defaults)

### Accessibility
- Content descriptions on all new interactive elements (3-dot menus, new buttons)
- Ensure touch targets ≥ 48dp
- Screen reader support for onboarding flow

### Testing Strategy
- **Unit tests:** New ViewModels (Recents, updated Playlist), new DAO queries, batch operations
- **UI tests:** Onboarding flow, bottom nav switching, add-tracks picker, search filtering
- **Regression:** Existing playback, loop, and practice tests must pass

---

## Implementation Priority

| Order | Item | Phase | Issue |
|-------|------|-------|-------|
| 1 | Bottom navigation bar | 1.2 | #26 |
| 2 | 3-dot menu per item | 1.3 | #27 |
| 3 | Create playlist from picker when empty | 1.3 | #43 |
| 4 | Add tracks from playlist | 1.6 | #25 |
| 5 | Contextual empty states | 1.4 | #28 |
| 6 | Import progress indicator | 1.5 | #18 |
| 7 | Skippable onboarding | 1.1 | #29 |
| 8 | Search | 2.1 | #30 |
| 9 | Playlist visual identity | 2.2 | #31 |
| 10 | Sorting & filtering | 2.3 | #32 |
| 11 | Playback progressive disclosure | 2.4 | #33 |
| 12 | Gesture hints | 2.5 | #34 |

Phase 3 items (12–17) are tracked in [UX_PHASE3_PLAN.md](UX_PHASE3_PLAN.md).

---

## Verification
- Run full test suite after each item: `./gradlew test` (unit) + `./gradlew connectedAndroidTest` (UI)
- Manual verification on phone emulator (API 26 min, API 36 target)
- Manual verification on tablet emulator for layout changes
- Theme switching (light/dark/system) after every UI change
- Test with 0, 5, 50, and 150+ files to verify scale behavior
