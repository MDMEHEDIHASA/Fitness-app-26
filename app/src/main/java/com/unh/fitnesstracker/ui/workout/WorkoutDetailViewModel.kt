package com.unh.fitnesstracker.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.SetEntry
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.data.model.UserProfile
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutTemplate
import com.unh.fitnesstracker.data.repository.TemplateRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val missing: Boolean = false,
)

data class EditableSet(
    val weight: String,
    val reps: String,
)

/** S18 Workout detail: live document, edit sets/notes, repeat, template, delete. */
class WorkoutDetailViewModel(
    private val uid: String,
    workoutId: String,
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val session: ActiveWorkoutSession,
    userRepository: UserRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var lastDeleted: Workout? = null
    private var profile: UserProfile? = null

    val uiState: StateFlow<WorkoutDetailUiState> = combine(
        workoutRepository.observe(uid, workoutId).catch { e ->
            Log.w(TAG, "Detail listener failed", e)
            emit(null)
        },
        userRepository.observeProfile(uid),
    ) { workout, currentProfile ->
        profile = currentProfile
        WorkoutDetailUiState(
            workout = workout,
            units = currentProfile?.unitSystem ?: UnitSystem.METRIC,
            missing = workout == null,
        )
    }
        .catch { emit(WorkoutDetailUiState(missing = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())

    fun saveEdits(notes: String, setsByExercise: List<List<EditableSet>>): Boolean {
        val workout = uiState.value.workout ?: return false
        val units = uiState.value.units
        if (setsByExercise.size != workout.exercises.size) return false
        val exercises = workout.exercises.mapIndexed { exerciseIndex, entry ->
            val edited = setsByExercise[exerciseIndex].map { draft ->
                draft.toSetEntry(entry.trackedBy, units)
            }
            entry.copy(sets = edited)
        }
        val saved = workout.copy(notes = notes.trim(), exercises = exercises).withRecalculatedTotals()
        workoutRepository.save(uid, saved)
        _message.value = "Workout updated"
        return true
    }

    fun saveAsTemplate(name: String) {
        val workout = uiState.value.workout ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty() || workout.exercises.isEmpty()) {
            _message.value = "Add an exercise before saving a template"
            return
        }
        templateRepository.save(uid, WorkoutTemplate(
            name = trimmed,
            createdAt = System.currentTimeMillis(),
            exercises = workout.toRepeatTemplate().exercises,
        ))
        _message.value = "Template \"$trimmed\" saved"
    }

    /** Returns false when a workout is already running. */
    fun repeat(): Boolean {
        val workout = uiState.value.workout ?: return false
        if (session.isActive) {
            _message.value = "Finish or discard the workout in progress first"
            return false
        }
        val current = profile
        session.startFromWorkout(
            workout = workout,
            units = current?.unitSystem ?: UnitSystem.METRIC,
            bodyWeightKg = current?.weightKg,
            defaultRestSec = current?.defaultRestSec ?: UserProfile.DEFAULT_REST_SEC,
        )
        return true
    }

    fun delete() {
        val workout = uiState.value.workout ?: return
        lastDeleted = workout
        workoutRepository.delete(uid, workout.id)
    }

    fun undoDelete(): Boolean {
        val workout = lastDeleted ?: return false
        lastDeleted = null
        workoutRepository.save(uid, workout)
        return true
    }

    fun onMessageShown() {
        _message.value = null
    }

    private companion object {
        const val TAG = "WorkoutDetailViewModel"
    }
}

internal fun EditableSet.toSetEntry(trackedBy: TrackedBy, units: UnitSystem): SetEntry {
    val typed = weight.replace(',', '.').toDoubleOrNull() ?: 0.0
    val kg = if (units == UnitSystem.IMPERIAL) Units.lbToKg(typed) else typed
    val number = reps.toIntOrNull() ?: 0
    return SetEntry(
        weightKg = (kg * 100).roundToInt() / 100.0,
        reps = if (trackedBy == TrackedBy.REPS) number else 0,
        durationSec = if (trackedBy == TrackedBy.TIME) number else 0,
        completed = true,
    )
}

internal fun SetEntry.toEditable(trackedBy: TrackedBy, units: UnitSystem): EditableSet {
    val display = if (units == UnitSystem.IMPERIAL) Units.kgToLb(weightKg) else weightKg
    val weightText = ActiveWorkoutSession.formatDecimal(display)
    val repsText = when (trackedBy) {
        TrackedBy.TIME -> if (durationSec > 0) durationSec.toString() else ""
        TrackedBy.REPS -> if (reps > 0) reps.toString() else ""
    }
    return EditableSet(weightText, repsText)
}
