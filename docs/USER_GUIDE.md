# RehearsAll User Guide

RehearsAll is an audio practice tool designed for musicians learning pieces and speakers memorizing material. This guide walks through every feature.

## Table of Contents

- [Getting Started](#getting-started)
- [Playing Audio](#playing-audio)
- [Waveform](#waveform)
- [Markers & Editing](#markers--editing)
- [A-B Looping](#a-b-looping)
- [Bookmarks](#bookmarks)
- [Chunked Practice](#chunked-practice)
- [Playlists](#playlists)
- [Queue & Repeat](#queue--repeat)
- [Android Auto](#android-auto)
- [Settings](#settings)
- [Tips & Tricks](#tips--tricks)

---

## Getting Started

### Importing Audio Files

1. Tap the **+** button (floating action button) on the file list screen
2. The system file picker opens — select one or more audio files (tap individually or use long-press multi-select in the picker)
3. Files are copied to the app's private storage and appear in your library
4. When importing multiple files, a progress bar appears and a summary snackbar confirms how many were added

### Supported Formats

- MP3
- WAV
- OGG (Vorbis)
- FLAC
- M4A (AAC)

### Managing Files

- **Rename:** Long-press a file in the list, then tap the rename option
- **Delete:** Swipe a file left to remove it from your library (the original file is not affected)

### Multi-Select

Long-press any file to enter selection mode. A selection bar appears at the top of the list showing how many files are selected.

- **Select more:** Tap additional files to add them to the selection (or tap again to deselect)
- **Add to Playlist:** Tap the playlist icon in the selection bar, then choose a target playlist
- **Cancel:** Tap the **✕** button in the selection bar to exit selection mode

---

## Playing Audio

Tap any file in the library to open the playback screen.

### Transport Controls

| Control | Action |
|---------|--------|
| Play/Pause | Large center button |
| Skip Forward | Fast-forward button (configurable: 2s–30s) |
| Skip Backward | Rewind button (same increment) |
| Next Track | Skip to next file in queue |
| Previous Track | If more than 3 seconds into the track, restarts from the beginning. If within the first 3 seconds, skips to the previous file in the queue. |
| Shuffle | Toggle random queue order |
| Repeat | Cycle: Off → Repeat All → Repeat One |

### Speed Control

1. Tap the **speed icon button** (with a badge showing the current speed, e.g., "1.0x") below the transport controls
2. Use the slider to adjust from **0.25x** to **3.0x**
3. Pitch is preserved at all speeds
4. Preset buttons available for common speeds (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x)

Speed is saved per file and restored when you reopen it.

### Time Display

All timestamps throughout the app show tenths of seconds (e.g., "1:23.4") for precise positioning when editing loop boundaries or placing markers.

---

## Waveform

The interactive waveform gives you visual context for the audio.

### Main Playback Waveform

The waveform appears automatically on the playback screen when a loop is active. It shows:
- **Tap to seek** — tap anywhere to jump to that position
- **Pinch to zoom** — zoom in/out to see more or less detail
- **Zoom buttons** — dedicated +/- buttons below the waveform
- **Horizontal scroll** — pan through the waveform when zoomed in
- **Overlay list** — saved loops or chunk markers, shown in the top-left corner

The main waveform is **read-only** — loop boundary handles are not draggable here. Use the Markers sheet for editing.

### Dismissing the Waveform

When the overlay list is visible, tap the **✕ button** in its header to:
- Close the overlay list
- Clear the active loop
- Return to the default playback screen (track info only)

---

## Markers & Editing

Tap the **Markers icon button** below the transport controls to open the Markers bottom sheet. This is the central hub for managing all position-based markers.

### Markers Sheet Features

The sheet includes:
- **Interactive waveform** with play/pause button, time display, and zoom controls
- **Editable loop handles** — drag loop boundaries directly on the waveform
- **Overview bar** — minimap with drag-to-scroll when zoomed in
- **Three tabs:** Loops (default), Chunks, Bookmarks

---

## A-B Looping

A-B looping lets you repeat a section of audio continuously — perfect for practicing difficult passages.

### Creating a Loop

1. Tap **Markers** → the **Loops** tab is shown by default
2. Tap the **+** button to create a new loop (defaults to full track)
3. The loop editor card appears with Begin and End controls

### Loop Editor

The loop editor is a distinct card at the top of the Loops tab with:
- **Loop name** — shows the name if editing an existing loop, or "New Loop" for a new one
- **Close button (X)** — dismiss the editor and clear the active loop
- **Begin / End times** — tap the time to enter an exact value, or use the **+/- 0.25s** buttons for fine adjustment
- **Duration display** — shows the loop length
- **Save button** — saves the loop with current boundaries

### Adjusting Loop Boundaries

- **On the waveform** (Markers sheet only): Drag the A or B handle to fine-tune
- **+/- buttons:** Adjust each boundary by 0.25 seconds per tap
- **Direct time entry:** Tap the Begin or End time to type an exact value
- The loop must be at least 100ms long
- The Begin boundary cannot be dragged past the current playback position

### Saving Loops

- **New loop:** Tap Save → enter a name → the loop is saved and the editor closes
- **Existing loop:** Tap Save → confirm overwrite → the loop is updated with new boundaries

### Loading Loops

Tap any saved loop in the list to load it into the editor. The playback position jumps to the loop start.

### Loop Crossfade

When enabled in Settings, a smooth 50ms volume fade is applied at loop boundaries to eliminate clicks. Automatically disabled for very short loops (< 150ms).

### Clearing a Loop

Tap the **X** button on the loop editor to close it and stop looping.

---

## Bookmarks

Bookmarks are named positions in a file. Use them to mark sections, cue points, or rehearsal marks.

### Adding Bookmarks

1. Tap **Markers** below the transport controls
2. Switch to the **Bookmarks** tab (rightmost tab)
3. Tap the **+** button — a bookmark is created at the current playback position

### Managing Bookmarks

- **Navigate:** Tap any bookmark to seek to that position
- **Rename:** Tap the bookmark name to edit it
- **Reposition:** Adjust the bookmark's position
- **Delete:** Tap the delete icon next to a bookmark

---

## Chunked Practice

Chunked practice breaks a file into sections and drills them systematically. This is the core feature for memorization.

### Setting Up Chunks

1. Tap **Markers** → switch to the **Chunks** tab
2. Place markers at section boundaries by seeking to each point and tapping **+**
3. Markers divide the file into chunks (the region before the first marker is chunk 1, between first and second is chunk 2, etc.)

### Starting Practice

1. Tap **Start Practice** in the Chunks tab
2. The Practice Controls sheet opens with mode selection and settings

### Practice Modes

See [Practice Modes](Practice-Modes) for a detailed explanation of each mode.

**Quick summary:**

| Mode | Best For | How It Works |
|------|----------|--------------|
| Single Chunk Loop | Drilling one section at a time | Plays each chunk N times before moving on |
| Cumulative Build-Up | Memorizing in order | Chunk 1 (×N), then 1+2 (×N), then 1+2+3 (×N)... |
| Sequential Play | Full run-through | Plays all chunks in order, once each |

### Practice Controls

- **Repeat Count:** How many times to repeat each step (1–10)
- **Gap Between Reps:** Pause between repetitions (0–5 seconds)
- **Gap Between Chunks:** Pause between different chunks/steps (0–10 seconds)

### During Practice

- The active chunk is highlighted on the waveform
- A progress indicator shows: step X of Y, rep X of Y
- Use **Skip Forward/Back** to jump between steps
- Tap **Stop** to end the session early

**Note:** If you navigate away during practice, you'll be asked to confirm since the session will stop.

---

## Playlists

### Creating a Playlist

1. On the file list screen, tap the playlist icon in the top bar
2. Tap **Create Playlist** and enter a name

### Adding Files to Playlists

- Long-press a file in the library
- Select **Add to Playlist** and choose the target playlist

### Playing a Playlist

Tap a playlist to open it, then tap **Play All** to load all tracks into the queue.

### Reordering Playlist Tracks

Inside a playlist, each track has a **drag handle** (⠿) on the right side. Press and hold the handle to pick up the track, then drag it up or down to the desired position. Release to drop. The new order is saved automatically.

### Drag-and-Drop from Library

On the main library screen, each file card has a **drag handle** on the right side. Long-press and drag the handle, then drop it onto any playlist card to add the file to that playlist. The target playlist highlights with a border as you hover over it.

---

## Queue & Repeat

### Queue Management

Tap the **Queue** button on the playback screen to see the current queue.

- **Tap** a track to skip to it
- **Remove** a track with the delete button
- **Clear All** to empty the queue

### Repeat Modes

Cycle through repeat modes with the repeat button on the transport bar:

| Mode | Icon | Behavior |
|------|------|----------|
| Off | Repeat (dim) | Stops after the last track |
| Repeat All | Repeat (lit) | Loops the entire queue |
| Repeat One | Repeat One | Loops the current track |

### Shuffle

Toggle shuffle to randomize queue order. The current track continues playing.

---

## Android Auto

RehearsAll works with Android Auto for hands-free browsing and playback.

### Browsing

The content tree has three top-level folders:

- **Recent** — Last 20 played files
- **All Files** — Your full library. Files with saved loops show as folders containing "Full Track" plus each loop
- **Playlists** — All your playlists

### Search

Use voice or the search button to find files and playlists by name.

### Loop Toggle

On the Now Playing screen, use the custom **Toggle Loop** action to activate or deactivate the first saved loop for the current file.

### Setup

RehearsAll appears automatically in Android Auto's media app list. No manual setup is needed.

---

## Settings

Access settings via the gear icon on the playback screen's top bar.

### Appearance

Choose your theme:
- **Light** — Always light mode
- **Dark** — Always dark mode
- **Follow System** (default) — Matches your device setting

On Android 12+, colors adapt to your wallpaper via Material You dynamic color.

### Playback

- **Skip Increment** — Choose how far forward/back buttons skip: 2s, 5s (default), 10s, 15s, or 30s
- **Loop Crossfade** — When enabled, applies a smooth volume fade at loop boundaries for less jarring repeats (default: on)

---

## Tips & Tricks

- **Resume playback:** The app remembers your position and speed for every file. Reopen a file and it picks up where you left off.
- **Mini player:** When you navigate away from the playback screen, a mini player bar appears at the bottom showing the track name, elapsed time, and a play/pause button. Tap it to return to the full playback screen.
- **Notification controls:** Full transport controls are available in the notification and on the lock screen while audio is playing.
- **Headphone buttons:** Play/pause, next, and previous work with wired and Bluetooth headphone controls.
- **Waveform extraction:** The waveform is generated in the background when you first import a file. If it's still loading when you open the file, a simple slider is shown as a fallback.
- **File safety:** Imported files are copied to the app's private storage. Deleting the original file from your device won't affect the app's copy.
- **Precise editing:** Use the +/- buttons in the loop editor for 0.25s adjustments, or tap the time display to enter an exact value.
- **Quick loop loading:** Load any saved loop from the Markers sheet — the waveform and loop overlay will appear on the playback screen automatically.
