package com.unh.fitnesstracker.data.session

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager
import com.unh.fitnesstracker.data.model.Calories
import com.unh.fitnesstracker.data.model.Exercise
import com.unh.fitnesstracker.data.model.ExerciseEntry
import com.unh.fitnesstracker.data.model.SetEntry
import com.unh.fitnesstracker.data.model.TemplateExercise
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutTemplate
import com.unh.fitnesstracker.data.model.WorkoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/** A set as the user is typing it: text fields stay strings until the workout is saved. */
data class ActiveSet(
    val key: Long,
    val weight: String = "",
    /** Reps, or seconds for [TrackedBy.TIME] exercises. */
    val reps: String = "",
    val completed: Boolean = false,
)

data class ActiveExercise(
    val key: Long,
    val exercise: Exercise,
    val sets: List<ActiveSet>,
    val collapsed: Boolean = false,
) {
    val completedSets: Int get() = sets.count { it.completed }
}

/** [id] changes for every new rest, so the S11 sheet can reopen even if the user hid the previous one. */
data class RestTimer(val id: Long, val totalSec: Int, val remainingSec: Int, val nextLabel: String?)

data class ActiveWorkout(
    val name: String,
    val startedAt: Long,
    val exercises: List<ActiveExercise> = emptyList(),
    val elapsedSec: Long = 0,
    val paused: Boolean = false,
    val rest: RestTimer? = null,
    val units: UnitSystem,
    val bodyWeightKg: Double,
    val defaultRestSec: Int,
) {
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val completedSets: Int get() = exercises.sumOf { it.completedSets }
    val calories: Int get() = Calories.estimate(Calories.STRENGTH_MET, bodyWeightKg, elapsedSec)
}

/**
 * The one strength workout in progress (S9–S12). It lives in [com.unh.fitnesstracker.AppContainer],
 * not in a ViewModel, so the workout keeps running while the user opens S10, rotates the phone, or
 * goes back to Home. Clock times are wall-clock based, so the ticker delay never causes drift.
 * The session is in memory only: it is lost if Android kills the app process.
 */
class ActiveWorkoutSession(context: Context, private val scope: CoroutineScope) {

    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    private val _state = MutableStateFlow<ActiveWorkout?>(null)
    val state: StateFlow<ActiveWorkout?> = _state.asStateFlow()

    /** The workout just saved from S12, shown by S16. */
    private val _lastSaved = MutableStateFlow<Workout?>(null)
    val lastSaved: StateFlow<Workout?> = _lastSaved.asStateFlow()

    private var nextKey = 0L
    private var accumulatedMs = 0L
    private var runningSinceMs: Long? = null
    private var restEndsAtMs = 0L
    private var restRemainingWhilePausedMs = 0L
    private var ticker: Job? = null

    val isActive: Boolean get() = _state.value != null

    fun start(
        name: String,
        units: UnitSystem,
        bodyWeightKg: Double?,
        defaultRestSec: Int,
        template: WorkoutTemplate? = null,
    ) {
        val now = System.currentTimeMillis()
        accumulatedMs = 0
        runningSinceMs = now
        _state.value = ActiveWorkout(
            name = template?.name ?: name,
            startedAt = now,
            exercises = template?.exercises.orEmpty().map { it.toActive(units) },
            units = units,
            bodyWeightKg = bodyWeightKg ?: Calories.DEFAULT_BODY_WEIGHT_KG,
            defaultRestSec = defaultRestSec,
        )
        startTicker()
    }

    fun rename(name: String) = edit { it.copy(name = name) }

    fun pause() {
        val running = runningSinceMs ?: return
        val now = System.currentTimeMillis()
        accumulatedMs += now - running
        runningSinceMs = null
        restRemainingWhilePausedMs = (restEndsAtMs - now).coerceAtLeast(0)
        edit { it.copy(paused = true) }
    }

    fun resume() {
        if (runningSinceMs != null) return
        val now = System.currentTimeMillis()
        runningSinceMs = now
        restEndsAtMs = now + restRemainingWhilePausedMs
        edit { it.copy(paused = false) }
    }

    fun addExercises(exercises: List<Exercise>) = edit { workout ->
        workout.copy(exercises = workout.exercises + exercises.map { ActiveExercise(key(), it, listOf(ActiveSet(key()))) })
    }

    fun removeExercise(exerciseKey: Long) = edit { workout ->
        workout.copy(exercises = workout.exercises.filterNot { it.key == exerciseKey })
    }

