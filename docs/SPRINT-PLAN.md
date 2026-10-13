# Fitness Tracker: Sprint Plan

**Project:** Fitness Tracker (Android Development, Fall 2026)
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Stack:** Kotlin · Jetpack Compose (Material 3) · Navigation Compose · MVVM · Firebase Auth · Cloud Firestore · Health Connect · Fused Location + Google Maps · WorkManager / AlarmManager · Wear OS Data Layer

This document is the roadmap for the whole project. Each sprint ends the same way:

1. The app **builds and runs** with the new screens working end to end.
2. A **`docs/SPRINT-N.md`** explains every step, every file, and how to test it.
3. A **git commit** for the sprint.
4. A **sprint review**: you test it with the checklist and approve it (or ask for changes) before the next sprint starts.

---

## 1. Overview

| Sprint | Theme | Wireframe screens | Depends on |
|---|---|---|---|
| **1 ✅** | Setup & authentication | S1, S2, S3, S4 | — |
| **2** | App shell: Home, navigation, profile | S5, S6, S7, S28, S29 | 1 |
| **3** | Strength workout logging (core feature) | S9, S10, S11, S12, S16 (strength) | 2 |
| **4** | Workout history & detail | S17, S18, S30, S31 | 3 |
| **5** | Progress charts & personal records | S19, S20 | 4 |
| **6** | Goals | S21, S22 | 4, 5 |
| **7** | Reminders & notifications | S8, S23, S24 | 3, 6 |
| **8** | Outdoor GPS tracking | S13, S14, S15, S16 (outdoor) | 3 |
| **9** | Health sensors (steps & heart rate) | Live data on S5, S9, S14, S19, S25 | 2, 5 |
| **10** | Wear OS smartwatch app | S25, S26, S27, S32, W1–W4 | 3, 8, 9 |
| **11** | Polish, data export/delete, testing, final submission | S28 rows, all screens | All |

Why this order:

- **Sprint 2 first** because every later screen sits inside the Home / bottom-tab / drawer shell.
- **Strength workouts (3) before history (4), progress (5), and goals (6)**: those screens all read saved workouts, so workouts must exist first.
- **GPS (8), sensors (9), and the watch (10) come last**: they're supplemental features, the hardest to test, and they plug into screens built earlier. If time runs short, the core app is already complete.

---

## 2. Final Firestore data model

Every user's data lives under their own document, so one security rule (`request.auth.uid == uid`) protects everything.

```
users/{uid}                                   Sprint 1 (+ settings in Sprint 2)
    fullName, email, age, heightCm, weightKg, onboardingComplete, createdAt
    settings: { units: "metric"|"imperial", defaultRestSec: 90, autoPause: false }

users/{uid}/workouts/{workoutId}              Sprint 3, 8
    type: "STRENGTH" | "RUN" | "WALK" | "CYCLE"
    name, startedAt, endedAt, durationSec
    exercises: [ { exerciseId, name, muscleGroup, trackedBy: "REPS"|"TIME",
                   sets: [ { weightKg, reps, durationSec, completed } ] } ]
    totalSets, totalVolumeKg
    distanceM, avgPaceSecPerKm, avgSpeedKmh, route: [GeoPoint], splits: [ { km, sec } ]
    avgHr, maxHr, hrSamples: [ { t, bpm } ], steps, calories
    rating (1–5), notes, source: "PHONE"|"WATCH"

users/{uid}/templates/{templateId}            Sprint 3
    name, exercises: [ { exerciseId, name, sets, targetReps, targetWeightKg } ]

users/{uid}/customExercises/{exerciseId}      Sprint 3
    name, muscleGroup, trackedBy

users/{uid}/personalRecords/{recordId}        Sprint 5
    type: "HEAVIEST_LIFT"|"FASTEST_5K"|"LONGEST_RUN"|..., exerciseName, value, workoutId, achievedAt

users/{uid}/dailyStats/{yyyy-MM-dd}           Sprint 9
    steps, restingHr, activeMinutes, calories

users/{uid}/bodyWeight/{entryId}              Sprint 5
    weightKg, recordedAt

users/{uid}/goals/{goalId}                    Sprint 6
    type: "STEPS"|"WORKOUTS_PER_WEEK"|"DISTANCE"|"ACTIVE_MINUTES"|"RESTING_HR"|"BODY_WEIGHT"
    target, period: "DAILY"|"WEEKLY"|"MONTHLY", remindIfBehind, status: "ACTIVE"|"PAUSED"|"COMPLETED"
    createdAt, completedAt

users/{uid}/reminders/{reminderId}            Sprint 7
    templateId, label, hour, minute, days: [1..7], notifyMinutesBefore, vibrateWatch, enabled

users/{uid}/smartAlerts (document)            Sprint 7
    stepNudgeEnabled, stepNudgeHour, inactivityEnabled, inactivityMinutes, weeklySummaryEnabled
```

