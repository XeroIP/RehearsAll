# Practice Modes — Deep Dive

RehearsAll offers three practice modes for chunked repetition. Each mode is designed for a different learning scenario. This document explains how each one works, when to use it, and gives step-by-step examples.

## Prerequisites

Before starting any practice mode, you need **chunk markers** placed in your audio file. Markers divide the file into sections:

```
|--- Chunk 1 ---|--- Chunk 2 ---|--- Chunk 3 ---|--- Chunk 4 ---|
0:00          0:30            1:00            1:45            2:30
               ^                ^                ^
           Marker 1         Marker 2         Marker 3
```

Three markers create four chunks. The region before the first marker is chunk 1, between markers is the middle chunks, and after the last marker is the final chunk.

---

## Mode 1: Single Chunk Loop

### When to Use

- Drilling **one difficult section** at a time
- Learning each chunk independently before connecting them
- Warming up or isolating technique

### How It Works

Plays each chunk in order, repeating it N times before moving to the next.

### Example

With 3 chunks and repeat count = 3:

```
Step 1: Chunk 1 × 3   (play chunk 1, repeat, repeat)
Step 2: Chunk 2 × 3   (play chunk 2, repeat, repeat)
Step 3: Chunk 3 × 3   (play chunk 3, repeat, repeat)
```

Total steps: 3 (one per chunk)

### Settings That Matter

| Setting | Effect |
|---------|--------|
| Repeat Count | How many times each chunk plays (1–10) |
| Gap Between Reps | Pause between repetitions of the same chunk |
| Gap Between Chunks | Pause before moving to the next chunk |

---

## Mode 2: Cumulative Build-Up

### When to Use

- **Memorizing material in order** (speeches, song lyrics, musical phrases)
- Building up to a full performance from scratch
- When the transition between sections is as important as the sections themselves

### How It Works

Starts with just chunk 1, then adds chunk 2, then chunk 3, and so on — each time replaying from the beginning.

### Example

With 4 chunks and repeat count = 2:

```
Step 1: Chunk 1              × 2
Step 2: Chunk 1 + Chunk 2    × 2
Step 3: Chunk 1 + 2 + 3      × 2
Step 4: Chunk 1 + 2 + 3 + 4  × 2
```

Total steps: 4. Each step is longer than the last because it includes all previous chunks.

### Why This Works

Cumulative build-up leverages **spaced repetition** naturally. Chunk 1 gets the most repetitions (it's in every step), chunk 2 gets one fewer, and so on. The sections you learn first are reinforced the most, which matches how memory consolidation works.

### Settings That Matter

| Setting | Effect |
|---------|--------|
| Repeat Count | How many times each cumulative step plays |
| Gap Between Reps | Pause between repetitions of the same cumulative block |
| Gap Between Chunks | Pause before the next (longer) cumulative step |

### Tips

- Start with repeat count 2–3. Higher counts make early steps very repetitive since the later steps already include all the repetition.
- Use a gap between chunks of 2–3 seconds to mentally prepare for the longer block.

---

## Mode 3: Sequential Play

### When to Use

- **Run-throughs** after you've practiced individual chunks
- Checking flow and transitions between sections
- Performance simulation

### How It Works

Simply plays all chunks in order, once each, with no repetition.

```
Step 1: Chunk 1   (once)
Step 2: Chunk 2   (once)
Step 3: Chunk 3   (once)
Step 4: Chunk 4   (once)
```

### Comparison with A-B Looping

| Feature | Sequential Play | A-B Loop |
|---------|----------------|----------|
| Scope | Entire file, broken into chunks | Any arbitrary section |
| Repetition | Once per chunk | Continuous |
| Automation | Hands-free through all chunks | Manual set/clear |
| Gaps | Configurable pauses between chunks | No pauses |
| Best for | Full practice run-throughs | Drilling one specific passage |

Use A-B looping when you want to hammer one passage. Use sequential play when you want to hear the whole piece with brief pauses between sections.

### Settings That Matter

| Setting | Effect |
|---------|--------|
| Gap Between Chunks | Pause between chunks (useful for mental preparation) |

Repeat count is ignored in this mode (each chunk plays exactly once).

---

## Skip Controls During Practice

During any practice mode, you can:

- **Skip Forward** — Jump to the next step (next chunk or next cumulative block)
- **Skip Back** — Jump to the previous step

This is useful when you realize you need more time on a section, or when you want to skip ahead past sections you've already mastered.

---

## Choosing the Right Mode

```
Are you learning new material?
├── Yes → Do the transitions matter?
│         ├── Yes → Cumulative Build-Up
│         └── No  → Single Chunk Loop
└── No  → Are you doing a run-through?
          ├── Yes → Sequential Play
          └── No  → Use A-B Looping instead
```
