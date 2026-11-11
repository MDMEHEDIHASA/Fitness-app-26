# Sprint 6: Goals

**Project:** Fitness Tracker (Android Development, Fall 2026)
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Wireframe screens covered:** S21 Goals list, S22 New / edit goal
**GitHub issue:** [#6 Sprint 6: Goals](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/6)
**Branch:** `sprint-5-6` (from `sprint-4`). Not pushed until you say so.
**Status:** Built locally.

---

## 1. Sprint goal

Users set goals, and progress updates on its own when they train.

**Done when:** creating a "4 workouts / week" goal, then logging workouts, updates the bar, and the 4th workout moves it to Completed with a notification.

---

## 2. What we built

| Area | Files |
|---|---|
| Model | `Goal.kt` (`GoalType`, `GoalPeriod`, `GoalStatus`) |
| Math | `GoalProgressCalculator.kt` |
| Complete once | `GoalCompletionTracker.kt` |
| Data | `GoalRepository.kt` (`users/{uid}/goals`) |
| Notify | `GoalNotifier.kt` (channel `goals`) |
| UI | `GoalsScreen.kt`, `GoalDialogs.kt`, `GoalsViewModel.kt` |
| Home | Weekly card uses the real weekly-workout goal |

---

## 3. Goal types and units

Targets are stored in metric. The dialog converts kg/lb and km/mi from Profile units.

| Type | Target means | Progress source |
|---|---|---|
| Workouts | count | Workouts in the period |
| Active minutes | minutes | Sum of workout duration |
| Distance | meters | Sum of `distanceM` (0 until Sprint 8) |
| Body weight | kg | Latest weigh-in / profile weight. Complete within 0.5 kg |
| Steps | count | 0 until Sprint 9 |
| Resting HR | bpm | `--` until Sprint 9 |

Period: Daily (from midnight), Weekly (Monday), Monthly (1st). Caption includes "resets Monday" and so on.

⋮ on a card: Edit, Pause / Resume, Delete. Completed goals sit in a collapsible section.

"Remind me if I'm behind" is stored for Sprint 7 reminders.

---

## 4. Home

If there is an **active weekly workout goal**, Home shows `Weekly goal · 3 of 4 workouts` and fills the bar. Otherwise it still shows `This week · N workouts` and "tap to set one".

A daily steps goal, if you add one, becomes the step-card target (still `--` until Sprint 9).

---

## 5. Notifications

When current >= target, the goal is marked **COMPLETED** once and a notification "Goal reached" is posted. If notification permission was denied on S4, the mark still saves; the notification is skipped.

---

## 6. How to test

| # | Test | Expected |
|---|---|---|
| 1 | Drawer → Goals with none | Empty state + New goal |
| 2 | New goal: Workouts, Weekly, 4 | Card with 0 of 4 · resets Monday |
| 3 | Home | Weekly goal · 0 of 4 workouts |
| 4 | Finish a strength workout | Bar moves to 1 of 4 |
| 5 | Finish until 4 this week | Goal moves to Completed; notification (if allowed) |
| 6 | Pause a goal | Bar hidden, status Paused |
| 7 | Body weight goal, then save a new weight on Profile | Progress uses that weigh-in |
| 8 | Invalid target 0 | Dialog stays open with an error |