The **built-in exercise library** (Bench Press, Squat, Lat Pulldown, ...) ships inside the app as a Kotlin list, so it works offline and costs no database reads. Only **custom** exercises go in Firestore.

---

## 3. Sprint details

### Sprint 2: App shell (Home, navigation drawer, bottom tabs, profile)

**Goal:** Replace the temporary Home with the real dashboard and the navigation every other screen uses.

**Screens**
- **S5 Home dashboard:** greeting and date, watch status chip, Steps / Heart rate / Active min / Calories cards, "Today's plan" card with **Start**, weekly goal progress, and the **+** button.
- **S6 Navigation drawer:** profile header; Goals, Reminders, Smartwatch, Settings, Help & Feedback; Log out.
- **S7 Start Workout bottom sheet:** Strength / Outdoor run / Outdoor walk / Cycling, templates, and the "Track heart rate with watch" toggle (greyed out with no watch).
- **S28 Profile & Settings:** stats (workouts, streak, total km), body stats, units, default rest timer, and links to Goals / Reminders / Smartwatch / Permissions / Export.
- **S29 Log out confirmation dialog.**

**Tasks**
1. `MainScaffold`: one Compose `Scaffold` with a `NavigationBar` (Home, Workouts, Progress, Profile) and a `ModalNavigationDrawer`.
2. Nested navigation graph: an `auth` graph (S1–S4) and a `main` graph (the tabs). Each tab keeps its own back stack.
3. `ProfileRepository` extensions: live profile `Flow` (Firestore snapshot listener), update body stats and settings.
4. `HomeViewModel`: combines profile plus placeholders for steps and heart rate (real data arrives in Sprint 9) and the weekly workout count (real in Sprint 3/4).
5. Edit-profile dialog: name and body stats. Settings pickers: units, rest timer.
6. Empty placeholder screens for tabs and drawer items that aren't built yet, labeled with the sprint they arrive in.
7. Log out dialog → `signOut()` → back to S1 with the back stack cleared.

**New libraries:** none.
**Done when:** you can move between all four tabs and every drawer item, edit your body stats and see them saved in Firestore, and log out through the confirmation dialog.

---

### Sprint 3: Strength workout logging (core feature)

**Goal:** A user can start a strength workout, add exercises, log sets, rest between sets, and save the workout to Firestore.

**Screens**
- **S9 Active strength workout:** running timer, ✕ discard, ⋮ menu (Pause, Reorder, Save as template); exercise cards with a Set / kg / Reps / ✓ table; **+ Add set**; **+ Add Exercise**; **Finish Workout**; live HR / steps / calories strip (shows "--" until Sprint 9/10).
- **S10 Add Exercise:** search, muscle-group chips (All, Chest, Back, Legs, Arms, Cardio, Core), multi-select, **Create custom exercise** form, "Add N Exercises".
- **S11 Rest timer sheet:** starts automatically when a set is checked; −15s / +15s / Skip; vibrates when finished.
- **S12 Finish dialog:** duration, sets, avg bpm, "How did it feel?" 1–5, notes; warns about unchecked sets; Keep going / Finish & Save.
- **S16 Summary (strength version):** exercise and set list, totals, Save / Discard / Share.

**Tasks**
1. Models: `Workout`, `ExerciseEntry`, `SetEntry`, `Exercise`, `WorkoutTemplate`.
2. Built-in exercise library (around 40 exercises with muscle groups).
3. `WorkoutRepository` (save, update, delete), `ExerciseRepository` (custom exercises), `TemplateRepository`.
4. **`ActiveWorkoutSession`**: one shared in-memory session so the workout survives moving between S9 and S10, rotation, and leaving the screen. It holds the timer (coroutine ticker), exercises, and sets.
5. Number keypad input for kg and reps.
6. Rest timer using the default rest time from S28 settings; vibration through `Vibrator`.
7. Calorie estimate (MET value × body weight × time).
8. "Start" on Today's plan and templates in S7 open S9 pre-filled.
9. Share summary through the Android share sheet (text summary; image in Sprint 11).

