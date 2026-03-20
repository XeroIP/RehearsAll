# RehearsAll User Guide

RehearsAll is an audio practice tool designed for musicians learning pieces and speakers memorizing material. This guide walks through every feature.

## Table of Contents

- [Getting Started](#getting-started)
- [Playing Audio](#playing-audio)
- [Waveform](#waveform)
- [Bookmarks](#bookmarks)
- [A-B Looping](#a-b-looping)
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
2. The system file picker opens — select one or more audio files
3. Files are copied to the app's private storage and appear in your library

### Supported Formats

- MP3
- WAV
- OGG (Vorbis)
- FLAC
- M4A (AAC)

### Managing Files

- **Rename:** Long-press a file in the list, then tap the rename option
- **Delete:** Swipe a file left to remove it from your library (the original file is not affected)

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
| Previous Track | Skip to previous file in queue |
| Shuffle | Toggle random queue order |
| Repeat | Cycle: Off → Repeat All → Repeat One |

### Speed Control

1. Tap the **speed badge** (e.g., "1.00x") below the transport controls
2. Use the slider to adjust from **0.25x** to **3.0x**
3. Pitch is preserved at all speeds
4. Preset buttons available for common speeds (0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 2.0x)

Speed is saved per file and restored when you reopen it.

---

## Waveform

The interactive waveform replaces a traditional seek bar and gives you visual context for the audio.

### Gestures

| Gesture | Action |
|---------|--------|
| Tap | Seek to that position |
| Horizontal scroll | Pan through the waveform |
| Pinch | Zoom in/out (min = full file, max = ~2 seconds visible) |

### Overview Bar

When zoomed in, a thin overview bar appears below the waveform showing:
- The full file's amplitude as a minimap
- A highlighted rectangle showing your current viewport
- The playback cursor position

### Auto-Scroll

During playback, the waveform automatically scrolls to keep the playback cursor visible.

---

## Bookmarks

Bookmarks are named positions in a file. Use them to mark sections, cue points, or rehearsal marks.

### Adding Bookmarks

1. Tap **Markers** below the transport controls
2. In the **Bookmarks** tab, tap the **+** button
3. A bookmark is created at the current playback position

### Managing Bookmarks

- **Navigate:** Tap any bookmark to seek to that position
- **Rename:** Tap the bookmark name to edit it
- **Delete:** Tap the delete icon next to a bookmark

Bookmarks appear as vertical lines on the waveform.

---

## A-B Looping

A-B looping lets you repeat a section of audio continuously — perfect for practicing difficult passages.

### Setting a Loop

1. Tap **Markers** → switch to the **Loops** tab
2. Seek to the start of the section, then tap **Set A**
3. Seek to the end, then tap **Set B**
4. Playback now loops between A and B

### Visual Feedback

- The loop region appears as a semi-transparent overlay on the waveform
- **A** and **B** markers show as draggable handles at the edges

### Adjusting Loop Boundaries

- **Drag** the A or B handle on the waveform to fine-tune boundaries
- The loop must be at least 100ms long

### Saving Loops

1. With an active loop, tap **Save Loop**
2. Give it a name (e.g., "Chorus", "Bridge measure 4-8")
3. Saved loops appear in the list below

### Loading Loops

Tap any saved loop to activate it. The playback position jumps to the loop start.

### Clearing a Loop

Tap **Clear** to stop looping and return to normal playback.

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

See [Practice Modes](PRACTICE_MODES.md) for a detailed explanation of each mode.

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

For development testing with the Desktop Head Unit (DHU):

```bash
adb forward tcp:5277 tcp:5277
cd $ANDROID_HOME/extras/google/auto
./desktop-head-unit
```

---

## Settings

Access settings via the gear icon on the file list screen.

### Appearance

Choose your theme:
- **Light** — Always light mode
- **Dark** — Always dark mode
- **Follow System** (default) — Matches your device setting

On Android 12+, colors adapt to your wallpaper via Material You dynamic color.

### Playback

- **Skip Increment** — Choose how far forward/back buttons skip: 2s, 5s (default), 10s, 15s, or 30s
- **Loop Crossfade** — When enabled, applies a smooth volume fade at loop boundaries for less jarring repeats

---

## Tips & Tricks

- **Resume playback:** The app remembers your position and speed for every file. Reopen a file and it picks up where you left off.
- **Mini player:** When you navigate away from the playback screen, a mini player bar appears at the bottom so you can see what's playing and tap to return.
- **Notification controls:** Full transport controls are available in the notification and on the lock screen while audio is playing.
- **Headphone buttons:** Play/pause, next, and previous work with wired and Bluetooth headphone controls.
- **Waveform extraction:** The waveform is generated in the background when you first import a file. If it's still loading when you open the file, a simple slider is shown as a fallback.
- **File safety:** Imported files are copied to the app's private storage. Deleting the original file from your device won't affect the app's copy.
