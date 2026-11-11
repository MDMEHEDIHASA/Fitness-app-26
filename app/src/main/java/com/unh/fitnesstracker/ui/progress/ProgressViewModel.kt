package com.unh.fitnesstracker.ui.progress

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unh.fitnesstracker.data.model.BodyWeightEntry
import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalStatus
import com.unh.fitnesstracker.data.model.GoalType
import com.unh.fitnesstracker.data.model.PersonalRecord
import com.unh.fitnesstracker.data.model.ProgressMetric
import com.unh.fitnesstracker.data.model.ProgressRange
import com.unh.fitnesstracker.data.model.UnitSystem
import com.unh.fitnesstracker.data.model.Units
import com.unh.fitnesstracker.data.model.Workout
import com.unh.fitnesstracker.data.repository.GoalRepository
import com.unh.fitnesstracker.data.repository.ProgressRepository
import com.unh.fitnesstracker.data.repository.UserRepository
import com.unh.fitnesstracker.data.repository.WorkoutRepository
import com.unh.fitnesstracker.ui.components.ChartPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ProgressUiState(
    val range: ProgressRange = ProgressRange.WEEK,
    val metric: ProgressMetric = ProgressMetric.WORKOUTS,
    val points: List<ChartPoint> = emptyList(),
    val selectedIndex: Int? = null,
    val units: UnitSystem = UnitSystem.METRIC,
    val workouts: Int = 0,
    val activeMinutes: Int = 0,
    val distanceM: Double = 0.0,
    val restingHr: Int? = null,
    val records: List<PersonalRecord> = emptyList(),
    val goalLine: Double? = null,
    val empty: Boolean = true,
) {
    val selected: ChartPoint? get() = selectedIndex?.let { points.getOrNull(it) }
}

