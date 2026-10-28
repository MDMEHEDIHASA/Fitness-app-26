package com.unh.fitnesstracker.ui.navigation

/** Navigation routes. The comment on each one is its screen ID in the wireframes. */
object Routes {
    // Top-level graph (AppNavHost)
    const val LOGIN = "login"             // S1, S2
    const val SIGN_UP = "sign_up"         // S3
    const val PERMISSIONS = "permissions" // S4
    const val MAIN = "main"               // Everything behind log-in (MainScreen)

    // Main graph (MainScreen). The first four are the bottom tabs.
    const val HOME = "home"               // S5
    const val WORKOUTS = "workouts"       // S17, S31
    const val PROGRESS = "progress"       // S19
    const val PROFILE = "profile"         // S28

    const val GOALS = "goals"             // S21
    const val REMINDERS = "reminders"     // S23
    const val SMARTWATCH = "smartwatch"   // S25, S27
    const val HELP = "help"

    const val STRENGTH_WORKOUT = "workout/strength"             // S9, S11, S12
    const val ADD_EXERCISE = "workout/strength/add"             // S10
    const val WORKOUT_SUMMARY = "workout/summary"               // S16
    const val OUTDOOR_WORKOUT = "workout/outdoor/{type}"        // S13
    const val OUTDOOR_TYPE_ARG = "type"

    fun outdoorWorkout(type: OutdoorType) = "workout/outdoor/${type.name}"
}

enum class OutdoorType(val label: String) { RUN("Outdoor run"), WALK("Outdoor walk"), CYCLE("Cycling") }
