package com.unh.fitnesstracker.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.data.session.ActiveWorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Everything S5 shows. A null metric means "no data source yet" and is drawn as "--".
 * Steps and heart rate come from sensors in Sprint 9.
 */
data class HomeUiState(
    val firstName: String = "",
    val dateText: String = "",
    val watchConnected: Boolean = false,
    val steps: Int? = null,
    val stepGoal: Int = DEFAULT_STEP_GOAL,
    val heartRateBpm: Int? = null,
    val activeMinutes: Int = 0,
    val calories: Int = 0,
    val todaysPlan: TodaysPlan? = null,
    val workoutsThisWeek: Int = 0,
    val weeklyWorkoutGoal: Int? = null,
    val workoutInProgress: WorkoutInProgress? = null,
) {
    companion object {
        const val DEFAULT_STEP_GOAL = 10_000
    }
}

/** A planned workout for today, from reminders (Sprint 7). */
data class TodaysPlan(val name: String, val timeText: String)

/** The running strength session, shown as a Resume card. */
data class WorkoutInProgress(val name: String, val elapsedSec: Long)

class HomeViewModel(
    uid: String,
    userRepository: UserRepository,
    workoutRepository: WorkoutRepository,
    session: ActiveWorkoutSession,
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now()
    private val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    private val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone).toInstant().toEpochMilli()

    private val thisWeek = workoutRepository.observeSince(uid, weekStart)
        .onStart { emit(emptyList()) }
        .catch { e ->
            Log.w(TAG, "Workout listener failed", e)
            emit(emptyList())
        }

    val uiState: StateFlow<HomeUiState> =
        combine(userRepository.observeProfile(uid), thisWeek, session.state) { profile, workouts, active ->
            val todays = workouts.filter { it.startedAt >= todayStart }
            HomeUiState(
                firstName = profile?.firstName.orEmpty(),
                dateText = today.format(dateFormatter),
                activeMinutes = todays.sumOf(Workout::durationMinutes),
                calories = todays.sumOf(Workout::calories),
                workoutsThisWeek = workouts.size,
                workoutInProgress = active?.let { WorkoutInProgress(it.name, it.elapsedSec) },
            )
        }
            .catch { emit(HomeUiState(dateText = today.format(dateFormatter))) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState(dateText = today.format(dateFormatter)),
            )

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