class ProgressViewModel(
    uid: String,
    userRepository: UserRepository,
    workoutRepository: WorkoutRepository,
    progressRepository: ProgressRepository,
    goalRepository: GoalRepository,
) : ViewModel() {

    private val range = MutableStateFlow(ProgressRange.WEEK)
    private val metric = MutableStateFlow(ProgressMetric.WORKOUTS)
    private val selected = MutableStateFlow<Int?>(null)
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now()
    private val yearAgo = today.minusYears(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private val workouts = workoutRepository.observeSince(uid, yearAgo)
        .catch { e -> Log.w(TAG, "Workouts failed", e); emit(emptyList()) }
    private val weights = progressRepository.observeBodyWeight(uid)
        .catch { e -> Log.w(TAG, "Weights failed", e); emit(emptyList()) }
    private val records = progressRepository.observeRecords(uid)
        .catch { e -> Log.w(TAG, "Records failed", e); emit(emptyList()) }
    private val goals = goalRepository.observeGoals(uid)
        .catch { e -> Log.w(TAG, "Goals failed", e); emit(emptyList()) }

    val uiState: StateFlow<ProgressUiState> = combine(
        combine(range, metric, selected, userRepository.observeProfile(uid)) { r, m, s, p -> Quad(r, m, s, p?.unitSystem ?: UnitSystem.METRIC) },
        combine(workouts, weights, records, goals) { w, b, rec, g -> Quad(w, b, rec, g) },
    ) { rangeMetric, data ->
        val (currentRange, currentMetric, selectedIndex, units) = rangeMetric
        val (allWorkouts, allWeights, allRecords, allGoals) = data
        val buckets = buckets(currentRange)
        val inRange = allWorkouts.filter { it.startedAt >= buckets.first().start && it.startedAt < buckets.last().end }
        val points = buckets.map { bucket ->
            ChartPoint(bucket.label, valueFor(currentMetric, inRange.filter { it.startedAt >= bucket.start && it.startedAt < bucket.end }, allWeights, bucket))
        }
        val goalLine = goalLine(currentMetric, currentRange, allGoals)
        ProgressUiState(
            range = currentRange,
            metric = currentMetric,
            points = points,
            selectedIndex = selectedIndex,
            units = units,
            workouts = inRange.size,
            activeMinutes = inRange.sumOf { it.durationMinutes },
            distanceM = inRange.sumOf { it.distanceM },
            records = allRecords,
            goalLine = goalLine,
            empty = allWorkouts.isEmpty() && allWeights.isEmpty(),
        )
    }
        .catch { emit(ProgressUiState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun setRange(value: ProgressRange) {
        range.value = value
        selected.value = null
    }

    fun setMetric(value: ProgressMetric) {
        metric.value = value
        selected.value = null
    }

    fun select(index: Int) {
        selected.value = if (selected.value == index) null else index
    }

    private fun buckets(range: ProgressRange): List<Bucket> = when (range) {
        ProgressRange.WEEK -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            (0..6).map { offset ->
                val day = monday.plusDays(offset.toLong())
                Bucket(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), startOf(day), startOf(day.plusDays(1)))
            }
        }
        ProgressRange.MONTH -> {
            val month = YearMonth.from(today)
            (1..month.lengthOfMonth()).map { d ->
                val day = month.atDay(d)
                Bucket(d.toString(), startOf(day), startOf(day.plusDays(1)))
            }
        }
        ProgressRange.YEAR -> {
            (1..12).map { m ->
                val start = LocalDate.of(today.year, m, 1)
                Bucket(start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()), startOf(start), startOf(start.plusMonths(1)))
            }
        }
    }

    private fun valueFor(
        metric: ProgressMetric,
        workouts: List<Workout>,
        weights: List<BodyWeightEntry>,
        bucket: Bucket,
    ): Double = when (metric) {
        ProgressMetric.WORKOUTS -> workouts.size.toDouble()
        ProgressMetric.VOLUME -> workouts.sumOf { it.totalVolumeKg }
        ProgressMetric.ACTIVE_MINUTES -> workouts.sumOf { it.durationMinutes }.toDouble()
        ProgressMetric.DISTANCE -> workouts.sumOf { it.distanceM }
        ProgressMetric.BODY_WEIGHT -> weights.filter { it.recordedAt >= bucket.start && it.recordedAt < bucket.end }
            .maxByOrNull { it.recordedAt }?.weightKg ?: 0.0
        ProgressMetric.STEPS, ProgressMetric.RESTING_HR -> 0.0
    }

    private fun goalLine(metric: ProgressMetric, range: ProgressRange, goals: List<Goal>): Double? {
        val type = when (metric) {
            ProgressMetric.WORKOUTS -> GoalType.WORKOUTS
            ProgressMetric.ACTIVE_MINUTES -> GoalType.ACTIVE_MINUTES
            ProgressMetric.DISTANCE -> GoalType.DISTANCE
            ProgressMetric.BODY_WEIGHT -> GoalType.BODY_WEIGHT
            ProgressMetric.STEPS -> GoalType.STEPS
            ProgressMetric.RESTING_HR -> GoalType.RESTING_HR
            ProgressMetric.VOLUME -> return null
        }
        val period = when (range) {
            ProgressRange.WEEK -> com.unh.fitnesstracker.data.model.GoalPeriod.WEEKLY
            ProgressRange.MONTH -> com.unh.fitnesstracker.data.model.GoalPeriod.MONTHLY
            ProgressRange.YEAR -> return null
        }
        return goals.firstOrNull { it.status == GoalStatus.ACTIVE && it.type == type && it.period == period }?.target
    }

    private fun startOf(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    private data class Bucket(val label: String, val start: Long, val end: Long)
    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private companion object {
        const val TAG = "ProgressViewModel"
    }
}

fun ProgressMetric.formatValue(value: Double, units: UnitSystem): String = when (this) {
    ProgressMetric.WORKOUTS -> value.toInt().toString()
    ProgressMetric.VOLUME -> Units.formatWeight(value, units) ?: "0"
    ProgressMetric.ACTIVE_MINUTES -> "${value.toInt()} min"
    ProgressMetric.DISTANCE -> Units.formatDistance(value, units)
    ProgressMetric.BODY_WEIGHT -> Units.formatWeight(value, units) ?: "--"
    ProgressMetric.STEPS -> if (value == 0.0) "--" else value.toInt().toString()
    ProgressMetric.RESTING_HR -> if (value == 0.0) "--" else "${value.toInt()} bpm"
}