    /** Moves an exercise up ([delta] = -1) or down (+1). */
    fun moveExercise(exerciseKey: Long, delta: Int) = edit { workout ->
        val list = workout.exercises.toMutableList()
        val from = list.indexOfFirst { it.key == exerciseKey }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@edit workout
        list.add(to, list.removeAt(from))
        workout.copy(exercises = list)
    }

    fun toggleCollapsed(exerciseKey: Long) = editExercise(exerciseKey) { it.copy(collapsed = !it.collapsed) }

    /** Adds a set pre-filled with the previous set's numbers, since most sets repeat them. */
    fun addSet(exerciseKey: Long) = editExercise(exerciseKey) { exercise ->
        val last = exercise.sets.lastOrNull()
        exercise.copy(sets = exercise.sets + ActiveSet(key(), last?.weight.orEmpty(), last?.reps.orEmpty()))
    }

    fun removeSet(exerciseKey: Long, setKey: Long) = editExercise(exerciseKey) { exercise ->
        exercise.copy(sets = exercise.sets.filterNot { it.key == setKey })
    }

    fun updateSet(exerciseKey: Long, setKey: Long, weight: String? = null, reps: String? = null) =
        editSet(exerciseKey, setKey) { set -> set.copy(weight = weight ?: set.weight, reps = reps ?: set.reps) }

    /** Checking a set starts the S11 rest timer; un-checking it does not. */
    fun toggleSetCompleted(exerciseKey: Long, setKey: Long) {
        var nowCompleted = false
        editSet(exerciseKey, setKey) { set ->
            nowCompleted = !set.completed
            set.copy(completed = !set.completed)
        }
        val workout = _state.value ?: return
        if (nowCompleted) startRest(workout.defaultRestSec, nextSetLabel(workout))
    }

    fun adjustRest(deltaSec: Int) {
        val rest = _state.value?.rest ?: return
        val now = System.currentTimeMillis()
        val remainingMs = if (runningSinceMs == null) restRemainingWhilePausedMs else restEndsAtMs - now
        val newRemainingMs = (remainingMs + deltaSec * 1000L).coerceAtLeast(0)
        restEndsAtMs = now + newRemainingMs
        restRemainingWhilePausedMs = newRemainingMs
        if (newRemainingMs == 0L) {
            skipRest()
        } else {
            val remainingSec = secondsLeft(newRemainingMs)
            edit { it.copy(rest = rest.copy(totalSec = maxOf(rest.totalSec, remainingSec), remainingSec = remainingSec)) }
        }
    }

    fun skipRest() = edit { it.copy(rest = null) }

    /** Builds the [Workout] to save. Only checked sets count; exercises with none are left out. */
    fun buildWorkout(rating: Int?, notes: String): Workout? {
        val workout = _state.value ?: return null
        val entries = workout.exercises.mapNotNull { active ->
            val sets = active.sets.filter { it.completed }.map { it.toSetEntry(active.exercise.trackedBy, workout.units) }
            if (sets.isEmpty()) null
            else ExerciseEntry(active.exercise.id, active.exercise.name, active.exercise.muscleGroup, active.exercise.trackedBy, sets)
        }
        val endedAt = System.currentTimeMillis()
        return Workout(
            type = WorkoutType.STRENGTH,
            name = workout.name,
            startedAt = workout.startedAt,
            endedAt = endedAt,
            durationSec = elapsedMs(endedAt) / 1000,
            exercises = entries,
            totalSets = entries.sumOf { it.sets.size },
            totalVolumeKg = entries.flatMap { it.sets }.sumOf { it.weightKg * it.reps },
            calories = workout.calories,
            rating = rating,
            notes = notes.trim(),
        )
    }

    /** Turns the current exercises into a template; the numbers of each exercise's last set become the targets. */
    fun buildTemplate(name: String): WorkoutTemplate? {
        val workout = _state.value ?: return null
        return WorkoutTemplate(
            name = name,
            createdAt = System.currentTimeMillis(),
            exercises = workout.exercises.map { active ->
                val last = active.sets.lastOrNull()?.toSetEntry(active.exercise.trackedBy, workout.units) ?: SetEntry()
                TemplateExercise(
                    exerciseId = active.exercise.id,
                    name = active.exercise.name,
                    muscleGroup = active.exercise.muscleGroup,
                    trackedBy = active.exercise.trackedBy,
                    sets = active.sets.size.coerceAtLeast(1),
                    targetReps = if (active.exercise.trackedBy == TrackedBy.TIME) last.durationSec else last.reps,
                    targetWeightKg = last.weightKg,
                )
            },
        )
    }

