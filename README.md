# Fitness Tracker

An Android app for recording workouts, tracking exercise history, monitoring progress, and reaching fitness goals. Built with **Kotlin** and **Jetpack Compose**, with **Firebase Authentication** for accounts and **Cloud Firestore** for data, so everything syncs across devices.

**Course:** Android Development, Fall 2026 · Final Project
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Sprint board:** [GitHub Project](https://github.com/users/MDMEHEDIHASA/projects/2)

---

## Features

### Core
- **Accounts:** sign up, log in, password reset, and Google sign-in (Firebase Auth). Profiles are stored in Firestore.
- **Strength workouts:** add exercises, log sets / reps / weight, rest timer, finish with rating and notes.
- **Workout history:** browse, search, filter, edit, repeat, and delete past workouts.
- **Progress:** weekly / monthly / yearly charts for steps, resting heart rate, workouts, distance, body weight, and volume lifted, plus personal records.
- **Goals:** steps, workouts per week, distance, active minutes, resting heart rate, and body weight, updated automatically.

### Supplemental
1. **Smartwatch (Wear OS):** the watch streams live heart rate and steps, and workouts can be started, paused, and stopped from either device.
2. **Location services:** GPS tracking for outdoor runs, walks, and rides, with a live route map, distance, pace, and splits.
3. **Notifications:** scheduled workout reminders with Start / Snooze, step-goal nudges, inactivity alerts, and weekly summaries.

### Health sensors
Heart rate and steps from **Health Connect**, the phone's step counter, and a paired watch, shown on the dashboard and during workouts.

---

## Project status

The app is built in sprints. Each sprint has a card on the [sprint board](https://github.com/users/MDMEHEDIHASA/projects/2) and a write-up in [`docs/`](docs/).

| Sprint | Feature | Wireframe screens | Status |
|---|---|---|---|
| 1 | [Setup & authentication](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/1) | S1–S4 | ✅ Done · [docs](docs/SPRINT-1.md) |
| 2 | [App shell: Home, navigation, profile](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/2) | S5–S7, S28, S29 | 🔍 In review · [docs](docs/SPRINT-2.md) |
| 3 | [Strength workout logging](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/3) | S9–S12, S16 | 🔍 In review · [docs](docs/SPRINT-3.md) |
| 4 | [Workout history & detail](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/4) | S17, S18, S30, S31 | Backlog |
| 5 | [Progress charts & personal records](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/5) | S19, S20 | Backlog |
| 6 | [Goals](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/6) | S21, S22 | Backlog |
| 7 | [Reminders & notifications](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/7) | S8, S23, S24 | Backlog |
| 8 | [Outdoor GPS tracking](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/8) | S13–S16 | Backlog |
| 9 | [Health sensors](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/9) | S5, S9, S14, S19 | Backlog |
| 10 | [Wear OS smartwatch app](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/10) | S25–S27, S32, W1–W4 | Backlog |
| 11 | [Polish, testing & submission](https://github.com/MDMEHEDIHASA/Fitness-app-26/issues/11) | All | Backlog |

The full roadmap is in [`docs/SPRINT-PLAN.md`](docs/SPRINT-PLAN.md).

---

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 (no XML layouts) |
| Navigation | Navigation Compose |
| Architecture | MVVM: Composable screens → ViewModels (`StateFlow`) → Repositories → Firebase |
| Accounts | Firebase Authentication (email/password, Google via Credential Manager) |
| Database | Cloud Firestore |
| Health data | Health Connect, phone step counter, Wear OS Health Services |
| Location | Fused Location Provider, Google Maps Compose |
| Background work | WorkManager, AlarmManager, foreground service |
| Watch | Wear OS, Compose for Wear OS, Data Layer API |

| Build setting | Version |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Firebase BOM | 34.19.0 |
| minSdk / targetSdk | 33 / 37 |

---

## Getting started

### Requirements
- Android Studio (latest stable) with its bundled JDK
- Android SDK 37
- An emulator or phone running Android 13+. Use a **Google Play** system image for Google sign-in.

### 1. Clone

```bash
git clone https://github.com/MDMEHEDIHASA/Fitness-app-26.git
cd Fitness-app-26
```

Open the folder in Android Studio (**File → Open**) and let Gradle sync.

### 2. Connect Firebase

`google-services.json` is **not** in git, so each developer adds their own.

1. In the [Firebase console](https://console.firebase.google.com), open the project (or create one) and **Add app → Android** with package name **`com.unh.fitnesstracker`**.
2. Download `google-services.json` into the **`app/`** folder. The file name must be exactly `google-services.json`; browsers sometimes add a space or `(1)`.
3. **Authentication → Sign-in method:** enable **Email/Password** and **Google**.
4. **Firestore Database → Create database** (test mode is fine for development).
5. For Google sign-in, add your debug **SHA-1** under **Project settings → your Android app → Add fingerprint**:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
   Then download `google-services.json` again.
6. Put the OAuth **Web client ID** (Authentication → Sign-in method → Google → Web SDK configuration) in `app/src/main/res/values/strings.xml` as `google_web_client_id`.

### 3. Run

Press **Run ▶** in Android Studio, or build from the terminal:

```bash
./gradlew assembleDebug
```

Later sprints add a Google Maps API key (Sprint 8), Health Connect (Sprint 9), and a Wear OS emulator (Sprint 10). Their setup steps will be added here.

---

## Project structure

```
app/src/main/java/com/unh/fitnesstracker/
├── FitnessApp.kt              Application class + AppContainer (shared repositories)
├── MainActivity.kt            Single activity hosting all Compose UI
├── data/
│   ├── model/                 Firestore data classes (UserProfile, Workout, ...) + exercise library
│   ├── repository/            The only code that talks to Firebase
│   └── session/               ActiveWorkoutSession: the workout in progress
└── ui/
    ├── theme/                 Colors, typography, Material 3 theme
    ├── navigation/            Routes and NavHost
    ├── components/            Shared Compose widgets
    ├── auth/                  S1–S3 Log In / Sign Up (+ ViewModels)
    ├── onboarding/            S4 Health & Permissions
    ├── main/                  App shell: drawer (S6), bottom tabs, MainScreen
    ├── home/                  S5 Home dashboard
    ├── workout/               S7 sheet, S9–S12 active workout, S16 summary, Workouts tab
    └── profile/               S28 Profile & Settings
docs/
├── SPRINT-PLAN.md             Full roadmap and Firestore data model
└── SPRINT-N.md                Write-up for each completed sprint
```

## Data

All user data lives under `users/{uid}` in Firestore, so a single security rule keeps each user's data private:

```
users/{uid}                    profile and settings
users/{uid}/workouts/{id}      strength and outdoor workouts
users/{uid}/templates/{id}     saved workout templates
users/{uid}/customExercises/{id}  exercises the user created
users/{uid}/goals/{id}         goals
users/{uid}/reminders/{id}     reminders
users/{uid}/dailyStats/{date}  steps, resting heart rate, active minutes
```

See [`docs/SPRINT-PLAN.md`](docs/SPRINT-PLAN.md#2-final-firestore-data-model) for every field.

---

## Team

| Name | GitHub |
|---|---|
| MD Mehedi Hasan | [@MDMEHEDIHASA](https://github.com/MDMEHEDIHASA) |
| Mohammed Mustafa Siddiq | [@MustafaSiddiq010](https://github.com/MustafaSiddiq010) |
