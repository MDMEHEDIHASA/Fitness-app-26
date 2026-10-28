package com.unh.fitnesstracker.ui.workout

import androidx.lifecycle.ViewModel
import com.unh.fitnesstracker.data.repository.TemplateRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.data.session.ActiveWorkout
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** S9 Active workout, S11 rest timer and S12 finish dialog. The workout itself lives in [ActiveWorkoutSession]. */
class ActiveWorkoutViewModel(
    private val uid: String,
    val session: ActiveWorkoutSession,
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    val workout: StateFlow<ActiveWorkout?> = session.state

    private val _message = MutableStateFlow<String?>(null)
    /** One-time snackbar text. */
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Saves the workout and ends the session. Returns false when there is nothing to save. */
    fun finish(rating: Int?, notes: String): Boolean {
        val workout = session.buildWorkout(rating, notes) ?: return false
        if (workout.totalSets == 0) return false
        session.complete(workoutRepository.save(uid, workout))
        return true
    }

    fun saveAsTemplate(name: String) {
        val template = session.buildTemplate(name.trim())
        if (template == null || template.exercises.isEmpty()) {
            _message.value = "Add an exercise before saving a template"
            return
        }
        templateRepository.save(uid, template)
        _message.value = "Template \"${template.name}\" saved"
    }

    fun onMessageShown() {
        _message.value = null
    }
}
