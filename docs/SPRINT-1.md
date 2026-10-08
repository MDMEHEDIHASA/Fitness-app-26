# Sprint 1: Project Setup and Authentication

**Project:** Fitness Tracker (Android Development, Fall 2026)
**Team:** MD Mehedi Hasan & Mohammed Mustafa Siddiq
**Wireframe screens covered:** S1 Log In, S2 Log In (error state), S3 Sign Up, S4 Health & Permissions
**Status:** Done. Builds with no errors or warnings (`./gradlew assembleDebug`).
**Commit:** `Sprint 1: project setup, Firebase Auth, Firestore user profile, S1-S4 screens`

---

## 1. Sprint goal

Build the base that every later sprint relies on:

1. An Android Studio project that uses **Kotlin and Jetpack Compose** for all of its UI.
2. **Firebase** connected, with **Firebase Authentication** for accounts and **Cloud Firestore** as the database.
3. A full log-in and sign-up flow that matches wireframes S1 to S4. A new user can create an account, has their profile saved to Firestore, grants permissions, and lands on Home.

The rest of the app (dashboard, workouts, history, and so on) needs a logged-in user, which is why authentication comes first.

---

## 2. Full sprint roadmap

| Sprint | Screens | Feature |
|---|---|---|
| **1 (this one)** | S1–S4 | Project setup, Firebase Auth, Firestore profile, permissions |
| 2 | S5–S7, S28, S29 | Home dashboard, navigation drawer, bottom tabs, start-workout sheet, profile, log out |
| 3 | S9–S12, S16 | Strength workout logging, add exercise, rest timer, finish dialog, summary |
| 4 | S17, S18, S30, S31 | Workout history, detail, delete with undo, empty state |
| 5 | S19, S20 | Progress charts, personal records |
| 6 | S21, S22 | Goals |
| 7 | S8, S23, S24 | Reminders and notifications (WorkManager / AlarmManager) |
| 8 | S13–S16 | Outdoor GPS tracking (Fused Location Provider + Google Maps) |
| 9 | (sensors) | Step counter and heart rate through Health Connect |
| 10 | S25–S27, S32, W1–W4 | Wear OS watch app and Data Layer sync |

---

## 3. Technology choices

| Area | Choice | Why |
|---|---|---|
| Language | Kotlin | Standard language for modern Android |
| UI | **Jetpack Compose** + Material 3 | Required for the project. Every screen is a `@Composable` function, and the project has **no XML layout files**. |
| Navigation | Navigation Compose | Moves between Compose screens and manages the Back button |
| Screen logic | `ViewModel` + `StateFlow` | Keeps screen data alive when the phone rotates and separates logic from UI |
| Accounts | Firebase Authentication | Email/password, password reset, and Google sign-in |
| Database | Cloud Firestore | Cloud database that syncs across devices and works offline |
| Google sign-in | Credential Manager (`androidx.credentials`) + Google ID | Google's current recommended way to sign in with Google |
| Async calls | Kotlin coroutines + `kotlinx-coroutines-play-services` | Lets us write `task.await()` instead of nested callbacks |

### Versions

| Item | Version |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Gradle | 9.6.0 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| Navigation Compose | 2.10.1 |
| Lifecycle / ViewModel | 2.11.0 |
| Firebase BOM | 34.19.0 |
| Google Services plugin | 4.5.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 33 (Android 13) |

These versions match the other projects already on this Mac (for example `mehediApplication2`), so they're known to work with this Android Studio install.

---

## 4. Project structure

