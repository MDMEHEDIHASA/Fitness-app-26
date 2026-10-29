package com.unh.fitnesstracker.ui.workout

import com.unh.fitnesstracker.data.model.ExerciseEntry
import com.unh.fitnesstracker.data.model.SetEntry
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutType
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Text formatting shared by the workout screens (S9–S16). */
object WorkoutFormat {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val listDateFormatter = DateTimeFormatter.ofPattern("EEE d")

    /** 24:13, or 1:02:03 past an hour. */
    fun clock(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun unitLabel(units: UnitSystem) = if (units == UnitSystem.IMPERIAL) "lb" else "kg"

    fun weight(kg: Double, units: UnitSystem): String {
        val value = if (units == UnitSystem.IMPERIAL) Units.kgToLb(kg) else kg
        return "${ActiveWorkoutSession.formatDecimal(value).ifEmpty { "0" }} ${unitLabel(units)}"
    }

    fun volume(kg: Double, units: UnitSystem): String {
        val value = if (units == UnitSystem.IMPERIAL) Units.kgToLb(kg) else kg
        return "${NumberFormat.getIntegerInstance().format(value.toLong())} ${unitLabel(units)}"
    }

    /** "60 kg × 10", "Bodyweight × 12" or "45 s". */
    fun set(set: SetEntry, trackedBy: TrackedBy, units: UnitSystem): String = when (trackedBy) {
        TrackedBy.TIME -> if (set.weightKg > 0) "${weight(set.weightKg, units)} · ${set.durationSec} s" else "${set.durationSec} s"
        TrackedBy.REPS -> if (set.weightKg > 0) "${weight(set.weightKg, units)} × ${set.reps}" else "Bodyweight × ${set.reps}"
    }

    fun dateTime(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    fun monthHeader(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(monthFormatter)

    fun listDate(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(listDateFormatter)

    fun typeLabel(type: WorkoutType): String = when (type) {
        WorkoutType.STRENGTH -> "Strength"
        WorkoutType.RUN -> "Run"
        WorkoutType.WALK -> "Walk"
        WorkoutType.CYCLE -> "Cycle"
    }

    /** One-line subtitle on an S17 history row. */
    fun listSubtitle(workout: Workout, units: UnitSystem): String = buildString {
        append(listDate(workout.startedAt))
        append(" · ")
        append(clock(workout.durationSec))
        if (workout.type == WorkoutType.STRENGTH) {
            append(" · ")
            append("${workout.totalSets} sets")
            append(" · ")
            append(volume(workout.totalVolumeKg, units))
        }
    }

    /** Plain-text summary for the Android share sheet. */
    fun shareText(workout: Workout, units: UnitSystem): String = buildString {
        val date = Instant.ofEpochMilli(workout.startedAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
        appendLine("${workout.name} — $date")
        appendLine(
            "${clock(workout.durationSec)} · ${workout.totalSets} sets · " +
                "${volume(workout.totalVolumeKg, units)} lifted · ${workout.calories} kcal",
        )
        workout.exercises.forEach { appendLine(exerciseLine(it, units)) }
        if (workout.notes.isNotEmpty()) appendLine("Notes: ${workout.notes}")
        append("Logged with Fitness Tracker")
    }

    private fun exerciseLine(entry: ExerciseEntry, units: UnitSystem): String =
        "${entry.name}: " + entry.sets.joinToString(", ") { set(it, entry.trackedBy, units) }
}
