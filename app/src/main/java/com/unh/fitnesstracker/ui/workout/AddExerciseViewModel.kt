package com.unh.fitnesstracker.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.Exercise
import com.unh.fitnesstracker.data.model.MuscleGroup
import com.unh.fitnesstracker.data.model.TrackedBy
import com.unh.fitnesstracker.data.repository.ExerciseRepository
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class AddExerciseUiState(
    val exercises: List<Exercise> = emptyList(),
    val query: String = "",
    val group: MuscleGroup? = null,
    /** Selected exercise IDs, in the order they were tapped. */
    val selectedIds: List<String> = emptyList(),
)

/** S10 Add Exercise. */
class AddExerciseViewModel(
    private val uid: String,
    private val exerciseRepository: ExerciseRepository,
    private val session: ActiveWorkoutSession,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val group = MutableStateFlow<MuscleGroup?>(null)
    private val selectedIds = MutableStateFlow<List<String>>(emptyList())

    /** Exercises made in this screen, so they can be added even before Firestore echoes them back. */
    private val createdHere = MutableStateFlow<List<Exercise>>(emptyList())

    private val allExercises: StateFlow<List<Exercise>> =
        combine(
            exerciseRepository.observeAll(uid).catch { e -> Log.w(TAG, "Exercise listener failed", e) },
            createdHere,
        ) { all, created ->
            all + created.filter { new -> all.none { it.id == new.id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<AddExerciseUiState> =
        combine(allExercises, query, group, selectedIds) { all, query, group, selected ->
            val visible = all.filter { exercise ->
                (group == null || exercise.muscleGroup == group) &&
                    (query.isBlank() || exercise.name.contains(query.trim(), ignoreCase = true))
            }
            AddExerciseUiState(visible, query, group, selected)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddExerciseUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onGroupSelected(value: MuscleGroup?) {
        group.value = value
    }

    fun toggle(exerciseId: String) = selectedIds.update { ids ->
        if (exerciseId in ids) ids - exerciseId else ids + exerciseId
    }

    fun createCustom(name: String, muscleGroup: MuscleGroup, trackedBy: TrackedBy) {
        val exercise = exerciseRepository.addCustom(uid, name.trim(), muscleGroup, trackedBy)
        createdHere.update { it + exercise }
        selectedIds.update { it + exercise.id }
    }

    /** Adds the selected exercises to the running workout, in the order they were picked. */
    fun addSelected() {
        val byId = allExercises.value.associateBy { it.id }
        session.addExercises(selectedIds.value.mapNotNull { byId[it] })
    }

    private companion object {
        const val TAG = "AddExerciseViewModel"
    }
}