**New libraries:** none.
**Done when:** a full workout (2+ exercises, several sets, rest timer, finish with rating and notes) appears in Firestore under `users/{uid}/workouts`, and "Save as template" creates a reusable template.

---

### Sprint 4: Workout history & detail

**Goal:** Users can browse, search, view, edit, repeat, and delete past workouts.

**Screens**
- **S17 Workout history (Workouts tab):** filter chips (All, Strength, Run, Walk, Cycle), search, grouped by month, loads more while scrolling, **+** opens S7.
- **S18 Workout detail:** duration, kg lifted, avg bpm, heart-rate chart, exercise list, notes; **Edit**, **Repeat Workout**, ⋮ (Save as template, Share, Delete).
- **S30 Delete confirmation** → delete → "Undo" snackbar for 5 seconds.
- **S31 Empty state:** "No workouts yet" + **Start First Workout**.

**Tasks**
1. Paged Firestore query ordered by `startedAt` (`limit` + `startAfter`), with month headers.
2. Type filter (Firestore `whereEqualTo`) and name/exercise search (filtered in the app over loaded items).
3. Long-press → delete; Undo re-writes the same document.
4. Edit mode on S18 updates sets and notes.
5. Repeat → opens S9 with the same exercises and last time's weights.
6. Simple heart-rate line chart drawn with Compose `Canvas` (reused in Sprint 5).
7. Composite Firestore index (type + startedAt). The doc explains how to create it from the console link.

**New libraries:** none (charts drawn with Compose `Canvas`).
**Done when:** the history list shows saved workouts grouped by month, filters and search work, detail edit saves, Repeat pre-fills S9, and delete + Undo works.

---

### Sprint 5: Progress charts & personal records

**Goal:** Show progress over time and personal bests.

**Screens**
- **S19 Progress (Week):** Week / Month / Year tabs, metric selector, bar chart with goal line (tap a bar for the value), summary cards (Workouts, Resting HR, Distance, Active min), Personal records.
- **S20 Progress (Month + metric menu open):** line chart for Month / Year; metric menu: Steps, Resting heart rate, Workouts completed, Distance, Body weight, Volume lifted.

**Tasks**
1. `ProgressRepository`: workouts and daily stats for a date range, grouped by day/week/month.
2. Reusable Compose `Canvas` charts: `BarChart` (week) and `LineChart` (month/year), with tap-to-select.
3. Personal-record detection when a workout is saved (heaviest lift per exercise, fastest 5K, longest run, most volume), stored in `personalRecords`. The S16 summary shows "★ New record!".
4. Personal records list screen.
5. Body-weight entries (entered from S28), stored in `bodyWeight`.
6. Empty state for new users.

**New libraries:** none.
**Done when:** after logging a few workouts, every metric draws for Week / Month / Year, and breaking a record shows on the summary and in the records list.

---

### Sprint 6: Goals

**Goal:** Users set goals, and progress updates on its own.

**Screens**
- **S21 Goals:** Active list with progress bars and text ("6,420 today · 64%", "3 of 4 · resets Monday"), ⋮ Edit / Pause / Delete, collapsible **Completed** section, **+** button.
- **S22 New Goal dialog:** goal type, target, period (Daily / Weekly / Monthly), "Remind me if I'm behind", Save / Cancel.

**Tasks**
1. `Goal` model + `GoalRepository`.
2. `GoalProgressCalculator`: computes progress for each goal type from workouts, daily stats, and body weight for the current period.
3. Goals re-evaluated when a workout is saved (and in Sprint 9, when steps update). Reaching a target marks the goal **Completed** and sends a notification.
4. Home dashboard's "Weekly goal · 3 of 4 workouts" uses the real goal.
5. Validation: positive targets, sensible ranges per type.

**New libraries:** none.
**Done when:** creating a "4 workouts / week" goal, then logging workouts, updates the bar, and the 4th workout moves it to Completed with a notification.

---

### Sprint 7: Reminders & notifications (supplemental feature 3)

**Goal:** Scheduled workout reminders with actions, plus smart alerts.

