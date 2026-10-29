# Sprint 4: Workout history & detail

**Project:** Fitness Tracker (Android Development, Fall 2026)
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Wireframe screens covered:** S17 Workout history, S18 Workout detail, S30 Delete confirmation, S31 Empty history
**GitHub issue:** [#4 Sprint 4: Workout history & detail](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/4)
**Branch:** `sprint-4` (branched from `sprint-3`, since Sprint 3 isn't merged into `dev` yet)
**Status:** Built. Waiting for sprint review.

---

## 1. Sprint goal

Users can **browse saved workouts, open one, edit it, repeat it, share it, save it as a template, or delete it**. The Workouts tab is no longer an empty placeholder.

**Done when:** the history list shows saved workouts grouped by month, filters and search work, detail edit saves, Repeat pre-fills S9, and delete + Undo works.

---

## 2. What changed at a glance

| Area | New / changed files |
|---|---|
| Data | `Workout.kt` (`withRecalculatedTotals`, `toRepeatTemplate`), `WorkoutRepository.kt` (`observe`, `observeHistory`), `FirestoreFlows.kt` (document listener) |
| Session | `ActiveWorkoutSession.startFromWorkout` |
| S17 / S31 | **`WorkoutsViewModel.kt`**, `WorkoutsScreen.kt` rewritten |
| S18 / S30 | **`WorkoutDetailViewModel.kt`**, **`WorkoutDetailScreen.kt`**, `DeleteWorkoutDialog` in `WorkoutDialogs.kt` |
| Chart | **`HeartRateChart.kt`** (empty until Sprint 9 has samples; reused in Sprint 5) |
| Wiring | `Routes.kt`, `MainScreen.kt` |
| Index | **`docs/firestore.indexes.json`** (type + startedAt) |

**Bold** = new file.

---

## 3. How history flows through the app

```
Workouts tab (S17)
  filter chips + search + month headers
  tap a row ──────────────────────────────► S18 detail
  long-press a row ──► S30 confirm ──► delete + Undo snackbar (5 s)
  + / Start First Workout ──► S7 sheet (unchanged)

S18 detail
  Edit ──► change notes and sets ──► Save writes the same Firestore document
  Repeat Workout ──► ActiveWorkoutSession.startFromWorkout() ──► S9
  ⋮ Save as template ──► templates/{id}
  ⋮ Share ──► Android share sheet
  ⋮ Delete ──► S30 ──► delete + Undo
```

---

## 4. Step-by-step explanation

### Step 1: Paged query (`WorkoutRepository.observeHistory`)

Newest first, optional type filter, `limit` starts at 20 and grows when the list is scrolled to the bottom.

- **All:** `orderBy("startedAt", DESC).limit(n)` — Firestore auto-indexes a single field.
- **Strength / Run / Walk / Cycle:** `whereEqualTo("type", type).orderBy("startedAt", DESC).limit(n)` — needs the **composite index** in `docs/firestore.indexes.json`.

The listener is a `callbackFlow`, same as Sprint 2's profile listener, so a workout saved from S12 appears on the Workouts tab without a refresh.

`observe(uid, id)` is a live document listener for S18. If the workout is deleted, it emits null.

### Step 2: S17 list (`WorkoutsViewModel` + `WorkoutsScreen`)

| Control | What it does |
|---|---|
| Search | Filters **loaded** items by name, exercise name or notes (client-side, so it does not need another index) |
| Chips | All, Strength, Run, Walk, Cycle. Changing a chip resets the page size to 20 |
| Month headers | `MMMM yyyy`, from `startedAt` |
| Row | Name, type, date, duration, sets and volume (kg or lb from Profile) |
| Empty (S31) | "No workouts yet" + **Start First Workout** — only when the user has zero workouts |
| No matches | Shown when a filter or search hides everything |

Long-press opens **S30**. Confirm deletes the Firestore document. A snackbar offers **Undo for 5 seconds**, which writes the same document id back.

### Step 3: S18 detail

Stats: duration, volume, avg bpm (`--` until Sprint 9), sets, calories, exercise count.

Heart-rate chart is a Compose `Canvas` line. There are no samples yet (no watch / Health Connect), so it shows "No heart-rate data". The composable is ready for Sprint 5 and 9.

**Edit** turns each set into weight + reps (or seconds) fields and a notes box. Save recalculates `totalSets` and `totalVolumeKg` and overwrites the document. Weights are converted to kg before save, same as Sprint 2 body stats.

**Repeat Workout** copies the exercises and last-set numbers into a new S9 session. If a workout is already running, it does not discard it — a snackbar says to finish or discard first.

The ⋮ menu: Save as template, Share, Delete.

### Step 4: Composite index

The first time you tap **Strength** (or Run / Walk / Cycle) on a device that has never run that query, Firestore may fail with a link in Logcat:

```
FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/...
```

Open the link, wait a minute, and the filter works. The same definition is in [`docs/firestore.indexes.json`](firestore.indexes.json). **All** does not need this index.

No security-rule change. Sprint 1's `users/{uid}/**` already covers these documents.

---

## 5. Jetpack Compose and Kotlin concepts introduced this sprint

| Concept | Where | Purpose |
|---|---|---|
| `flatMapLatest` | `WorkoutsViewModel` | New listener when the filter or page size changes |
| `FilterChip` + `LazyRow` | S17 | Type filter |
| `combinedClickable` | S17 rows | Tap = open, long-press = delete |
| `LazyColumn` grouped items | S17 | Month headers + rows |
| `viewModel(key = workoutId)` | S18 | One ViewModel per workout, not one for every detail visit |
| Compose `Canvas` | `HeartRateChart` | Line chart without a charting library |
| Snackbar Undo (5 s) | S17, S18 | Restore the deleted document |

---

## 6. How to test this sprint

| # | Test | Expected result |
|---|---|---|
| 1 | Workouts tab with no saved workouts | S31 empty state and **Start First Workout** |
| 2 | Finish a strength workout, open Workouts | The new row is under this month, with duration and sets |
| 3 | Tap the row | S18: stats, exercises, notes, Repeat, Delete |
| 4 | Search for an exercise name | Only matching workouts remain |
| 5 | Chip **Strength** | Strength workouts only. If Logcat shows an index link, create the index and retry |
| 6 | Chip **Run** | "No matching workouts" until Sprint 8 |
| 7 | S18 **Edit**, change a weight, Save | Firestore `weightKg` and `totalVolumeKg` update; list subtitle updates |
| 8 | **Repeat Workout** | S9 opens, timer running, same exercises and last-set numbers |
| 9 | Repeat while a workout is already running | Snackbar; the in-progress workout is kept |
| 10 | ⋮ **Save as template**, then Home **+** | New template chip |
| 11 | ⋮ **Share** | Android share sheet with the same text as S16 |
| 12 | Long-press a row → Delete → Undo within 5 s | Row comes back, same Firestore id |
| 13 | S18 Delete, wait 5 s | Returns to the list; document is gone |
| 14 | Switch Profile units to lb | History and detail show pounds; Firestore stays kg |

---

## 7. What we did not do (on purpose)

- **Heart-rate samples on the chart.** `avgHr` and the line stay empty until Sprint 9 / 10.
- **Outdoor distance on the row.** Run / Walk / Cycle rows will gain distance in Sprint 8.
- **Merging into `dev`.** This PR targets `sprint-3`. After PRs #12 and #13 merge, switch the base to `dev`.
