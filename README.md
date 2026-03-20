# RehearsAll

[![Build & Test](https://github.com/XeroIP/RehearsAll/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/XeroIP/RehearsAll/actions/workflows/build-and-test.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

**RehearsAll** is an Android audio practice app for musicians and speakers. Import audio files, set bookmarks, create A-B loops, break tracks into chunks for repetition drills, and manage playlists — all with a clean Material You interface and full Android Auto support.

## Features

- **Audio import** — Import MP3, WAV, OGG, FLAC, and M4A files via the system file picker
- **Interactive waveform** — Pinch-to-zoom, scroll, and tap-to-seek with real-time playback cursor
- **Variable speed** — 0.25x to 3.0x playback with pitch preservation
- **Bookmarks** — Drop, rename, delete, and tap to navigate
- **A-B looping** — Set loop points visually on the waveform, save and load loops, drag boundaries to adjust
- **Chunked practice** — Place markers, then run three practice modes:
  - **Single chunk loop** — repeat each chunk N times
  - **Cumulative build-up** — progressively add chunks (1, 1+2, 1+2+3, ...)
  - **Sequential play** — play through all chunks in order
- **Playlists** — Create, reorder, and manage playlists with queue controls
- **Android Auto** — Browse files, playlists, and loops; search; toggle loops from Now Playing
- **Theme support** — Light, Dark, and Follow System with Material You dynamic colors
- **Configurable skip** — 2s, 5s, 10s, 15s, or 30s skip increment
- **Resume playback** — Remembers position and speed per file
- **Notification & lock screen** — Full transport controls via MediaSession

## Architecture

Built with modern Android stack:

- **Kotlin** + **Jetpack Compose** (Material Design 3)
- **Media3 ExoPlayer** in a `MediaLibraryService` foreground service
- **Room** database (7 entities, 7 DAOs)
- **Hilt** dependency injection
- **DataStore** for user preferences
- **Coroutines** + **Flow** for reactive data
- **Timber** for structured logging

The playback architecture separates concerns:
- **Service side** (`RehearsAllPlaybackService`) — owns the ExoPlayer, enforces loop regions, serves the Android Auto content tree
- **App side** (`PlaybackManagerImpl`) — communicates via `MediaController` commands only, polls position at ~60fps

## Documentation

- [User Guide](docs/USER_GUIDE.md) — Full feature walkthrough
- [Practice Modes](docs/PRACTICE_MODES.md) — Deep dive on the three practice modes
- [Security](docs/SECURITY.md) — OWASP Mobile Top 10 security overview

## Building

### Prerequisites

- Android Studio (latest stable)
- JDK 21
- Android SDK 36

### Build

```bash
./gradlew assembleDebug
```

### Run tests

```bash
./gradlew testDebugUnitTest
```

## Installing

Download the latest APK from the [Releases](https://github.com/XeroIP/RehearsAll/releases) page and sideload it:

1. Enable "Install from unknown sources" for your file manager
2. Open the downloaded APK
3. Tap Install

## Android Auto Setup

RehearsAll appears automatically in Android Auto's media app list. Features available:

- **Browse** — Recent files, All Files, Playlists
- **Files with loops** — Expand to see "Full Track" + individual saved loops
- **Search** — Find files and playlists by name
- **Toggle Loop** — Custom action on the Now Playing screen

To test with the Desktop Head Unit (DHU):

```bash
adb forward tcp:5277 tcp:5277
cd $ANDROID_HOME/extras/google/auto
./desktop-head-unit
```

## License

```
Copyright 2026 Peter (XeroIP)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for the full text.
