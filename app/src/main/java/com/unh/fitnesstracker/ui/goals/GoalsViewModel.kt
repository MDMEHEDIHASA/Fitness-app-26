package com.unh.fitnesstracker.ui.goals

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalPeriod
import com.unh.fitnesstracker.data.model.GoalStatus
import com.unh.fitnesstracker.data.model.GoalType
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.data.progress.GoalCompletionTracker
import com.unh.fitnesstracker.data.progress.GoalProgress
import com.unh.fitnesstracker.data.progress.GoalProgressCalculator
import com.unh.fitnesstracker.data.repository.GoalRepository
import com.unh.fitnesstracker.data.repository.ProgressRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class GoalItem(val goal: Goal, val progress: GoalProgress)

data class GoalsUiState(
    val active: List<GoalItem> = emptyList(),
    val completed: List<GoalItem> = emptyList(),
    val units: UnitSystem = UnitSystem.METRIC,
)

data class GoalDraft(
    val type: GoalType = GoalType.WORKOUTS,
    val period: GoalPeriod = GoalPeriod.WEEKLY,
    val targetText: String = "4",
    val remindIfBehind: Boolean = false,
)

class GoalsViewModel(
    private val uid: String,
    userRepository: UserRepository,
    workoutRepository: WorkoutRepository,
    progressRepository: ProgressRepository,
    private val goalRepository: GoalRepository,
    private val completionTracker: GoalCompletionTracker,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.observeGoals(uid).catch { e -> Log.w(TAG, "Goals failed", e); emit(emptyList()) },
        workoutRepository.observeSince(uid, monthStart).catch { e -> Log.w(TAG, "Workouts failed", e); emit(emptyList()) },
        progressRepository.observeBodyWeight(uid).catch { e -> Log.w(TAG, "Weights failed", e); emit(emptyList()) },
        userRepository.observeProfile(uid),
    ) { goals, workouts, weights, profile ->
        val latestWeight = weights.firstOrNull()?.weightKg ?: profile?.weightKg
        val items = goals.map { goal ->
            val progress = GoalProgressCalculator.progress(goal, workouts, latestWeight)
            completionTracker.check(uid, goal, progress)
            GoalItem(goal, progress)
        }
        GoalsUiState(
            active = items.filter { it.goal.status != GoalStatus.COMPLETED },
            completed = items.filter { it.goal.status == GoalStatus.COMPLETED },
            units = profile?.unitSystem ?: UnitSystem.METRIC,
        )
    }
        .catch { emit(GoalsUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun save(draft: GoalDraft, existingId: String = ""): String? {
        val target = parseTarget(draft) ?: return "Enter a valid target"
        val goal = Goal(
            id = existingId,
            type = draft.type,
            target = target,
            period = draft.period,
            remindIfBehind = draft.remindIfBehind,
            status = GoalStatus.ACTIVE,
            createdAt = if (existingId.isEmpty()) System.currentTimeMillis() else
                uiState.value.active.plus(uiState.value.completed).firstOrNull { it.goal.id == existingId }?.goal?.createdAt
                    ?: System.currentTimeMillis(),
        )
        goalRepository.save(uid, goal)
        _message.value = if (existingId.isEmpty()) "Goal saved" else "Goal updated"
        return null
    }

    fun pause(goal: Goal) {
        val next = if (goal.status == GoalStatus.PAUSED) GoalStatus.ACTIVE else GoalStatus.PAUSED
        goalRepository.setStatus(uid, goal.id, next)
    }

    fun delete(goal: Goal) {
        goalRepository.delete(uid, goal.id)
        _message.value = "Goal deleted"
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun parseTarget(draft: GoalDraft): Double? {
        val raw = draft.targetText.replace(',', '.').toDoubleOrNull() ?: return null
        val units = uiState.value.units
        val metric = when (draft.type) {
            GoalType.DISTANCE -> if (units == UnitSystem.IMPERIAL) Units.milesToMeters(raw) else Units.kmToMeters(raw)
            GoalType.BODY_WEIGHT -> if (units == UnitSystem.IMPERIAL) Units.lbToKg(raw) else raw
            else -> raw
        }
        return if (metric > 0) metric else null
    }

    companion object {
        private const val TAG = "GoalsViewModel"

        fun targetHint(type: GoalType, units: UnitSystem): String = when (type) {
            GoalType.WORKOUTS -> "e.g. 4"
            GoalType.ACTIVE_MINUTES -> "minutes"
            GoalType.DISTANCE -> if (units == UnitSystem.IMPERIAL) "miles" else "km"
            GoalType.BODY_WEIGHT -> if (units == UnitSystem.IMPERIAL) "lb" else "kg"
            GoalType.STEPS -> "e.g. 10000"
            GoalType.RESTING_HR -> "bpm"
        }

        fun displayTarget(goal: Goal, units: UnitSystem): String = when (goal.type) {
            GoalType.WORKOUTS -> "${goal.target.toInt()} workouts"
            GoalType.ACTIVE_MINUTES -> "${goal.target.toInt()} min"
            GoalType.STEPS -> "${goal.target.toInt()} steps"
            GoalType.RESTING_HR -> "${goal.target.toInt()} bpm"
            GoalType.DISTANCE -> Units.formatDistance(goal.target, units)
            GoalType.BODY_WEIGHT -> Units.formatWeight(goal.target, units).orEmpty()
        }
    }
}