**Screens**
- **S8 Reminder notification:** "Workout in 15 min: Upper Body" with **Start now** and **Snooze 15m**; step-goal nudge.
- **S23 Reminders:** list with on/off toggles, tap to edit, swipe to delete, Smart alerts section (step nudge at 6 PM, inactivity alert, weekly summary), banner if notifications are blocked.
- **S24 New Reminder:** time wheel, repeat days (M–S), workout template, "Notify me N minutes before", "Also vibrate my watch".

**Tasks**
1. Notification channels (Reminders, Goals, Smart alerts, Workout in progress).
2. **AlarmManager** exact alarms for reminders at a specific time (`SCHEDULE_EXACT_ALARM` permission handling); repeat days reschedule the next alarm.
3. `BroadcastReceiver` for the **Start now** action (deep link to S9 with the template) and **Snooze** (reschedule +15 min).
4. **WorkManager** periodic jobs: step-goal nudge, inactivity check, weekly summary (Sunday).
5. Re-schedule all alarms after the phone restarts (`BOOT_COMPLETED` receiver).
6. Time-wheel picker in Compose.

**New libraries:** `androidx.work:work-runtime-ktx`.
**Done when:** a reminder set 2 minutes ahead appears on time, Start now opens S9 with the template loaded, Snooze brings it back 15 minutes later, and reminders survive a reboot.

---

### Sprint 8: Outdoor GPS tracking (supplemental feature 2)

**Goal:** Track runs, walks, and rides with a live map.

**Screens**
- **S13 Outdoor ready:** Run / Walk / Cycle selector, map centered on the user, GPS signal indicator ("Acquiring GPS…" keeps Start disabled), optional goal (distance), **START** with a 3-2-1 countdown.
- **S14 Tracking:** live route line, distance, time, pace (or speed for cycling), HR, steps, HR zone; **Lock**, **Pause**, **Lap**.
- **S15 Paused:** frozen, greyed stats; **Resume**; **Hold to finish** (1-second long press).
- **S16 Summary (outdoor):** route map, distance, time, pace, avg/max bpm, steps, HR chart, new-record banner, Save / Discard / Share.

**Tasks**
1. **Foreground service** (`location` type) so tracking continues with the screen off, with an ongoing notification.
2. **Fused Location Provider** updates (1–2 s), filtering inaccurate points, distance with `Location.distanceTo`.
3. Pace, speed, splits per km, laps; optional auto-pause when not moving.
4. **Google Maps Compose**: live polyline and camera following.
5. Save route as `GeoPoint` list; summary map shows the full route.
6. Lock mode ignores taps; hold-to-finish button.

**New libraries:** `com.google.android.gms:play-services-location`, `com.google.maps.android:maps-compose`.
**You will need:** a **Google Maps API key** (Google Cloud Console → enable "Maps SDK for Android" for project `fitness-app-2c041`). Testing: emulator "Extended controls → Location → Routes" to simulate a run.
**Done when:** a simulated route draws live, pause/resume freezes and restarts stats, the workout saves with route and splits, and S17/S18 show it.

---

### Sprint 9: Health sensors (steps & heart rate)

**Goal:** Replace the "--" placeholders with real data.

**Screens affected:** S5 cards, S9 / S14 live strip, S19 steps and resting HR, S25 "Phone sensors & Health Connect".

**Tasks**
1. **Phone step counter** (`Sensor.TYPE_STEP_COUNTER`) for live steps today, with a daily baseline stored so the count resets at midnight.
2. **Health Connect**: read steps, heart rate, and resting heart rate history; permission screen (S25 "Manage permissions").
3. `HealthRepository` merging both sources; writes a `dailyStats/{date}` document once per day (WorkManager).
4. Active minutes and calories estimate for the day.
5. Steps update goals (Sprint 6) and the step nudge (Sprint 7).
6. Graceful fallback: no Health Connect → phone sensor only; no heart-rate source → "--".

**New libraries:** `androidx.health.connect:connect-client`.
**You will need:** the **Health Connect** app (built into Android 14+). On the emulator, the Health Connect test-data screen can be used to add sample heart-rate data.
**Done when:** walking with the phone raises the Steps card, Health Connect heart rate appears on Home, and S19 Steps / Resting HR show real history.

---

### Sprint 10: Wear OS smartwatch app (supplemental feature 1)

**Goal:** A watch app that streams heart rate and steps and controls workouts in sync with the phone.

