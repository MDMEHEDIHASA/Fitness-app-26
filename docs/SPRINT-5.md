# Sprint 5: Progress charts & personal records

**Project:** Fitness Tracker (Android Development, Fall 2026)
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Wireframe screens covered:** S19 Progress (Week), S20 Progress (Month / Year + metric menu)
**GitHub issue:** [#5 Sprint 5: Progress charts & personal records](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/5)
**Branch:** `sprint-5-6` (from `sprint-4`). Not pushed until you say so.
**Status:** Built locally.

---

## 1. Sprint goal

Show how training is going over time, and keep personal bests.

**Done when:** after logging a few workouts, Week / Month / Year charts draw, and breaking a record shows on the summary and in the records list.

---

## 2. What we built

| Area | Files |
|---|---|
| Models | `Progress.kt` (`PersonalRecord`, `BodyWeightEntry`, `ProgressRange`, `ProgressMetric`) |
| Detector | `PersonalRecordDetector.kt` |
| Data | `ProgressRepository.kt` (`personalRecords`, `bodyWeight`) |
| Charts | `Charts.kt` (`BarChart` week, `LineChart` month/year, tap to select) |
| UI | `ProgressScreen.kt`, `RecordsScreen.kt`, `ProgressViewModel.kt` |
| Summary | S16 shows **★ New record!** when a set or volume is a personal best |
| Profile | Saving a new weight also writes a `bodyWeight` entry; header stats (workouts, streak, distance) are live |

**Workout.distanceM** was added (default 0) so distance charts work once Sprint 8 saves GPS data.

---

## 3. How charts are filled

| Metric | Source now | Empty until |
|---|---|---|
| Workouts completed | Count of saved workouts in the range | — |
| Volume lifted | Sum of `totalVolumeKg` | — |
| Active minutes | Sum of workout duration | — |
| Body weight | `bodyWeight` entries from Profile | Log a weight |
| Distance | `distanceM` | Sprint 8 |
| Steps, resting HR | — | Sprint 9 |

Week = current Monday–Sunday bars. Month = each day. Year = each month. A red line is drawn if you have a matching active goal (Sprint 6).

---

## 4. Personal records

On **Finish & Save**, the detector compares the workout to `users/{uid}/personalRecords`:

- **Heaviest lift** per exercise
- **Most volume** in one workout
- **Longest run** / **Fastest 5K** once outdoor workouts exist

Beating a record overwrites the same document id. The summary lists every record this workout just set.

---

## 5. How to test

| # | Test | Expected |
|---|---|---|
| 1 | Progress with no workouts and no weigh-ins | "No progress yet" |
| 2 | Finish a strength workout, open Progress | Week bars show a workout today; volume is non-zero |
| 3 | Switch Month / Year | Line chart. Tap a point for the value |
| 4 | Metric menu → Volume lifted | Chart uses kg or lb from Profile |
| 5 | Metric menu → Steps | "No data yet" |
| 6 | Summary after a first workout | ★ New record for volume and each lift |
| 7 | See all records | Same records on the list screen |
| 8 | Profile → Edit → change weight → Save | A body-weight point appears on the Body weight metric |
