package com.unh.fitnesstracker.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.PersonalRecord
import com.unh.fitnesstracker.data.progress.PersonalRecordDetector
import com.unh.fitnesstracker.data.repository.ProgressRepository
import com.unh.fitnesstracker.data.repository.TemplateRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.data.session.ActiveWorkout
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** S9 Active workout, S11 rest timer and S12 finish dialog. The workout itself lives in [ActiveWorkoutSession]. */
class ActiveWorkoutViewModel(
    private val uid: String,
    val session: ActiveWorkoutSession,
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    val workout: StateFlow<ActiveWorkout?> = session.state

    private val _message = MutableStateFlow<String?>(null)
    /** One-time snackbar text. */
    val message: StateFlow<String?> = _message.asStateFlow()

    private var records: List<PersonalRecord> = emptyList()

    init {
        viewModelScope.launch {
            progressRepository.observeRecords(uid).catch { emit(emptyList()) }.collect { records = it }
        }
    }

    /** Saves the workout and ends the session. Returns false when there is nothing to save. */
    fun finish(rating: Int?, notes: String): Boolean {
        val workout = session.buildWorkout(rating, notes) ?: return false
        if (workout.totalSets == 0) return false
        val saved = workoutRepository.save(uid, workout)
        val broken = PersonalRecordDetector.detect(saved, records)
        broken.forEach { progressRepository.saveRecord(uid, it) }
        session.complete(saved, broken)
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
