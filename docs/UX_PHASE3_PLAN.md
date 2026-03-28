# RehearsAll UX Overhaul — Phase 3: Scale & Power Users

> **Status:** Future — not yet scheduled for implementation.
> **Prerequisite:** Phases 1 & 2 from [UX_OVERHAUL_PLAN.md](UX_OVERHAUL_PLAN.md) must be complete first.
> **Target audience:** Users with 100+ audio files who need advanced organization and navigation.

---

## 3.1 Alphabet Scroller (#35)
- Fast scroll indicator for Library (A-Z sidebar when sorted alphabetically)
- Uses LazyColumn `stickyHeader` for letter group headers
- Only visible when sorted by name; hidden for other sort modes

## 3.2 Batch Operations (#36)
- Multi-select mode (already exists via long-press) enhanced with:
  - Select All / Deselect All in selection bar
  - Batch delete confirmation
  - Batch add to playlist
  - Batch move between playlists

## 3.3 Recently Played Intelligence (#37)
- "Continue Practicing" card on Recents tab showing last file with position
- "Pick up where you left off" — one-tap resume with saved speed and position
- Uses existing `lastPositionMs` and `lastSpeed` fields from AudioFileEntity

## 3.4 Playlist Enhancements (#38)
- Duplicate playlist (copy with all tracks, "(Copy)" suffix)
- Merge playlists (combine two or more, skip duplicates)

## 3.5 Smart Playlists (#40)
- Auto-populate based on criteria (e.g., "Recently Added", "Most Practiced")
- Predefined rules first, custom rule builder as stretch goal
- Visually distinguished from manual playlists
- **Needs refinement** before implementation

## 3.6 Adaptive Layouts / Tablet (#39)
- Library + Playback side-by-side on tablets (list-detail pattern)
- Use existing WindowSizeClass support
- Bottom nav converts to NavigationRail on expanded width
- **Needs refinement** before implementation

---

## Implementation Priority

| Order | Item | Issue |
|-------|------|-------|
| 1 | Alphabet scroller | #35 |
| 2 | Batch operations | #36 |
| 3 | Recently played intelligence | #37 |
| 4 | Playlist enhancements | #38 |
| 5 | Smart playlists | #40 |
| 6 | Adaptive tablet layouts | #39 |