**Screens**
- Phone: **S25** no watch paired, **S26** pair a watch (lists connected watches; Connect / Install), **S27** connected (battery, last sync, live HR and steps, Sync Now, feature toggles, Disconnect), **S32** watch disconnected banner during a workout.
- Watch (Compose for Wear OS): **W1** start workout list, **W2** active workout (HR, zone, distance, steps), **W3** paused controls (Resume / End), **W4** saved & synced.

**Tasks**
1. New **`:wear` module** (Wear OS, Compose for Wear OS) sharing the same `applicationId` so the Data Layer connects them.
2. **Health Services** on the watch: `ExerciseClient` for live heart rate and steps during workouts.
3. **Data Layer API**: `MessageClient` for start / pause / resume / end commands (both directions); `DataClient` for HR every second and final workout data.
4. Phone `WearableListenerService`; a "who is in charge" rule: the device that started the workout owns the timer, and the other mirrors it.
5. Disconnect handling: phone keeps GPS/steps, HR shows "--", watch buffers data and syncs it on reconnect ("Saved on watch. Will sync later.").
6. Reminder notifications appear on the watch with Start / Snooze (Sprint 7 bridged).
7. S7 heart-rate toggle and S5 watch chip become real.

**New libraries:** `play-services-wearable`, `androidx.health:health-services-client`, `androidx.wear.compose:*`.
**You will need:** a **Wear OS emulator** (Device Manager → Wear OS Small Round, API 34+) paired with the phone emulator through the Wear OS pairing assistant, or a real Wear OS watch.
**Done when:** starting a workout on the watch opens it on the phone (and the reverse), HR streams to S9/S14, pause on either device pauses both, ending saves to Firestore with the watch's HR data, and disconnecting mid-workout shows S32.

---

### Sprint 11: Polish, data controls, testing & submission

**Goal:** Make the app ready to demo and hand in.

**Tasks**
1. S28 **Export / delete my data**: export all Firestore data as JSON (share sheet); delete account and all data (re-authentication required).
2. S28 **Permissions** row → system app settings; Help & Feedback.
3. Production **Firestore security rules** and required indexes.
4. Dark mode check, accessibility (content descriptions, touch target sizes, font scaling), loading and error states on every screen.
5. Share-summary **image** for S16.
6. Unit tests (validation, goal progress, PR detection, pace/calorie math) and a few Compose UI tests (log-in flow, add set).
7. App icon, splash screen, release build check.
8. Final docs: `README.md`, architecture diagram, demo script, and answers to the wireframe feedback questions.

**New libraries:** `androidx.core:core-splashscreen`.
**Done when:** all test checklists from Sprints 1–10 pass and the release build installs and runs.

---

## 4. Things you'll need to provide during the project

| When | What | Where |
|---|---|---|
| Sprint 1 ✅ | `google-services.json`, Email/Password + Google enabled, SHA-1 | Firebase console |
| Sprint 4 | Click the "create index" link if Firestore asks for one | Firebase console |
| Sprint 8 | Google Maps API key | Google Cloud Console |
| Sprint 9 | Health Connect permissions on the test device | Phone settings |
| Sprint 10 | Wear OS emulator (or real watch) paired with the phone | Android Studio Device Manager |
| Sprint 11 | Final security rules published | Firebase console |

## 5. Risks and how we handle them

| Risk | Plan |
|---|---|
| Wear OS pairing on emulators is unreliable | Sprint 10 is last; the phone works fully without a watch. The doc covers both emulator and real-watch setup. |
| GPS is hard to test indoors | Emulator route simulation (GPX/KML playback). |
| Health Connect has little data on an emulator | Phone step sensor as the main source; Health Connect sample data for heart rate. |
| Exact alarms restricted on Android 14+ | Ask for the exact-alarm permission and fall back to WorkManager if refused. |
| Firestore read costs while scrolling history | Paging (`limit` 20) and offline cache. |
| Running out of time | Core features (Sprints 2–7) come first; supplemental features (8–10) can be trimmed without breaking the app. |

## 6. Sprint review template (used at the end of every sprint)

Each `docs/SPRINT-N.md` includes:

1. **Sprint goal** and screens covered.
2. **What was built:** step by step, every new and changed file, and why.
3. **Firestore changes:** new collections and fields, with an example document.
4. **How to test:** numbered checklist with expected results.
5. **Known limitations:** what's left for later sprints.
6. **Review checklist:** to approve before the next sprint.