```
FitnessTracker/
├── build.gradle.kts                 Root build file (declares plugins)
├── settings.gradle.kts              Project name and repositories
├── gradle/libs.versions.toml        Version catalog: every library and version in one place
├── docs/SPRINT-1.md                 This document
└── app/
    ├── build.gradle.kts             App module: SDK levels, Compose, dependencies
    ├── google-services.json         (You add this; see section 8)
    └── src/main/
        ├── AndroidManifest.xml      App entry point and permissions
        ├── res/values/              App name and base theme
        └── java/com/unh/fitnesstracker/
            ├── FitnessApp.kt        Application class + AppContainer
            ├── MainActivity.kt      The only Activity; hosts all Compose UI
            ├── data/
            │   ├── model/UserProfile.kt
            │   └── repository/
            │       ├── AuthRepository.kt
            │       └── UserRepository.kt
            └── ui/
                ├── theme/           Color.kt, Theme.kt, Type.kt
                ├── navigation/      Routes.kt, AppNavHost.kt
                ├── components/      FormComponents.kt (shared form widgets)
                ├── auth/            LoginScreen, LoginViewModel, SignUpScreen,
                │                    SignUpViewModel, GoogleSignInHelper
                ├── onboarding/      PermissionsScreen.kt
                └── home/            HomeScreen.kt (temporary)
```

### Architecture

The app follows the **MVVM** pattern (Model, View, ViewModel) that Google recommends:

```
Composable screen (View)    shows the state and sends user actions
        │  ▲
        ▼  │ StateFlow<UiState>
ViewModel                   validation and screen logic; holds UI state
        │
        ▼
Repository                  the only code that talks to Firebase
        │
        ▼
Firebase Auth / Firestore
```

- **Screens never call Firebase directly.** They call the ViewModel, which calls a repository. If the database ever changes, only the repositories need to change.
- Each screen has a single `UiState` data class. The screen redraws whenever that state changes, which is how Compose is designed to work.

---

## 5. Step-by-step explanation

### Step 1: Gradle setup

**`gradle/libs.versions.toml`** (version catalog)
Lists every library and its version in one file. The build files refer to libraries by name, for example `libs.firebase.auth`, so version numbers never get repeated across files.

**`build.gradle.kts` (root)**
Declares three plugins without applying them (`apply false`): the Android application plugin, the Kotlin Compose compiler plugin, and the Google Services plugin.

**`app/build.gradle.kts`**
- `namespace` and `applicationId` = `com.unh.fitnesstracker`. Firebase uses this package name to recognize the app.
- `buildFeatures { compose = true }` turns on Jetpack Compose.
- The Firebase BOM (`platform(libs.firebase.bom)`) picks matching versions of `firebase-auth`, `firebase-firestore`, and `firebase-analytics`, so we don't list a version for each one.
- **The Google Services plugin is applied only if `google-services.json` exists:**
  ```kotlin
  if (file("google-services.json").exists()) {
      apply(plugin = libs.plugins.google.services.get().pluginId)
  }
  ```
  Without this, the build fails until the Firebase file is added. With it, the project builds right away and the app shows a "Firebase is not set up" message until the file is there.

### Step 2: Manifest and resources

**`AndroidManifest.xml`**
- `android:name=".FitnessApp"` tells Android to run our `Application` class at startup.
- Permissions:

| Permission | Used for | Screen |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Talking to Firebase | All |
| `READ_HEART_RATE` (Android 16+) / `BODY_SENSORS` (Android 15 and below) | Heart rate | S4, later S9/S14 |
| `ACTIVITY_RECOGNITION` | Step counter | S4, S5 |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS runs | S4, S13–S16 |
| `POST_NOTIFICATIONS` | Reminders | S4, S8, S23 |

  Android 16 (API 36) replaced `BODY_SENSORS` with the Health Connect permission `android.permission.health.READ_HEART_RATE`. The manifest declares both, and limits `BODY_SENSORS` to older versions with `maxSdkVersion="35"`.

**`res/values/themes.xml`** has a plain base theme with no action bar. Compose draws the top bars itself.

### Step 3: Theme (`ui/theme/`)

- `Color.kt` has a green/teal fitness palette. The wireframes are grayscale, so the colors are our own choice.
- `Theme.kt` defines `FitnessTrackerTheme`, a Material 3 light and dark color scheme. The app follows the phone's dark-mode setting.
- `Type.kt` sets the text sizes for headlines, titles, and body text.

All Composables are wrapped in `FitnessTrackerTheme` in `MainActivity`, so every screen uses the same colors and fonts.

### Step 4: Data model (`data/model/UserProfile.kt`)

