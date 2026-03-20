# Security Overview

This document maps RehearsAll's security posture to the [OWASP Mobile Top 10 (2024)](https://owasp.org/www-project-mobile-top-10/). RehearsAll is a local-only audio practice app with no network communication, no authentication, and no cloud services — so many categories are not applicable. This review documents what protections exist and why certain risks don't apply.

---

## M1 — Improper Credential Usage

**Status: Not applicable**

RehearsAll has no credentials, API keys, tokens, or authentication of any kind. The app operates entirely offline with no backend services.

---

## M2 — Inadequate Supply Chain Security

**Status: Mitigated**

- All dependencies are pinned via Gradle version catalog (`libs.versions.toml`) with explicit version numbers
- Dependencies are sourced from Google's Maven repository and Maven Central only
- [Dependabot](https://docs.github.com/en/code-security/dependabot) or [Renovate](https://docs.renovatebot.com/) is recommended for automated dependency update PRs
- No third-party SDKs with broad permissions are included
- The app requests only the permissions it needs (no network, location, camera, etc.)

---

## M3 — Insecure Authentication/Authorization

**Status: Not applicable**

RehearsAll is a single-user local app. There is no authentication, no authorization, no user accounts, and no multi-user access control.

---

## M4 — Insufficient Input Validation

**Status: Mitigated**

| Input | Validation |
|-------|-----------|
| Audio file import | Format validated by MediaExtractor; files over 500MB rejected; UUID renaming prevents path traversal |
| Database queries | All Room queries use parameterized statements — no SQL injection possible |
| Waveform cache | Binary format validated via magic bytes + version header on load; corrupted caches are rejected and re-extracted |
| User-entered text | Bookmark names, loop names, playlist names are stored via Room parameterized queries |
| MediaSession commands | Custom command arguments validated (speed clamped to 0.25–3.0, loop regions must be ≥100ms) |

---

## M5 — Insecure Communication

**Status: Not applicable**

RehearsAll makes **zero network calls**. There is no telemetry, analytics, crash reporting, cloud sync, or any other network communication. The app has no INTERNET permission.

---

## M6 — Inadequate Privacy Controls

**Status: Mitigated**

- No personally identifiable information (PII) is collected
- Release build logging (`FileLoggingTree`) records only WARN+ level events using internal IDs and event types — no file paths, URIs, or user-entered names are logged
- Debug logging (`Timber.DebugTree`) is stripped from release builds via ProGuard rules
- No data leaves the device

---

## M7 — Insufficient Binary Protections

**Status: Mitigated**

- R8 minification and code shrinking enabled for release builds
- Resource shrinking enabled (`isShrinkResources = true`)
- `isDebuggable = false` in release build configuration
- ProGuard rules strip verbose/debug/info log calls from release binaries

---

## M8 — Security Misconfiguration

**Status: Mitigated**

| Area | Protection |
|------|-----------|
| File storage | All files stored in Android app sandbox (`context.filesDir`) — not accessible to other apps |
| Content providers | No exported content providers |
| MediaBrowser access | MediaLibrarySession uses standard Android Auto caller validation |
| Exported components | Only `MediaLibraryService` is exported (required for MediaSession/Auto), protected by media intent filter |
| Backup | Uses Android's default backup behavior; sensitive paths (databases, audio cache) should be excluded via backup rules in production |

---

## M9 — Insecure Data Storage

**Status: Mitigated**

- All user data (database, audio files, waveform cache, preferences) stored in app-private internal storage
- Android's sandbox prevents other apps from accessing this data
- Encryption at rest is provided by Android's File-Based Encryption (FBE), which is mandatory on all devices running Android 10+
- No data is written to external/shared storage

---

## M10 — Insufficient Cryptography

**Status: Not applicable**

RehearsAll does not implement any custom cryptography. At-rest encryption is handled entirely by Android's File-Based Encryption at the OS level. No data is transmitted, so transport encryption is not needed.

---

## Summary

| OWASP Category | Status | Notes |
|---------------|--------|-------|
| M1 — Credentials | N/A | No credentials |
| M2 — Supply Chain | Mitigated | Pinned deps, standard repos |
| M3 — Auth | N/A | Local single-user app |
| M4 — Input Validation | Mitigated | Parameterized queries, format checks |
| M5 — Communication | N/A | No network calls |
| M6 — Privacy | Mitigated | No PII, minimal logging |
| M7 — Binary Protection | Mitigated | R8, shrinking, non-debuggable |
| M8 — Misconfiguration | Mitigated | Sandboxed storage, no exported providers |
| M9 — Data Storage | Mitigated | App-private + FBE |
| M10 — Cryptography | N/A | No custom crypto |
