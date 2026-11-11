package com.unh.fitnesstracker.data.model

import com.google.firebase.firestore.Exclude

enum class RecordType(val label: String) {
    HEAVIEST_LIFT("Heaviest lift"),
    MOST_VOLUME("Most volume"),
    FASTEST_5K("Fastest 5K"),
    LONGEST_RUN("Longest run"),
}

/** One personal best at users/{uid}/personalRecords/{id}. */
data class PersonalRecord(
    val id: String = "",
    val type: RecordType = RecordType.MOST_VOLUME,
    val exerciseId: String = "",
    val exerciseName: String = "",
    val value: Double = 0.0,
    val workoutId: String = "",
    val achievedAt: Long = 0,
) {
    /** Stable document id so beating a record overwrites the old one. */
    @get:Exclude
    val key: String
        get() = if (type == RecordType.HEAVIEST_LIFT) "HEAVIEST_LIFT_$exerciseId" else type.name
}

/** One weigh-in at users/{uid}/bodyWeight/{id}. */
data class BodyWeightEntry(
    val id: String = "",
    val weightKg: Double = 0.0,
    val recordedAt: Long = 0,
)

enum class ProgressRange(val label: String) { WEEK("Week"), MONTH("Month"), YEAR("Year") }

enum class ProgressMetric(val label: String) {
    WORKOUTS("Workouts completed"),
    VOLUME("Volume lifted"),
    ACTIVE_MINUTES("Active minutes"),
    DISTANCE("Distance"),
    BODY_WEIGHT("Body weight"),
    STEPS("Steps"),
    RESTING_HR("Resting heart rate"),
}