```kotlin
data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val onboardingComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)
```

- Saved in Firestore at **`users/{uid}`**, where `uid` is the Firebase Auth user ID. Each user has exactly one profile document.
- **Every field has a default value.** Firestore's `toObject()` needs a no-argument constructor, and default values give Kotlin one.
- Body stats can be empty (`null`) because the S3 wireframe marks them optional. They'll be used for calorie estimates in later sprints and can be edited in S28.
- `onboardingComplete` records whether the user has finished S4, so returning Google users skip it.

Example document in the Firestore console:

```
users / 8fJ2kd...  (document)
  uid: "8fJ2kd..."
  fullName: "Mehedi Hasan"
  email: "mehedi@email.com"
  age: 24
  heightCm: 178
  weightKg: 76
  onboardingComplete: true
  createdAt: 1791561600000
```

### Step 5: Repositories (`data/repository/`)

**`AuthRepository.kt`**: wraps `FirebaseAuth`.

| Function | What it does |
|---|---|
| `currentUser` | The logged-in user, or `null` |
| `signIn(email, password)` | `signInWithEmailAndPassword` |
| `signUp(name, email, password)` | `createUserWithEmailAndPassword`, then saves the display name on the Auth user |
| `signInWithGoogle(idToken)` | Turns the Google ID token into a Firebase credential and signs in. Also reports `isNewUser`. |
| `sendPasswordReset(email)` | Sends Firebase's reset email |
| `signOut()` | Logs out |

Each function is `suspend` and uses `.await()`, so the caller waits for Firebase without blocking the screen.

`toAuthMessage()` turns Firebase errors into the short messages the wireframes show:

| Firebase exception | Message shown |
|---|---|
| `FirebaseNetworkException` | "No internet connection. Try again." |
| `FirebaseAuthWeakPasswordException` | "Password is too weak. Use at least 8 characters." |
| `FirebaseAuthUserCollisionException` | "An account with this email already exists." |
| `FirebaseAuthInvalidUserException` / `InvalidCredentialsException` | "Incorrect email or password" |

The check for the weak-password error comes **before** the invalid-credentials check, because `WeakPassword` is a subclass of `InvalidCredentials` and would otherwise be caught by the wrong branch.

**`UserRepository.kt`**: wraps `FirebaseFirestore`.

| Function | What it does |
|---|---|
| `createProfile(profile)` | Writes the whole `UserProfile` to `users/{uid}` |
| `getProfile(uid)` | Reads it back, or returns `null` if it doesn't exist |
| `setOnboardingComplete(uid)` | Sets only `onboardingComplete = true` using `SetOptions.merge()`, so other fields aren't overwritten |

### Step 6: Application class and dependency container (`FitnessApp.kt`)

`FitnessApp` runs once when the app starts and creates an **`AppContainer`**:

- `isFirebaseConfigured`: `true` only if Firebase started, which means `google-services.json` was present at build time.
- `authRepository`, `userRepository`: created once with `by lazy` and shared by every screen.
- `appScope`: a coroutine scope that lives as long as the app. `markOnboardingComplete()` uses it so the Firestore write finishes even after the S4 screen has closed.

This is "manual dependency injection." It does the same job as a library like Hilt, but without the extra setup, which keeps the code easier to follow for a class project.

### Step 7: MainActivity (`MainActivity.kt`)

The app has **one Activity**, and every screen is a Composable inside it.

```kotlin
setContent {
    FitnessTrackerTheme {
        if (container.isFirebaseConfigured) AppNavHost(container)
        else FirebaseMissingScreen()
    }
}
```

`enableEdgeToEdge()` lets the app draw behind the status bar, which is the modern Android look. Each screen's `Scaffold` adds the right padding.

### Step 8: Navigation (`ui/navigation/`)

**`Routes.kt`** lists one route string per screen, each labeled with its wireframe ID:

| Route | Wireframe |
|---|---|
| `login` | S1, S2 |
| `sign_up` | S3 |
| `permissions` | S4 |
| `home` | S5 |

**`AppNavHost.kt`** builds the navigation graph:

