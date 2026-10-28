package com.unh.fitnesstracker.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.UserProfile
import com.unh.fitnesstracker.data.model.WorkoutTemplate
import com.unh.fitnesstracker.data.repository.TemplateRepository
import com.unh.fitnesstracker.data.session.ActiveWorkout
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

/** Backs the S7 sheet: lists templates and starts (or resumes) the strength session. */
class StartWorkoutViewModel(
    uid: String,
    templateRepository: TemplateRepository,
    private val session: ActiveWorkoutSession,
) : ViewModel() {

    val templates: StateFlow<List<WorkoutTemplate>> = templateRepository.observeTemplates(uid)
        .catch { e ->
            Log.w("StartWorkoutViewModel", "Template listener failed", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeWorkout: StateFlow<ActiveWorkout?> = session.state

    /** Starts a new workout unless one is already running, in which case that one is resumed. */
    fun startStrength(profile: UserProfile?, template: WorkoutTemplate? = null) {
        if (session.isActive) return
        session.start(
            name = DEFAULT_NAME,
            units = profile?.unitSystem ?: UnitSystem.METRIC,
            bodyWeightKg = profile?.weightKg,
            defaultRestSec = profile?.defaultRestSec ?: UserProfile.DEFAULT_REST_SEC,
            template = template,
        )
    }

    private companion object {
        const val DEFAULT_NAME = "Strength workout"
    }
}
