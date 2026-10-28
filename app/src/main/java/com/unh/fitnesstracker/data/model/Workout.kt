package com.unh.fitnesstracker.data.model

import com.google.firebase.firestore.Exclude

enum class WorkoutType { STRENGTH, RUN, WALK, CYCLE }

enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    LEGS("Legs"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    CORE("Core"),
    CARDIO("Cardio"),
}

/** Whether a set is logged as weight × reps, or as a duration (planks, rowing, ...). */
enum class TrackedBy { REPS, TIME }

/**
 * One exercise the user can pick in S10. Built-in ones live in [ExerciseLibrary];
 * custom ones are stored at users/{uid}/customExercises/{id}.
 */
data class Exercise(
    val id: String = "",
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val trackedBy: TrackedBy = TrackedBy.REPS,
    val custom: Boolean = false,
)

/** One logged set. Weight is always stored in kg, whatever unit the user typed in. */
data class SetEntry(
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val durationSec: Int = 0,
    val completed: Boolean = false,
)

data class ExerciseEntry(
    val exerciseId: String = "",
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val trackedBy: TrackedBy = TrackedBy.REPS,
    val sets: List<SetEntry> = emptyList(),
)

/** A saved workout at users/{uid}/workouts/{id}. Outdoor fields (distance, route, ...) arrive in Sprint 8. */
data class Workout(
    val id: String = "",
    val type: WorkoutType = WorkoutType.STRENGTH,
    val name: String = "",
    val startedAt: Long = 0,
    val endedAt: Long = 0,
    val durationSec: Long = 0,
    val exercises: List<ExerciseEntry> = emptyList(),
    val totalSets: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val calories: Int = 0,
    val avgHr: Int? = null,
    val rating: Int? = null,
    val notes: String = "",
    val source: String = "PHONE",
) {
    @get:Exclude
    val durationMinutes: Int get() = (durationSec / 60).toInt()
}

data class TemplateExercise(
    val exerciseId: String = "",
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val trackedBy: TrackedBy = TrackedBy.REPS,
    val sets: Int = 3,
    val targetReps: Int = 0,
    val targetWeightKg: Double = 0.0,
)

/** A reusable workout at users/{uid}/templates/{id}, shown as a chip in S7. */
data class WorkoutTemplate(
    val id: String = "",
    val name: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val createdAt: Long = 0,
)