- **Start screen:** `home` if a user is already logged in (Firebase remembers the session), otherwise `login`.
- Each route creates its ViewModel with `viewModel { LoginViewModel(...) }` and passes in the repositories from the container.
- **`navigateClearingBackStack(route)`** navigates and removes every earlier screen. After you log in, pressing Back closes the app instead of returning to the log-in form. After you log out, Back can't reach Home.

Navigation flow built in this sprint:

```
            ┌─────────── Sign up ───────────┐
            ▼                               │
   S3 Sign Up ── Create Account ──► S4 Permissions ── Continue / Skip ──► S5 Home
            │                               ▲                                │
           Back                             │                             Log out
            ▼                               │                                │
   S1 Log In ── Continue with Google ───────┘ (first-time Google user)        │
     │  ▲                                                                    │
     │  └────────────────────────────────────────────────────────────────────┘
     └── Log In (success) ─────────────────────────────────────────► S5 Home
     └── Log In (failure) ──► S2 error state (same screen)
```

### Step 9: Shared form components (`ui/components/FormComponents.kt`)

Reused by S1 and S3 so both screens look and behave the same:

- **`FormTextField`**: an outlined text field. When `error` is set, the outline turns red and the message appears under the field, matching the S2 wireframe ("fields with problems get a heavy outline... a hint appears under the field").
- **`PasswordTextField`**: the same, with a **show / hide** button that toggles password visibility (S1 "show"). It uses `rememberSaveable`, so the toggle survives rotation.
- **`ErrorBanner`**: the red banner with an icon at the top of the form ("! Incorrect email or password").

### Step 10: S1 / S2 Log In (`ui/auth/LoginViewModel.kt`, `LoginScreen.kt`)

**State (`LoginUiState`)**: `email`, `password`, `emailError`, `passwordError`, `bannerError`, `isLoading`, `snackbarMessage`, `destination`.

**S1 and S2 are the same screen.** S2 is simply S1 with `bannerError` and the field errors filled in. The wireframes call S2 a "state," and this follows that idea.

| Wireframe element | How it's built |
|---|---|
| ① **Log In** | `logIn()` checks the email format and password length (8+ characters). If those pass, it calls Firebase. Success goes to Home; failure shows the S2 banner. |
| ② **Sign up** link | Navigates to S3 |
| ③ **Forgot password?** | `sendPasswordReset()` needs a valid email in the field, sends the Firebase reset email, then shows a snackbar: "Check your inbox for a reset link" |
| ④ **Continue with Google** | See below |
| S2: "error clears as soon as the user edits a field" | `onEmailChange` / `onPasswordChange` clear the errors |
| S2: "No internet connection. Try again." | Comes from `toAuthMessage()` |

While a request is running, the Log In button shows a spinner and is disabled, so you can't submit twice.

**Navigation through state:** the ViewModel never navigates itself. It sets `destination = HOME` or `ONBOARDING`. The screen's `LaunchedEffect(state.destination)` sees the change, navigates, and calls `onNavigated()` to reset it. Keeping navigation out of the ViewModel is the standard Compose approach, because the ViewModel has no access to the `NavController`.

**Google sign-in (`GoogleSignInHelper.kt`):**
1. Builds a `GetGoogleIdOption` with the app's **web client ID**.
2. `CredentialManager.getCredential()` shows the Google account picker.
3. Reads the **ID token** from the result.
4. `LoginViewModel.signInWithGoogle(token)` signs in to Firebase. If the user has no Firestore profile yet, one is created from their Google name and email.
5. Users who haven't finished onboarding go to **S4**, matching the wireframe ("First-time users go to S4"). Everyone else goes to Home.

The web client ID (`default_web_client_id`) is a string the Google Services plugin generates from `google-services.json`. The helper looks it up by name at runtime instead of using `R.string.default_web_client_id`, which wouldn't compile before Firebase is set up. If the ID is missing, the user sees a clear message instead of a crash.

Cancelling the account picker does nothing, which is the expected behavior. "No Google account on device" and other errors appear in the banner.

