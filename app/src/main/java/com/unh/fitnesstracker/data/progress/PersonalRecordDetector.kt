package com.unh.fitnesstracker.data.progress

import com.unh.fitnesstracker.data.model.PersonalRecord
import com.unh.fitnesstracker.data.model.RecordType
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutType

/** Compares a just-saved workout against current records and returns the ones it broke. */
object PersonalRecordDetector {

    fun detect(workout: Workout, existing: List<PersonalRecord>): List<PersonalRecord> {
        val byKey = existing.associateBy { it.key }
        val found = mutableListOf<PersonalRecord>()
        val at = workout.endedAt.takeIf { it > 0 } ?: workout.startedAt

        fun consider(
            type: RecordType,
            value: Double,
            exerciseId: String = "",
            exerciseName: String = "",
            lowerIsBetter: Boolean = false,
        ) {
            if (value <= 0.0) return
            val key = if (type == RecordType.HEAVIEST_LIFT) "HEAVIEST_LIFT_$exerciseId" else type.name
            val previous = byKey[key]
            val beaten = when {
                previous == null -> true
                lowerIsBetter -> value < previous.value
                else -> value > previous.value
            }
            if (!beaten) return
            found += PersonalRecord(
                id = key,
                type = type,
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                value = value,
                workoutId = workout.id,
                achievedAt = at,
            )
        }

        consider(RecordType.MOST_VOLUME, workout.totalVolumeKg)
        workout.exercises.forEach { entry ->
            val heaviest = entry.sets.maxOfOrNull { it.weightKg } ?: 0.0
            consider(RecordType.HEAVIEST_LIFT, heaviest, entry.exerciseId, entry.name)
        }
        if (workout.type == WorkoutType.RUN) {
            consider(RecordType.LONGEST_RUN, workout.distanceM)
            if (workout.distanceM >= 5_000 && workout.durationSec > 0) {
                val fiveKSec = workout.durationSec * (5_000.0 / workout.distanceM)
                consider(RecordType.FASTEST_5K, fiveKSec, lowerIsBetter = true)
            }
        }
        return found
    }
}