    /** Ends the session after S12 saved it, and hands the result to S16. */
    fun complete(saved: Workout) {
        _lastSaved.value = saved
        reset()
    }

    fun discard() = reset()

    fun clearLastSaved() {
        _lastSaved.value = null
    }

    private fun reset() {
        ticker?.cancel()
        ticker = null
        runningSinceMs = null
        accumulatedMs = 0
        _state.value = null
    }

    private fun startRest(seconds: Int, nextLabel: String?) {
        restEndsAtMs = System.currentTimeMillis() + seconds * 1000L
        restRemainingWhilePausedMs = seconds * 1000L
        edit { it.copy(rest = RestTimer(key(), seconds, seconds, nextLabel)) }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            // delay() throws when the job is cancelled, which ends the loop.
            while (true) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val paused = runningSinceMs == null
        var restFinished = false
        _state.update { workout ->
            workout ?: return@update null
            val rest = workout.rest?.let { rest ->
                if (paused) return@let rest
                val remainingMs = restEndsAtMs - now
                if (remainingMs <= 0) {
                    restFinished = true
                    null
                } else {
                    rest.copy(remainingSec = secondsLeft(remainingMs))
                }
            }
            workout.copy(elapsedSec = elapsedMs(now) / 1000, rest = rest)
        }
        if (restFinished) vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
    }

    private fun elapsedMs(now: Long): Long = accumulatedMs + (runningSinceMs?.let { now - it } ?: 0)

    private fun secondsLeft(ms: Long): Int = ((ms + 999) / 1000).toInt()

    /** "Bench Press · Set 3 of 3" for the next unchecked set, or null when everything is done. */
    private fun nextSetLabel(workout: ActiveWorkout): String? {
        for (exercise in workout.exercises) {
            val index = exercise.sets.indexOfFirst { !it.completed }
            if (index >= 0) return "${exercise.exercise.name} · Set ${index + 1} of ${exercise.sets.size}"
        }
        return null
    }

    private fun edit(transform: (ActiveWorkout) -> ActiveWorkout) {
        _state.update { it?.let(transform) }
    }

    private fun editExercise(exerciseKey: Long, transform: (ActiveExercise) -> ActiveExercise) = edit { workout ->
        workout.copy(exercises = workout.exercises.map { if (it.key == exerciseKey) transform(it) else it })
    }

    private fun editSet(exerciseKey: Long, setKey: Long, transform: (ActiveSet) -> ActiveSet) =
        editExercise(exerciseKey) { exercise ->
            exercise.copy(sets = exercise.sets.map { if (it.key == setKey) transform(it) else it })
        }

    private fun key(): Long = nextKey++

    private fun TemplateExercise.toActive(units: UnitSystem): ActiveExercise {
        val weight = formatDecimal(if (units == UnitSystem.IMPERIAL) Units.kgToLb(targetWeightKg) else targetWeightKg)
        val reps = if (targetReps > 0) targetReps.toString() else ""
        return ActiveExercise(
            key = key(),
            exercise = Exercise(exerciseId, name, muscleGroup, trackedBy),
            sets = List(sets.coerceAtLeast(1)) { ActiveSet(key(), weight, reps) },
        )
    }

    private fun ActiveSet.toSetEntry(trackedBy: TrackedBy, units: UnitSystem): SetEntry {
        val typed = weight.replace(',', '.').toDoubleOrNull() ?: 0.0
        val kg = if (units == UnitSystem.IMPERIAL) Units.lbToKg(typed) else typed
        val number = reps.toIntOrNull() ?: 0
        return SetEntry(
            weightKg = (kg * 100).roundToInt() / 100.0,
            reps = if (trackedBy == TrackedBy.REPS) number else 0,
            durationSec = if (trackedBy == TrackedBy.TIME) number else 0,
            completed = completed,
        )
    }

    companion object {
        private const val TICK_MS = 500L

        /** "60", "62.5", or "" for zero, so empty fields stay empty. */
        fun formatDecimal(value: Double): String = when {
            value <= 0.0 -> ""
            value % 1.0 == 0.0 -> value.toLong().toString()
            else -> String.format(Locale.US, "%.1f", value)
        }
    }
}
