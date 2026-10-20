package com.unh.fitnesstracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Everything S5 shows. A null metric means "no data source yet" and is drawn as "--".
 * Steps and heart rate come from sensors in Sprint 9; workouts and active minutes from Sprint 3.
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
) {
    companion object {
        const val DEFAULT_STEP_GOAL = 10_000
    }
}

/** A planned workout for today, from reminders (Sprint 7). */
data class TodaysPlan(val name: String, val timeText: String)

class HomeViewModel(uid: String, userRepository: UserRepository) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    val uiState: StateFlow<HomeUiState> = userRepository.observeProfile(uid)
        .map { profile ->
            HomeUiState(
                firstName = profile?.firstName.orEmpty(),
                dateText = LocalDate.now().format(dateFormatter),
            )
        }
        .catch { emit(HomeUiState(dateText = LocalDate.now().format(dateFormatter))) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(dateText = LocalDate.now().format(dateFormatter)),
        )
}