### Step 11: S3 Sign Up (`ui/auth/SignUpViewModel.kt`, `SignUpScreen.kt`)

**Fields:** full name, email, password, confirm password, age, height (cm), weight (kg), and the Terms checkbox.

**Validation rules** (all run when you tap Create Account; each error appears under its field):

| Field | Rule |
|---|---|
| Full name | Required |
| Email | Required and a valid email format |
| Password | At least 8 characters |
| Confirm password | Must match the password |
| Age | Optional; if filled, 10–120 |
| Height | Optional; if filled, 50–250 cm |
| Weight | Optional; if filled, 20–400 kg |
| Terms | Must be checked |

Number fields filter what you type: age accepts digits only, and height and weight accept digits and a decimal point.

Errors are stored in a `Map<SignUpField, String>`, so one map covers every field. Editing a field removes only that field's error.

**When you tap Create Account** (wireframe ②):
1. `authRepository.signUp()` creates the Firebase Auth user and sets the display name.
2. `userRepository.createProfile()` writes the profile to **`users/{uid}`** in Firestore.
3. `accountCreated = true` sends the user to **S4**.

**①** The back arrow and the "Already have an account? Log in" link both return to S1.

### Step 12: S4 Health & Permissions (`ui/onboarding/PermissionsScreen.kt`)

| Row | Android permission(s) |
|---|---|
| Heart rate | `READ_HEART_RATE` on Android 16+, `BODY_SENSORS` before that |
| Steps | `ACTIVITY_RECOGNITION` |
| Location | `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION` |
| Notifications | `POST_NOTIFICATIONS` |

- **① Allow** opens the Android system permission dialog through `rememberLauncherForActivityResult(RequestMultiplePermissions())`.
- After a result comes back, the row changes to **"✓ Allowed"** (wireframe: "The button changes to ✓ Allowed once granted").
- `LifecycleResumeEffect` re-checks every permission each time the screen comes back into view. If the user grants something in phone Settings and returns, the screen updates.
- **② Pair a smartwatch** is visible but disabled. Watch pairing (S26) is built in Sprint 10.
- **③ Continue** and **Skip for now** both save `onboardingComplete = true` to Firestore and go to Home, removing the auth screens from the back stack.

This follows our answer to wireframe feedback question 4: permissions are offered all together during setup, but each one is optional and can be granted later.

### Step 13: Temporary Home (`ui/home/HomeScreen.kt`)

A placeholder that shows "Hi, {first name}" and a **Log out** button, so this sprint's full log-in/log-out cycle can be tested. **Sprint 2 replaces it** with the real S5 dashboard, drawer, and bottom tabs.

### Step 14: Firebase-not-configured screen

If `google-services.json` is missing, the app shows "Firebase is not set up" with instructions instead of crashing when it first tries to use Firebase.

---

## 6. Jetpack Compose concepts used

| Concept | Where | Purpose |
|---|---|---|
| `@Composable` functions | Every screen | Builds UI in Kotlin code, with no XML layouts |
| `Scaffold`, `TopAppBar`, `SnackbarHost` | Login, Sign Up, Permissions | Standard Material 3 screen layout |
| `StateFlow` + `collectAsStateWithLifecycle()` | All screens with a ViewModel | The UI redraws when state changes and stops listening when the app is in the background |
| `LaunchedEffect` | Login, Sign Up | Runs navigation or snackbars once when a state value changes |
| `remember` / `rememberSaveable` | Password toggle, permission refresh | Keeps small UI values; `Saveable` also survives rotation |
| `rememberLauncherForActivityResult` | S4 | Requests runtime permissions from Compose |
| `LifecycleResumeEffect` | S4 | Re-checks permissions when the screen comes back into view |
| `Modifier.verticalScroll`, `imePadding` | Forms | Forms scroll, and the keyboard doesn't cover the fields |
| `NavHost` / `composable()` | `AppNavHost` | Navigation between Composables |

---

## 7. Firestore data stored so far

```
users/{uid}
    uid, fullName, email, age?, heightCm?, weightKg?, onboardingComplete, createdAt
```

