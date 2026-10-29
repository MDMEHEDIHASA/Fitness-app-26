package com.unh.fitnesstracker.ui.workout

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.model.WorkoutType
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class HistoryFilter(val label: String, val type: WorkoutType?) {
    ALL("All", null),
    STRENGTH("Strength", WorkoutType.STRENGTH),
    RUN("Run", WorkoutType.RUN),
    WALK("Walk", WorkoutType.WALK),
    CYCLE("Cycle", WorkoutType.CYCLE),
}

data class HistorySection(val title: String, val workouts: List<Workout>)

data class HistoryUiState(
    val sections: List<HistorySection> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val query: String = "",
    val units: UnitSystem = UnitSystem.METRIC,
    val canLoadMore: Boolean = false,
    val emptyHistory: Boolean = true,
    val noMatches: Boolean = false,
)

/** S17 Workout history: live paged list, type chips and in-memory search. */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutsViewModel(
    private val uid: String,
    userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val query = MutableStateFlow("")
    private val limit = MutableStateFlow(WorkoutRepository.PAGE_SIZE)
    private var lastDeleted: Workout? = null

    private val history = combine(filter, limit) { currentFilter, currentLimit ->
        currentFilter.type to currentLimit
    }.flatMapLatest { (type, currentLimit) ->
        workoutRepository.observeHistory(uid, type, currentLimit)
            .catch { e ->
                Log.w(TAG, "History listener failed", e)
                emit(emptyList())
            }
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        history,
        filter,
        query,
        limit,
        userRepository.observeProfile(uid),
    ) { workouts, currentFilter, currentQuery, currentLimit, profile ->
        val matches = workouts.filter { it.matches(currentQuery) }
        HistoryUiState(
            sections = matches.groupByMonth(),
            filter = currentFilter,
            query = currentQuery,
            units = profile?.unitSystem ?: UnitSystem.METRIC,
            canLoadMore = workouts.size >= currentLimit.toInt(),
            emptyHistory = workouts.isEmpty() && currentQuery.isBlank() && currentFilter == HistoryFilter.ALL,
            noMatches = workouts.isNotEmpty() && matches.isEmpty() ||
                (workouts.isEmpty() && (currentQuery.isNotBlank() || currentFilter != HistoryFilter.ALL)),
        )
    }
        .catch { emit(HistoryUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(value: HistoryFilter) {
        if (filter.value == value) return
        filter.value = value
        limit.value = WorkoutRepository.PAGE_SIZE
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun loadMore() {
        if (!uiState.value.canLoadMore) return
        limit.update { it + WorkoutRepository.PAGE_SIZE }
    }

    /** Deletes now. Call [undoDelete] if the user taps Undo within 5 seconds. */
    fun delete(workout: Workout) {
        lastDeleted = workout
        workoutRepository.delete(uid, workout.id)
    }

    fun undoDelete(): Boolean {
        val workout = lastDeleted ?: return false
        lastDeleted = null
        workoutRepository.save(uid, workout)
        return true
    }

    private companion object {
        const val TAG = "WorkoutsViewModel"
    }
}

private fun Workout.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return name.contains(q, ignoreCase = true) ||
        notes.contains(q, ignoreCase = true) ||
        exercises.any { it.name.contains(q, ignoreCase = true) }
}

private fun List<Workout>.groupByMonth(): List<HistorySection> =
    groupBy { WorkoutFormat.monthHeader(it.startedAt) }
        .map { (title, items) -> HistorySection(title, items) }
