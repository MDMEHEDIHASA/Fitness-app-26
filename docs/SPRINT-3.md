# Sprint 3: Strength Workout Logging

**Project:** Fitness Tracker (Android Development, Fall 2026)
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Wireframe screens covered:** S9 Active strength workout, S10 Add Exercise, S11 Rest timer, S12 Finish dialog, S16 Summary (strength version). Also updates S5 Home and S7 Start Workout sheet.
**GitHub issue:** [#3 Sprint 3: Strength workout logging](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/3)
**Branch:** `sprint-3` (branched from `sprint-2`, since Sprint 2 isn't merged into `dev` yet)
**Status:** Builds with no errors or warnings. Waiting for sprint review.

---

## 1. Sprint goal

The core feature of the app: a user can **start a strength workout, add exercises, log sets, rest between sets, finish with a rating and notes, and save it to Firestore**. They can also save the workout as a **template** and start from it next time.

**Done when** (from the sprint plan): a full workout (2+ exercises, several sets, rest timer, finish with rating and notes) appears in Firestore under `users/{uid}/workouts`, and "Save as template" creates a reusable template.

---

## 2. What changed at a glance

| Area | New / changed files |
|---|---|
| Models | **`Workout.kt`** (Workout, ExerciseEntry, SetEntry, Exercise, WorkoutTemplate, enums), **`ExerciseLibrary.kt`**, **`Calories.kt`** |
| Repositories | **`WorkoutRepository.kt`**, **`ExerciseRepository.kt`**, **`TemplateRepository.kt`**, **`FirestoreFlows.kt`** (shared helpers) |
| Session | **`data/session/ActiveWorkoutSession.kt`** |
| S9 / S11 / S12 | **`ActiveWorkoutScreen.kt`**, **`ActiveWorkoutViewModel.kt`**, **`RestTimerSheet.kt`**, **`WorkoutDialogs.kt`** |
| S10 | **`AddExerciseScreen.kt`**, **`AddExerciseViewModel.kt`** |
| S16 | **`WorkoutSummaryScreen.kt`**, **`WorkoutFormat.kt`** (shared text formatting) |
| S7 | `StartWorkoutSheet.kt` (templates, resume), **`StartWorkoutViewModel.kt`** |
| S5 | `HomeScreen.kt`, `HomeViewModel.kt` (real active minutes, calories, weekly count, Resume card) |
| Wiring | `FitnessApp.kt` (new repositories + session), `Routes.kt`, `MainScreen.kt`, `MainScreenScaffold.kt` (✕ button), `AndroidManifest.xml` (`VIBRATE`) |

**Bold** = new file. Total: 25 files (17 new, 8 changed).

---

## 3. How a workout flows through the app

```
S7 sheet ── Strength / template chip ──► ActiveWorkoutSession.start()
                                               │
                                               ▼
                         S9 Active workout  (workout/strength)
                          │      ▲      │             │
          + Add Exercise  │      │      │ ✓ a set     │ Finish Workout
                          ▼      │      ▼             ▼
            S10 Add Exercise ────┘   S11 Rest sheet   S12 Finish dialog
          (workout/strength/add)    (counts down,       │ Finish & Save
                                     vibrates at 0)     ▼
                                              WorkoutRepository.save()
                                                        │
                                                        ▼
                                    S16 Summary  (workout/summary)
                                     Done │ Discard (deletes) │ Share
                                          ▼
                                 back to where you started
```

**Navigation details**
- S9 → S16 uses `popUpTo(STRENGTH_WORKOUT) { inclusive = true }`, so Back from the summary can't reopen a finished workout.
- **System Back on S9 does not end the workout.** It goes back to Home, where a **"Workout in progress · Resume"** card appears. Only **✕** (with a confirm dialog) discards it. This matches how gym apps behave: you can check something else mid-workout without losing it.
- If a workout is already running, **Strength training** in the S7 sheet says "In progress · tap to resume", and template chips are disabled, so there is never more than one workout at a time.

---

## 4. Step-by-step explanation

### Step 1: Data models (`Workout.kt`)

All fields have default values, because Firestore needs a no-argument constructor to read documents back into Kotlin objects (same rule as `UserProfile`).

| Class | Purpose |
|---|---|
| `Exercise` | One pickable exercise: `id`, `name`, `muscleGroup`, `trackedBy`, `custom` |
| `SetEntry` | One logged set: `weightKg`, `reps`, `durationSec`, `completed` |
| `ExerciseEntry` | An exercise inside a saved workout, with its list of sets |
| `Workout` | The saved document: type, name, start/end time, duration, exercises, totals, calories, rating, notes, source |
| `TemplateExercise`, `WorkoutTemplate` | A reusable workout: exercises with set count and target reps/weight |

Three enums: `WorkoutType` (STRENGTH, RUN, WALK, CYCLE; the last three are for Sprint 8), `MuscleGroup` (Chest, Back, Legs, Shoulders, Arms, Core, Cardio), and `TrackedBy` (REPS or TIME). Firestore stores enums as their name, for example `"CHEST"`.

**Weights are always stored in kg**, just like body stats in Sprint 2. If the user picked lb / mi, the screens show and accept pounds and convert when saving.

### Step 2: Built-in exercise library (`ExerciseLibrary.kt`)

43 exercises covering every muscle group, for example Bench Press, Deadlift, Back Squat, Overhead Press, Barbell Curl, Plank, Rowing Machine. Plank, Rowing Machine and Jump Rope are **TIME** exercises (logged in seconds); the rest are **REPS**.

The library lives in code, not Firestore: it's the same for everyone, works offline, and costs no reads. Each exercise has a fixed ID such as `bench_press`, because saved workouts refer to it (Sprint 5 personal records will group by this ID).

### Step 3: Repositories

| Repository | Firestore path | Functions |
|---|---|---|
| `WorkoutRepository` | `users/{uid}/workouts` | `save`, `delete`, `observeSince(startMillis)` |
| `ExerciseRepository` | `users/{uid}/customExercises` | `observeAll` (library + custom, sorted by name), `addCustom` |
| `TemplateRepository` | `users/{uid}/templates` | `observeTemplates` (newest first), `save` |

**Offline-friendly writes.** Firestore writes to its local cache instantly, but the `Task` only completes once the **server** confirms. With no internet it would never complete. If S12 waited for it, "Finish & Save" would spin forever in a gym basement. So these writes are **fire-and-forget**: we create the document ID locally (`collection.document()`), call `set()`, and move on. Firestore uploads it when the phone is back online. Failures are logged (`logFailure` in `FirestoreFlows.kt`).

`FirestoreFlows.kt` also has `Query.observe { }`, which wraps a snapshot listener in a `callbackFlow` (the same pattern as `observeProfile` in Sprint 2, now reusable for every list).

### Step 4: `ActiveWorkoutSession` (the heart of this sprint)

One shared object, created once in `AppContainer`, holds the workout in progress:

```
ActiveWorkout
├── name, startedAt, elapsedSec, paused
├── units, bodyWeightKg, defaultRestSec     ← copied from the profile at start
├── rest: RestTimer? (id, totalSec, remainingSec, nextLabel)
└── exercises: [ActiveExercise]
      ├── exercise, collapsed
      └── sets: [ActiveSet (weight: String, reps: String, completed)]
```

**Why not keep it in a ViewModel?** A ViewModel belongs to one screen. The workout must survive moving between S9 and S10, rotating the phone, **and** leaving to Home and coming back. An app-wide object does all three. Screens read it as a `StateFlow` and call functions such as `addSet`, `toggleSetCompleted` and `moveExercise`.

**Timer.** A coroutine ticks every 0.5 s and recomputes the time from the **wall clock** (`System.currentTimeMillis()`), not by adding 1 each tick. So even if a tick is late, the timer never drifts. Pause stores the time so far; Resume continues from it.

**Text fields stay strings** (`"62.5"`, `""`) while the user is typing. They're only converted to numbers when the workout is saved. Otherwise typing "6" then "2" then "." would be impossible, because `62.` isn't a valid number yet.

**Building the saved workout (`buildWorkout`)**: only **checked** sets are saved, and exercises with no checked sets are left out. It computes `totalSets`, `totalVolumeKg` (sum of kg × reps), duration and calories.

**Known limitation:** the session is kept in memory. If Android kills the app process while it's in the background (rare, usually after a long time), the unsaved workout is lost. The plan is to move it into a foreground service in Sprint 8, which outdoor GPS tracking needs anyway.

### Step 5: S9 Active strength workout (`ActiveWorkoutScreen.kt`)

| Wireframe | UI | Behavior |
|---|---|---|
| ✕ (top left) | Close icon | "Discard workout?" dialog → deletes the session |
| Title + timer | Workout name, `24:13` | Timer is grey while paused |
| ⋮ menu | Pause / Resume, Reorder, Rename, Save as template | See below |
| Live strip | ♥ `--` bpm · steps `--` · 🔥 kcal | kcal is a live estimate; HR and steps arrive in Sprints 9–10 |
| Exercise card | Name, "2 / 3 sets", ▾, ⋮ (Remove exercise) | Tap the header to collapse / expand |
| Set table | Set · kg (or lb) · Reps (or Sec) · ✓ · ✕ | Number keypad; checked rows are highlighted |
| + Add set | Text button | New set copies the previous set's weight and reps |
| + Add Exercise | Button | Opens S10 |
| Finish Workout | Button | Opens S12 |

- **Number keypad:** weight uses `KeyboardType.Decimal` and reps use `KeyboardType.Number`. Input is filtered with a regex: up to 4 digits plus 2 decimals for weight, and up to 4 digits for reps. A comma is turned into a dot, for keyboards in other languages.
- **Reorder:** turns on ↑ / ↓ buttons on each card and hides the set tables, so the list is short and easy to rearrange.
- **Save as template:** asks for a name. The template stores each exercise's number of sets and its last set's weight and reps as targets.
- **Rename:** the default name is "Strength workout"; templates use the template's name.
- `LazyColumn` items use the exercise's stable `key`, so `Modifier.animateItem()` animates reordering, adding and removing.
- `imePadding()` keeps the focused field above the keyboard.

### Step 6: S10 Add Exercise (`AddExerciseScreen.kt`)

- **Search** field (case-insensitive, matches anywhere in the name).
- **Muscle-group chips:** All, Chest, Back, Legs, Shoulders, Arms, Core, Cardio.
- **Multi-select** with checkboxes. The button counts them: "Add 3 Exercises". Exercises are added in the order they were tapped.
- **Create custom exercise** dialog: name, muscle group, and "Weight × reps" or "Time". It's saved to `customExercises`, marked "Custom" in the list, and auto-selected. The name is pre-filled from the search text (if you searched "Cable crossover" and found nothing, it's already typed in).

`AddExerciseViewModel` uses `combine(exercises, query, group, selected)` so the list re-filters instantly as you type.

### Step 7: S11 Rest timer (`RestTimerSheet.kt`)

- Starts **automatically when a set is checked** (not when one is unchecked), using **Default rest timer** from Profile & Settings (S28).
- Shows a circular countdown, **"Next: Bench Press · Set 3 of 3"** (the next unchecked set, or "All sets checked"), and a `-- bpm` heart-rate-recovery line for Sprint 10.
- **−15s / +15s / Skip.** −15s below zero ends the rest.
- **Vibrates** (two short buzzes) when it reaches 0, through `VibratorManager`, then closes. `VIBRATE` is a normal permission, so it's granted at install with no prompt.
- **Swiping the sheet down hides it but keeps counting.** A "Resting · 1:12 · show timer" button appears at the top of S9 to bring it back. Every rest has a new `id`, so the next checked set opens the sheet again.

### Step 8: S12 Finish dialog (`WorkoutDialogs.kt`)

- Duration, completed sets, and avg bpm (`--` until the watch, Sprint 10).
- **"How did it feel?"** 1–5 chips (optional; tap again to clear) and **Notes** (optional, up to 500 characters).
- **Warns about unchecked sets:** "2 sets aren't checked and won't be saved."
- If **no** set is checked, Finish & Save is disabled: "Check at least one set to save this workout."
- **Keep going** closes the dialog; **Finish & Save** saves to Firestore and opens S16.

### Step 9: S16 Summary, strength version (`WorkoutSummaryScreen.kt`)

- Name, date and time, then a stats card: **Duration, Sets, Volume, kcal (est.), Avg bpm, Exercises**.
- Rating and notes (if entered), and each exercise with its sets: "Set 1 · 60 kg × 10", "Bodyweight × 12", "45 s".
- **Share** (top right): opens Android's share sheet with a text summary (an image card comes in Sprint 11):
  ```
  Upper Body — Tue, Oct 28
  48:12 · 14 sets · 4,820 kg lifted · 410 kcal
  Bench Press: 60 kg × 10, 60 kg × 8, 60 kg × 8
  ...
  Logged with Fitness Tracker
  ```
- **Done** closes the summary. **Discard** asks "Delete this workout?" and removes it from Firestore.

**Why save in S12, not in S16?** The wireframe has S12 say "Finish & Save" and S16 show Save / Discard. If we only saved on S16, closing the app on the summary would lose the whole workout. Saving in S12 means nothing can be lost; S16's **Done** keeps it and **Discard** deletes it, so the user still gets the same choice.

### Step 10: Calories (`Calories.kt`)

```
kcal = MET × body weight (kg) × hours
```

Strength training uses **MET 5.0** (vigorous resistance training, from the Compendium of Physical Activities). Body weight comes from the profile, or 70 kg if it isn't set. A 76 kg user lifting for 48 minutes: 5.0 × 76 × 0.8 = **304 kcal**. Sprint 10 can replace this with the watch's heart-rate-based number.

### Step 11: S7 sheet and S5 Home updates

**S7 Start Workout sheet**
- **Templates** appear as chips under "From a template". Tapping one starts S9 with its exercises and sets **pre-filled** (weight shown in kg or lb).
- With no templates: "No templates yet · save one from a workout's ⋮ menu" + **New**.
- `StartWorkoutViewModel` reads templates live and starts the session with the profile's units, body weight and rest time.

**S5 Home** now uses real workout data (`HomeViewModel` combines the profile, this week's workouts and the session):

| Card | Before | Now |
|---|---|---|
| Active min | 0 | Minutes of today's workouts |
| Calories | 0 | kcal of today's workouts |
| Weekly card | "Weekly goal · not set" | "This week · 3 workouts" (goals come in Sprint 6) |
| New: Resume card | — | "Workout in progress · Upper Body · 12:34" + **Resume** |

The week starts on Monday. Only this week's workouts are read (`whereGreaterThanOrEqualTo("startedAt", mondayMillis)`), not the whole history.

### Step 12: Wiring (`FitnessApp.kt`, `MainScreen.kt`, `Routes.kt`)

- `AppContainer` now takes the app `Context` (the session needs it for the vibrator) and creates the three new repositories plus `workoutSession`.
- New routes: `workout/strength/add` (S10) and `workout/summary` (S16). The Sprint 2 "Coming in Sprint 3" page for `workout/strength` is replaced by S9.
- `TopBarNav.Close` adds the ✕ button used by S9 and S16.
- **Log out also ends any running workout**, so the next person to log in on the same phone doesn't see it.

---

## 5. Jetpack Compose and Kotlin concepts introduced this sprint

| Concept | Where | Purpose |
|---|---|---|
| App-scoped state holder + `StateFlow` | `ActiveWorkoutSession` | State that outlives any one screen |
| Coroutine ticker (`while` + `delay`) | Session | Workout and rest timers |
| `MutableStateFlow.update { }` | Session, ViewModels | Thread-safe state changes |
| `combine(a, b, c, d)` | `AddExerciseViewModel`, `HomeViewModel` | Merge several live sources into one UI state |
| `LazyColumn` with `key` + `Modifier.animateItem()` | S9, S10 | Smooth add / remove / reorder |
| `KeyboardOptions(keyboardType = Decimal / Number)` | Set table | Number keypad |
| `imePadding()` | S9, S10 | Fields stay above the keyboard |
| `CircularProgressIndicator(progress = { })` | S11 | Countdown ring |
| `DropdownMenu` | S9 ⋮ menus | Overflow menus |
| `FilterChip`, `FlowRow`, `SingleChoiceSegmentedButtonRow` | S10, S12 | Filters, rating, custom-exercise form |
| `Intent.ACTION_SEND` + `createChooser` | S16 | Share sheet |
| `VibratorManager` + `VibrationEffect` | Session | Rest-timer buzz |

---

## 6. Firestore changes

Three new subcollections under the user's document (covered by the existing `users/{uid}/**` rule, so **no rule change is needed**):

```
users / {uid} / workouts / {workoutId}
  id: "Xk29..."
  type: "STRENGTH"
  name: "Upper Body"
  startedAt: 1793200000000      endedAt: 1793202892000      durationSec: 2892
  exercises: [
    { exerciseId: "bench_press", name: "Bench Press", muscleGroup: "CHEST", trackedBy: "REPS",
      sets: [ { weightKg: 60, reps: 10, durationSec: 0, completed: true }, ... ] },
    { exerciseId: "plank", name: "Plank", muscleGroup: "CORE", trackedBy: "TIME",
      sets: [ { weightKg: 0, reps: 0, durationSec: 45, completed: true } ] }
  ]
  totalSets: 14    totalVolumeKg: 4820    calories: 304
  avgHr: null      rating: 4    notes: "Felt strong"    source: "PHONE"

users / {uid} / templates / {templateId}
  name: "Upper Body"    createdAt: 1793202900000
  exercises: [ { exerciseId: "bench_press", name: "Bench Press", muscleGroup: "CHEST",
                 trackedBy: "REPS", sets: 3, targetReps: 8, targetWeightKg: 60 } ]

users / {uid} / customExercises / {exerciseId}
  name: "Cable Crossover"    muscleGroup: "CHEST"    trackedBy: "REPS"
```

If your Firestore is still in **test mode**, remember that test-mode rules expire after 30 days. The rule to switch to is in [`SPRINT-1.md`](SPRINT-1.md).

---

## 7. How to test this sprint

| # | Test | Expected result |
|---|---|---|
| 1 | Home → **+** → **Strength training** | S9 opens: "Strength workout", timer counting, "No exercises yet" |
| 2 | **+ Add Exercise** → chip **Chest** | Only chest exercises |
| 3 | Search "curl" | Barbell, Dumbbell, Hammer Curl, Leg Curl |
| 4 | Select Bench Press and Plank → **Add 2 Exercises** | Back on S9 with two cards, one empty set each |
| 5 | Plank's table header | Says **Sec** instead of Reps |
| 6 | Tap the kg field | Number keypad; letters can't be typed |
| 7 | Enter 60 × 10, tap ✓ | Row highlighted, "1 / 1 sets", **rest sheet opens** with your default rest |
| 8 | +15s, −15s | Countdown changes by 15 s |
| 9 | Let it reach 0 | Phone vibrates and the sheet closes |
| 10 | **+ Add set** | New row pre-filled with 60 and 10 |
| 11 | Check a set, then swipe the sheet down | "Resting · 1:12 · show timer" button on S9; tapping it reopens the sheet |
| 12 | ⋮ → **Pause** | Timer stops and turns grey; "Paused · tap to resume" button appears |
| 13 | ⋮ → **Reorder** → ↓ on the first card | Cards swap with an animation |
| 14 | ⋮ → **Rename** → "Upper Body" | Title changes |
| 15 | Rotate the phone | Everything stays (timer, sets, typed values) |
| 16 | Press system **Back** | Home shows "Workout in progress · Upper Body · mm:ss" → **Resume** returns to S9 |
| 17 | ⋮ → **Save as template** → Save | Snackbar "Template "Upper Body" saved"; appears in Firestore `templates` |
| 18 | Leave one set unchecked → **Finish Workout** | S12 warns "1 set isn't checked and won't be saved." |
| 19 | Rate 4, add a note → **Finish & Save** | S16 summary with totals; workout in Firestore `workouts` |
| 20 | S16 → **Share** | Android share sheet with the text summary |
| 21 | S16 → **Done** | Back on Home; Active min, Calories and "This week · 1 workout" updated |
| 22 | **+** → the "Upper Body" template chip | S9 pre-filled with the same exercises, sets, weights and reps |
| 23 | S10 → **Create custom exercise** "Cable Crossover", Chest | Appears as "Chest · Custom", already selected; in Firestore `customExercises` |
| 24 | ✕ on S9 → **Discard** | Back to Home, no Resume card, nothing saved |
| 25 | Finish a workout, then **Discard** on S16 → Delete | Workout removed from Firestore; Home counts go back down |
| 26 | Profile → Units → lb / mi, start a workout | Column header says **lb**; Firestore still stores kg |
| 27 | Turn on airplane mode, finish a workout | Saves instantly; uploads when you reconnect |
| 28 | Uncheck every set → Finish Workout | "Finish & Save" disabled, with a message |

---

## 8. Known limitations (planned for later sprints)

- **The Workouts tab still shows the empty state.** The history list, workout detail, edit and repeat are **Sprint 4**.
- Heart rate and steps in the live strip, rest sheet, S12 and S16 show `--` until **Sprints 9–10**.
- Profile stats (workouts, streak) are still `0`; they use workout history in **Sprint 4–5**.
- "Start" on Today's plan needs reminders (**Sprint 7**).
- The workout is held in memory; a foreground service that survives the app being killed arrives with GPS tracking in **Sprint 8**.
- Templates can be created but not renamed or deleted yet (**Sprint 4**, with the history screens).
- The share summary is text only; an image card is planned for **Sprint 11**.

---

## 9. Sprint review checklist

- [ ] App builds and runs from Android Studio
- [ ] S9: add exercises, log sets with the number keypad, check sets, pause, reorder, rename
- [ ] S10: search, filter by muscle group, multi-select, create a custom exercise
- [ ] S11: rest timer starts on ✓, ±15s / Skip work, vibrates at 0
- [ ] S12: rating, notes, unchecked-set warning
- [ ] S16: summary, Share, Done, Discard
- [ ] Workout, template and custom exercise appear in Firestore
- [ ] Template chip in S7 starts a pre-filled workout
- [ ] Home shows today's active minutes / calories and the weekly count
- [ ] Approved to merge `sprint-3` and start **Sprint 4: Workout history & detail**
