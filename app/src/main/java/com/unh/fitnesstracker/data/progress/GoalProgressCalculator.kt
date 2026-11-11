package com.unh.fitnesstracker.data.progress

import com.unh.fitnesstracker.data.model.Goal
import com.unh.fitnesstracker.data.model.GoalPeriod
import com.unh.fitnesstracker.data.model.GoalStatus
import com.unh.fitnesstracker.data.model.GoalType
import com.unh.fitnesstracker.data.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

data class GoalProgress(
    val current: Double,
    val target: Double,
    val ratio: Float,
    val caption: String,
    val reached: Boolean,
)

/** Figures out how far along a goal is from workouts and the latest weigh-in. */
object GoalProgressCalculator {

    fun progress(
        goal: Goal,
        workouts: List<Workout>,
        latestWeightKg: Double?,
        steps: Int? = null,
        restingHr: Int? = null,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(),
    ): GoalProgress {
        val start = periodStart(goal.period, today, zone)
        val inPeriod = workouts.filter { it.startedAt >= start }
        val current = when (goal.type) {
            GoalType.WORKOUTS -> inPeriod.size.toDouble()
            GoalType.ACTIVE_MINUTES -> inPeriod.sumOf { it.durationMinutes }.toDouble()
            GoalType.DISTANCE -> inPeriod.sumOf { it.distanceM }
            GoalType.BODY_WEIGHT -> latestWeightKg ?: 0.0
            GoalType.STEPS -> (steps ?: 0).toDouble()
            GoalType.RESTING_HR -> (restingHr ?: 0).toDouble()
        }
        val reached = when (goal.type) {
            GoalType.BODY_WEIGHT -> latestWeightKg != null && abs(latestWeightKg - goal.target) <= 0.5
            GoalType.RESTING_HR -> restingHr != null && restingHr > 0 && abs(restingHr - goal.target) <= 2
            else -> current >= goal.target && goal.target > 0
        }
        val ratio = when {
            goal.target <= 0 -> 0f
            goal.type == GoalType.BODY_WEIGHT || goal.type == GoalType.RESTING_HR ->
                if (current <= 0) 0f else (1.0 - abs(current - goal.target) / goal.target).toFloat().coerceIn(0f, 1f)
            else -> (current / goal.target).toFloat().coerceIn(0f, 1f)
        }
        return GoalProgress(current, goal.target, ratio, caption(goal, current), reached)
    }

    fun periodStart(period: GoalPeriod, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = when (period) {
            GoalPeriod.DAILY -> today
            GoalPeriod.WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            GoalPeriod.MONTHLY -> today.withDayOfMonth(1)
        }
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun resetLabel(period: GoalPeriod): String = when (period) {
        GoalPeriod.DAILY -> "resets tomorrow"
        GoalPeriod.WEEKLY -> "resets Monday"
        GoalPeriod.MONTHLY -> "resets next month"
    }

    private fun caption(goal: Goal, current: Double): String {
        val reset = resetLabel(goal.period)
        val status = if (goal.status == GoalStatus.COMPLETED) "completed" else reset
        return when (goal.type) {
            GoalType.WORKOUTS -> "${current.toInt()} of ${goal.target.toInt()} · $status"
            GoalType.ACTIVE_MINUTES -> "${current.toInt()} of ${goal.target.toInt()} min · $status"
            GoalType.STEPS -> "${current.toInt()} of ${goal.target.toInt()} · $status"
            GoalType.RESTING_HR -> "${if (current > 0) current.toInt().toString() else "--"} / ${goal.target.toInt()} bpm · $status"
            GoalType.DISTANCE, GoalType.BODY_WEIGHT -> status
        }
    }
}