Later sprints add sub-collections under each user, for example `users/{uid}/workouts`, `users/{uid}/goals`, and `users/{uid}/reminders`.

**Suggested security rules** (Firestore → Rules). Each user can read and write only their own data:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

---

## 8. Firebase setup (one-time)

1. Open the [Firebase console](https://console.firebase.google.com). Use the existing project or create a new one.
2. **Add app → Android**. Package name: **`com.unh.fitnesstracker`**.
3. Download **`google-services.json`** and put it in `FitnessTracker/app/`.
4. **Authentication → Sign-in method**: turn on **Email/Password**. Turn on **Google** if you want Google sign-in.
5. **Firestore Database → Create database**. Test mode works while developing; switch to the rules in section 7 before submitting.
6. For Google sign-in only:
   - In Android Studio: **Gradle panel → app → Tasks → android → signingReport** and copy the **SHA-1** value.
   - Firebase → **Project settings → your Android app → Add fingerprint** and paste it.
   - Download `google-services.json` **again** (it now includes the web client ID) and replace the old file.
7. In Android Studio: **File → Sync Project with Gradle Files**, then **Run**.

`app/google-services.json` is listed in `.gitignore`, so the Firebase keys don't get committed to git.

---

## 9. How to test this sprint

| # | Test | Expected result |
|---|---|---|
| 1 | Launch the app without `google-services.json` | "Firebase is not set up" screen |
| 2 | Launch with Firebase configured | S1 Log In |
| 3 | Tap Log In with empty fields | Banner "Please fix the highlighted fields"; red outlines; "Email is required" and "Password must be at least 8 characters" |
| 4 | Type in a field after an error | That field's error and the banner disappear |
| 5 | Log in with a wrong password | Banner "Incorrect email or password" (S2) |
| 6 | Turn on airplane mode and log in | "No internet connection. Try again." |
| 7 | Tap the show/hide toggle | Password text appears or is hidden |
| 8 | Forgot password? with an empty email | Email error shown |
| 9 | Forgot password? with a real email | Snackbar "Check your inbox for a reset link"; the email arrives |
| 10 | Sign up → Create Account with empty fields | An error under each required field and under Terms |
| 11 | Sign up with mismatched passwords | "Passwords don't match" |
| 12 | Sign up with valid data | Goes to S4. The user appears in **Authentication**, and `users/{uid}` appears in **Firestore** with the body stats |
| 13 | Sign up with an email that's already used | "An account with this email already exists." |
| 14 | S4: tap Allow on each row → grant | Row changes to "✓ Allowed" |
| 15 | S4: deny, then grant in phone Settings, return | Row updates to "✓ Allowed" |
| 16 | S4: Continue | Home shows "Hi, {first name}"; `onboardingComplete` becomes `true` in Firestore |
| 17 | On Home, press Back | App closes (can't go back to the auth screens) |
| 18 | Close and reopen the app | Opens straight to Home (session remembered) |
| 19 | Log out | Back to S1; pressing Back doesn't return to Home |
| 20 | Continue with Google (first time) | Account picker → S4 → Home; profile created in Firestore |
| 21 | Continue with Google (second time) | Account picker → straight to Home |

---

## 10. Known limitations (planned for later sprints)

- Home is a placeholder until **Sprint 2**.
- "Pair a smartwatch" on S4 is disabled until **Sprint 10**.
- Profile editing (body stats, name) comes in **Sprint 2** (S28).
- Signing out doesn't yet "clear local data" (S29). There's no local data to clear yet; it'll be handled when local caching is added.
- The Terms & Privacy Policy text is not linked to an actual document.

---

## 11. Sprint review checklist

- [ ] Project opens and syncs in Android Studio
- [ ] `google-services.json` added and app connects to Firebase
- [ ] S1 log-in works and S2 errors appear correctly
- [ ] S3 sign-up creates the Auth user and the Firestore profile
- [ ] S4 permissions request and display correctly
- [ ] Session persists across app restarts; log out works
- [ ] Google sign-in works (optional)
- [ ] Approved to start **Sprint 2: Home dashboard, drawer, bottom tabs, profile**
