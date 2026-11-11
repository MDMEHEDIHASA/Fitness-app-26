package com.unh.fitnesstracker.data.model

enum class GoalType(val label: String) {
    WORKOUTS("Workouts"),
    ACTIVE_MINUTES("Active minutes"),
    DISTANCE("Distance"),
    BODY_WEIGHT("Body weight"),
    STEPS("Steps"),
    RESTING_HR("Resting heart rate"),
}

enum class GoalPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
}

enum class GoalStatus { ACTIVE, PAUSED, COMPLETED }

/** One goal at users/{uid}/goals/{id}. Target is always metric (kg, meters, minutes, count). */
data class Goal(
    val id: String = "",
    val type: GoalType = GoalType.WORKOUTS,
    val target: Double = 0.0,
    val period: GoalPeriod = GoalPeriod.WEEKLY,
    val remindIfBehind: Boolean = false,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Long = 0,
    val completedAt: Long? = null,
)
